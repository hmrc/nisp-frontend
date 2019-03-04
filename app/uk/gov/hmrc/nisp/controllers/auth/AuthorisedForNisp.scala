/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.nisp.auth.{NispAuthProvider, NispCompositePageVisibilityPredicate, VerifyProvider}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.exceptions.EmptyPayeException
import uk.gov.hmrc.nisp.models.citizen.CitizenDetailsResponse
import uk.gov.hmrc.nisp.services.CitizenDetailsService
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.play.frontend.auth._
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Accounts, ConfidenceLevel}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, SessionKeys }

trait AuthorisedForNisp extends Actions {
  val citizenDetailsService: CitizenDetailsService
  val applicationConfig: ApplicationConfig

  private type PlayRequest = Request[AnyContent] => Result
  private type UserRequest = NispUser => PlayRequest
  private type AsyncPlayRequest = Request[AnyContent] => Future[Result]
  private type AsyncUserRequest = NispUser => AsyncPlayRequest

  implicit private def hc(implicit request: Request[_]): HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

  class AuthorisedBy(regime: TaxRegime) {
    val authedBy: AuthenticatedBy = {
      if (applicationConfig.identityVerification) {
        AuthorisedFor(regime, NispCompositePageVisibilityPredicate)
      } else {
        AuthorisedFor(NispVerifyRegime, VerifyConfidence)
      }
    }

    def async(action: AsyncUserRequest): Action[AnyContent] = {
      authedBy.async {
        authContext: AuthContext =>
          implicit request =>
            retrievePerson(authContext) flatMap { citizen =>
              action(NispUser(
                authContext = authContext,
                name = citizen.flatMap(_.person.getNameFormatted),
                authProvider = request.session.get(SessionKeys.authProvider).getOrElse(""),
                sex = citizen.flatMap(_.person.sex),
                dateOfBirth = citizen.map(_.person.dateOfBirth),
                address = citizen.flatMap(_.address)
              ))(request)
            }
      }
    }

    def apply(action: UserRequest): Action[AnyContent] = async(user => request => Future.successful(action(user)(request)))
  }

  object AuthorisedByAny extends AuthorisedBy(NispAnyRegime)

  object AuthorisedByVerify extends AuthorisedBy(NispVerifyRegime)

  def retrievePerson(authContext: AuthContext)(implicit request: Request[AnyContent]): Future[Option[CitizenDetailsResponse]] =
    citizenDetailsService.retrievePerson(retrieveNino(authContext.principal))

  private def retrieveNino(principal: Principal): Nino = {
    principal.accounts.paye match {
      case Some(account) => account.nino
      case None => throw new EmptyPayeException("PAYE Account is empty")
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
    if (confidenceLevel.level == 500) Constants.verify else Constants.iv
  }
}
