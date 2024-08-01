/*
 * Copyright 2024 HM Revenue & Customs
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

import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any => mockAny, eq => mockEQ}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers, Injecting}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.connectors.{IdentityVerificationConnector, IdentityVerificationSuccessResponse}
import uk.gov.hmrc.nisp.utils.UnitSpec

import java.util.Locale
import scala.concurrent.Future

class LandingControllerSpec extends UnitSpec with BeforeAndAfterEach with GuiceOneAppPerSuite with Injecting {

  implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/")
  val fakeRequestWelsh: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/cymraeg")
  val mockApplicationConfig: ApplicationConfig       = mock[ApplicationConfig]
  val mockIVConnector: IdentityVerificationConnector = mock[IdentityVerificationConnector]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[IdentityVerificationConnector].toInstance(mockIVConnector),
      bind[ApplicationConfig].toInstance(mockApplicationConfig)
    )
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockApplicationConfig)
    reset(mockIVConnector)
    when(mockApplicationConfig.reportAProblemNonJSUrl).thenReturn("/reportAProblem")
    when(mockApplicationConfig.contactFormServiceIdentifier).thenReturn("/id")
  }

  val landingController: LandingController = inject[LandingController]

  implicit val messages: MessagesImpl = MessagesImpl(Lang(Locale.getDefault), inject[MessagesApi])

  "GET /" should {
    "return 200" in {
      val result = landingController.show(fakeRequest)
      status(result) shouldBe OK
    }

    "return HTML" in {
      val result = landingController.show(fakeRequest)
      Helpers.contentType(result) shouldBe Some("text/html")
      charset(result)             shouldBe Some("utf-8")
    }

    "have a start button" in {
      val result = landingController.show(fakeRequest)
      contentAsString(result) should include("Continue")
    }

    "return IVLanding page" in {
      val result = landingController.show(fakeRequest)
      val doc    = Jsoup.parse(contentAsString(result))
      doc.getElementById("landing-signin-heading").text shouldBe messages("nisp.landing.signin.heading")
    }
  }

  "GET /not-authorised" must {
    "show not authorised page" when {
      "journey Id is None" in {
        val result = landingController.showNotAuthorised(None)(fakeRequest)

        status(result)        shouldBe UNAUTHORIZED
        contentAsString(result) should include("We cannot confirm your identity")
      }

      "show generic not_authorised template for FailedMatching journey" in {
        val journeyId = "failed-matching-journey-id"

        when(mockIVConnector.identityVerificationResponse(mockEQ(journeyId))(mockAny())).thenReturn(
          Future.successful(IdentityVerificationSuccessResponse("FailedMatching"))
        )

        val result = landingController.showNotAuthorised(Some(journeyId))(fakeRequest)
        status(result)        shouldBe UNAUTHORIZED
        contentAsString(result) should include("We cannot confirm your identity")
      }

      "show generic not_authorised template for InsufficientEvidence journey" in {
        val journeyId = "insufficient-evidence-journey-id"

        when(mockIVConnector.identityVerificationResponse(mockEQ(journeyId))(mockAny())).thenReturn(
          Future.successful(IdentityVerificationSuccessResponse("InsufficientEvidence"))
        )

        val result = landingController.showNotAuthorised(Some(journeyId))(fakeRequest)
        status(result)        shouldBe UNAUTHORIZED
        contentAsString(result) should include("We cannot confirm your identity")
      }

      "show generic not_authorised template for Incomplete journey" in {
        val journeyId = "incomplete-journey-id"

        when(mockIVConnector.identityVerificationResponse(mockEQ(journeyId))(mockAny())).thenReturn(
          Future.successful(IdentityVerificationSuccessResponse("Incomplete"))
        )

        val result = landingController.showNotAuthorised(Some(journeyId))(fakeRequest)
        status(result)        shouldBe UNAUTHORIZED
        contentAsString(result) should include("We cannot confirm your identity")
      }

      "show generic not_authorised template for PreconditionFailed journey" in {
        val journeyId = "precondition-failed-journey-id"

        when(mockIVConnector.identityVerificationResponse(mockEQ(journeyId))(mockAny())).thenReturn(
          Future.successful(IdentityVerificationSuccessResponse("PreconditionFailed"))
        )

        val result = landingController.showNotAuthorised(Some(journeyId))(fakeRequest)
        status(result)        shouldBe UNAUTHORIZED
        contentAsString(result) should include("We cannot confirm your identity")
      }

      "show generic not_authorised template for UserAborted journey" in {
        val journeyId = "user-aborted-journey-id"

        when(mockIVConnector.identityVerificationResponse(mockEQ(journeyId))(mockAny())).thenReturn(
          Future.successful(IdentityVerificationSuccessResponse("UserAborted"))
        )

        val result = landingController.showNotAuthorised(Some(journeyId))(fakeRequest)
        status(result)        shouldBe UNAUTHORIZED
        contentAsString(result) should include("We cannot confirm your identity")
      }

      "show generic not_authorised template for FailedIV journey" in {
        val journeyId = "user-aborted-journey-id"

        when(mockIVConnector.identityVerificationResponse(mockEQ(journeyId))(mockAny())).thenReturn(
          Future.successful(IdentityVerificationSuccessResponse("FailedIV"))
        )

        val result = landingController.showNotAuthorised(Some(journeyId))(fakeRequest)
        status(result)        shouldBe UNAUTHORIZED
        contentAsString(result) should include("We cannot confirm your identity")
      }
    }

    "show technical_issue template for TechnicalIssue journey" in {
      val journeyId = "technical-issue-journey-id"

      when(mockIVConnector.identityVerificationResponse(mockEQ(journeyId))(mockAny())).thenReturn(
        Future.successful(IdentityVerificationSuccessResponse("TechnicalIssue"))
      )

      val result = landingController.showNotAuthorised(Some(journeyId))(fakeRequest)
      status(result)        shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result) should include("This online service is experiencing technical difficulties.")
    }

    "show locked_out template for LockedOut journey" in {
      val journeyId = "locked-out-journey-id"

      when(mockIVConnector.identityVerificationResponse(mockEQ(journeyId))(mockAny())).thenReturn(
        Future.successful(IdentityVerificationSuccessResponse("LockedOut"))
      )

      val result = landingController.showNotAuthorised(Some(journeyId))(fakeRequest)
      status(result)        shouldBe LOCKED
      contentAsString(result) should include(
        "You have reached the maximum number of attempts to confirm your identity."
      )
    }

    "show timeout template for Timeout journey" in {
      val journeyId = "timeout-journey-id"

      when(mockIVConnector.identityVerificationResponse(mockEQ(journeyId))(mockAny())).thenReturn(
        Future.successful(IdentityVerificationSuccessResponse("Timeout"))
      )

      val result = landingController.showNotAuthorised(Some(journeyId))(fakeRequest)
      status(result)        shouldBe UNAUTHORIZED
      contentAsString(result) should include(
        "Your session has ended because you have not done anything for 15 minutes."
      )
    }

    "show 2FA failure page when no journey ID specified" in {
      val result = landingController.showNotAuthorised(None)(fakeRequest)
      status(result)        shouldBe UNAUTHORIZED
      contentAsString(result) should include("We cannot confirm your identity")
      contentAsString(result) should not include "If you cannot confirm your identity and you have a query you can"
    }
  }

  "GET /cymraeg" must {
    "return 200" in {
      val result = landingController.show(fakeRequestWelsh)
      status(result) shouldBe OK
    }

    "return HTML" in {
      val result = landingController.show(fakeRequestWelsh)
      Helpers.contentType(result) shouldBe Some("text/html")
      charset(result)             shouldBe Some("utf-8")
    }
    "load the landing page in welsh" in {
      when(mockApplicationConfig.isWelshEnabled).thenReturn(true)

      val result = landingController.show(fakeRequestWelsh)

      contentAsString(result) should include("data-journey-click=\"link - click:lang-select:Cymraeg\"")
    }
  }
}
