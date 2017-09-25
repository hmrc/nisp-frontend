/*
 * Copyright 2017 HM Revenue & Customs
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

import org.joda.time.{LocalDate, Period}
import play.api.mvc.{Action, AnyContent, Request, Session}
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.config.wiring.NispSessionCache
import uk.gov.hmrc.nisp.controllers.auth.{AuthorisedForNisp, NispUser}
import uk.gov.hmrc.nisp.controllers.connectors.{AuthenticationConnectors, CustomAuditConnector}
import uk.gov.hmrc.nisp.controllers.partial.PartialRetriever
import uk.gov.hmrc.nisp.controllers.pertax.PertaxHelper
import uk.gov.hmrc.nisp.events.{AccountAccessEvent, AccountExclusionEvent}
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.models.enums.{Exclusion, MQPScenario, Scenario}
import uk.gov.hmrc.nisp.services._
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.nisp.utils.Constants._
import uk.gov.hmrc.nisp.views.html._
import uk.gov.hmrc.play.frontend.controller.UnauthorisedAction
import uk.gov.hmrc.play.http.HeaderCarrier

object StatePensionController extends StatePensionController {

  override val sessionCache: SessionCache = NispSessionCache
  override val customAuditConnector = CustomAuditConnector
  override val applicationConfig: ApplicationConfig = ApplicationConfig
  override val citizenDetailsService: CitizenDetailsService = CitizenDetailsService
  override val metricsService: MetricsService = MetricsService
  override val statePensionService: StatePensionService =
    if (applicationConfig.useStatePensionAPI) StatePensionService else NispStatePensionService
  override val nationalInsuranceService: NationalInsuranceService =
    if (applicationConfig.useNationalInsuranceAPI) NationalInsuranceService else NispNationalInsuranceService
}

trait StatePensionController extends NispFrontendController with AuthorisedForNisp with PertaxHelper with AuthenticationConnectors with PartialRetriever {

  import play.api.Play.current
  import play.api.i18n.Messages.Implicits._

  def statePensionService: StatePensionService

  def nationalInsuranceService: NationalInsuranceService

  val customAuditConnector: CustomAuditConnector
  val applicationConfig: ApplicationConfig

  def showCope: Action[AnyContent] = AuthorisedByAny.async { implicit user =>
    implicit request =>
      isFromPertax.flatMap { isPertax =>

        statePensionService.getSummary(user.nino) map {
          case Right(statePension) if statePension.contractedOut => {
            Ok(statepension_cope(
              statePension.amounts.cope.weeklyAmount,
              isPertax
            ))
          }
          case _ => Redirect(routes.StatePensionController.show())
        }
      }
  }

  private def sendAuditEvent(statePension: StatePension, user: NispUser)(implicit hc: HeaderCarrier) = {
    customAuditConnector.sendEvent(AccountAccessEvent(
      user.nino.nino,
      statePension.pensionDate,
      statePension.amounts.current.weeklyAmount,
      statePension.amounts.forecast.weeklyAmount,
      user.dateOfBirth,
      user.name,
      user.sex,
      statePension.contractedOut,
      statePension.forecastScenario,
      statePension.amounts.cope.weeklyAmount,
      user.authProviderOld
    ))
  }

  def show: Action[AnyContent] = AuthorisedByAny.async { implicit user =>
    implicit request =>
      isFromPertax.flatMap { isPertax =>

        val statePensionResponseF = statePensionService.getSummary(user.nino)
        val nationalInsuranceResponseF = nationalInsuranceService.getSummary(user.nino)

        (for (
          statePensionResponse <- statePensionResponseF;
          nationalInsuranceResponse <- nationalInsuranceResponseF
        ) yield {
          (statePensionResponse, nationalInsuranceResponse) match {
/*            case (Right(statePension), Left(nationalInsuranceExclusion)) if statePension.reducedRateElection =>
              customAuditConnector.sendEvent(AccountExclusionEvent(
                user.nino.nino,
                user.name,
                nationalInsuranceExclusion
              ))
              Redirect(routes.ExclusionController.showSP()).withSession(storeUserInfoInSession(user, contractedOut = false))*/

            case (Right(statePension), Right(nationalInsuranceRecord)) =>

              sendAuditEvent(statePension, user)

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
                  user.dateOfBirth.map(calculateAge(_, now().toLocalDate)),
                  isPertax,
                  yearsToContributeUntilPensionAge
                )).withSession(storeUserInfoInSession(user, statePension.contractedOut))
              } else if (statePension.forecastScenario.equals(Scenario.ForecastOnly)) {

                Ok(statepension_forecastonly(
                  statePension,
                  nationalInsuranceRecord.numberOfGaps,
                  nationalInsuranceRecord.numberOfGapsPayable,
                  user.dateOfBirth.map(calculateAge(_, now().toLocalDate)),
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
                  user.dateOfBirth.map(calculateAge(_, now().toLocalDate)),
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

  def pta(): Action[AnyContent] = AuthorisedByAny { implicit user =>
    implicit request =>
      setFromPertax
      Redirect(routes.StatePensionController.show())
  }

  def calculateChartWidths(current: StatePensionAmount, forecast: StatePensionAmount, personalMaximum: StatePensionAmount): (SPChartModel, SPChartModel, SPChartModel) = {
    // scalastyle:off magic.number
    if (personalMaximum.weeklyAmount > forecast.weeklyAmount) {
      val currentChart = SPChartModel((current.weeklyAmount / personalMaximum.weeklyAmount * 100).toInt.max(Constants.chartWidthMinimum), current)
      val forecastChart = SPChartModel((forecast.weeklyAmount / personalMaximum.weeklyAmount * 100).toInt.max(Constants.chartWidthMinimum), forecast)
      val personalMaxChart = SPChartModel(100, personalMaximum)
      (currentChart, forecastChart, personalMaxChart)
    } else {
      if (forecast.weeklyAmount > current.weeklyAmount) {
        val currentPercentage = (current.weeklyAmount / forecast.weeklyAmount * 100).toInt
        val currentChart = SPChartModel(currentPercentage.max(Constants.chartWidthMinimum), current)
        val forecastChart = SPChartModel(100, forecast)
        (currentChart, forecastChart, forecastChart)
      } else {
        val currentChart = SPChartModel(100, current)
        val forecastChart = SPChartModel((forecast.weeklyAmount / current.weeklyAmount * 100).toInt, forecast)
        (currentChart, forecastChart, forecastChart)
      }
    }
  }

  private def storeUserInfoInSession(user: NispUser, contractedOut: Boolean)(implicit request: Request[AnyContent]): Session = {
    request.session +
      (NAME -> user.name.getOrElse("N/A")) +
      (NINO -> user.nino.nino) +
      (CONTRACTEDOUT -> contractedOut.toString)
  }

  private[controllers] def calculateAge(dateOfBirth: LocalDate, currentDate: LocalDate): Int = {
    new Period(dateOfBirth, currentDate).getYears
  }

  def signOut: Action[AnyContent] = UnauthorisedAction { implicit request =>
    if (applicationConfig.showGovUkDonePage) {
      Redirect(applicationConfig.govUkFinishedPageUrl).withNewSession
    } else {
      val name = request.session.get(NAME).getOrElse("")
      val nino = request.session.get(NINO).getOrElse("")
      val contractedOut = request.session.get(CONTRACTEDOUT).getOrElse("")

      Redirect(routes.QuestionnaireController.show()).withNewSession.withSession(
        NAME -> name,
        NINO -> nino,
        CONTRACTEDOUT -> contractedOut)
    }
  }

  def timeout = UnauthorisedAction { implicit request =>
    Ok(sessionTimeout())
  }
}
