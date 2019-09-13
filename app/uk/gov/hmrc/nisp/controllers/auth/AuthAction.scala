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

import java.net.{URI, URLEncoder}

import com.google.inject.{ImplementedBy, Inject}
import play.api.Mode.Mode
import play.api.mvc.Results._
import play.api.mvc._
import play.api.{Application, Configuration, Play}
import uk.gov.hmrc.auth.core.AuthProvider.Verify
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{Name, ~}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{CorePost, HeaderCarrier, InternalServerException}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.config.wiring.WSHttp
import uk.gov.hmrc.nisp.connectors.CitizenDetailsConnector
import uk.gov.hmrc.nisp.controllers.routes
import uk.gov.hmrc.nisp.models.UserName
import uk.gov.hmrc.nisp.models.citizen._
import uk.gov.hmrc.nisp.services.CitizenDetailsService
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.{ExecutionContext, Future}

case class AuthenticatedRequest[A](request: Request[A],
                                   nispAuthedUser: NispAuthedUser
                                  ) extends WrappedRequest[A](request)

case class ExcludedAuthenticatedRequest[A](request: Request[A],
                                           nino: Nino,
                                           confidenceLevel: ConfidenceLevel) extends WrappedRequest[A](request)

class AuthActionImpl @Inject()(override val authConnector: NispAuthConnector,
                               cds: CitizenDetailsService)
                              (implicit ec: ExecutionContext) extends AuthAction with AuthorisedFunctions {

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authorised(ConfidenceLevel.L200 and Enrolment("HMRC-NI"))
      .retrieve(Retrievals.nino and Retrievals.confidenceLevel) {
        case Some(nino) ~ confidenceLevel => {
          cds.retrievePerson(Nino(nino)).flatMap {
            case Right(cdr) => {
              block(AuthenticatedRequest(request,
                NispAuthedUser(Nino(nino),
                  confidenceLevel,
                  cdr.person.dateOfBirth,
                  UserName(Name(cdr.person.firstName, cdr.person.lastName)),
                  cdr.address)))
            }
            case Left(TECHNICAL_DIFFICULTIES) | Left(NOT_FOUND) => throw new InternalServerException("")
            case Left(MCI_EXCLUSION) => Future.successful(Redirect(routes.ExclusionController.showNI))
          }

        }
        case _ => throw new RuntimeException("Can't find credentials for user")
      } recover {
      case _: NoActiveSession => Redirect(ApplicationConfig.ggSignInUrl, Map("continue" -> Seq(ApplicationConfig.postSignInRedirectUrl),
        "origin" -> Seq("nisp-frontend"), "accountType" -> Seq("individual")))
      case _:InsufficientConfidenceLevel => Redirect(ivUpliftURI.toURL.toString)
    }
  }

  def getAuthenticationProvider(confidenceLevel: ConfidenceLevel): String = {
    if (confidenceLevel.level == 500) Constants.verify else Constants.iv
  }

  private val ivUpliftURI: URI =
  new URI(s"${ApplicationConfig.ivUpliftUrl}?origin=NISP&" +
    s"completionURL=${URLEncoder.encode(ApplicationConfig.postSignInRedirectUrl, "UTF-8")}&" +
    s"failureURL=${URLEncoder.encode(ApplicationConfig.notAuthorisedRedirectUrl, "UTF-8")}" +
    s"&confidenceLevel=200")
}

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction extends ActionBuilder[AuthenticatedRequest] with ActionFunction[Request, AuthenticatedRequest] {
  def getAuthenticationProvider(confidenceLevel: ConfidenceLevel): String
}

class NispAuthConnector extends PlayAuthConnector with ServicesConfig {
  override lazy val serviceUrl: String = baseUrl("auth")

  override def http: CorePost = WSHttp

  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration
}

class VerifyAuthActionImpl @Inject()(override val authConnector: NispAuthConnector,
                                     cds: CitizenDetailsService)
                                    (implicit ec: ExecutionContext) extends AuthAction with AuthorisedFunctions {

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authorised(ConfidenceLevel.L500 and Enrolment("HMRC-NI")and AuthProviders(Verify))
      .retrieve(Retrievals.nino and Retrievals.confidenceLevel) {
        case Some(nino) ~ confidenceLevel => {
          cds.retrievePerson(Nino(nino)).flatMap {
            case Right(cdr) => {
              block(AuthenticatedRequest(request,
                NispAuthedUser(Nino(nino),
                  confidenceLevel,
                  cdr.person.dateOfBirth,
                  UserName(Name(cdr.person.firstName, cdr.person.lastName)),
                  cdr.address)))
            }
            case Left(TECHNICAL_DIFFICULTIES) | Left(NOT_FOUND) => throw new InternalServerException("")
            case Left(MCI_EXCLUSION) => Future.successful(Redirect(routes.ExclusionController.showSP))
          }
        }
        case _ => throw new RuntimeException("Can't find credentials for user")
      } recover {
      case _: NoActiveSession => Redirect(ApplicationConfig.verifySignIn, parameters)
    }
  }

  def parameters: Map[String, Seq[String]] = {

    val continueUrl = Map[String, Seq[String]]()

    if (ApplicationConfig.verifySignInContinue) {
      continueUrl + ("continue" -> Seq(ApplicationConfig.postSignInRedirectUrl))
    } else continueUrl
  }

  def getAuthenticationProvider(confidenceLevel: ConfidenceLevel): String = {
    if (confidenceLevel.level == 500) Constants.verify else Constants.iv
  }
}

object AuthActionSelector {
  def decide(applicationConfig: ApplicationConfig)(implicit app: Application): AuthAction = {
    if (applicationConfig.identityVerification) {
      app.injector.instanceOf[AuthActionImpl]
    } else {
      app.injector.instanceOf[VerifyAuthActionImpl]
    }
  }
}
