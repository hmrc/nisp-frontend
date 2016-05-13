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

import org.joda.time.LocalDateTime
import org.scalatest._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.config.wiring.NispCachedStaticHtmlPartialRetriever
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.models.SPAmountModel
import uk.gov.hmrc.nisp.services.{CitizenDetailsService, NpsAvailabilityChecker}
import uk.gov.hmrc.play.frontend.auth.AuthenticationProviderIds
import uk.gov.hmrc.play.http.SessionKeys
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.time.DateTimeUtils.now

class AccountControllerSpec extends UnitSpec with MockitoSugar with BeforeAndAfter with OneAppPerSuite {

  val mockUserNino = TestAccountBuilder.regularNino
  val mockUserNinoExcluded = TestAccountBuilder.excludedNino
  val mockUserNinoNotFound = TestAccountBuilder.blankNino
  val json = s"test/resources/$mockUserNino.json"
  val mockUsername = "mockuser"
  val mockUserId = "/auth/oid/" + mockUsername
  val mockUserIdExcluded = "/auth/oid/mockexcluded"
  val mockUserIdContractedOut = "/auth/oid/mockcontractedout"
  val mockUserIdBlank = "/auth/oid/mockblank"
  val mockUserIdMQP = "/auth/oid/mockmqp"
  val mockUserIdForecastOnly =  "/auth/oid/mockforecastonly"
  val mockUserIdWeak =  "/auth/oid/mockweak"
  val mockUserIdAbroad = "/auth/oid/mockabroad"
  val mockUserIdMQPAbroad = "/auth/oid/mockmqpabroad"

  val ggSignInUrl = "http://localhost:9949/gg/sign-in?continue=http%3A%2F%2Flocalhost%3A9234%2Fcheckmystatepension%2Faccount&accountType=individual"
  val twoFactorUrl = "http://localhost:9949/coafe/two-step-verification/register/?continue=http%3A%2F%2Flocalhost%3A9234%2Fcheckmystatepension%2Faccount&failure=http%3A%2F%2Flocalhost%3A9234%2Fcheckmystatepension%2Fnot-authorised"

  lazy val fakeRequest = FakeRequest()
  private def authenticatedFakeRequest(userId: String = mockUserId) = FakeRequest().withSession(
    SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
    SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
    SessionKeys.userId -> userId,
    SessionKeys.authProvider -> AuthenticationProviderIds.VerifyProviderId
  )

  def testAccountController(testNow: LocalDateTime): AccountController = new MockAccountController {
    override val npsAvailabilityChecker: NpsAvailabilityChecker = new NpsAvailabilityChecker {
      override def now: LocalDateTime = testNow
    }
    override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
    override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
  }

  "Account controller" should {
    "GET /account" should {
      "return 303 when no session" in {
        val result = MockAccountController.show().apply(fakeRequest)
        status(result) shouldBe Status.SEE_OTHER
      }

      "return the forecast only page for a user with a forecast lower than current amount" in {
        val result = MockAccountController.show()(authenticatedFakeRequest(mockUserIdForecastOnly))
        contentAsString(result) should not include ("£80.38")
      }

      "redirect to the GG Login" in {
        val result = MockAccountController.show(fakeRequest)
        redirectLocation(result) shouldBe Some(ggSignInUrl)
      }

      "redirect to Verify with IV disabled" in {
        val controller = new MockAccountController {
          override val npsAvailabilityChecker: NpsAvailabilityChecker = MockNpsAvailabilityChecker
          override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
          override val applicationConfig: ApplicationConfig = new ApplicationConfig {
            override val assetsPrefix: String = ""
            override val reportAProblemNonJSUrl: String = ""
            override val ssoUrl: Option[String] = None
            override val betaFeedbackUnauthenticatedUrl: String = ""
            override val contactFrontendPartialBaseUrl: String = ""
            override val govUkFinishedPageUrl: String = "govukdone"
            override val showGovUkDonePage: Boolean = false
            override val analyticsHost: String = ""
            override val analyticsToken: Option[String] = None
            override val betaFeedbackUrl: String = ""
            override val reportAProblemPartialUrl: String = ""
            override val citizenAuthHost: String = ""
            override val postSignInRedirectUrl: String = ""
            override val notAuthorisedRedirectUrl: String = ""
            override val identityVerification: Boolean = false
            override val ivUpliftUrl: String = "ivuplift"
            override val ggSignInUrl: String = "ggsignin"
            override val twoFactorUrl: String = "twofactor"
            override val pertaxFrontendUrl: String = ""
            override val contactFormServiceIdentifier: String = ""
            override val breadcrumbPartialUrl: String = ""
          }
          override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
        }
        val result = controller.show(fakeRequest)
        redirectLocation(result) shouldBe Some("http://localhost:9029/ida/login")
      }

      "redirect to the GG Login, for session ID NOSESSION" in {
        val result = MockAccountController.show().apply(fakeRequest.withSession(
          SessionKeys.sessionId -> "NOSESSION"
        ))
        redirectLocation(result) shouldBe Some(ggSignInUrl)
      }

      "return 200, create an authenticated session" in {
        val result = MockAccountController.show()(authenticatedFakeRequest())
        contentAsString(result) should include ("Sign out")
      }

      "return timeout error for last request -14 minutes, 59 seconds" in {
        val result = MockAccountController.show()(fakeRequest.withSession(
          SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
          SessionKeys.lastRequestTimestamp -> now.minusMinutes(14).minusSeconds(59).getMillis.toString,
          SessionKeys.userId -> mockUserId,
          SessionKeys.authProvider -> AuthenticationProviderIds.VerifyProviderId
        ))

        redirectLocation(result) should not be Some("/checkmystatepension/timeout")
      }

      "return timeout error for last request -15 minutes" in {
        val result = MockAccountController.show()(fakeRequest.withSession(
          SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
          SessionKeys.lastRequestTimestamp -> now.minusMinutes(15).getMillis.toString,
          SessionKeys.userId -> mockUserId,
          SessionKeys.authProvider -> AuthenticationProviderIds.VerifyProviderId
        ))

        redirectLocation(result) shouldBe Some("/checkmystatepension/timeout")
      }

      "return 200, with exclusion message for excluded user" in {
        val result = MockAccountController.show()(fakeRequest.withSession(
          SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
          SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
          SessionKeys.userId -> mockUserIdExcluded,
          SessionKeys.authProvider -> AuthenticationProviderIds.VerifyProviderId
        ))
        redirectLocation(result) shouldBe Some("/checkmystatepension/exclusion")
      }

      "return 200, account page (1.59.59am)" in {
        val result = testAccountController(new LocalDateTime(2015,6,29,1,59,59)).show()(authenticatedFakeRequest())
        redirectLocation(result) should not be Some("/checkmystatepension/service-unavailable")
      }

      "return redirect, unavailability page for NPS down (2am)" in {
        val result = testAccountController(new LocalDateTime(2015,6,29,2,0,0)).show()(authenticatedFakeRequest())
        redirectLocation(result) shouldBe Some("/checkmystatepension/service-unavailable")
      }

      "return redirect, unavailability page for NPS down (4.59.59am)" in {
        val result = testAccountController(new LocalDateTime(2015,6,29,4,59,59)).show()(authenticatedFakeRequest())
        redirectLocation(result) shouldBe Some("/checkmystatepension/service-unavailable")
      }

      "return 200, account page (5am)" in {
        val result = testAccountController(new LocalDateTime(2015,6,29,5,0,0)).show()(authenticatedFakeRequest())
        redirectLocation(result) should not be Some("/checkmystatepension/service-unavailable")
      }

      "return error for blank user" in {
       intercept[RuntimeException] {
          val result = MockAccountController.show()(authenticatedFakeRequest(mockUserIdBlank))
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }
      "return content about COPE for contracted out (B) user" in {
        val result = MockAccountController.show()(authenticatedFakeRequest(mockUserIdContractedOut))
        contentAsString(result) should include ("How contracting out affects your pension income")
      }

      "return COPE page for contracted out (B) user" in {
        val result = MockAccountController.showCope()(authenticatedFakeRequest(mockUserIdContractedOut))
        contentAsString(result) should include ("You were contracted out ")
      }

      "return abroad message for abroad user" in {
        val result = MockAccountController.show()(authenticatedFakeRequest(mockUserIdAbroad))
        contentAsString(result) should include ("As you are living or working overseas")
      }

      "return abroad message for forecast only user" in {
        val result = MockAccountController.show()(authenticatedFakeRequest(mockUserIdForecastOnly))
        contentAsString(result) should include ("As you are living or working overseas")
        contentAsString(result) should not include ("£80.38")
      }

      "return abroad message for an mqp user instead of standard mqp overseas message" in {
        val result = MockAccountController.show()(authenticatedFakeRequest(mockUserIdMQPAbroad))
        contentAsString(result) should include ("As you are living or working overseas")
        contentAsString(result) should not include ("If you have lived or worked overseas")
      }

      "redirect to account page for non contracted out user" in {
        val result = MockAccountController.showCope()(authenticatedFakeRequest(mockUserIdMQP))
        redirectLocation(result) shouldBe Some("/checkmystatepension/account")
      }
      "return page with MQP messaging for MQP user" in {
        val result = MockAccountController.show()(authenticatedFakeRequest(mockUserIdMQP))
        contentAsString(result) should include ("It may be possible for you to get some State Pension")
      }

      "redirect to 2FA when authentication is not strong" in {
        val result = MockAccountController.show()(fakeRequest.withSession(
          SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
          SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
          SessionKeys.userId -> mockUserIdWeak,
          SessionKeys.authProvider -> AuthenticationProviderIds.VerifyProviderId
        ))
        redirectLocation(result) shouldBe Some(twoFactorUrl)
      }
    }

    "GET /signout" should {
      "redirect to the questionnaire page when govuk done page is disabled" in {
        val controller = new MockAccountController {
          override val npsAvailabilityChecker: NpsAvailabilityChecker = MockNpsAvailabilityChecker
          override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
          override val applicationConfig: ApplicationConfig = new ApplicationConfig {
            override val assetsPrefix: String = ""
            override val reportAProblemNonJSUrl: String = ""
            override val ssoUrl: Option[String] = None
            override val betaFeedbackUnauthenticatedUrl: String = ""
            override val contactFrontendPartialBaseUrl: String = ""
            override val govUkFinishedPageUrl: String = "govukdone"
            override val showGovUkDonePage: Boolean = false
            override val analyticsHost: String = ""
            override val analyticsToken: Option[String] = None
            override val betaFeedbackUrl: String = ""
            override val reportAProblemPartialUrl: String = ""
            override val citizenAuthHost: String = ""
            override val postSignInRedirectUrl: String = ""
            override val notAuthorisedRedirectUrl: String = ""
            override val identityVerification: Boolean = false
            override val ivUpliftUrl: String = "ivuplift"
            override val ggSignInUrl: String = "ggsignin"
            override val twoFactorUrl: String = "twofactor"
            override val pertaxFrontendUrl: String = ""
            override val contactFormServiceIdentifier: String = ""
            override val breadcrumbPartialUrl: String = ""
          }
          override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
        }
        val result = controller.signOut(fakeRequest)
        redirectLocation(result).get shouldBe routes.QuestionnaireController.show().url
      }

      "redirect to the gov.uk done page when govuk done page is enabled" in {
        val controller = new MockAccountController {
          override val npsAvailabilityChecker: NpsAvailabilityChecker = MockNpsAvailabilityChecker
          override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
          override val applicationConfig: ApplicationConfig = new ApplicationConfig {
            override val assetsPrefix: String = ""
            override val reportAProblemNonJSUrl: String = ""
            override val ssoUrl: Option[String] = None
            override val betaFeedbackUnauthenticatedUrl: String = ""
            override val contactFrontendPartialBaseUrl: String = ""
            override val govUkFinishedPageUrl: String = "govukdone"
            override val showGovUkDonePage: Boolean = true
            override val analyticsHost: String = ""
            override val analyticsToken: Option[String] = None
            override val betaFeedbackUrl: String = ""
            override val reportAProblemPartialUrl: String = ""
            override val citizenAuthHost: String = ""
            override val postSignInRedirectUrl: String = ""
            override val notAuthorisedRedirectUrl: String = ""
            override val identityVerification: Boolean = false
            override val ivUpliftUrl: String = "ivuplift"
            override val ggSignInUrl: String = "ggsignin"
            override val twoFactorUrl: String = "twofactor"
            override val pertaxFrontendUrl: String = ""
            override val contactFormServiceIdentifier: String = ""
            override val breadcrumbPartialUrl: String = ""
          }
          override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
        }
        val result = controller.signOut(fakeRequest)
        redirectLocation(result).get shouldBe "govukdone"
      }
    }

    "GET /timeout" should {
      "return the timeout page" in {
        val result = MockAccountController.timeout(fakeRequest)
        contentType(result) shouldBe Some("text/html")
        contentAsString(result).contains("For your security we signed you out because you didn't use the service for 15 minutes or more.")
      }
    }

    "calculate chart widths" should {
      def calculateCharts(currentAmount: BigDecimal, forecastAmount: BigDecimal) =
        MockAccountController.calculateChartWidths(SPAmountModel(currentAmount, 0, 0), SPAmountModel(forecastAmount, 0, 0))

      "current chart is 100 when current amount is higher" in {
        val (currentChart, forecastChart) = calculateCharts(70, 30)
        currentChart.width shouldBe 100
      }

      "forecast chart is 100 when forecast amount is higher" in {
        val (currentChart, forecastChart) = calculateCharts(70, 80)
        forecastChart.width shouldBe 100
      }

      "current chart and forecast chart are 100 when amounts are equal" in {
        val (currentChart, forecastChart) = calculateCharts(70, 70)
        currentChart.width shouldBe 100
        forecastChart.width shouldBe 100
      }

      "current chart is 66 when current amount is 2 and forecast is 3" in {
        val (currentChart, forecastChart) = calculateCharts(2, 3)
        currentChart.width shouldBe 66
      }

      "forecast chart is 30 when forecast amount is 4 and current is 13" in {
        val (currentChart, forecastChart) = calculateCharts(13, 4)
        forecastChart.width shouldBe 30
      }
    }

  }
}
