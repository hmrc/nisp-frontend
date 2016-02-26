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

import org.joda.time.LocalDateTime
import org.scalatestplus.play.OneAppPerSuite
import play.api.http._
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.connectors.IdentityVerificationConnector
import uk.gov.hmrc.nisp.helpers.{MockIdentityVerificationConnector, MockNpsAvailabilityChecker, MockCitizenDetailsService}
import uk.gov.hmrc.nisp.services.{CitizenDetailsService, NpsAvailabilityChecker}
import uk.gov.hmrc.play.test.UnitSpec

class LandingPageControllerSpec extends UnitSpec with OneAppPerSuite {

  val fakeRequest = FakeRequest("GET", "/")

  def testLandingPageController(testNow: LocalDateTime): LandingController = new LandingController {
    override val npsAvailabilityChecker: NpsAvailabilityChecker = new NpsAvailabilityChecker {
      override def now: LocalDateTime = testNow
    }
    override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
    override val applicationConfig: ApplicationConfig = ApplicationConfig
    override val identityVerificationConnector: IdentityVerificationConnector = MockIdentityVerificationConnector
  }

  "GET /" should {
    "return 200" in {
      val result = LandingController.show(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return HTML" in {
      val result = LandingController.show(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    "load the landing page" in {
      val result = LandingController.show(fakeRequest)
      val headingText = Messages("nisp.landing.estimateprovided")
      contentAsString(result) should include (headingText)
    }

    "have a start button" in {
      val result = LandingController.show(fakeRequest)
      val buttonText = Messages("nisp.continue")
      contentAsString(result) should include (s"$buttonText</a>")
    }

    "return IVLanding page" in {
      val result = new LandingController {
        override val npsAvailabilityChecker: NpsAvailabilityChecker = MockNpsAvailabilityChecker
        override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
        override val applicationConfig: ApplicationConfig = new ApplicationConfig {
          override val citizenAuthHost: String = ""
          override val assetsPrefix: String = ""
          override val reportAProblemNonJSUrl: String = ""
          override val ssoUrl: Option[String] = None
          override val identityVerification: Boolean = true
          override val betaFeedbackUnauthenticatedUrl: String = ""
          override val notAuthorisedRedirectUrl: String = ""
          override val contactFrontendPartialBaseUrl: String = ""
          override val govUkFinishedPageUrl: String = ""
          override val showGovUkDonePage: Boolean = false
          override val excludeCopeTab: Boolean = true
          override val analyticsHost: String = ""
          override val betaFeedbackUrl: String = ""
          override val analyticsToken: Option[String] = None
          override val reportAProblemPartialUrl: String = ""
          override val postSignInRedirectUrl: String = ""
          override val ivUpliftUrl: String = "ivuplift"
          override val ggSignInUrl: String = "ggsignin"
          override val twoFactorUrl: String = "twofactor"
        }
        override val identityVerificationConnector: IdentityVerificationConnector = MockIdentityVerificationConnector
      }.show(fakeRequest)
      contentAsString(result) should include ("You need to sign in to use this service")
    }
  }

  "GET /service-unavailable" should {
    "return service unavailable page" in {
      val result = LandingController.showNpsUnavailable(fakeRequest)
      contentAsString(result) should include ("The service is unavailable due to maintenance")
    }
  }

  "GET /signin/verify" should {
    "redirect to verify" in {
      val result = LandingController.verifySignIn(fakeRequest)
      redirectLocation(result) shouldBe Some(ApplicationConfig.verifySignIn)
    }
  }

  "GET /not-authorised" should {
    "show not authorised page" in {
      val result = LandingController.showNotAuthorised(None)(fakeRequest)
      contentAsString(result) should include ("We were unable to confirm your identity")
    }
  }
}
