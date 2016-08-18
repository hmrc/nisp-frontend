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
import uk.gov.hmrc.nisp.controllers.connectors.{AuthenticationConnectors, CustomAuditConnector}
import uk.gov.hmrc.nisp.controllers.partial.PartialRetriever
import uk.gov.hmrc.nisp.events.QuestionnaireEvent
import uk.gov.hmrc.nisp.models.forms.QuestionnaireForm
import uk.gov.hmrc.nisp.utils.Constants._
import uk.gov.hmrc.nisp.views.html.{finalPage, questionnaire}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.UnauthorisedAction

import scala.concurrent.Future

object QuestionnaireController extends QuestionnaireController with PartialRetriever {
  override val customAuditConnector: CustomAuditConnector = CustomAuditConnector
}

trait QuestionnaireController extends NispFrontendController with Actions with AuthenticationConnectors {
  val customAuditConnector: CustomAuditConnector

  def show: Action[AnyContent] = UnauthorisedAction.async {
    implicit request =>
      Future.successful(Ok(questionnaire(QuestionnaireForm.form)))
  }

  def submit: Action[AnyContent] = UnauthorisedAction.async {
    implicit request =>
      QuestionnaireForm.form.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(questionnaire(formWithErrors))),
        value => {


          val whatWillYouDoNext = Vector(
            value.speakToFinancialAdvisor,
            Some("moreOnline").filter(b => value.moreOnline),
            Some("askEmployer").filter(b => value.askEmployer),
            Some("askFriend").filter(b => value.askFriend),
            Some("askAgent").filter(b => value.askAgent),
            Some("dontKnow").filter(b => value.dontKnow),
            Some("other").filter(b => value.other)).flatten


          customAuditConnector.sendEvent(new QuestionnaireEvent(
            value.easyToUse,
            value.useItByYourself,
            value.likelyToUse,
            value.satisfied,
            value.understanding,
            value.whatWillYouDoNext,
            value.otherFollowUp,
            value.improve,
            value.research,
            value.email,
            request.session.get(NAME).getOrElse(""),
            request.session.get(NINO).getOrElse(""),
            request.session.get(CONTRACTEDOUT).getOrElse("")
          ))
          Future.successful(Redirect(routes.QuestionnaireController.showFinished()))
        }
      )
  }

  def showFinished: Action[AnyContent] = UnauthorisedAction(implicit request => Ok(finalPage()))
}
