/*
 * Copyright 2017 HM Revenue & Customs
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

import org.joda.time.{LocalDate, LocalDateTime}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.models.StatePensionAmountRegular
import uk.gov.hmrc.nisp.services.{CitizenDetailsService, NationalInsuranceService}
import uk.gov.hmrc.play.frontend.auth.AuthenticationProviderIds
import uk.gov.hmrc.play.http.SessionKeys
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.time.DateTimeUtils.now

class StatePensionControllerSpec extends UnitSpec with MockitoSugar with OneAppPerSuite {

  val mockUsername = "mockuser"
  val mockUserId = "/auth/oid/" + mockUsername
  val mockUserIdExcluded = "/auth/oid/mockexcludedall"
  val mockUserIdContractedOut = "/auth/oid/mockcontractedout"
  val mockUserIdBlank = "/auth/oid/mockblank"
  val mockUserIdMQP = "/auth/oid/mockmqp"
  val mockUserIdForecastOnly = "/auth/oid/mockforecastonly"
  val mockUserIdWeak = "/auth/oid/mockweak"
  val mockUserIdAbroad = "/auth/oid/mockabroad"
  val mockUserIdMQPAbroad = "/auth/oid/mockmqpabroad"
  val mockUserIdMwrre = "/auth/oid/mockmwrre"

  val mockUserIdFillGapsSingle = "/auth/oid/mockfillgapssingle"
  val mockUserIdFillGapsMultiple = "/auth/oid/mockfillgapsmultiple"
  val mockUserIdBackendNotFound = "/auth/oid/mockbackendnotfound"

  val ggSignInUrl = "http://localhost:9949/auth-login-stub/gg-sign-in?continue=http%3A%2F%2Flocalhost%3A9234%2Fcheck-your-state-pension%2Faccount&origin=nisp-frontend&accountType=individual"
  val twoFactorUrl = "http://localhost:9949/coafe/two-step-verification/register/?continue=http%3A%2F%2Flocalhost%3A9234%2Fcheck-your-state-pension%2Faccount&failure=http%3A%2F%2Flocalhost%3A9234%2Fcheck-your-state-pension%2Fnot-authorised"

  lazy val fakeRequest = FakeRequest()

  private def authenticatedFakeRequest(userId: String = mockUserId) = FakeRequest().withSession(
    SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
    SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
    SessionKeys.userId -> userId,
    SessionKeys.authProvider -> AuthenticationProviderIds.VerifyProviderId
  )

  def testAccountController(testNow: LocalDateTime): StatePensionController = new MockStatePensionController {
    override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
    override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
    override val nationalInsuranceService: NationalInsuranceService = MockNationalInsuranceServiceViaNisp
  }

  "State Pension controller" should {

    "GET /statepension" should {
      "return 303 when no session" ignore {
        val result = MockStatePensionController.show().apply(fakeRequest)
        status(result) shouldBe Status.SEE_OTHER
      }

      "return 500 when backend 404" ignore {
        val result = MockStatePensionController.show()(authenticatedFakeRequest(mockUserIdBackendNotFound))
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }

      "return the forecast only page for a user with a forecast lower than current amount" ignore {
        val result = MockStatePensionController.show()(authenticatedFakeRequest(mockUserIdForecastOnly))
        contentAsString(result) should not include ("£80.38")
      }

      "redirect to the GG Login" ignore {
        val result = MockStatePensionController.show(fakeRequest)

        redirectLocation(result) shouldBe Some(ggSignInUrl)
      }

      "redirect to Verify with IV disabled" ignore {
        val controller = new MockStatePensionController {
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
            override val verifySignIn: String = ""
            override val verifySignInContinue: Boolean = false
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
            override val useStatePensionAPI: Boolean = true
            override val useNationalInsuranceAPI: Boolean = true
            override val isWelshEnabled = false
          }
          override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
        }

        val result = controller.show(fakeRequest)
        redirectLocation(result) shouldBe Some("http://localhost:9949/auth-login-stub/verify-sign-in?continue=http%3A%2F%2Flocalhost%3A9234%2Fcheck-your-state-pension%2Faccount")
      }

      "redirect to the GG Login, for session ID NOSESSION" ignore {
        val result = MockStatePensionController.show().apply(fakeRequest.withSession(
          SessionKeys.sessionId -> "NOSESSION"
        ))
        redirectLocation(result) shouldBe Some(ggSignInUrl)
      }

      "return 200, create an authenticated session" ignore {
        val result = MockStatePensionController.show()(authenticatedFakeRequest())
        contentAsString(result) should include("Sign out")
      }

      "return timeout error for last request -14 minutes, 59 seconds" ignore {
        val result = MockStatePensionController.show()(fakeRequest.withSession(
          SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
          SessionKeys.lastRequestTimestamp -> now.minusMinutes(14).minusSeconds(59).getMillis.toString,
          SessionKeys.userId -> mockUserId,
          SessionKeys.authProvider -> AuthenticationProviderIds.VerifyProviderId
        ))

        redirectLocation(result) should not be Some("/check-your-state-pension/timeout")
      }

      "return timeout error for last request -15 minutes" ignore {
        val result = MockStatePensionController.show()(fakeRequest.withSession(
          SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
          SessionKeys.lastRequestTimestamp -> now.minusMinutes(15).getMillis.toString,
          SessionKeys.userId -> mockUserId,
          SessionKeys.authProvider -> AuthenticationProviderIds.VerifyProviderId
        ))

        redirectLocation(result) shouldBe Some("/check-your-state-pension/timeout")
      }

      "return 200, with exclusion message for excluded user" ignore {
        val result = MockStatePensionController.show()(fakeRequest.withSession(
          SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
          SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
          SessionKeys.userId -> mockUserIdExcluded,
          SessionKeys.authProvider -> AuthenticationProviderIds.VerifyProviderId
        ))
        redirectLocation(result) shouldBe Some("/check-your-state-pension/exclusion")
      }

      "return error for blank user" ignore {
        val result = MockStatePensionController.show()(authenticatedFakeRequest(mockUserIdBlank))
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }

      "return content about COPE for contracted out (B) user" ignore {
        val result = MockStatePensionController.show()(authenticatedFakeRequest(mockUserIdContractedOut))
        contentAsString(result) should include("You’ve been in a contracted-out pension scheme")
      }

      "return COPE page for contracted out (B) user" ignore {
        val result = MockStatePensionController.showCope()(authenticatedFakeRequest(mockUserIdContractedOut))
        contentAsString(result) should include("You were contracted out")
      }

      "return abroad message for abroad user" ignore {
        val result = MockStatePensionController.show()(authenticatedFakeRequest(mockUserIdAbroad))
        contentAsString(result) should include("As you are living or working overseas")
      }

      "return RRE message for abroad user" in {
        val result = MockStatePensionController.show()(authenticatedFakeRequest(mockUserIdMwrre))
        contentAsString(result) should include("As you are living or working overseas")
      }

      "return abroad message for forecast only user" ignore {
        val result = MockStatePensionController.show()(authenticatedFakeRequest(mockUserIdForecastOnly))
        contentAsString(result) should include("As you are living or working overseas")
        contentAsString(result) should not include "£80.38"
      }

      "return abroad message for an mqp user instead of standard mqp overseas message" ignore {
        val result = MockStatePensionController.show()(authenticatedFakeRequest(mockUserIdMQPAbroad))
        contentAsString(result) should include("As you are living or working overseas")
        contentAsString(result) should not include "If you have lived or worked overseas"
      }

      "return mwree message for an RRE user instead of standard mqp overseas message" in {
        val result = MockStatePensionController.show()(authenticatedFakeRequest(mockUserIdMwrre))
        contentAsString(result) should include("As you are living or working overseas")
        contentAsString(result) should not include "If you have lived or worked overseas"
      }

      "redirect to statepension page for non contracted out user" ignore {
        val result = MockStatePensionController.showCope()(authenticatedFakeRequest(mockUserIdMQP))
        redirectLocation(result) shouldBe Some("/check-your-state-pension/account")
      }
      "return page with MQP messaging for MQP user" ignore {
        val result = MockStatePensionController.show()(authenticatedFakeRequest(mockUserIdMQP))
        contentAsString(result) should include("10 years needed on your National Insurance record to get any State Pension")
      }

      "redirect to 2FA when authentication is not strong" ignore {
        val result = MockStatePensionController.show()(fakeRequest.withSession(
          SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
          SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
          SessionKeys.userId -> mockUserIdWeak,
          SessionKeys.authProvider -> AuthenticationProviderIds.VerifyProviderId
        ))
        redirectLocation(result) shouldBe Some(twoFactorUrl)
      }

      "display the correct Google Analytics code" should {
        "dimension7 should be the scenario" ignore {
          val result = MockStatePensionController.show()(authenticatedFakeRequest(mockUserId))
          contentAsString(result) contains "'dimension7': 'FillGaps'" shouldBe true
        }

        "dimension8 should be the forecast weekly amount" ignore {
          val result = MockStatePensionController.show()(authenticatedFakeRequest(mockUserId))
          contentAsString(result) contains "'dimension8': '146.76'" shouldBe true
        }

        "dimension10 should be the number of qualifying years" ignore {
          val result = MockStatePensionController.show()(authenticatedFakeRequest(mockUserId))
          contentAsString(result) contains "'dimension10': '30'" shouldBe true
        }

        "dimension11 should be the number of gaps" ignore {
          val result = MockStatePensionController.show()(authenticatedFakeRequest(mockUserId))
          contentAsString(result) contains "'dimension11': '10'" shouldBe true
        }

        "dimension12 should be the number of gaps payable" ignore {
          val result = MockStatePensionController.show()(authenticatedFakeRequest(mockUserId))
          contentAsString(result) contains "'dimension12': '4'" shouldBe true
        }

        "dimension13 should be years until state pension age" ignore {
          val result = MockStatePensionController.show()(authenticatedFakeRequest(mockUserId))
          contentAsString(result) contains "'dimension13': '3'" shouldBe true
        }

        "dimension14 should be if the user is contracted out" ignore {
          val result = MockStatePensionController.show()(authenticatedFakeRequest(mockUserId))
          contentAsString(result) contains "'dimension14': 'false'" shouldBe true
        }

        "dimension15 should be the pension age" ignore {
          val result = MockStatePensionController.show()(authenticatedFakeRequest(mockUserId))
          contentAsString(result) contains "'dimension15': '64'" shouldBe true
        }

        "dimension16 should be the COPE amount" ignore {
          val result = MockStatePensionController.show()(authenticatedFakeRequest(mockUserId))
          contentAsString(result) contains "'dimension16': '0'" shouldBe true
        }

        "dimension22 should be the old auth provider" ignore {
          val result = MockStatePensionController.show()(authenticatedFakeRequest(mockUserId))
          contentAsString(result) contains "'dimension22': 'verify'" shouldBe true
        }

        "dimension38 should be the auth provider" ignore {
          val result = MockStatePensionController.show()(authenticatedFakeRequest(mockUserId))
          contentAsString(result) contains "'dimension38': 'IDA'" shouldBe true
        }

        "dimension39 should be the confidence level" ignore {
          val result = MockStatePensionController.show()(authenticatedFakeRequest(mockUserId))
          contentAsString(result) contains "'dimension39': '500'" shouldBe true
        }

        "dimension40 should be the customer age" ignore {
          val result = MockStatePensionController.show()(authenticatedFakeRequest(mockUserId))
          contentAsString(result) contains "'dimension40': '63'" shouldBe true
        }

        "dimension41 should be the sex" ignore {
          val result = MockStatePensionController.show()(authenticatedFakeRequest(mockUserId))
          contentAsString(result) contains "'dimension41': 'M'" shouldBe true
        }

        "metric should be 1" ignore {
          val result = MockStatePensionController.show()(authenticatedFakeRequest(mockUserId))
          contentAsString(result) contains "'metric5': 1" shouldBe true
        }
      }
    }

    "GET /signout" should {
      "redirect to the questionnaire page when govuk done page is disabled" ignore {
        val controller = new MockStatePensionController {
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
            override val verifySignIn: String = ""
            override val verifySignInContinue: Boolean = false
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
            override val useStatePensionAPI: Boolean = true
            override val useNationalInsuranceAPI: Boolean = true
            override val isWelshEnabled = false
          }
          override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
        }
        val result = controller.signOut(fakeRequest)
        redirectLocation(result).get shouldBe routes.QuestionnaireController.show().url
      }

      "redirect to the gov.uk done page when govuk done page is enabled" ignore {
        val controller = new MockStatePensionController {
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
            override val verifySignIn: String = ""
            override val verifySignInContinue: Boolean = false
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
            override val useStatePensionAPI: Boolean = true
            override val useNationalInsuranceAPI: Boolean = true
            override val isWelshEnabled = false
          }
          override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
        }
        val result = controller.signOut(fakeRequest)
        redirectLocation(result).get shouldBe "govukdone"
      }
    }

    "GET /timeout" should {
      "return the timeout page" ignore {
        val result = MockStatePensionController.timeout(fakeRequest)
        contentType(result) shouldBe Some("text/html")
        contentAsString(result).contains("For your security we signed you out because you didn't use the service for 15 minutes or more.")
      }
    }

    "calculate chart widths" should {
      def calculateCharts(currentAmount: BigDecimal, forecastAmount: BigDecimal, personalMax: BigDecimal) =
        MockStatePensionController.calculateChartWidths(StatePensionAmountRegular(currentAmount, 0, 0), StatePensionAmountRegular(forecastAmount, 0, 0), StatePensionAmountRegular(personalMax, 0, 0))

      "current chart is 100 when current amount is higher" ignore {
        val (currentChart, forecastChart, personalMaxChart) = calculateCharts(70, 30, 0)
        currentChart.width shouldBe 100
      }

      "forecast chart is 100 when forecast amount is higher" ignore {
        val (currentChart, forecastChart, personalMaxChart) = calculateCharts(70, 80, 80)
        forecastChart.width shouldBe 100
        personalMaxChart.width shouldBe 100
      }

      "current chart and forecast chart are 100 when amounts are equal" ignore {
        val (currentChart, forecastChart, personalMaxChart) = calculateCharts(70, 70, 70)
        currentChart.width shouldBe 100
        forecastChart.width shouldBe 100
        personalMaxChart.width shouldBe 100
      }

      "current chart is 66 when current amount is 2 and forecast is 3" ignore {
        val (currentChart, forecastChart, personalMaxChart) = calculateCharts(2, 3, 4)
        currentChart.width shouldBe 50
        forecastChart.width shouldBe 75
        personalMaxChart.width shouldBe 100
      }

      "forecast chart is 30 when forecast amount is 4 and current is 13" ignore {
        val (currentChart, forecastChart, personalMaxChart) = calculateCharts(13, 4, 20)
        forecastChart.width shouldBe 31
        currentChart.width shouldBe 65
        personalMaxChart.width shouldBe 100

      }
    }

    "when there is a Fill Gaps Scenario" when {
      "the future config is set to off" should {
        "show year information when there is multiple years" ignore {
          val result = MockStatePensionController.show()(authenticatedFakeRequest(mockUserIdFillGapsMultiple))
          contentAsString(result) should include("You have years on your National Insurance record where you did not contribute enough.")
          contentAsString(result) should include("filling years can improve your forecast")
          contentAsString(result) should include("you only need to fill 7 years to get the most you can")
          contentAsString(result) should include("The most you can get by filling any 7 years in your record is")
        }
        "show specific text when is only one payable gap" ignore {
          val result = MockStatePensionController.show()(authenticatedFakeRequest(mockUserIdFillGapsSingle))
          contentAsString(result) should include("You have a year on your National Insurance record where you did not contribute enough. You only need to fill this year to get the most you can.")
          contentAsString(result) should include("The most you can get by filling this year in your record is")
        }
      }

      "the future proof config is set to true" should {
        val controller = new MockStatePensionController {
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
            override val verifySignIn: String = ""
            override val verifySignInContinue: Boolean = false
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
            override val useStatePensionAPI: Boolean = true
            override val useNationalInsuranceAPI: Boolean = true
            override val isWelshEnabled = false
          }
          override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
        }
        "show new personal max text when there are multiple/single year" ignore {
          val result = controller.show()(authenticatedFakeRequest(mockUserIdFillGapsMultiple))
          contentAsString(result) should include("You have shortfalls in your National Insurance record that you can fill and make count towards your State Pension.")
          contentAsString(result) should include("The most you can increase your forecast to is")
        }
      }
    }

    "calculateAge" should {
      "return 30 when the currentDate is 2016-11-2 their dateOfBirth is 1986-10-28" ignore {
        MockStatePensionController.calculateAge(new LocalDate(1986, 10, 28), new LocalDate(2016, 11, 2)) shouldBe 30
      }
      "return 30 when the currentDate is 2016-11-2 their dateOfBirth is 1986-11-2" ignore {
        MockStatePensionController.calculateAge(new LocalDate(1986, 11, 2), new LocalDate(2016, 11, 2)) shouldBe 30

      }
      "return 29 when the currentDate is 2016-11-2 their dateOfBirth is 1986-11-3" ignore {
        MockStatePensionController.calculateAge(new LocalDate(1986, 11, 3), new LocalDate(2016, 11, 2)) shouldBe 29
      }
    }

  }
}