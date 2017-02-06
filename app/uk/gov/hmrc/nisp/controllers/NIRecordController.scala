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

import org.joda.time.{DateTimeZone, LocalDate}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.config.wiring.NispSessionCache
import uk.gov.hmrc.nisp.controllers.auth.AuthorisedForNisp
import uk.gov.hmrc.nisp.controllers.connectors.{AuthenticationConnectors, CustomAuditConnector}
import uk.gov.hmrc.nisp.controllers.partial.PartialRetriever
import uk.gov.hmrc.nisp.controllers.pertax.PertaxHelper
import uk.gov.hmrc.nisp.events.{AccountExclusionEvent, NIRecordEvent}
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.services.{CitizenDetailsService, MetricsService, NationalInsuranceService, StatePensionService}
import uk.gov.hmrc.nisp.utils.{Constants, Formatting}
import uk.gov.hmrc.nisp.views.html.{nirecordGapsAndHowToCheckThem, nirecordVoluntaryContributions, nirecordpage}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.time.TaxYear


object NIRecordController extends NIRecordController with AuthenticationConnectors with PartialRetriever {
  override val citizenDetailsService: CitizenDetailsService = CitizenDetailsService
  override val applicationConfig: ApplicationConfig = ApplicationConfig
  override val customAuditConnector: CustomAuditConnector = CustomAuditConnector
  override val sessionCache: SessionCache = NispSessionCache
  override val showFullNI: Boolean = ApplicationConfig.showFullNI
  override val currentDate = new LocalDate(DateTimeZone.forID("Europe/London"))
  override val metricsService: MetricsService = MetricsService
  override val nationalInsuranceService: NationalInsuranceService = ???
  override val statePensionService: StatePensionService = ???
}

trait NIRecordController extends NispFrontendController with AuthorisedForNisp with PertaxHelper {
  val customAuditConnector: CustomAuditConnector
  val showFullNI: Boolean
  val currentDate: LocalDate

  def nationalInsuranceService: NationalInsuranceService

  def statePensionService: StatePensionService

  def showFull: Action[AnyContent] = show(gapsOnlyView = false)

  def showGaps: Action[AnyContent] = show(gapsOnlyView = true)

  def pta: Action[AnyContent] = AuthorisedByAny { implicit user =>
    implicit request =>
      setFromPertax
      Redirect(routes.NIRecordController.showFull())
  }

  private def sendAuditEvent(nino: Nino, niRecord: NationalInsuranceRecord, yearsToContribute: Int)(implicit hc: HeaderCarrier) = {
    customAuditConnector.sendEvent(NIRecordEvent(
      nino.nino,
      yearsToContribute,
      niRecord.qualifyingYears,
      niRecord.numberOfGaps,
      niRecord.numberOfGapsPayable,
      niRecord.numberOfGaps - niRecord.numberOfGapsPayable,
      niRecord.qualifyingYearsPriorTo1975
    ))
  }

  private[controllers] def showPre1975Years(dateOfEntry: LocalDate, dateOfBirth: LocalDate): Boolean = {

    val dateOfEntryDiff = Constants.niRecordStartYear - TaxYear.taxYearFor(dateOfEntry).startYear

    val sixteenthBirthdayTaxYear = TaxYear.taxYearFor(dateOfBirth.plusYears(Constants.niRecordMinAge))
    val sixteenthBirthdayDiff = Constants.niRecordStartYear - sixteenthBirthdayTaxYear.startYear

    val yearsPre75 = dateOfEntryDiff.min(sixteenthBirthdayDiff)
    yearsPre75 > 0
  }

  private def show(gapsOnlyView: Boolean): Action[AnyContent] = AuthorisedByAny.async {
    implicit user =>
      implicit request =>

        //TODO multiple unavailable years
        //TODO Pre 75 years

        val nationalInsuranceResponseF = nationalInsuranceService.getSummary(user.nino)
        val statePensionResponseF = statePensionService.getSummary(user.nino)

        for (
          nationalInsuranceRecordResponse <- nationalInsuranceResponseF;
          statePensionResponse <- statePensionResponseF
        ) yield {
          (nationalInsuranceRecordResponse, statePensionResponse) match {
            case (Right(niRecord), Right(statePension)) =>
              if (gapsOnlyView && niRecord.numberOfGaps < 1) {
                Redirect(routes.NIRecordController.showFull())
              } else {
                val yearsToContribute = statePensionService.yearsToContributeUntilPensionAge(statePension.earningsIncludedUpTo, statePension.finalRelevantStartYear)

                sendAuditEvent(user.nino, niRecord, yearsToContribute)

                val recordHasEnded = yearsToContribute < 1
                val tableStart = if (recordHasEnded) statePension.finalRelevantYear else Formatting.startYearToTaxYear(niRecord.earningsIncludedUpTo.getYear)

                Ok(nirecordpage(
                  niRecord,
                  gapsOnlyView,
                  tableStart,
                  recordHasEnded,
                  yearsToContribute,
                  statePension.finalRelevantEndYear,
                  getAuthenticationProvider(user.authContext.user.confidenceLevel),
                  showFullNI,
                  currentDate))
              }
            case (Left(exclusion), _) =>
              customAuditConnector.sendEvent(AccountExclusionEvent(
                user.nino.nino,
                user.name,
                List(exclusion)
              ))
              Redirect(routes.ExclusionController.showNI())
            case _ => throw new RuntimeException("NIRecordController: SP and NIR are unmatchable. This is probably a logic error.")
          }
        }
  }

  def showGapsAndHowToCheckThem: Action[AnyContent] = AuthorisedByAny.async { implicit user =>
    implicit request =>
      nationalInsuranceService.getSummary(user.nino) map {
        case Right(niRecord) =>
          Ok(nirecordGapsAndHowToCheckThem(niRecord.homeResponsibilitiesProtection))
        case Left(_) =>
          Redirect(routes.ExclusionController.showNI())
      }
  }

  def showVoluntaryContributions: Action[AnyContent] = AuthorisedByAny { implicit user =>
    implicit request =>
      Ok(nirecordVoluntaryContributions())
  }

}
