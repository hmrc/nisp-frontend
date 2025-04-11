/*
 * Copyright 2024 HM Revenue & Customs
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
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.auth.{AuthenticatedRequest, NispAuthedUser, StandardAuthJourney}
import uk.gov.hmrc.nisp.controllers.pertax.PertaxHelper
import uk.gov.hmrc.nisp.events.{AccountAccessEvent, AccountExclusionEvent}
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.models.admin.NewStatePensionUIToggle
import uk.gov.hmrc.nisp.models.enums.{MQPScenario, Scenario}
import uk.gov.hmrc.nisp.services._
import uk.gov.hmrc.nisp.utils.Calculate._
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.nisp.views.html._
import uk.gov.hmrc.nisp.views.html.statePension.{StatePensionCopeView, StatePensionForecastOnlyView, StatePensionMqpView, StatePensionView}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class StatePensionController @Inject()(
                                        authenticate: StandardAuthJourney,
                                        statePensionService: StatePensionService,
                                        nationalInsuranceService: NationalInsuranceService,
                                        auditConnector: AuditConnector,
                                        applicationConfig: ApplicationConfig,
                                        pertaxHelper: PertaxHelper,
                                        mcc: MessagesControllerComponents,
                                        sessionTimeout: sessionTimeout,
                                        statePensionMQP: statepension_mqp,
                                        statePensionCope: statepension_cope,
                                        statePensionForecastOnly: statepension_forecastonly,
                                        statePensionView: statepension,
                                        featureFlagService: FeatureFlagService,
                                        newStatePensionView: StatePensionView,
                                        newStatePensionForecastOnlyView: StatePensionForecastOnlyView,
                                        newStatePensionMqpView: StatePensionMqpView,
                                        newStatePensionCopeView: StatePensionCopeView
                                      )(implicit
                                        ec: ExecutionContext
                                      ) extends NispFrontendController(mcc) with I18nSupport {

  def showCope: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async { implicit request =>
    implicit val user: NispAuthedUser = request.nispAuthedUser

    featureFlagService.get(NewStatePensionUIToggle).flatMap { newUI =>
      pertaxHelper.isFromPertax.flatMap { isPertax =>
        statePensionService.getSummary(user.nino) map {
          case Right(Right(statePension)) if statePension.contractedOut =>
            Ok(
              if (newUI.isEnabled)
                newStatePensionCopeView(
                  statePension.amounts.cope.weeklyAmount
                )
              else
                statePensionCope(
                  statePension.amounts.cope.weeklyAmount,
                  isPertax
                )
            )
          case _ =>
            Redirect(routes.StatePensionController.show)
        }
      }
    }
  }

  private def sendAuditEvent(
                              statePension: StatePension,
                              user: NispAuthedUser
                            )(implicit
                              hc: HeaderCarrier
                            ): Unit =
    auditConnector.sendEvent(
      AccountAccessEvent(
        user.nino.nino,
        statePension.pensionDate,
        statePension.amounts.current.weeklyAmount,
        statePension.amounts.forecast.weeklyAmount,
        user.dateOfBirth,
        user.name.toString,
        statePension.contractedOut,
        statePension.forecastScenario,
        statePension.amounts.cope.weeklyAmount
      )
    )

  private def doShowStatePensionMQP(
                                     statePension: StatePension,
                                     nationalInsuranceRecord: NationalInsuranceRecord,
                                     isPertax: Boolean,
                                     newUI: Boolean
                                   )(implicit
                                     user: NispAuthedUser,
                                     authRequest: AuthenticatedRequest[?]
                                   ): Result =
    Ok(
      if (newUI)
        newStatePensionMqpView(
          statePension,
          nationalInsuranceRecord.numberOfGaps,
          nationalInsuranceRecord.numberOfGapsPayable,
          Constants.minimumQualifyingYearsNSP - statePension.numberOfQualifyingYears,
          calculateAge(user.dateOfBirth, LocalDate.now)
        )
      else
        statePensionMQP(
          statePension,
          nationalInsuranceRecord.numberOfGaps,
          nationalInsuranceRecord.numberOfGapsPayable,
          Constants.minimumQualifyingYearsNSP - statePension.numberOfQualifyingYears,
          user.livesAbroad,
          calculateAge(user.dateOfBirth, LocalDate.now),
          isPertax
        )
    )

  private def doShowStatePensionForecast(
                                          statePension: StatePension,
                                          nationalInsuranceRecord: NationalInsuranceRecord,
                                          isPertax: Boolean,
                                          newUI: Boolean
                                        )(implicit
                                          user: NispAuthedUser,
                                          authRequest: AuthenticatedRequest[?]
                                        ): Result =

    Ok(
      if (newUI)
        newStatePensionForecastOnlyView(
          statePension,
          nationalInsuranceRecord.numberOfGaps,
          nationalInsuranceRecord.numberOfGapsPayable,
          calculateAge(user.dateOfBirth, LocalDate.now),
          user.livesAbroad
        )
      else
        statePensionForecastOnly(
          statePension,
          nationalInsuranceRecord.numberOfGaps,
          nationalInsuranceRecord.numberOfGapsPayable,
          calculateAge(user.dateOfBirth, LocalDate.now),
          user.livesAbroad,
          isPertax
        )
    )

  private def doShowStatePension(
                                  statePension: StatePension,
                                  nationalInsuranceRecord: NationalInsuranceRecord,
                                  isPertax: Boolean,
                                  newUI: Boolean
                                )(implicit
                                  user: NispAuthedUser,
                                  authRequest: AuthenticatedRequest[?]
                                ): Result = {
    val yearsToContributeUntilPensionAge = statePensionService.yearsToContributeUntilPensionAge(
      statePension.earningsIncludedUpTo,
      statePension.finalRelevantStartYear
    )

    val (currentChart, forecastChart, personalMaximumChart) =
      calculateChartWidths(
        statePension.amounts.current,
        statePension.amounts.forecast,
        statePension.amounts.maximum
      )

    Ok(
      if (newUI)
        newStatePensionView(
          statePension,
          nationalInsuranceRecord.numberOfGaps,
          nationalInsuranceRecord.numberOfGapsPayable,
          currentChart,
          forecastChart,
          personalMaximumChart,
          hidePersonalMaxYears = applicationConfig.futureProofPersonalMax,
          calculateAge(user.dateOfBirth, LocalDate.now),
          user.livesAbroad,
          yearsToContributeUntilPensionAge
        )
      else
        statePensionView(
          statePension,
          nationalInsuranceRecord.numberOfGaps,
          nationalInsuranceRecord.numberOfGapsPayable,
          currentChart,
          forecastChart,
          personalMaximumChart,
          isPertax,
          hidePersonalMaxYears = applicationConfig.futureProofPersonalMax,
          calculateAge(user.dateOfBirth, LocalDate.now),
          user.livesAbroad,
          yearsToContributeUntilPensionAge
        )
    )
  }

  private def sendExclusion(
                             exclusion: Exclusion
                           )(
                             implicit
                             hc: HeaderCarrier,
                             user: NispAuthedUser
                           ): Result = {
    auditConnector.sendEvent(
      AccountExclusionEvent(
        user.nino.nino,
        user.name,
        exclusion
      )
    )
    Redirect(routes.ExclusionController.showSP)
  }

  def show: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async { implicit request =>
    implicit val user: NispAuthedUser = request.nispAuthedUser

    featureFlagService.get(NewStatePensionUIToggle).flatMap { newUI =>
      pertaxHelper.isFromPertax.flatMap { isPertax =>
        statePensionService.getSummary(user.nino).flatMap {
          case Right(Left(statePensionExclusion)) =>
            Future.successful(sendExclusion(statePensionExclusion.exclusion))
          case Right(Right(statePension)) =>
            nationalInsuranceService.getSummary(user.nino) map {
              case Right(Left(nationalInsuranceExclusion)) if statePension.reducedRateElection =>
                sendExclusion(nationalInsuranceExclusion.exclusion)
              case Right(Right(nationalInsuranceRecord)) =>
                sendAuditEvent(statePension, user)
                if (statePension.mqpScenario.fold(false)(_ != MQPScenario.ContinueWorking)) {
                  doShowStatePensionMQP(statePension, nationalInsuranceRecord, isPertax, newUI.isEnabled)
                } else if (statePension.forecastScenario.equals(Scenario.ForecastOnly)) {
                  doShowStatePensionForecast(statePension, nationalInsuranceRecord, isPertax, newUI.isEnabled)
                } else {
                  doShowStatePension(statePension, nationalInsuranceRecord, isPertax, newUI.isEnabled)
                }
              case _ => throw new RuntimeException(
                "StatePensionController: SP and NIR are unmatchable. This is probably a logic error."
              )
            }
          case _ => throw new RuntimeException(
            "StatePensionController: SP and NIR are unmatchable. This is probably a logic error."
          )
        }
      }
    }
  }

  def pta(): Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails { implicit request =>
    pertaxHelper.setFromPertax
    Redirect(routes.StatePensionController.show)
  }

  def signOut: Action[AnyContent] = Action { _ =>
    Redirect(applicationConfig.feedbackFrontendUrl).withNewSession
  }

  def timeout: Action[AnyContent] = Action { implicit request =>
    Ok(sessionTimeout())
  }
}
