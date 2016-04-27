/*
 * Copyright 2016 HM Revenue & Customs
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

import play.api.mvc.{Action, AnyContent, Request, Session}
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.config.wiring.NispSessionCache
import uk.gov.hmrc.nisp.connectors.NispConnector
import uk.gov.hmrc.nisp.controllers.auth.{AuthorisedForNisp, NispUser}
import uk.gov.hmrc.nisp.controllers.connectors.{AuthenticationConnectors, CustomAuditConnector}
import uk.gov.hmrc.nisp.controllers.partial.PartialRetriever
import uk.gov.hmrc.nisp.controllers.pertax.PertaxHelper
import uk.gov.hmrc.nisp.events.{AccountAccessEvent, AccountExclusionEvent}
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.models.enums.{ABTest, Scenario}
import uk.gov.hmrc.nisp.services.{CitizenDetailsService, MetricsService, NpsAvailabilityChecker}
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.nisp.utils.Constants._
import uk.gov.hmrc.nisp.views.html._
import uk.gov.hmrc.play.frontend.controller.UnauthorisedAction

object AccountController extends AccountController with AuthenticationConnectors with PartialRetriever {
  override val nispConnector: NispConnector = NispConnector
  override val metricsService: MetricsService = MetricsService
  override val sessionCache: SessionCache = NispSessionCache

  override val customAuditConnector = CustomAuditConnector
  override val npsAvailabilityChecker: NpsAvailabilityChecker = NpsAvailabilityChecker
  override val applicationConfig: ApplicationConfig = ApplicationConfig
  override val citizenDetailsService: CitizenDetailsService = CitizenDetailsService
}

trait AccountController extends NispFrontendController with AuthorisedForNisp with PertaxHelper {
  def nispConnector: NispConnector
  def metricsService: MetricsService

  val customAuditConnector: CustomAuditConnector
  val applicationConfig: ApplicationConfig

  def showCope: Action[AnyContent] = AuthorisedByAny.async { implicit user => implicit request =>
    isFromPertax.flatMap { isPertax =>
      val nino = user.nino.getOrElse("")
      val authenticationProvider = getAuthenticationProvider(user.authContext.user.confidenceLevel)

      nispConnector.connectToGetSPResponse(nino).map {
        case SPResponseModel(Some(spSummary: SPSummaryModel), None, None) => spSummary.contractedOutFlag
          match {
            case true => Ok(account_cope(nino, spSummary.forecast.forecastAmount.week,
                    spSummary.copeAmount.week, spSummary.forecast.forecastAmount.week + spSummary.copeAmount.week,
                    authenticationProvider, isPertax))
            case _ => Redirect(routes.AccountController.show())
        }
        case _ => throw new RuntimeException("SP Response Model is empty")
      }
    }
  }

  def show: Action[AnyContent] = AuthorisedByAny.async { implicit user => implicit request =>
    isFromPertax.flatMap { isPertax =>
      val nino = user.nino.getOrElse("")
      val authenticationProvider = getAuthenticationProvider(user.authContext.user.confidenceLevel)
      nispConnector.connectToGetSPResponse(nino).map {
        case SPResponseModel(Some(spSummary: SPSummaryModel), None, None) =>

          customAuditConnector.sendEvent(AccountAccessEvent(nino, spSummary.contextMessage,
            spSummary.statePensionAge.date, spSummary.statePensionAmount.week, spSummary.forecast.forecastAmount.week,
            spSummary.dateOfBirth, user.name, spSummary.contractedOutFlag, spSummary.forecast.scenario,
            spSummary.copeAmount.week, authenticationProvider))

          if (spSummary.numberOfQualifyingYears + spSummary.yearsToContributeUntilPensionAge < Constants.minimumQualifyingYearsNSP) {
            val canGetPension = spSummary.numberOfQualifyingYears +
              spSummary.yearsToContributeUntilPensionAge + spSummary.numberOfGapsPayable >= Constants.minimumQualifyingYearsNSP
            val yearsMissing = Constants.minimumQualifyingYearsNSP - spSummary.numberOfQualifyingYears
            Ok(account_mqp(nino, spSummary, canGetPension, yearsMissing, authenticationProvider, isPertax))
              .withSession(storeUserInfoInSession(user, contractedOut = false))
          } else if (spSummary.forecast.scenario.equals(Scenario.ForecastOnly)) {
            Ok(account_forecastonly(nino, spSummary, authenticationProvider,isPertax)).withSession(storeUserInfoInSession(user, contractedOut = false))
          } else {
            val (currentChart, forecastChart) = calculateChartWidths(spSummary.statePensionAmount, spSummary.forecast.forecastAmount)
            Ok(account(nino, spSummary, currentChart, forecastChart, authenticationProvider, isPertax))
              .withSession(storeUserInfoInSession(user, spSummary.contractedOutFlag))
          }

        case SPResponseModel(_, Some(spExclusions: ExclusionsModel), _) =>
          customAuditConnector.sendEvent(AccountExclusionEvent(
            nino,
            user.name,
            spExclusions.exclusions
          ))
          Redirect(routes.ExclusionController.showSP()).withSession(storeUserInfoInSession(user, contractedOut = false))
        case _ => throw new RuntimeException("SP Response Model is empty")
      }
    }
  }

  def pta(): Action[AnyContent] = AuthorisedByAny { implicit user => implicit request =>
    setFromPertax
    Redirect(routes.AccountController.show())
  }

  def calculateChartWidths(currentAmountModel: SPAmountModel, forecastAmountModel: SPAmountModel): (SPChartModel, SPChartModel) = {
    // scalastyle:off magic.number
    if (forecastAmountModel.week > currentAmountModel.week) {
      val currentPercentage = (currentAmountModel.week/forecastAmountModel.week * 100).toInt
      val currentChart = SPChartModel(currentPercentage.max(Constants.chartWidthMinimum), currentAmountModel)
      val forecastChart = SPChartModel(100, forecastAmountModel)
      (currentChart, forecastChart)
    } else {
      val currentChart = SPChartModel(100, currentAmountModel)
      val forecastChart = SPChartModel((forecastAmountModel.week/currentAmountModel.week * 100).toInt, forecastAmountModel)
      (currentChart, forecastChart)
    }
  }
  private def storeUserInfoInSession(user: NispUser, contractedOut: Boolean)(implicit request: Request[AnyContent]): Session = {
    request.session +
      (NAME -> user.name.getOrElse("N/A")) +
      (NINO -> user.nino.getOrElse("")) +
      (CONTRACTEDOUT -> contractedOut.toString)
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

  def timeout: Action[AnyContent] = UnauthorisedAction { implicit request =>
    Ok(sessionTimeout())
  }
}
