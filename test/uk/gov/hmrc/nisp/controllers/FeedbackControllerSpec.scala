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

package uk.gov.hmrc.nisp.controllers

import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.http.Status
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.connectors.{AuthenticationConnectors}
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.services.{CitizenDetailsService}
import uk.gov.hmrc.play.http.{HttpResponse, HttpPost}
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.partials.{FormPartialRetriever, CachedStaticHtmlPartialRetriever}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class FeedbackControllerSpec extends UnitSpec with OneAppPerSuite with MockitoSugar {
  val fakeRequest = FakeRequest("GET", "/")

  val mockHttp = mock[WSHttp]

  val testFeedbackController = new FeedbackController with AuthenticationConnectors {
    override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
    override implicit val formPartialRetriever: FormPartialRetriever = MockFormPartialRetriever

    override def httpPost: HttpPost = mockHttp
    override def localSubmitUrl(implicit request: Request[AnyContent]): String = ""

    override def contactFormReferer(implicit request: Request[AnyContent]): String = request.headers.get(REFERER).getOrElse("")

    override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
    override val applicationConfig: ApplicationConfig = new ApplicationConfig {
      override val ggSignInUrl: String = ""
      override val citizenAuthHost: String = ""
      override val twoFactorUrl: String = ""
      override val assetsPrefix: String = ""
      override val reportAProblemNonJSUrl: String = ""
      override val ssoUrl: Option[String] = None
      override val identityVerification: Boolean = false
      override val betaFeedbackUnauthenticatedUrl: String = ""
      override val notAuthorisedRedirectUrl: String = ""
      override val contactFrontendPartialBaseUrl: String = ""
      override val govUkFinishedPageUrl: String = ""
      override val showGovUkDonePage: Boolean = false
      override val analyticsHost: String = ""
      override val betaFeedbackUrl: String = ""
      override val analyticsToken: Option[String] = None
      override val reportAProblemPartialUrl: String = ""
      override val contactFormServiceIdentifier: String = "NISP"
      override val postSignInRedirectUrl: String = ""
      override val ivUpliftUrl: String = ""
      override val pertaxFrontendUrl: String = ""
      override val breadcrumbPartialUrl: String = ""
      override val showFullNI: Boolean = false
      override val futureProofPersonalMax: Boolean = false
    }
  }

  "GET /feedback" should {
    "return feedback page" in {
      val result = testFeedbackController.show(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "capture the referer in the session on initial session on the feedback load" in {
      val result = testFeedbackController.show(fakeRequest.withHeaders("Referer" -> "Blah"))
      status(result) shouldBe Status.OK
    }
  }

  "POST /feedback" should {
    val fakePostRequest = FakeRequest("POST", "/check-your-state-pension/feedback").withFormUrlEncodedBody("test" -> "test")
    "return form with thank you for valid selections" in {
      when(mockHttp.POSTForm[HttpResponse](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(
        Future.successful(HttpResponse(Status.OK, responseString = Some("1234"))))

      val result = testFeedbackController.submit(fakePostRequest)
      redirectLocation(result) shouldBe Some(routes.FeedbackController.showThankYou().url)
    }

    "return form with errors for invalid selections" in {
      when(mockHttp.POSTForm[HttpResponse](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(
        Future.successful(HttpResponse(Status.BAD_REQUEST, responseString = Some("<p>:^(</p>"))))
      val result = testFeedbackController.submit(fakePostRequest)
      status(result) shouldBe Status.BAD_REQUEST
    }

    "return error for other http code back from contact-frontend" in {
      when(mockHttp.POSTForm[HttpResponse](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(
        Future.successful(HttpResponse(418))) // 418 - I'm a teapot
      val result = testFeedbackController.submit(fakePostRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return internal server error when there is an empty form" in {
      when(mockHttp.POSTForm[HttpResponse](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(
        Future.successful(HttpResponse(Status.OK, responseString = Some("1234"))))

      val result = testFeedbackController.submit(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

  "GET /feedback/thankyou" should {
    "should return the thank you page" in {
      val result = testFeedbackController.showThankYou(fakeRequest)
      status(result) shouldBe Status.OK
    }
  }
}
