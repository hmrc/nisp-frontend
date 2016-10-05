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
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.connectors.NispConnector
import uk.gov.hmrc.nisp.controllers.connectors.CustomAuditConnector
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.models.SPAmountModel
import uk.gov.hmrc.nisp.services.CitizenDetailsService
import uk.gov.hmrc.play.frontend.auth.AuthenticationProviderIds
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.SessionKeys
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.time.DateTimeUtils.now

class AccountControllerSpec extends UnitSpec with MockitoSugar with BeforeAndAfter with OneAppPerSuite {

  val mockUserNino = TestAccountBuilder.regularNino
  val mockUserNinoExcluded = TestAccountBuilder.excludedAll
  val mockUserNinoNotFound = TestAccountBuilder.blankNino
  val json = s"test/resources/$mockUserNino.json"
  val mockUsername = "mockuser"
  val mockUserId = "/auth/oid/" + mockUsername
  val mockUserIdExcluded = "/auth/oid/mockexcludedall"
  val mockUserIdContractedOut = "/auth/oid/mockcontractedout"
  val mockUserIdBlank = "/auth/oid/mockblank"
  val mockUserIdMQP = "/auth/oid/mockmqp"
  val mockUserIdForecastOnly =  "/auth/oid/mockforecastonly"
  val mockUserIdWeak =  "/auth/oid/mockweak"
  val mockUserIdAbroad = "/auth/oid/mockabroad"
  val mockUserIdMQPAbroad = "/auth/oid/mockmqpabroad"
  val mockUserIdFillGapsSingle = "/auth/oid/mockfillgapssingle"
  val mockUserIdFillGapsMultiple = "/auth/oid/mockfillgapsmultiple"

  val ggSignInUrl = "http://localhost:9949/gg/sign-in?continue=http%3A%2F%2Flocalhost%3A9234%2Fcheck-your-state-pension%2Faccount&origin=nisp-frontend&accountType=individual"
  val twoFactorUrl = "http://localhost:9949/coafe/two-step-verification/register/?continue=http%3A%2F%2Flocalhost%3A9234%2Fcheck-your-state-pension%2Faccount&failure=http%3A%2F%2Flocalhost%3A9234%2Fcheck-your-state-pension%2Fnot-authorised"

  lazy val fakeRequest = FakeRequest()
  private def authenticatedFakeRequest(userId: String = mockUserId) = FakeRequest().withSession(
    SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
    SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
    SessionKeys.userId -> userId,
    SessionKeys.authProvider -> AuthenticationProviderIds.VerifyProviderId
  )

  def testAccountController(testNow: LocalDateTime): AccountController = new MockAccountController {
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
            override val showFullNI: Boolean = false
            override val futureProofPersonalMax: Boolean = false
            override val copeTable: Boolean = false
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

        redirectLocation(result) should not be Some("/check-your-state-pension/timeout")
      }

      "return timeout error for last request -15 minutes" in {
        val result = MockAccountController.show()(fakeRequest.withSession(
          SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
          SessionKeys.lastRequestTimestamp -> now.minusMinutes(15).getMillis.toString,
          SessionKeys.userId -> mockUserId,
          SessionKeys.authProvider -> AuthenticationProviderIds.VerifyProviderId
        ))

        redirectLocation(result) shouldBe Some("/check-your-state-pension/timeout")
      }

      "return 200, with exclusion message for excluded user" in {
        val result = MockAccountController.show()(fakeRequest.withSession(
          SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
          SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
          SessionKeys.userId -> mockUserIdExcluded,
          SessionKeys.authProvider -> AuthenticationProviderIds.VerifyProviderId
        ))
        redirectLocation(result) shouldBe Some("/check-your-state-pension/exclusion")
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
        contentAsString(result) should include ("You were contracted out")
      }

      "show COPE table when the user is contracted out and the copeTable feature flag is set to true" in {
        val controller = new MockAccountController {
          override val citizenDetailsService = MockCitizenDetailsService
          override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
          override val applicationConfig: ApplicationConfig = new ApplicationConfig {
            override val assetsPrefix: String = ""
            override val reportAProblemNonJSUrl: String = ""
            override val ssoUrl: Option[String] = None
            override val betaFeedbackUnauthenticatedUrl: String = ""
            override val contactFrontendPartialBaseUrl: String = ""
            override val analyticsHost: String = ""
            override val analyticsToken: Option[String] = None
            override val betaFeedbackUrl: String = ""
            override val reportAProblemPartialUrl: String = ""
            override val showGovUkDonePage: Boolean = true
            override val govUkFinishedPageUrl: String = "govukdone"
            override val citizenAuthHost: String = ""
            override val postSignInRedirectUrl: String = ""
            override val notAuthorisedRedirectUrl: String = ""
            override val identityVerification: Boolean = true
            override val ivUpliftUrl: String = "ivuplift"
            override val ggSignInUrl: String = "ggsignin"
            override val twoFactorUrl: String = "twofactor"
            override val pertaxFrontendUrl: String = ""
            override val contactFormServiceIdentifier: String = ""
            override val breadcrumbPartialUrl: String = ""
            override val showFullNI: Boolean = false
            override val futureProofPersonalMax: Boolean = false
            override val copeTable: Boolean = true
          }
        }
        val result = controller.showCope()(authenticatedFakeRequest(mockUserIdContractedOut))
        contentAsString(result) should include ("<tr><td>April 1980</td><td>October 1997</td>")

      }

      "do not show COPE table when the user is contracted out the copeTable feature flag is set to false" in {
        val controller = new MockAccountController {
          override val citizenDetailsService = MockCitizenDetailsService
          override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
          override val applicationConfig: ApplicationConfig = new ApplicationConfig {
            override val assetsPrefix: String = ""
            override val reportAProblemNonJSUrl: String = ""
            override val ssoUrl: Option[String] = None
            override val betaFeedbackUnauthenticatedUrl: String = ""
            override val contactFrontendPartialBaseUrl: String = ""
            override val analyticsHost: String = ""
            override val analyticsToken: Option[String] = None
            override val betaFeedbackUrl: String = ""
            override val reportAProblemPartialUrl: String = ""
            override val showGovUkDonePage: Boolean = true
            override val govUkFinishedPageUrl: String = "govukdone"
            override val citizenAuthHost: String = ""
            override val postSignInRedirectUrl: String = ""
            override val notAuthorisedRedirectUrl: String = ""
            override val identityVerification: Boolean = true
            override val ivUpliftUrl: String = "ivuplift"
            override val ggSignInUrl: String = "ggsignin"
            override val twoFactorUrl: String = "twofactor"
            override val pertaxFrontendUrl: String = ""
            override val contactFormServiceIdentifier: String = ""
            override val breadcrumbPartialUrl: String = ""
            override val showFullNI: Boolean = false
            override val futureProofPersonalMax: Boolean = false
            override val copeTable: Boolean = false
          }
        }
        val result = controller.showCope()(authenticatedFakeRequest(mockUserIdContractedOut))
        contentAsString(result) shouldNot include ("<tr><td>April 1980</td><td>October 1997</td>")
      }

      "return abroad message for abroad user" in {
        val result = MockAccountController.show()(authenticatedFakeRequest(mockUserIdAbroad))
        contentAsString(result) should include ("As you are living or working overseas")
      }

      "return abroad message for forecast only user" in {
        val result = MockAccountController.show()(authenticatedFakeRequest(mockUserIdForecastOnly))
        contentAsString(result) should include ("As you are living or working overseas")
        contentAsString(result) should not include "£80.38"
      }

      "return abroad message for an mqp user instead of standard mqp overseas message" in {
        val result = MockAccountController.show()(authenticatedFakeRequest(mockUserIdMQPAbroad))
        contentAsString(result) should include ("As you are living or working overseas")
        contentAsString(result) should not include "If you have lived or worked overseas"
      }

      "redirect to account page for non contracted out user" in {
        val result = MockAccountController.showCope()(authenticatedFakeRequest(mockUserIdMQP))
        redirectLocation(result) shouldBe Some("/check-your-state-pension/account")
      }
      "return page with MQP messaging for MQP user" in {
        val result = MockAccountController.show()(authenticatedFakeRequest(mockUserIdMQP))
        contentAsString(result) should include ("10 years needed on your National Insurance record to get any State Pension")
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
            override val showFullNI: Boolean = false
            override val futureProofPersonalMax: Boolean = false
            override val copeTable: Boolean = false
          }
          override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
        }
        val result = controller.signOut(fakeRequest)
        redirectLocation(result).get shouldBe routes.QuestionnaireController.show().url
      }

      "redirect to the gov.uk done page when govuk done page is enabled" in {
        val controller = new MockAccountController {
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
            override val showFullNI: Boolean = false
            override val futureProofPersonalMax: Boolean = false
            override val copeTable: Boolean = false
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
      def calculateCharts(currentAmount: BigDecimal, forecastAmount: BigDecimal, personalMax: BigDecimal) =
        MockAccountController.calculateChartWidths(SPAmountModel(currentAmount, 0, 0), SPAmountModel(forecastAmount, 0, 0), SPAmountModel(personalMax, 0, 0))

      "current chart is 100 when current amount is higher" in {
        val (currentChart, forecastChart, personalMaxChart) = calculateCharts(70, 30, 0)
        currentChart.width shouldBe 100
      }

      "forecast chart is 100 when forecast amount is higher" in {
        val (currentChart, forecastChart, personalMaxChart) = calculateCharts(70, 80, 80)
        forecastChart.width shouldBe 100
        personalMaxChart.width shouldBe 100
      }

      "current chart and forecast chart are 100 when amounts are equal" in {
        val (currentChart, forecastChart, personalMaxChart) = calculateCharts(70, 70, 70)
        currentChart.width shouldBe 100
        forecastChart.width shouldBe 100
        personalMaxChart.width shouldBe 100
      }

      "current chart is 66 when current amount is 2 and forecast is 3" in {
        val (currentChart, forecastChart, personalMaxChart) = calculateCharts(2, 3, 4)
        currentChart.width shouldBe 50
        forecastChart.width shouldBe 75
        personalMaxChart.width shouldBe 100
      }

      "forecast chart is 30 when forecast amount is 4 and current is 13" in {
        val (currentChart, forecastChart, personalMaxChart) = calculateCharts(13, 4, 20)
        forecastChart.width shouldBe 31
        currentChart.width shouldBe 65
        personalMaxChart.width shouldBe 100

      }
    }

    "when there is a Fill Gaps Scenario" when {
      "the future config is set to off" should {
        "show year information when there is multiple years" in {
          val result = MockAccountController.show()(authenticatedFakeRequest(mockUserIdFillGapsMultiple))
          contentAsString(result) should include ("You have years on your National Insurance record where you did not contribute enough.")
          contentAsString(result) should include ("filling years can improve your forecast")
          contentAsString(result) should include ("you only need to fill 7 years to get the most you can")
          contentAsString(result) should include ("The most you can get by filling any 7 years in your record is")
        }
        "show specific text when is only one payable gap" in {
          val result = MockAccountController.show()(authenticatedFakeRequest(mockUserIdFillGapsSingle))
          contentAsString(result) should include ("You have a year on your National Insurance record where you did not contribute enough. You only need to fill this year to get the most you can.")
          contentAsString(result) should include ("The most you can get by filling this year in your record is")
        }
      }

      "the future proof config is set to true" should {
        val controller = new MockAccountController {
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
            override val showFullNI: Boolean = false
            override val futureProofPersonalMax: Boolean = true
            override val copeTable: Boolean = false
          }
          override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
        }
        "show less text when there multiple years" in {
          val result = controller.show()(authenticatedFakeRequest(mockUserIdFillGapsMultiple))
          contentAsString(result) should include ("You have years on your National Insurance record where you did not contribute enough. Filling years can improve your forecast.")
          contentAsString(result) should include ("The most you can get is")
        }
        "show ordinary text when is only one payable gap" in {
          val result = controller.show()(authenticatedFakeRequest(mockUserIdFillGapsSingle))
          contentAsString(result) should include ("You have a year on your National Insurance record where you did not contribute enough. You only need to fill this year to get the most you can.")
          contentAsString(result) should include ("The most you can get by filling this year in your record is")
        }
      }
    }
  }
}
