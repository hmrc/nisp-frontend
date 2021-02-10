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

import com.google.inject.{ImplementedBy, Inject}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionBuilder, ActionFunction, Request, Result}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class ExcludedAuthActionImpl @Inject()(override val authConnector: PlayAuthConnector)
                                      (implicit ec: ExecutionContext) extends ExcludedAuthAction with AuthorisedFunctions {

  override def invokeBlock[A](request: Request[A], block: ExcludedAuthenticatedRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authorised(ConfidenceLevel.L200)
      .retrieve(Retrievals.nino and Retrievals.confidenceLevel and Retrievals.credentials and Retrievals.loginTimes) {
        case Some(nino) ~ confidenceLevel ~ credentials ~ loginTimes =>
          block(ExcludedAuthenticatedRequest(request,
            Nino(nino),
            AuthDetails(confidenceLevel, credentials.map(creds => creds.providerType), loginTimes))
          )
        case _ => throw new RuntimeException("Can't find credentials for user")
      } recover {
      case _: NoActiveSession => Redirect(ApplicationConfig.ggSignInUrl, Map("continue" -> Seq(ApplicationConfig.postSignInRedirectUrl),
        "origin" -> Seq("nisp-frontend"), "accountType" -> Seq("individual")))
      case _: InsufficientConfidenceLevel => Redirect(ivUpliftURI.toURL.toString)
    }
  }

  private val ivUpliftURI: URI =
  new URI(s"${ApplicationConfig.ivUpliftUrl}?origin=NISP&" +
    s"completionURL=${URLEncoder.encode(ApplicationConfig.postSignInRedirectUrl, "UTF-8")}&" +
    s"failureURL=${URLEncoder.encode(ApplicationConfig.notAuthorisedRedirectUrl, "UTF-8")}" +
    s"&confidenceLevel=200")

}

@ImplementedBy(classOf[ExcludedAuthActionImpl])
trait ExcludedAuthAction extends ActionBuilder[ExcludedAuthenticatedRequest] with ActionFunction[Request, ExcludedAuthenticatedRequest]
