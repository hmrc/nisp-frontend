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

import play.api.Logger
import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.nisp.controllers.routes
import uk.gov.hmrc.play.frontend.auth.{AuthContext, AuthenticationProvider, UserCredentials}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.http.{HeaderCarrier, SessionKeys}

import scala.concurrent.Future

object IDAProvider extends AuthenticationProvider {

  override def id: String = "IDA"

  private def redirectAfterLoginUrl(implicit request: Request[_]) =
    AuthUrlConfig.postSignInRedirectUrl.getOrElse(routes.AccountController.show().url)

  override def handleNotAuthenticated(implicit request: Request[_]):
    PartialFunction[UserCredentials, Future[Either[AuthContext, IDAProvider.FailureResult]]] = {
    case UserCredentials(None, token@_) =>
      implicit val hc = HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session))
      Logger.info(s"No userId found - redirecting to login. user: None token : $token")
      redirectToLogin.map(result => Right(result))
    case UserCredentials(Some(userId), Some(token)) =>
      implicit val hc = HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session))
      Logger.info(s"Wrong user type - redirecting to login. token : $token")
      redirectToLogin.map(result => Right(result))
  }

  override def redirectToLogin(implicit request: Request[_]): Future[FailureResult] = {
    val redirectToIdaAuthentication = Redirect(AuthUrlConfig.idaSignIn, getTokenQueryParams)
    Future.successful(redirectToIdaAuthentication.withSession(sessionWithRedirect(request.session, Some(redirectAfterLoginUrl))))
  }

  private def sessionWithRedirect(session: Session, redirectTo: Option[String]) =
    Session(session.data ++ redirectTo.map(SessionKeys.redirect -> _).toMap ++ Map(SessionKeys.loginOrigin -> "YSP"))

  private def getTokenQueryParams(implicit request: Request[_]): Map[String, Seq[String]] = {
    getTokenFromRequest match {
      case Some(token) => Map("token" -> Seq(token))
      case None => Map.empty[String, Seq[String]]
    }
  }

  private def getTokenFromRequest(implicit request: Request[_]): Option[String] = {
    // This gets the IDA token from the request's Query String
    val tokenZero = request.getQueryString("token")
    tokenZero match {
      case Some(t) if t.isEmpty => Logger.info("The provided Ida token is empty"); None
      case None => Logger.info("The user did not provide an Ida token"); None
      case t@Some(token) => t
    }
  }

  override def handleSessionTimeout(implicit request: Request[_]): Future[FailureResult] = {
    Future.successful(Redirect(routes.AccountController.timeout().url))
  }

}
