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
import uk.gov.hmrc.nisp.services.CitizenDetailsService
import uk.gov.hmrc.nisp.views.html.{nirecordGapsAndHowToCheckThem, nirecordVoluntaryContributions, nirecordpage}
import org.joda.time.{DateTimeZone, LocalDate}


object NIRecordController extends NIRecordController with AuthenticationConnectors with PartialRetriever {
  override val nispConnector: NispConnector = NispConnector
  override val citizenDetailsService: CitizenDetailsService = CitizenDetailsService
  override val applicationConfig: ApplicationConfig = ApplicationConfig
  override val customAuditConnector: CustomAuditConnector = CustomAuditConnector
  override val sessionCache: SessionCache = NispSessionCache
  override val showFullNI: Boolean = ApplicationConfig.showFullNI
}

trait NIRecordController extends NispFrontendController with AuthorisedForNisp with PertaxHelper {
  val nispConnector: NispConnector
  val customAuditConnector: CustomAuditConnector
  val showFullNI: Boolean
  val currentDate = new LocalDate(DateTimeZone.forID("Europe/London"))

  def showFull: Action[AnyContent] = show(niGaps = false)
  def showGaps: Action[AnyContent] = show(niGaps = true)
  def pta: Action[AnyContent] = AuthorisedByAny { implicit user => implicit request =>
    setFromPertax
    Redirect(routes.NIRecordController.showFull())
  }

  private def show(niGaps: Boolean): Action[AnyContent] = AuthorisedByAny.async {
    implicit user => implicit request =>
      val nino = user.nino.getOrElse("")
      nispConnector.connectToGetNIResponse(nino).map {
        case NIResponse(Some(niRecord: NIRecord), Some(niSummary: NISummary), None) =>
          if (niGaps && niSummary.noOfNonQualifyingYears < 1) {
            Redirect(routes.NIRecordController.showFull())
          } else {
            customAuditConnector.sendEvent(NIRecordEvent(nino, niSummary.yearsToContributeUntilPensionAge, niSummary.noOfQualifyingYears,
              niSummary.noOfNonQualifyingYears, niSummary.numberOfPayableGaps, niSummary.numberOfNonPayableGaps, niSummary.pre75QualifyingYears.getOrElse(0),
              niSummary.spaYear))

              val tableStart = niSummary.recordEnd.getOrElse(niSummary.earningsIncludedUpTo.taxYear + 1)
              

            Ok(nirecordpage(nino, niRecord, niSummary, niGaps, tableStart, niSummary.recordEnd.isDefined, getAuthenticationProvider(user.authContext.user.confidenceLevel), showFullNI, currentDate))
          }
        case NIResponse(_, _, Some(niExclusions: ExclusionsModel)) =>
          customAuditConnector.sendEvent(AccountExclusionEvent(
            nino,
            user.name,
            niExclusions.exclusions
          ))
          Redirect(routes.ExclusionController.showNI())
        case _ => throw new RuntimeException("NI Response Model is empty")
      }
  }

  def showGapsAndHowToCheckThem: Action[AnyContent] = AuthorisedByAny.async { implicit user => implicit request =>
    val nino = user.nino.getOrElse("")
    nispConnector.connectToGetNIResponse(nino).map {
      case NIResponse(_, Some(niSummary: NISummary), None) =>
        Ok(nirecordGapsAndHowToCheckThem(niSummary))
      case NIResponse(_, _, Some(niExclusions: ExclusionsModel)) =>
        Redirect(routes.ExclusionController.showNI())
      case _ => throw new RuntimeException("NI Response Model is empty")
    }
  }

  def showVoluntaryContributions: Action[AnyContent] = AuthorisedByAny { implicit user => implicit request =>
    Ok(nirecordVoluntaryContributions())
  }

}
