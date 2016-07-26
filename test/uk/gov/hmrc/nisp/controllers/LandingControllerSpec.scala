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

import java.util.UUID

import org.scalatestplus.play.OneAppPerSuite
import play.api.http._
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.connectors.IdentityVerificationConnector
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.services.{CitizenDetailsService, NpsAvailabilityChecker}
import uk.gov.hmrc.play.frontend.auth.AuthenticationProviderIds
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.SessionKeys
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.time.DateTimeUtils._

class LandingControllerSpec extends UnitSpec with OneAppPerSuite {

  val fakeRequest = FakeRequest("GET", "/")

  def testLandingController(identityVerificationEnabled: Boolean = true): LandingController = new LandingController {
    override val npsAvailabilityChecker: NpsAvailabilityChecker = MockNpsAvailabilityChecker
    override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
    override val applicationConfig: ApplicationConfig = new ApplicationConfig {
      override val ggSignInUrl: String = ""
      override val citizenAuthHost: String = ""
      override val twoFactorUrl: String = ""
      override val assetsPrefix: String = ""
      override val reportAProblemNonJSUrl: String = ""
      override val ssoUrl: Option[String] = None
      override val identityVerification: Boolean = identityVerificationEnabled
      override val betaFeedbackUnauthenticatedUrl: String = ""
      override val notAuthorisedRedirectUrl: String = ""
      override val contactFrontendPartialBaseUrl: String = ""
      override val govUkFinishedPageUrl: String = ""
      override val showGovUkDonePage: Boolean = true
      override val analyticsHost: String = ""
      override val betaFeedbackUrl: String = ""
      override val analyticsToken: Option[String] = None
      override val reportAProblemPartialUrl: String = ""
      override val postSignInRedirectUrl: String = ""
      override val ivUpliftUrl: String = ""
      override val pertaxFrontendUrl: String = ""
      override val contactFormServiceIdentifier: String = ""
      override val breadcrumbPartialUrl: String = ""
      override val showGovUkFullYearPage: Boolean = false
    }
    override val identityVerificationConnector: IdentityVerificationConnector = MockIdentityVerificationConnector

    override protected def authConnector: AuthConnector = MockAuthConnector

    override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
  }

  "GET /" should {
    "return 200" in {
      val result = testLandingController().show(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return HTML" in {
      val result = testLandingController().show(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    "load the landing page" in {
      val result = testLandingController().show(fakeRequest)
      val headingText = Messages("nisp.landing.estimateprovided")
      contentAsString(result) should include (headingText)
    }

    "have a start button" in {
      val result = testLandingController().show(fakeRequest)
      val buttonText = Messages("nisp.continue")
      contentAsString(result) should include (s"$buttonText</a>")
    }

    "return IVLanding page" in {
      val result = testLandingController().show(fakeRequest)
      contentAsString(result) should include ("You need to confirm your identity")
    }

    "return non-IV landing page when switched off" in {
      val result = testLandingController(identityVerificationEnabled = false).show(fakeRequest)
      contentAsString(result) should include ("You can use this service if you&rsquo;re")
    }
  }

  "GET /service-unavailable" should {
    "return service unavailable page" in {
      val result = testLandingController().showNpsUnavailable(fakeRequest)
      contentAsString(result) should include ("The service is unavailable due to maintenance")
    }
  }

  "GET /signin/verify" should {
    "redirect to verify" in {
      val result = testLandingController().verifySignIn(fakeRequest)
      redirectLocation(result) shouldBe Some("http://localhost:9029/ida/login")
    }

    "redirect to account page when signed in" in {
      val result = testLandingController().verifySignIn(FakeRequest().withSession(
        SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
        SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
        SessionKeys.userId -> "/auth/oid/mockuser"
      ))
      redirectLocation(result) shouldBe Some("/check-your-state-pension/account")
    }
  }

  "GET /not-authorised" should {
    "show not authorised page" in {
      val result = testLandingController().showNotAuthorised(None)(fakeRequest)
      contentAsString(result) should include ("We were unable to confirm your identity")
    }

    "show generic not_authorised template for FailedMatching journey" in {
      val result = testLandingController().showNotAuthorised(Some("failed-matching-journey-id"))(fakeRequest)
      contentAsString(result) should include ("We were unable to confirm your identity")
    }

    "show generic not_authorised template for InsufficientEvidence journey" in {
      val result = testLandingController().showNotAuthorised(Some("insufficient-evidence-journey-id"))(fakeRequest)
      contentAsString(result) should include ("We were unable to confirm your identity")
    }

    "show generic not_authorised template for Incomplete journey" in {
      val result = testLandingController().showNotAuthorised(Some("incomplete-journey-id"))(fakeRequest)
      contentAsString(result) should include ("We were unable to confirm your identity")
    }

    "show generic not_authorised template for PreconditionFailed journey" in {
      val result = testLandingController().showNotAuthorised(Some("precondition-failed-journey-id"))(fakeRequest)
      contentAsString(result) should include ("We were unable to confirm your identity")
    }

    "show generic not_authorised template for UserAborted journey" in {
      val result = testLandingController().showNotAuthorised(Some("user-aborted-journey-id"))(fakeRequest)
      contentAsString(result) should include ("We were unable to confirm your identity")
    }

    "show technical_issue template for TechnicalIssue journey" in {
      val result = testLandingController().showNotAuthorised(Some("technical-issue-journey-id"))(fakeRequest)
      contentAsString(result) should include ("This online service is experiencing technical difficulties.")
    }

    "show locked_out template for LockedOut journey" in {
      val result = testLandingController().showNotAuthorised(Some("locked-out-journey-id"))(fakeRequest)
      contentAsString(result) should include ("You have reached the maximum number of attempts to confirm your identity.")
    }

    "show timeout template for Timeout journey" in {
      val result = testLandingController().showNotAuthorised(Some("timeout-journey-id"))(fakeRequest)
      contentAsString(result) should include ("Your session has ended because you have not done anything for 15 minutes.")
    }

    "show 2FA failure page when no journey ID specified" in {
      val result = testLandingController().showNotAuthorised(None)(fakeRequest)
      contentAsString(result) should include ("We were unable to confirm your identity")
      contentAsString(result) should not include "If you cannot confirm your identity and you have a query you can"
    }
  }
}
