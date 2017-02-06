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
import uk.gov.hmrc.nisp.services._
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

  private[controllers] def showPre1975Years(dateOfEntry: LocalDate, dateOfBirth: Option[LocalDate]): Boolean = {

    val dateOfEntryDiff = Constants.niRecordStartYear - TaxYear.taxYearFor(dateOfEntry).startYear

    dateOfBirth match {
      case None => dateOfEntryDiff > 0
      case Some(dob) =>
        val sixteenthBirthdayTaxYear = TaxYear.taxYearFor(dob.plusYears(Constants.niRecordMinAge))
        val sixteenthBirthdayDiff = Constants.niRecordStartYear - sixteenthBirthdayTaxYear.startYear

        val yearsPre75 = dateOfEntryDiff.min(sixteenthBirthdayDiff)
        yearsPre75 > 0
    }
  }

  private[controllers] def generateTableList(tableStart: String, tableEnd: String): Seq[String] = {
    require(tableStart >= tableEnd)
    require(tableStart.take(Constants.yearStringLength).forall(_.isDigit))
    require(tableEnd.take(Constants.yearStringLength).forall(_.isDigit))

    val start = tableStart.take(Constants.yearStringLength).toInt
    val end = tableEnd.take(Constants.yearStringLength).toInt

    (start to end by -1) map Formatting.startYearToTaxYear
  }

  private def show(gapsOnlyView: Boolean): Action[AnyContent] = AuthorisedByAny.async {
    implicit user =>
      implicit request =>
        val nationalInsuranceResponseF = nationalInsuranceService.getSummary(user.nino)
        val statePensionResponseF = statePensionService.getSummary(user.nino)

        for (
          nationalInsuranceRecordResponse <- nationalInsuranceResponseF;
          statePensionResponse <- statePensionResponseF
        ) yield {
          nationalInsuranceRecordResponse match {
            case Right(niRecord) =>
              if (gapsOnlyView && niRecord.numberOfGaps < 1) {
                Redirect(routes.NIRecordController.showFull())
              } else {
                val finalRelevantStartYear = statePensionResponse match {
                  case Left(spExclusion) => spExclusion.finalRelevantStartYear
                    .getOrElse(throw new RuntimeException(s"NIRecordController: Can't get pensionDate from StatePensionExclusion $spExclusion"))
                  case Right(sp) => sp.finalRelevantStartYear
                }
                val yearsToContribute = statePensionService.yearsToContributeUntilPensionAge(niRecord.earningsIncludedUpTo, finalRelevantStartYear)
                val recordHasEnded = yearsToContribute < 1
                val tableStart: String =
                  if (recordHasEnded) Formatting.startYearToTaxYear(finalRelevantStartYear)
                  else Formatting.startYearToTaxYear(niRecord.earningsIncludedUpTo.getYear)
                val tableEnd: String = niRecord.taxYears.last.taxYear

                sendAuditEvent(user.nino, niRecord, yearsToContribute)

                Ok(nirecordpage(
                  tableList = generateTableList(tableStart, tableEnd),
                  niRecord = niRecord,
                  gapsOnlyView = gapsOnlyView,
                  recordHasEnded = recordHasEnded,
                  yearsToContribute = yearsToContribute,
                  finalRelevantEndYear = finalRelevantStartYear + 1,
                  showPre1975Years = showPre1975Years(niRecord.dateOfEntry, user.dateOfBirth),
                  authenticationProvider = getAuthenticationProvider(user.authContext.user.confidenceLevel),
                  showFullNI = showFullNI,
                  currentDate = currentDate))
              }
            case Left(exclusion) =>
              customAuditConnector.sendEvent(AccountExclusionEvent(
                user.nino.nino,
                user.name,
                List(exclusion)
              ))
              Redirect(routes.ExclusionController.showNI())
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
