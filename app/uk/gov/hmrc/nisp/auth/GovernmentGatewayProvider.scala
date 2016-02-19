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

package uk.gov.hmrc.nisp.auth

import java.net.URLEncoder

import play.api.mvc._
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.play.frontend.auth.GovernmentGateway
import uk.gov.hmrc.nisp.controllers.routes

import scala.concurrent.Future

object GovernmentGatewayProvider extends GovernmentGateway {

  private lazy val ggSignInUrl = {
    val encodedUrl = URLEncoder.encode(ApplicationConfig.postSignInRedirectUrl, "UTF-8")
    s"${ApplicationConfig.governmentGateway}/gg/sign-in?continue=$encodedUrl&accountType=individual"
  }

  override def handleSessionTimeout(implicit request: Request[_]): Future[FailureResult] =
    Future.successful(Redirect(routes.AccountController.timeout().url))

  override def login: String = ggSignInUrl
}
