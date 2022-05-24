/*
 * Copyright 2022 HM Revenue & Customs
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

import com.google.inject.Inject
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.{Name, ~}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.routes
import uk.gov.hmrc.nisp.models.UserName
import uk.gov.hmrc.nisp.models.citizen._
import uk.gov.hmrc.nisp.services.CitizenDetailsService
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.{ExecutionContext, Future}

class AuthActionImpl @Inject() (
  override val authConnector: AuthConnector,
  cds: CitizenDetailsService,
  val parser: BodyParsers.Default,
  val executionContext: ExecutionContext,
  applicationConfig: ApplicationConfig
) extends AuthAction
    with AuthorisedFunctions {

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised(ConfidenceLevel.L200)
      .retrieve(
        nino and confidenceLevel and credentials and loginTimes and allEnrolments and trustedHelper
      ) {
        case Some(nino) ~ confidenceLevel ~ credentials ~ loginTimes ~ Enrolments(enrolments) ~ optTrustedHelper =>
          val useNino = optTrustedHelper.fold(nino)(_.principalNino)
          cds.retrievePerson(Nino(useNino)).flatMap {
            case Right(cdr)                   =>
              val hasSa: Boolean = optTrustedHelper.fold(enrolments.exists(_.key == "IR-SA"))(_ => false)
              val name: UserName = UserName(Name(cdr.person.firstName, cdr.person.lastName))
              block(
                AuthenticatedRequest(
                  request,
                  NispAuthedUser(Nino(useNino), cdr.person.dateOfBirth, name, cdr.address, optTrustedHelper, hasSa),
                  AuthDetails(confidenceLevel, credentials.map(_.providerType), loginTimes)
                )
              )
            case Left(TECHNICAL_DIFFICULTIES) => throw new InternalServerException("Technical difficulties")
            case Left(NOT_FOUND)              => throw new InternalServerException("User not found")
            case Left(MCI_EXCLUSION)          =>
              if (request.path.contains("nirecord")) {
                Future.successful(Redirect(routes.ExclusionController.showNI))
              } else {
                Future.successful(Redirect(routes.ExclusionController.showSP))
              }
          }
        case _ => throw new RuntimeException("Can't find credentials for user")
      } recover {
      case _: NoActiveSession             =>
        Redirect(
          applicationConfig.ggSignInUrl,
          Map(
            "continue"    -> Seq(applicationConfig.postSignInRedirectUrl),
            "origin"      -> Seq("nisp-frontend"),
            "accountType" -> Seq("individual")
          )
        )
      case _: InsufficientConfidenceLevel =>
        Redirect(
          applicationConfig.ivUpliftUrl,
          Map(
            "origin"          -> Seq("NISP"),
            "completionURL"   -> Seq(applicationConfig.postSignInRedirectUrl),
            "failureURL"      -> Seq(applicationConfig.notAuthorisedRedirectUrl),
            "confidenceLevel" -> Seq(ConfidenceLevel.L200.toString)
          )
        )
    }
  }
}

trait AuthAction
    extends ActionBuilder[AuthenticatedRequest, AnyContent]
    with ActionFunction[Request, AuthenticatedRequest]
