/*
 * Copyright 2015 HM Revenue & Customs
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
import uk.gov.hmrc.nisp.controllers.connectors.AuthenticationConnectors
import uk.gov.hmrc.nisp.services.NpsAvailabilityChecker
import uk.gov.hmrc.nisp.views.html.{guidanceNotes, landing, preVerify}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.{FrontendController, UnauthorisedAction}

import scala.concurrent.Future

object LandingController extends LandingController {
  override val npsAvailabilityChecker: NpsAvailabilityChecker = NpsAvailabilityChecker
}

trait LandingController extends FrontendController with Actions with AuthenticationConnectors {
  val npsAvailabilityChecker: NpsAvailabilityChecker

  def show: Action[AnyContent] = UnauthorisedAction(implicit request => Ok(landing()))

  def showPreVerify: Action[AnyContent] = UnauthorisedAction(implicit request => Ok(preVerify()).withNewSession)

  def showGuidanceNotes: Action[AnyContent] = UnauthorisedAction(implicit request => Ok(guidanceNotes()))

  def showNpsUnavailable: Action[AnyContent] = UnauthorisedAction(implicit request => ServiceUnavailable(uk.gov.hmrc.nisp.views.html.npsUnavailable()))
}
