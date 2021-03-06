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
import org.joda.time.LocalDate
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.auth.{AuthAction, NispAuthedUser}
import uk.gov.hmrc.nisp.controllers.pertax.PertaxHelper
import uk.gov.hmrc.nisp.events.{AccountExclusionEvent, NIRecordEvent}
import uk.gov.hmrc.nisp.models.Exclusion.CopeProcessingFailed
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.services._
import uk.gov.hmrc.nisp.utils.{Constants, DateProvider, Formatting}
import uk.gov.hmrc.nisp.views.html.{nirecordGapsAndHowToCheckThem, nirecordVoluntaryContributions, nirecordpage}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.ExecutionContext

class NIRecordController @Inject()(auditConnector: AuditConnector,
                                   authenticate: AuthAction,
                                   nationalInsuranceService: NationalInsuranceService,
                                   statePensionService: StatePensionService,
                                   appConfig: ApplicationConfig,
                                   pertaxHelper: PertaxHelper,
                                   mcc: MessagesControllerComponents,
                                   dateProvider: DateProvider
                                  )(implicit val formPartialRetriever: FormPartialRetriever,
                                    val templateRenderer: TemplateRenderer,
                                    val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever,
                                    ec: ExecutionContext)
  extends NispFrontendController(mcc) with I18nSupport {

  val showFullNI: Boolean = appConfig.showFullNI


  def showFull: Action[AnyContent] = show(gapsOnlyView = false)

  def showGaps: Action[AnyContent] = show(gapsOnlyView = true)

  def pta: Action[AnyContent] = authenticate {
    implicit request =>
      pertaxHelper.setFromPertax
      Redirect(routes.NIRecordController.showFull())
  }

  private def sendAuditEvent(nino: Nino, niRecord: NationalInsuranceRecord, yearsToContribute: Int)(implicit hc: HeaderCarrier): Unit = {
    auditConnector.sendEvent(NIRecordEvent(
      nino.nino,
      yearsToContribute,
      niRecord.qualifyingYears,
      niRecord.numberOfGaps,
      niRecord.numberOfGapsPayable,
      niRecord.numberOfGaps - niRecord.numberOfGapsPayable,
      niRecord.qualifyingYearsPriorTo1975
    ))
  }

  private[controllers] def showPre1975Years(dateOfEntry: Option[LocalDate], dateOfBirth: LocalDate, pre1975Years: Int): Boolean = {

    val dateOfEntryDiff = dateOfEntry.map(Constants.niRecordStartYear - TaxYear.taxYearFor(_).startYear)

    val sixteenthBirthdayTaxYear = TaxYear.taxYearFor(dateOfBirth.plusYears(Constants.niRecordMinAge))
    val sixteenthBirthdayDiff = Constants.niRecordStartYear - sixteenthBirthdayTaxYear.startYear

    (sixteenthBirthdayDiff, dateOfEntryDiff) match {
      case (sb, Some(doe)) => sb.min(doe) > 0
      case (sb, _) => sb > 0
      case (_, Some(doe)) => doe > 0
      case _ => pre1975Years > 0
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

  private def show(gapsOnlyView: Boolean): Action[AnyContent] = authenticate.async {
    implicit request =>
      implicit val user: NispAuthedUser = request.nispAuthedUser
      val nino = user.nino

      val nationalInsuranceResponseF = nationalInsuranceService.getSummary(nino)
      val statePensionResponseF = statePensionService.getSummary(nino)
      for {
        nationalInsuranceRecordResponse <- nationalInsuranceResponseF
        statePensionResponse <- statePensionResponseF
      } yield {
        nationalInsuranceRecordResponse match {
          case Right(niRecord) =>
            if (gapsOnlyView && niRecord.numberOfGaps < 1) {
              Redirect(routes.NIRecordController.showFull())
            } else {
              val finalRelevantStartYear = statePensionResponse match {
                case Left(StatePensionExclusionFiltered(CopeProcessingFailed, _, _, _)) |
                     Left(StatePensionExclusionFilteredWithCopeDate(_, _, _)) => None
                case Left(spExclusion: StatePensionExclusionFiltered) => Some(spExclusion.finalRelevantStartYear
                  .getOrElse(throw new RuntimeException(s"NIRecordController: Can't get pensionDate from StatePensionExclusion $spExclusion")))
                case Right(sp) => Some(sp.finalRelevantStartYear)
              }

              finalRelevantStartYear.map { finalRelevantStartYear =>
                val yearsToContribute = statePensionService.yearsToContributeUntilPensionAge(niRecord.earningsIncludedUpTo, finalRelevantStartYear)
                val recordHasEnded = yearsToContribute < 1
                val tableStart: String =
                  if (recordHasEnded) Formatting.startYearToTaxYear(finalRelevantStartYear)
                  else Formatting.startYearToTaxYear(niRecord.earningsIncludedUpTo.getYear)
                val tableEnd: String = niRecord.taxYears match {
                  case Nil => tableStart
                  case _ => niRecord.taxYears.last.taxYear
                }
                sendAuditEvent(nino, niRecord, yearsToContribute)

                Ok(nirecordpage(
                  tableList = generateTableList(tableStart, tableEnd),
                  niRecord = niRecord,
                  gapsOnlyView = gapsOnlyView,
                  recordHasEnded = recordHasEnded,
                  yearsToContribute = yearsToContribute,
                  finalRelevantEndYear = finalRelevantStartYear + 1,
                  showPre1975Years = showPre1975Years(niRecord.dateOfEntry, request.nispAuthedUser.dateOfBirth, niRecord.qualifyingYearsPriorTo1975),
                  authenticationProvider = request.authDetails.authProvider.getOrElse("N/A"),
                  showFullNI = showFullNI,
                  currentDate = dateProvider.currentDate))

              }.getOrElse(Redirect(routes.ExclusionController.showSP()))
            }
          case Left(exclusion) =>
            auditConnector.sendEvent(AccountExclusionEvent(
              nino.nino,
              request.nispAuthedUser.name,
              exclusion
            ))
            Redirect(routes.ExclusionController.showNI())
        }
      }
  }

  def showGapsAndHowToCheckThem: Action[AnyContent] = authenticate.async {
    implicit request =>
      implicit val user: NispAuthedUser = request.nispAuthedUser
      nationalInsuranceService.getSummary(user.nino) map {
        case Right(niRecord) =>
          Ok(nirecordGapsAndHowToCheckThem(niRecord.homeResponsibilitiesProtection))
        case Left(_) =>
          Redirect(routes.ExclusionController.showNI())
      }
  }

  def showVoluntaryContributions: Action[AnyContent] = authenticate {
    implicit request =>
      implicit val user: NispAuthedUser = request.nispAuthedUser
      Ok(nirecordVoluntaryContributions())
  }

}
