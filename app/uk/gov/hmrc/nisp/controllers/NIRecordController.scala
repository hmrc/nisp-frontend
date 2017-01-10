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

import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.config.wiring.NispSessionCache
import uk.gov.hmrc.nisp.connectors.NispConnector
import uk.gov.hmrc.nisp.controllers.auth.AuthorisedForNisp
import uk.gov.hmrc.nisp.controllers.connectors.{AuthenticationConnectors, CustomAuditConnector}
import uk.gov.hmrc.nisp.controllers.partial.PartialRetriever
import uk.gov.hmrc.nisp.controllers.pertax.PertaxHelper
import uk.gov.hmrc.nisp.events.{AccountExclusionEvent, NIRecordEvent}
import uk.gov.hmrc.nisp.models.{ExclusionsModel, NIRecord, NIResponse, NISummary}
import uk.gov.hmrc.nisp.services.{CitizenDetailsService, MetricsService}
import uk.gov.hmrc.nisp.views.html.{nirecordGapsAndHowToCheckThem, nirecordVoluntaryContributions, nirecordpage}
import org.joda.time.{DateTimeZone, LocalDate}


object NIRecordController extends NIRecordController with AuthenticationConnectors with PartialRetriever {
  override val nispConnector: NispConnector = NispConnector
  override val citizenDetailsService: CitizenDetailsService = CitizenDetailsService
  override val applicationConfig: ApplicationConfig = ApplicationConfig
  override val customAuditConnector: CustomAuditConnector = CustomAuditConnector
  override val sessionCache: SessionCache = NispSessionCache
  override val showFullNI: Boolean = ApplicationConfig.showFullNI
  override val currentDate = new LocalDate(DateTimeZone.forID("Europe/London"))
  override val metricsService: MetricsService = MetricsService
}

trait NIRecordController extends NispFrontendController with AuthorisedForNisp with PertaxHelper {
  val nispConnector: NispConnector
  val customAuditConnector: CustomAuditConnector
  val showFullNI: Boolean
  val currentDate: LocalDate

  def showFull: Action[AnyContent] = show(gapsOnly = false)
  def showGaps: Action[AnyContent] = show(gapsOnly = true)
  def pta: Action[AnyContent] = AuthorisedByAny { implicit user => implicit request =>
    setFromPertax
    Redirect(routes.NIRecordController.showFull())
  }

  private def show(gapsOnly: Boolean): Action[AnyContent] = AuthorisedByAny.async {
    implicit user => implicit request =>
      nispConnector.connectToGetNIResponse(user.nino).map {
        case NIResponse(Some(niRecord: NIRecord), Some(niSummary: NISummary), None) =>
          if (gapsOnly && niSummary.noOfNonQualifyingYears < 1) {
            Redirect(routes.NIRecordController.showFull())
          } else {
            customAuditConnector.sendEvent(NIRecordEvent(user.nino.nino, niSummary.yearsToContributeUntilPensionAge, niSummary.noOfQualifyingYears,
              niSummary.noOfNonQualifyingYears, niSummary.numberOfPayableGaps, niSummary.numberOfNonPayableGaps, niSummary.pre75QualifyingYears.getOrElse(0),
              niSummary.spaYear))

              val tableStart = niSummary.recordEnd.getOrElse(niSummary.earningsIncludedUpTo.taxYear + 1)

            Ok(nirecordpage(niRecord, niSummary, gapsOnly, tableStart, niSummary.recordEnd.isDefined, getAuthenticationProvider(user.authContext.user.confidenceLevel), showFullNI, currentDate))
          }
        case NIResponse(_, _, Some(niExclusions: ExclusionsModel)) =>
          customAuditConnector.sendEvent(AccountExclusionEvent(
            user.nino.nino,
            user.name,
            niExclusions.exclusions
          ))
          Redirect(routes.ExclusionController.showNI())
        case _ => throw new RuntimeException("NI Response Model is unmatchable. This is probably a logic error.")
      }
  }

  def showGapsAndHowToCheckThem: Action[AnyContent] = AuthorisedByAny.async { implicit user => implicit request =>
    nispConnector.connectToGetNIResponse(user.nino).map {
      case NIResponse(_, Some(niSummary: NISummary), None) =>
        Ok(nirecordGapsAndHowToCheckThem(niSummary))
      case NIResponse(_, _, Some(niExclusions: ExclusionsModel)) =>
        Redirect(routes.ExclusionController.showNI())
      case _ => throw new RuntimeException("NI Response Model is unmatchable. This is probably a logic error.")
    }
  }

  def showVoluntaryContributions: Action[AnyContent] = AuthorisedByAny { implicit user => implicit request =>
    Ok(nirecordVoluntaryContributions())
  }

}
