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

package uk.gov.hmrc.nisp.controllers.auth

import java.net.{URI, URLEncoder}
import com.google.inject.Inject
import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import scala.concurrent.{ExecutionContext, Future}

class ExcludedAuthActionImpl @Inject() (
  val parser: BodyParsers.Default,
  val authConnector: AuthConnector,
  applicationConfig: ApplicationConfig,
  val executionContext: ExecutionContext
) extends ExcludedAuthAction
    with AuthorisedFunctions {

  override def invokeBlock[A](
    request: Request[A],
    block: ExcludedAuthenticatedRequest[A] => Future[Result]
  ): Future[Result] = {

    implicit val hc: HeaderCarrier    = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    implicit val ec: ExecutionContext = executionContext

    authorised(ConfidenceLevel.L200)
      .retrieve(Retrievals.nino and Retrievals.confidenceLevel and Retrievals.credentials and Retrievals.loginTimes) {
        case Some(nino) ~ confidenceLevel ~ credentials ~ loginTimes =>
          block(
            ExcludedAuthenticatedRequest(
              request,
              Nino(nino),
              AuthDetails(confidenceLevel, credentials.map(creds => creds.providerType), loginTimes)
            )
          )
        case _                                                       => throw new RuntimeException("Can't find credentials for user")
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
      case _: InsufficientConfidenceLevel => Redirect(ivUpliftURI.toURL.toString)
    }
  }

  lazy val ivUpliftURI: URI = new URI(
    s"${applicationConfig.ivUpliftUrl}?origin=NISP&" +
      s"completionURL=${URLEncoder.encode(applicationConfig.postSignInRedirectUrl, "UTF-8")}&" +
      s"failureURL=${URLEncoder.encode(applicationConfig.notAuthorisedRedirectUrl, "UTF-8")}" +
      s"&confidenceLevel=200"
  )

}

trait ExcludedAuthAction
    extends ActionBuilder[ExcludedAuthenticatedRequest, AnyContent]
    with ActionFunction[Request, ExcludedAuthenticatedRequest]
