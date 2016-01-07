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
import uk.gov.hmrc.nisp.connectors.NispConnector
import uk.gov.hmrc.nisp.controllers.auth.AuthorisedForNisp
import uk.gov.hmrc.nisp.controllers.connectors.{AuthenticationConnectors, CustomAuditConnector}
import uk.gov.hmrc.nisp.events.NIFeedbackEvent
import uk.gov.hmrc.nisp.models.forms.NIRecordFeedbackForm
import uk.gov.hmrc.nisp.models.{NIResponse, NISummary}
import uk.gov.hmrc.nisp.services.{CitizenDetailsService, NpsAvailabilityChecker}
import uk.gov.hmrc.nisp.views.html.{feedback_ni_thankyou, nirecordfeedback}
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

object NIRecordFeedbackController extends NIRecordFeedbackController with AuthenticationConnectors {
  override val customAuditConnector: CustomAuditConnector = CustomAuditConnector
  override val nispConnector: NispConnector = NispConnector
  override val citizenDetailsService: CitizenDetailsService = CitizenDetailsService
  override val npsAvailabilityChecker: NpsAvailabilityChecker = NpsAvailabilityChecker
}

trait NIRecordFeedbackController extends FrontendController with AuthorisedForNisp {
  val customAuditConnector: CustomAuditConnector
  val nispConnector: NispConnector

  def show: Action[AnyContent] = AuthorisedByIda.async { implicit user => implicit request =>
    val nino = user.nino.getOrElse("")
    nispConnector.connectToGetNIResponse(nino).map{
      case NIResponse(_, Some(niSummary: NISummary)) =>
        Ok(nirecordfeedback(user,NIRecordFeedbackForm.form, niSummary.noOfNonQualifyingYears>0))
      case _ => throw new RuntimeException("NI Response Model is empty")
    }
  }

  def submit: Action[AnyContent] = AuthorisedByIda {
    implicit user => implicit request =>
      NIRecordFeedbackForm.form.bindFromRequest().fold(
        formWithErrors => {
          BadRequest(nirecordfeedback(user, formWithErrors, hasGaps = request.body.asFormUrlEncoded.get("hasGaps").head.toBoolean))
        },
        value => {
          customAuditConnector.sendEvent(NIFeedbackEvent(user.name,value.comments,value.hasGaps, value.updatingRecord))
          Redirect(routes.NIRecordFeedbackController.showThankYou())
        }
      )
  }

  def showThankYou: Action[AnyContent] = AuthorisedByIda(implicit user => implicit request => Ok(feedback_ni_thankyou(user)))
}
