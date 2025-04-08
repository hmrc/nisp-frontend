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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.auth.{AuthenticatedRequest, NispAuthedUser, StandardAuthJourney}
import uk.gov.hmrc.nisp.controllers.pertax.PertaxHelper
import uk.gov.hmrc.nisp.events.{AccountExclusionEvent, NIRecordEvent, NIRecordNoGapsEvent}
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.models.admin.{FriendlyUserFilterToggle, ViewPayableGapsToggle}
import uk.gov.hmrc.nisp.services._
import uk.gov.hmrc.nisp.utils.{Constants, DateProvider}
import uk.gov.hmrc.nisp.views.html.{nirecordGapsAndHowToCheckThem, nirecordVoluntaryContributions, nirecordpage}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.time.TaxYear

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class NIRecordController @Inject()(
                                    auditConnector: AuditConnector,
                                    authenticate: StandardAuthJourney,
                                    nationalInsuranceService: NationalInsuranceService,
                                    statePensionService: StatePensionService,
                                    appConfig: ApplicationConfig,
                                    pertaxHelper: PertaxHelper,
                                    mcc: MessagesControllerComponents,
                                    dateProvider: DateProvider,
                                    niRecordPage: nirecordpage,
                                    niRecordGapsAndHowToCheckThemView: nirecordGapsAndHowToCheckThem,
                                    nirecordVoluntaryContributionsView: nirecordVoluntaryContributions,
                                  )(
                                    implicit ec: ExecutionContext,
                                    val featureFlagService: FeatureFlagService
                                  ) extends NispFrontendController(mcc) with I18nSupport {

  val showFullNI: Boolean = appConfig.showFullNI

  def showFull: Action[AnyContent] = show(gapsOnlyView = false)

  def showGaps: Action[AnyContent] = show(gapsOnlyView = true)

  def pta: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails { implicit request =>
    pertaxHelper.setFromPertax
    Redirect(routes.NIRecordController.showFull)
  }

  private def isFriendlyUser(nino: String): Boolean =
    appConfig.friendlyUsers.contains(nino.filterNot(_.isWhitespace).toUpperCase)

  private def ninoContainsNumberAtEnd(nino: String): Boolean = {
    val lastDigit: String = nino.filter(_.isDigit).last.toString
    appConfig.allowedUsersEndOfNino.contains(lastDigit)
  }

  private def sendAuditEvent(
                              nino: Nino,
                              niRecord: NationalInsuranceRecord,
                              yearsToContribute: Int
                            )(
                              implicit hc: HeaderCarrier
                            ): Unit =
    auditConnector.sendEvent(
      NIRecordEvent(
        nino.nino,
        yearsToContribute,
        niRecord.qualifyingYears,
        niRecord.numberOfGaps,
        niRecord.numberOfGapsPayable,
        niRecord.numberOfGaps - niRecord.numberOfGapsPayable,
        niRecord.qualifyingYearsPriorTo1975
      )
    )

  private def sendNoGapsAuditEvent(nino: Nino)(implicit headerCarrier: HeaderCarrier): Unit =
    auditConnector.sendEvent(
      NIRecordNoGapsEvent(nino.nino)
    )

  private[controllers] def showPre1975Years(
                                             dateOfEntry: Option[LocalDate],
                                             dateOfBirth: LocalDate,
                                             pre1975Years: Int
                                           ): Boolean = {

    val dateOfEntryDiff = dateOfEntry.map(Constants.niRecordStartYear - TaxYear.taxYearFor(_).startYear)

    val sixteenthBirthdayTaxYear = TaxYear.taxYearFor(dateOfBirth.plusYears(Constants.niRecordMinAge))
    val sixteenthBirthdayDiff = Constants.niRecordStartYear - sixteenthBirthdayTaxYear.startYear

    (sixteenthBirthdayDiff, dateOfEntryDiff) match {
      case (sb, Some(doe)) => sb.min(doe) > 0
      case (sb, _) => sb > 0
      case _ => pre1975Years > 0
    }
  }

  private[controllers] def generateTableList(tableStart: String, tableEnd: String): Seq[String] = {
    require(tableStart >= tableEnd)
    require(tableStart.take(Constants.yearStringLength).forall(_.isDigit))
    require(tableEnd.take(Constants.yearStringLength).forall(_.isDigit))

    val start = tableStart.take(Constants.yearStringLength).toInt
    val end = tableEnd.take(Constants.yearStringLength).toInt

    (start to end by -1).map(_.toString)
  }

  private def showNiRecordPage(gapsOnlyView: Boolean, yearsToContribute: Int, finalRelevantStartYear: Int, niRecord: NationalInsuranceRecord)
                              (implicit authRequest: AuthenticatedRequest[_], user: NispAuthedUser): Future[Result] = {
    val recordHasEnded = yearsToContribute < 1

    val tableStart: String =
      if (recordHasEnded) finalRelevantStartYear.toString
      else niRecord.earningsIncludedUpTo.getYear.toString
    val tableEnd: String = niRecord.taxYears match {
      case Nil => tableStart
      case _ => niRecord.taxYears.last.taxYear
    }

    for {
      payableGapsToggle  <- featureFlagService.get(ViewPayableGapsToggle)
      friendlyUserToggle <- featureFlagService.get(FriendlyUserFilterToggle)
    } yield {
      val showViewPayableGapsButton: Boolean = (
        payableGapsToggle.isEnabled,
        friendlyUserToggle.isEnabled,
        isFriendlyUser(user.nino.nino),
        ninoContainsNumberAtEnd(user.nino.nino)
      ) match {
        case(true, false, _, _) => true
        case(true, true, _, _) => ninoContainsNumberAtEnd(user.nino.nino) || isFriendlyUser(user.nino.nino)
        case _ => false
      }
      val payableGapInfo = PayableGapInfo(appConfig.niRecordPayableYears)

      Ok(
        niRecordPage(
          tableList = generateTableList(tableStart, tableEnd),
          niRecord = niRecord,
          gapsOnlyView = gapsOnlyView,
          recordHasEnded = recordHasEnded,
          yearsToContribute = yearsToContribute,
          finalRelevantEndYear = finalRelevantStartYear + 1,
          showPre1975Years = showPre1975Years(
            niRecord.dateOfEntry,
            authRequest.nispAuthedUser.dateOfBirth,
            niRecord.qualifyingYearsPriorTo1975
          ),
          showFullNI = showFullNI,
          currentDate = dateProvider.currentDate,
          showViewPayableGapsButton,
          appConfig.nispModellingFrontendUrl,
          payableGapInfo
        )
      )
    }
 }

  private def sendExclusion(exclusion: Exclusion)(implicit hc: HeaderCarrier, user: NispAuthedUser): Result = {
    auditConnector.sendEvent(
      AccountExclusionEvent(
        user.nino.nino,
        user.name,
        exclusion
      )
    )
    Redirect(routes.ExclusionController.showNI)
  }

  private def finalRelevantStartYear(statePensionResponse: Either[StatePensionExclusionFilter, StatePension]): Option[Int] = {

    statePensionResponse match {
      case Left(spExclusion: StatePensionExclusionFiltered) =>
        Some(
          spExclusion.finalRelevantStartYear
            .getOrElse(
              throw new RuntimeException(
                s"NIRecordController: Can't get pensionDate from StatePensionExclusion $spExclusion"
              )
            )
        )
      case Right(sp) => Some(sp.finalRelevantStartYear)
      case Left(_) => throw new RuntimeException("NIRecordController: an unexpected error has occurred")
    }
  }

  private def show(gapsOnlyView: Boolean): Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async { implicit request =>
    implicit val user: NispAuthedUser = request.nispAuthedUser
    val nino = user.nino

    statePensionService.getSummary(nino) flatMap {
      case Right(Left(copeExclusion: StatePensionExclusionFilteredWithCopeDate)) =>
        Future.successful(sendExclusion(copeExclusion.exclusion))
      case Right(statePensionResponse) =>
        nationalInsuranceService.getSummary(nino) flatMap {
          case Right(Right(nationalInsuranceRecord)) =>
            if (nationalInsuranceRecord.numberOfGaps == 0) sendNoGapsAuditEvent(nino)
            if (gapsOnlyView && nationalInsuranceRecord.numberOfGaps < 1)
              Future.successful(Redirect(routes.NIRecordController.showFull))
            else {
              finalRelevantStartYear(statePensionResponse)
                .map { finalRelevantStartYear =>
                  val yearsToContribute = statePensionService.yearsToContributeUntilPensionAge(
                    nationalInsuranceRecord.earningsIncludedUpTo,
                    finalRelevantStartYear
                  )
                  sendAuditEvent(nino, nationalInsuranceRecord, yearsToContribute)
                  showNiRecordPage(gapsOnlyView, yearsToContribute, finalRelevantStartYear, nationalInsuranceRecord)
                }
                .getOrElse(Future.successful(Redirect(routes.ExclusionController.showSP)))
            }
          case Right(Left(exclusion)) =>
            Future.successful(sendExclusion(exclusion.exclusion))
          case Left(_) =>
            throw new RuntimeException("NIRecordController: an unexpected error has occurred")
        }
      case Left(_) =>
        throw new RuntimeException("NIRecordController: an unexpected error has occurred")
    }
  }

  def showGapsAndHowToCheckThem: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async { implicit request =>
    implicit val user: NispAuthedUser = request.nispAuthedUser
    nationalInsuranceService.getSummary(user.nino) map {
      case Right(Right(niRecord)) =>
        Ok(niRecordGapsAndHowToCheckThemView(niRecord.homeResponsibilitiesProtection))
      case Right(Left(_)) =>
        Redirect(routes.ExclusionController.showNI)
      case Left(_) =>
        Redirect(routes.ExclusionController.showNI)
    }
  }

  def showVoluntaryContributions: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails { implicit request =>
    implicit val user: NispAuthedUser = request.nispAuthedUser
    Ok(nirecordVoluntaryContributionsView())
  }

}
