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

import com.google.inject.Inject
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.auth.{AuthAction, AuthDetails, NispAuthedUser}
import uk.gov.hmrc.nisp.controllers.pertax.PertaxHelper
import uk.gov.hmrc.nisp.events.{AccountAccessEvent, AccountExclusionEvent}
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.models.enums.{MQPScenario, Scenario}
import uk.gov.hmrc.nisp.services._
import uk.gov.hmrc.nisp.utils.Calculate._
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.nisp.utils.Constants._
import uk.gov.hmrc.nisp.views.html._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.time.DateTimeUtils

import scala.concurrent.ExecutionContext

class StatePensionController @Inject()(authenticate: AuthAction,
                                       statePensionService: StatePensionService,
                                       nationalInsuranceService: NationalInsuranceService,
                                       auditConnector: AuditConnector,
                                       applicationConfig: ApplicationConfig,
                                       pertaxHelper: PertaxHelper,
                                       mcc: MessagesControllerComponents)
                                      (implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever,
                                       val formPartialRetriever: FormPartialRetriever,
                                       val templateRenderer: TemplateRenderer,
                                       executor: ExecutionContext) extends NispFrontendController(mcc) with I18nSupport {

  def showCope: Action[AnyContent] = authenticate.async {
    implicit request =>
      implicit val user: NispAuthedUser = request.nispAuthedUser
      pertaxHelper.isFromPertax.flatMap { isPertax =>

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
    auditConnector.sendEvent(AccountAccessEvent(
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
      pertaxHelper.isFromPertax.flatMap { isPertax =>

        val statePensionResponseF = statePensionService.getSummary(user.nino)
        val nationalInsuranceResponseF = nationalInsuranceService.getSummary(user.nino)

        for {
          statePensionResponse <- statePensionResponseF
          nationalInsuranceResponse <- nationalInsuranceResponseF
        } yield {
          (statePensionResponse, nationalInsuranceResponse) match {
            case (Right(statePension), Left(nationalInsuranceExclusion)) if statePension.reducedRateElection =>
              auditConnector.sendEvent(AccountExclusionEvent(
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
              auditConnector.sendEvent(AccountExclusionEvent(
                user.nino.nino,
                user.name,
                statePensionExclusion.exclusion
              ))
              Redirect(routes.ExclusionController.showSP()).withSession(storeUserInfoInSession(user, contractedOut = false))
            case _ => throw new RuntimeException("StatePensionController: SP and NIR are unmatchable. This is probably a logic error.")
          }
        }
      }
  }

  def pta(): Action[AnyContent] = authenticate {
    implicit request =>
      pertaxHelper.setFromPertax
      Redirect(routes.StatePensionController.show())
  }

  private def storeUserInfoInSession(user: NispAuthedUser, contractedOut: Boolean)(implicit request: Request[AnyContent]): Session = {
    request.session +
      (NAME -> user.name.toString()) +
      (NINO -> user.nino.nino) +
      (CONTRACTEDOUT -> contractedOut.toString)
  }

  def signOut: Action[AnyContent] = Action { implicit request =>
    Redirect(applicationConfig.feedbackFrontendUrl).withNewSession
  }

  def timeout = Action { implicit request =>
    Ok(sessionTimeout())
  }
}
