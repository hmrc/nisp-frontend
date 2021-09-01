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

package uk.gov.hmrc.nisp.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import play.twirl.api.Html
import uk.gov.hmrc.http.{HttpClient, HttpResponse}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.helpers.{FakeCachedStaticHtmlPartialRetriever, FakeNispHeaderCarrierForPartialsConverter, FakePartialRetriever, FakeTemplateRenderer}
import uk.gov.hmrc.nisp.utils.UnitSpec
import uk.gov.hmrc.nisp.views.html.feedback_thankyou
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever, HeaderCarrierForPartialsConverter}
import uk.gov.hmrc.renderer.TemplateRenderer

import scala.concurrent.Future

class FeedbackControllerSpec extends UnitSpec with GuiceOneAppPerSuite with Injecting with BeforeAndAfterEach {
  val fakeRequest = FakeRequest("GET", "/")
  val mockApplicationConfig = mock[ApplicationConfig]
  val mockHttp = mock[HttpClient]
  val mockView = mock[feedback_thankyou]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[ApplicationConfig].toInstance(mockApplicationConfig),
      bind[HttpClient].toInstance(mockHttp),
      bind[TemplateRenderer].toInstance(FakeTemplateRenderer),
      bind[FormPartialRetriever].to[FakePartialRetriever],
      bind[CachedStaticHtmlPartialRetriever].toInstance(FakeCachedStaticHtmlPartialRetriever),
      bind[HeaderCarrierForPartialsConverter].toInstance(FakeNispHeaderCarrierForPartialsConverter),
      bind[feedback_thankyou].toInstance(mockView)
    ).build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockApplicationConfig, mockHttp)
  }

  val testFeedbackController = inject[FeedbackController]

  "GET /feedback" should {
    "return feedback page" in {
      when(mockApplicationConfig.contactFrontendPartialBaseUrl).thenReturn("baseUrl")
      when(mockApplicationConfig.contactFormServiceIdentifier).thenReturn("serviceIdentifier")

      val result = testFeedbackController.show(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "capture the referer in the session on initial session on the feedback load" in {
      when(mockApplicationConfig.contactFrontendPartialBaseUrl).thenReturn("baseUrl")
      when(mockApplicationConfig.contactFormServiceIdentifier).thenReturn("serviceIdentifier")

      val result = testFeedbackController.show(fakeRequest.withHeaders("Referer" -> "Blah"))
      status(result) shouldBe Status.OK
    }
  }

  "POST /feedback" should {
    val fakePostRequest = FakeRequest("POST", "/check-your-state-pension/feedback").withFormUrlEncodedBody("test" -> "test")
    "return form with thank you for valid selections" in {
      when(mockHttp.POSTForm[HttpResponse](any(), any(), any())
        (any(), any(),any())).thenReturn(
        Future.successful(HttpResponse(Status.OK, "1234")))

      val result = testFeedbackController.submit(fakePostRequest)
      redirectLocation(result) shouldBe Some(routes.FeedbackController.showThankYou.url)
    }

    "return form with errors for invalid selections" in {
      when(mockApplicationConfig.contactFrontendPartialBaseUrl).thenReturn("baseUrl")
      when(mockApplicationConfig.contactFormServiceIdentifier).thenReturn("serviceIdentifier")

      when(mockHttp.POSTForm[HttpResponse](any(), any(), any())
        (any(), any(),any())).thenReturn(
        Future.successful(HttpResponse(Status.BAD_REQUEST, "<p>:^(</p>")))

      val result = testFeedbackController.submit(fakePostRequest)
      status(result) shouldBe Status.BAD_REQUEST
    }

    "return error for other http code back from contact-frontend" in {
      when(mockHttp.POSTForm[HttpResponse](any(), any(), any())
        (any(), any(),any())).thenReturn(
        Future.successful(HttpResponse(418, "")))
      val result = testFeedbackController.submit(fakePostRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return internal server error when there is an empty form" in {
      when(mockHttp.POSTForm[HttpResponse](any(), any(), any())
        (any(), any(),any())).thenReturn(
        Future.successful(HttpResponse(Status.OK, "1234")))

      val result = testFeedbackController.submit(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

  "GET /feedback/thankyou" should {
    "should return the thank you page" in {
      
      when(mockView(any(), any())(any(), any())).thenReturn(Html(""))
      
      val result = testFeedbackController.showThankYou(fakeRequest)
      status(result) shouldBe Status.OK
    }
  }
}
