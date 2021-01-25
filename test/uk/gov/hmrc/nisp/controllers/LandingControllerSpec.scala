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

import java.util.UUID

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http._
import play.api.i18n.Lang
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.{AuthConnector, MissingBearerToken}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HttpClient, SessionKeys}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.connectors.IdentityVerificationConnector
import uk.gov.hmrc.nisp.controllers.auth.{AuthAction, VerifyAuthActionImpl}
import uk.gov.hmrc.nisp.helpers.{FakeTemplateRenderer, _}
import uk.gov.hmrc.nisp.views.html.{identity_verification_landing, landing}
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever, HeaderCarrierForPartialsConverter}
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.time.DateTimeUtils._

import scala.concurrent.Future

class LandingControllerSpec extends PlaySpec with MockitoSugar with GuiceOneAppPerSuite {

  implicit val fakeRequest = FakeRequest("GET", "/")
  val fakeRequestWelsh = FakeRequest("GET", "/cymraeg")
  val mockApplicationConfig = mock[ApplicationConfig]

//  private implicit val retriever = FakeCachedStaticHtmlPartialRetriever
//  implicit val formPartialRetriever: uk.gov.hmrc.play.partials.FormPartialRetriever = NispFormPartialRetriever
//  implicit val templateRenderer: TemplateRenderer = FakeTemplateRenderer

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[ApplicationConfig].toInstance(mockApplicationConfig),
      bind[TemplateRenderer].toInstance(FakeTemplateRenderer),
      bind[FormPartialRetriever].toInstance(FakePartialRetriever),
      bind[CachedStaticHtmlPartialRetriever].toInstance(FakeCachedStaticHtmlPartialRetriever),
      bind[HeaderCarrierForPartialsConverter].toInstance(FakeNispHeaderCarrierForPartialsConverter)
    ).build()

//  def testLandingControllerByNino(nino: Nino): LandingController = new LandingController {
//
//    override val applicationConfig: ApplicationConfig = mock[ApplicationConfig]
//
//    override val identityVerificationConnector: IdentityVerificationConnector = MockIdentityVerificationConnector
//
//    override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = retriever
//
//    override implicit val templateRenderer: TemplateRenderer = FakeTemplateRenderer
//
//    override val verifyAuthAction: AuthAction = new MockAuthAction(nino)
//  }

  val brokenAuthConnector = mock[AuthConnector]

//  val brokenLandingController: LandingController = new LandingController {
//
//    override val applicationConfig: ApplicationConfig = mock[ApplicationConfig]
//
//    override val identityVerificationConnector: IdentityVerificationConnector = MockIdentityVerificationConnector
//
//    override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = retriever
//
//    override implicit val templateRenderer: TemplateRenderer = FakeTemplateRenderer
//
//    override val verifyAuthAction: AuthAction = new VerifyAuthActionImpl(
//      authConnector = brokenAuthConnector,
//      cds = MockCitizenDetailsService)
//  }

  //lazy val testLandingController = testLandingControllerByNino(TestAccountBuilder.regularNino)
  val landingController = app.asInstanceOf[LandingController]

  "GET /" should {
    "return 200" in {
      val result = landingController.show(fakeRequest)
      status(result) mustBe Status.OK
    }

    "return HTML" in {
      val result = testLandingController.show(fakeRequest)
      Helpers.contentType(result) mustBe Some("text/html")
      charset(result) mustBe Some("utf-8")
    }

    "load the landing page" in {
      val result = testLandingController.show(fakeRequest)
      contentAsString(result) must include("Your State Pension forecast is provided for your information only and the " +
        "service does not offer financial advice. When planning for your retirement, you should seek professional advice.")
    }

    "have a start button" in {
      val result = testLandingController.show(fakeRequest)
      contentAsString(result) must include("Continue")
    }

    "return IVLanding page" in {
      when(testLandingController.applicationConfig.isWelshEnabled).thenReturn(false)
      val result = testLandingController.show(fakeRequest)
      contentAsString(result) must include(contentAsString(landing()))
    }

    "return non-IV landing page when switched on" in {

      when(testLandingController.applicationConfig.isWelshEnabled).thenReturn(false)
      when(testLandingController.applicationConfig.identityVerification).thenReturn(true)
      val result = testLandingController.show(fakeRequest)
      contentAsString(result) must include(contentAsString(identity_verification_landing()))
    }
  }

  "GET /signin/verify" must {
    "redirect to verify" in {
      when(brokenAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(MissingBearerToken("Missing Bearer Token!")))
      val result = brokenLandingController.verifySignIn(fakeRequest)
      redirectLocation(result) mustBe Some("http://localhost:9949/auth-login-stub/verify-sign-in?continue=http%3A%2F%2Flocalhost%3A9234%2Fcheck-your-state-pension%2Faccount")
    }

    "redirect to account page when signed in" in {
      val result = testLandingController.verifySignIn(FakeRequest().withSession(
        SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
        SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
        SessionKeys.userId -> "/auth/oid/mockuser"
      ))
      redirectLocation(result) mustBe Some("/check-your-state-pension/account")
    }
  }

  "GET /not-authorised" must {
    "show not authorised page" in {
      val result = testLandingController.showNotAuthorised(None)(fakeRequest)
      contentAsString(result) must include("We cannot confirm your identity")
    }

    "show generic not_authorised template for FailedMatching journey" in {
      val result = testLandingController.showNotAuthorised(Some("failed-matching-journey-id"))(fakeRequest)
      contentAsString(result) must include("We cannot confirm your identity")
    }

    "show generic not_authorised template for InsufficientEvidence journey" in {
      val result = testLandingController.showNotAuthorised(Some("insufficient-evidence-journey-id"))(fakeRequest)
      contentAsString(result) must include("We cannot confirm your identity")
    }

    "show generic not_authorised template for Incomplete journey" in {
      val result = testLandingController.showNotAuthorised(Some("incomplete-journey-id"))(fakeRequest)
      contentAsString(result) must include("We cannot confirm your identity")
    }

    "show generic not_authorised template for PreconditionFailed journey" in {
      val result = testLandingController.showNotAuthorised(Some("precondition-failed-journey-id"))(fakeRequest)
      contentAsString(result) must include("We cannot confirm your identity")
    }

    "show generic not_authorised template for UserAborted journey" in {
      val result = testLandingController.showNotAuthorised(Some("user-aborted-journey-id"))(fakeRequest)
      contentAsString(result) must include("We cannot confirm your identity")
    }

    "show technical_issue template for TechnicalIssue journey" in {
      val result = testLandingController.showNotAuthorised(Some("technical-issue-journey-id"))(fakeRequest)
      contentAsString(result) must include("This online service is experiencing technical difficulties.")
    }

    "show locked_out template for LockedOut journey" in {
      val result = testLandingController.showNotAuthorised(Some("locked-out-journey-id"))(fakeRequest)
      contentAsString(result) must include("You have reached the maximum number of attempts to confirm your identity.")
    }

    "show timeout template for Timeout journey" in {
      val result = testLandingController.showNotAuthorised(Some("timeout-journey-id"))(fakeRequest)
      contentAsString(result) must include("Your session has ended because you have not done anything for 15 minutes.")
    }

    "show 2FA failure page when no journey ID specified" in {
      val result = testLandingController.showNotAuthorised(None)(fakeRequest)
      contentAsString(result) must include("We cannot confirm your identity")
      contentAsString(result) must not include "If you cannot confirm your identity and you have a query you can"
    }
  }

  "GET /cymraeg" must {
    implicit val lang = Lang("cy")
    "return 200" in {
      val result = testLandingController.show(fakeRequestWelsh)
      status(result) mustBe Status.OK
    }

    "return HTML" in {
      val result = testLandingController.show(fakeRequestWelsh)
      Helpers.contentType(result) mustBe Some("text/html")
      charset(result) mustBe Some("utf-8")
    }
    "load the landing page in welsh" in {
      val result = testLandingController.show(fakeRequestWelsh)
      contentAsString(result) must include("data-journey-click=\"checkmystatepension:language: cy\"")
    }
  }
}
