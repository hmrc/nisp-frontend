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

package uk.gov.hmrc.nisp.controllers.redirect

import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.play.frontend.controller.{FrontendController, UnauthorisedAction}

import scala.concurrent.Future

object RedirectController extends FrontendController  {
  def redirectToHome(path: String): Action[AnyContent] = UnauthorisedAction.async { implicit request =>
    val newPath = path match {
      case "" => ""
      case p if p.startsWith("/") => p
      case p => "/" + p
    }
    Future.successful(Redirect(Constants.baseUrl + newPath, request.queryString, MOVED_PERMANENTLY))
  }
}
