/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.nisp.controllers

import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Session}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.config.wiring.{CustomAuditConnector, MetricsService, NationalInsuranceService, NispSessionCache, StatePensionService}
import uk.gov.hmrc.nisp.controllers.auth.{AuthAction, AuthActionSelector, AuthDetails, NispAuthedUser}
import uk.gov.hmrc.nisp.controllers.connectors.CustomAuditConnector
import uk.gov.hmrc.nisp.controllers.partial.PartialRetriever
import uk.gov.hmrc.nisp.controllers.pertax.PertaxHelper
import uk.gov.hmrc.nisp.events.{AccountAccessEvent, AccountExclusionEvent}
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.models.enums.{MQPScenario, Scenario}
import uk.gov.hmrc.nisp.services._
import uk.gov.hmrc.nisp.utils.Calculate._
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.nisp.utils.Constants._
import uk.gov.hmrc.nisp.views.html._
import uk.gov.hmrc.play.frontend.controller.UnauthorisedAction
import uk.gov.hmrc.time.DateTimeUtils

object StatePensionController extends StatePensionController {

  override val sessionCache: SessionCache = NispSessionCache
  override val customAuditConnector: CustomAuditConnector = CustomAuditConnector
  override val applicationConfig: ApplicationConfig = ApplicationConfig
  override val metricsService: MetricsService = MetricsService
  override val statePensionService: StatePensionService = StatePensionService
  override val nationalInsuranceService: NationalInsuranceService = NationalInsuranceService
  override val authenticate: AuthAction = AuthActionSelector.decide(applicationConfig)
}

trait StatePensionController extends NispFrontendController with PertaxHelper with PartialRetriever {

  def authenticate: AuthAction

  def statePensionService: StatePensionService

  def nationalInsuranceService: NationalInsuranceService

  val customAuditConnector: CustomAuditConnector

  val applicationConfig: ApplicationConfig

  def showCope: Action[AnyContent] = authenticate.async {
    implicit request =>
      implicit val user: NispAuthedUser = request.nispAuthedUser
      isFromPertax.flatMap { isPertax =>

        statePensionService.getSummary(user.nino) map {
          case Right(statePension) if statePension.contractedOut =>
            Ok(statepension_cope(
              statePension.amounts.cope.weeklyAmount,
              isPertax
            ))
          case _ => Redirect(routes.StatePensionController.show())
        }
      }
  }

  private def sendAuditEvent(statePension: StatePension, user: NispAuthedUser, authDetails: AuthDetails)(implicit hc: HeaderCarrier): Unit = {
    customAuditConnector.sendEvent(AccountAccessEvent(
      user.nino.nino,
      statePension.pensionDate,
      statePension.amounts.current.weeklyAmount,
      statePension.amounts.forecast.weeklyAmount,
      user.dateOfBirth,
      user.name.toString,
      statePension.contractedOut,
      statePension.forecastScenario,
      statePension.amounts.cope.weeklyAmount,
      authDetails.authProvider.getOrElse("N/A")
    ))
  }

  def show: Action[AnyContent] = authenticate.async {
    implicit request =>
      implicit val user: NispAuthedUser = request.nispAuthedUser
      implicit val authDetails: AuthDetails = request.authDetails
      isFromPertax.flatMap { isPertax =>

        val statePensionResponseF = statePensionService.getSummary(user.nino)
        val nationalInsuranceResponseF = nationalInsuranceService.getSummary(user.nino)

        (for (
          statePensionResponse <- statePensionResponseF;
          nationalInsuranceResponse <- nationalInsuranceResponseF
        ) yield {
          (statePensionResponse, nationalInsuranceResponse) match {
            case (Right(statePension), Left(nationalInsuranceExclusion)) if statePension.reducedRateElection =>
              customAuditConnector.sendEvent(AccountExclusionEvent(
                user.nino.nino,
                user.name,
                nationalInsuranceExclusion
              ))
              Redirect(routes.ExclusionController.showSP()).withSession(storeUserInfoInSession(user, contractedOut = false))

            case (Right(statePension), Right(nationalInsuranceRecord)) =>

              sendAuditEvent(statePension, user, request.authDetails)

              val yearsToContributeUntilPensionAge = statePensionService.yearsToContributeUntilPensionAge(
                statePension.earningsIncludedUpTo,
                statePension.finalRelevantStartYear
              )

              if (statePension.mqpScenario.fold(false)(_ != MQPScenario.ContinueWorking)) {
                val yearsMissing = Constants.minimumQualifyingYearsNSP - statePension.numberOfQualifyingYears
                Ok(statepension_mqp(
                  statePension,
                  nationalInsuranceRecord.numberOfGaps,
                  nationalInsuranceRecord.numberOfGapsPayable,
                  yearsMissing,
                  user.livesAbroad,
                  calculateAge(user.dateOfBirth, DateTimeUtils.now.toLocalDate),
                  isPertax,
                  yearsToContributeUntilPensionAge
                )).withSession(storeUserInfoInSession(user, statePension.contractedOut))
              } else if (statePension.forecastScenario.equals(Scenario.ForecastOnly)) {

                Ok(statepension_forecastonly(
                  statePension,
                  nationalInsuranceRecord.numberOfGaps,
                  nationalInsuranceRecord.numberOfGapsPayable,
                  calculateAge(user.dateOfBirth, DateTimeUtils.now.toLocalDate),
                  user.livesAbroad,
                  isPertax,
                  yearsToContributeUntilPensionAge
                )).withSession(storeUserInfoInSession(user, statePension.contractedOut))

              } else {
                val (currentChart, forecastChart, personalMaximumChart) =
                  calculateChartWidths(
                    statePension.amounts.current,
                    statePension.amounts.forecast,
                    statePension.amounts.maximum
                  )
                Ok(statepension(
                  statePension,
                  nationalInsuranceRecord.numberOfGaps,
                  nationalInsuranceRecord.numberOfGapsPayable,
                  currentChart,
                  forecastChart,
                  personalMaximumChart,
                  isPertax,
                  hidePersonalMaxYears = applicationConfig.futureProofPersonalMax,
                  calculateAge(user.dateOfBirth, DateTimeUtils.now.toLocalDate),
                  user.livesAbroad,
                  yearsToContributeUntilPensionAge
                )).withSession(storeUserInfoInSession(user, statePension.contractedOut))
              }

            case (Left(statePensionExclusion), _) =>
              customAuditConnector.sendEvent(AccountExclusionEvent(
                user.nino.nino,
                user.name,
                statePensionExclusion.exclusion
              ))
              Redirect(routes.ExclusionController.showSP()).withSession(storeUserInfoInSession(user, contractedOut = false))
            case _ => throw new RuntimeException("StatePensionController: SP and NIR are unmatchable. This is probably a logic error.")
          }
        }).recover {
          case ex: Exception => onError(ex)
        }
      }
  }

  def pta(): Action[AnyContent] = authenticate {
    implicit request =>
      setFromPertax
      Redirect(routes.StatePensionController.show())
  }

  private def storeUserInfoInSession(user: NispAuthedUser, contractedOut: Boolean)(implicit request: Request[AnyContent]): Session = {
    request.session +
      (NAME -> user.name.toString()) +
      (NINO -> user.nino.nino) +
      (CONTRACTEDOUT -> contractedOut.toString)
  }

  def signOut: Action[AnyContent] = UnauthorisedAction { implicit request =>
    Redirect(applicationConfig.feedbackFrontendUrl).withNewSession
  }

  def timeout = UnauthorisedAction { implicit request =>
    Ok(sessionTimeout())
  }
}
