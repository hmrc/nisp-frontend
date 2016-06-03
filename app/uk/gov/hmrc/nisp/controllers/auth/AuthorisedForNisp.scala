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

package uk.gov.hmrc.nisp.controllers.auth

import play.api.Logger
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.nisp.auth.{NispAuthProvider, NispCompositePageVisibilityPredicate, GovernmentGatewayProvider, VerifyProvider}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.routes
import uk.gov.hmrc.nisp.services.{NpsAvailabilityChecker, CitizenDetailsService}
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.play.frontend.auth._
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{ConfidenceLevel, Accounts}
import uk.gov.hmrc.play.frontend.auth.connectors.domain.ConfidenceLevel.L200
import uk.gov.hmrc.play.frontend.controller.UnauthorisedAction
import uk.gov.hmrc.play.http.{SessionKeys, HeaderCarrier}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

trait AuthorisedForNisp extends Actions {
  val citizenDetailsService: CitizenDetailsService
  val npsAvailabilityChecker: NpsAvailabilityChecker
  val applicationConfig: ApplicationConfig

  private type PlayRequest = Request[AnyContent] => Result
  private type UserRequest = NispUser => PlayRequest
  private type AsyncPlayRequest = Request[AnyContent] => Future[Result]
  private type AsyncUserRequest = NispUser => AsyncPlayRequest

  implicit private def hc(implicit request: Request[_]): HeaderCarrier = HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session))

  class AuthorisedBy(regime: TaxRegime) {
    val authedBy: AuthenticatedBy = {
      if(applicationConfig.identityVerification) {
        AuthorisedFor(regime, NispCompositePageVisibilityPredicate)
      } else {
        AuthorisedFor(NispVerifyRegime, VerifyConfidence)
      }
    }

    def async(action: AsyncUserRequest): Action[AnyContent] = {
      if (!npsAvailabilityChecker.isNPSAvailable) {
        UnauthorisedAction(request => Redirect(routes.LandingController.showNpsUnavailable()))
      } else {
        authedBy.async {
          authContext: AuthContext => implicit request =>
            retrieveName(authContext) flatMap { name =>
              action(NispUser(authContext, name, request.session.get(SessionKeys.authProvider).getOrElse("")))(request)
            }
        }
      }
    }

    def apply(action: UserRequest): Action[AnyContent] = async(user => request => Future.successful(action(user)(request)))
  }

  object AuthorisedByAny extends AuthorisedBy(NispAnyRegime)
  object AuthorisedByVerify extends AuthorisedBy(NispVerifyRegime)

  def retrieveName(authContext: AuthContext)(implicit request: Request[AnyContent]): Future[Option[String]] = {
    val nino = retrieveNino(authContext.principal)
    citizenDetailsService.retrievePerson(nino).map { citizenOption =>
      for (
        citizen <- citizenOption;
        name <- citizen.name;
        firstName <- name.firstName;
        lastName <- name.lastName
      ) yield {
        s"$firstName $lastName"
      }
    }
  }

  private def retrieveNino(principal: Principal): String = {
    principal.accounts.paye match {
      case Some(account) => account.nino.toString()
      case None => Logger.warn("User Paye account is empty"); ""
    }
  }

  trait NispRegime extends TaxRegime {
    override def isAuthorised(accounts: Accounts): Boolean = true
    override def authenticationType: AuthenticationProvider = NispAuthProvider
  }

  object NispAnyRegime extends NispRegime
  object NispVerifyRegime extends NispRegime {
    override def authenticationType: AuthenticationProvider = VerifyProvider
  }

  def getAuthenticationProvider(confidenceLevel: ConfidenceLevel): String = {
    if(confidenceLevel.level == 500) Constants.verify else Constants.iv
  }
}
