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

import play.api.Logger
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.nisp.connectors.NispConnector
import uk.gov.hmrc.nisp.controllers.auth.AuthorisedForNisp
import uk.gov.hmrc.nisp.controllers.connectors.{AuthenticationConnectors, CustomAuditConnector}
import uk.gov.hmrc.nisp.events.NIRecordEvent
import uk.gov.hmrc.nisp.models.{NIRecord, NIResponse, NISummary}
import uk.gov.hmrc.nisp.services.{CitizenDetailsService, MetricsService, NpsAvailabilityChecker}
import uk.gov.hmrc.nisp.views.html.{nirecordGapsAndHowToCheckThem, nirecordVoluntaryContributions, nirecordpage}
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

object NIRecordController extends NIRecordController with AuthenticationConnectors {
  override val nispConnector: NispConnector = NispConnector
  override val metricsService: MetricsService = MetricsService
  override val citizenDetailsService: CitizenDetailsService = CitizenDetailsService
  override val npsAvailabilityChecker: NpsAvailabilityChecker = NpsAvailabilityChecker
}

trait NIRecordController extends FrontendController with AuthorisedForNisp {
  val nispConnector: NispConnector
  val metricsService: MetricsService

  def showFull: Action[AnyContent] = show(niGaps = false)
  def showGaps: Action[AnyContent] = show(niGaps = true)

  private def show(niGaps: Boolean): Action[AnyContent] = AuthorisedByIda.async {
    implicit user => implicit request =>
      val nino = user.nino.getOrElse("")
      nispConnector.connectToGetNIResponse(nino).map {
        case NIResponse(Some(niRecord: NIRecord), Some(niSummary: NISummary)) =>
          if (niGaps && niSummary.noOfNonQualifyingYears < 1) {
            Redirect(routes.NIRecordController.showFull())
          } else {
            metricsService.niRecord(niSummary.noOfNonQualifyingYears, niSummary.numberOfPayableGaps, niSummary.pre75QualifyingYears.getOrElse(0),
              niSummary.noOfQualifyingYears, niSummary.yearsToContributeUntilPensionAge)

            CustomAuditConnector.sendEvent(NIRecordEvent(nino, niSummary.yearsToContributeUntilPensionAge, niSummary.noOfQualifyingYears,
              niSummary.noOfNonQualifyingYears, niSummary.numberOfPayableGaps, niSummary.numberOfNonPayableGaps, niSummary.pre75QualifyingYears.getOrElse(0),
              niSummary.spaYear))

            Ok(nirecordpage(nino, niRecord, niSummary, user, niGaps))
          }
        case _ => throw new RuntimeException("NI Response Model is empty")
      }
  }

  def showGapsAndHowToCheckThem: Action[AnyContent] = AuthorisedByIda { implicit user => implicit request =>
    Ok(nirecordGapsAndHowToCheckThem(user))
  }

  def showVoluntaryContributions: Action[AnyContent] = AuthorisedByIda { implicit user => implicit request =>
    Ok(nirecordVoluntaryContributions(user))
  }
}
