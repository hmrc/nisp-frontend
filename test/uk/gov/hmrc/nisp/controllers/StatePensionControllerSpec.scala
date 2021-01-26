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

import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.auth.AuthAction
import uk.gov.hmrc.nisp.controllers.connectors.CustomAuditConnector
import uk.gov.hmrc.nisp.controllers.pertax.PertaxHelper
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.services.{MetricsService, NationalInsuranceService, StatePensionService}
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.time.DateTimeUtils.now

class StatePensionControllerSpec extends UnitSpec with MockitoSugar with GuiceOneAppPerSuite with Injecting {

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
  val mockUserIdMwrre = "/auth/oid/mockexcludedmwrre"

  val mockUserIdFillGapsSingle = "/auth/oid/mockfillgapssingle"
  val mockUserIdFillGapsMultiple = "/auth/oid/mockfillgapsmultiple"
  val mockUserIdBackendNotFound = "/auth/oid/mockbackendnotfound"

  val ggSignInUrl = "http://localhost:9949/auth-login-stub/gg-sign-in?continue=http%3A%2F%2Flocalhost%3A9234%2Fcheck-your-state-pension%2Faccount&origin=nisp-frontend&accountType=individual"
  val twoFactorUrl = "http://localhost:9949/coafe/two-step-verification/register/?continue=http%3A%2F%2Flocalhost%3A9234%2Fcheck-your-state-pension%2Faccount&failure=http%3A%2F%2Flocalhost%3A9234%2Fcheck-your-state-pension%2Fnot-authorised"

  val fakeRequest = FakeRequest()

  val mockCustomAuditConnector: CustomAuditConnector = mock[CustomAuditConnector]
  val mockNationalInsuranceService: NationalInsuranceService = mock[NationalInsuranceService]
  val mockStatePensionService: StatePensionService = mock[StatePensionService]
  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  val mockPertaxHelper: PertaxHelper = mock[PertaxHelper]
  val mockMetricsService: MetricsService = mock[MetricsService]
  val mockSessionCache: SessionCache = mock[SessionCache]

  implicit val cachedRetriever: CachedStaticHtmlPartialRetriever = FakeCachedStaticHtmlPartialRetriever
  implicit val formPartialRetriever: FormPartialRetriever = FakePartialRetriever
  implicit val templateRenderer: TemplateRenderer = FakeTemplateRenderer

  private def authenticatedFakeRequest(userId: String = mockUserId) = FakeRequest().withSession(
    SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
    SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
    "userId" -> userId,
    "ap" -> Constants.VerifyProviderId
  )

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[AuthAction].to[FakeAuthAction],
      bind[StatePensionService].toInstance(mockStatePensionService),
      bind[NationalInsuranceService].toInstance(mockNationalInsuranceService),
      bind[CustomAuditConnector].toInstance(mockCustomAuditConnector),
      bind[ApplicationConfig].toInstance(mockAppConfig),
      bind[PertaxHelper].toInstance(mockPertaxHelper),
      bind[MetricsService].toInstance(mockMetricsService),
      bind[SessionCache].toInstance(mockSessionCache),
      bind[CachedStaticHtmlPartialRetriever].toInstance(cachedRetriever),
      bind[FormPartialRetriever].toInstance(formPartialRetriever),
      bind[TemplateRenderer].toInstance(templateRenderer)
    ).build()

  val statePensionController = inject[StatePensionController]

  "State Pension controller" should {

    "GET /statepension" should {

      "return 500 when backend 404" in {
        val result = statePensionController
          .show()(authenticatedFakeRequest(mockUserIdBackendNotFound))
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "return the forecast only page for a user with a forecast lower than current amount" in {
        val result = statePensionController
          .show()(authenticatedFakeRequest(mockUserIdForecastOnly))
        contentAsString(result) should not include ("£80.38")
      }

      "return 200, with exclusion message for excluded user" in {
        val result = statePensionController
          .show()(fakeRequest.withSession(
          SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
          SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
          "userId" -> mockUserIdExcluded,
          "ap" -> Constants.VerifyProviderId
        ))
        redirectLocation(result) shouldBe Some("/check-your-state-pension/exclusion")
      }

      "return content about COPE for contracted out (B) user" in {
        val result = statePensionController
          .show()(authenticatedFakeRequest(mockUserIdContractedOut))
        contentAsString(result) should include("You’ve been in a contracted-out pension scheme")
      }

      "return COPE page for contracted out (B) user" in {
        val result = statePensionController
          .showCope()(authenticatedFakeRequest(mockUserIdContractedOut))
        contentAsString(result) should include("You were contracted out")
      }

      "return abroad message for abroad user" in {
        val result = statePensionController
          .show()(authenticatedFakeRequest(mockUserIdAbroad))
        contentAsString(result) should include("As you are living or working overseas")
      }

      "return /exclusion for MWRRE user" in {
        val result = statePensionController
          .show()(authenticatedFakeRequest(mockUserIdMwrre))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/check-your-state-pension/exclusion")
      }

      "return abroad message for forecast only user" in {
        val result = statePensionController
          .show()(authenticatedFakeRequest(mockUserIdForecastOnly))
        contentAsString(result) should include("As you are living or working overseas")
        contentAsString(result) should not include "£80.38"
      }

      "return abroad message for an mqp user instead of standard mqp overseas message" in {
        val result = statePensionController
          .show()(authenticatedFakeRequest(mockUserIdMQPAbroad))
        contentAsString(result) should include("As you are living or working overseas")
        contentAsString(result) should not include "If you have lived or worked overseas"
      }

      "redirect to statepension page for non contracted out user" in {
        val result = statePensionController
          .showCope()(authenticatedFakeRequest(mockUserIdMQP))
        redirectLocation(result) shouldBe Some("/check-your-state-pension/account")
      }

      "return page with MQP messaging for MQP user" in {
        val result = statePensionController
          .show()(authenticatedFakeRequest(mockUserIdMQP))
        contentAsString(result) should include("10 years needed on your National Insurance record to get any State Pension")
      }
    }

    "when there is a Fill Gaps Scenario" when {
      "the future config is set to off" should {
        "show year information when there is multiple years" in {
          val result = statePensionController
            .show()(authenticatedFakeRequest(mockUserIdFillGapsMultiple))
          contentAsString(result) should include("You have years on your National Insurance record where you did not contribute enough.")
          contentAsString(result) should include("filling years can improve your forecast")
          contentAsString(result) should include("you only need to fill 7 years to get the most you can")
          contentAsString(result) should include("The most you can get by filling any 7 years in your record is")
        }
        "show specific text when is only one payable gap" in {
          val result = mockStatePensionController(TestAccountBuilder.fillGapSingle)
            .show()(authenticatedFakeRequest(mockUserIdFillGapsSingle))
          contentAsString(result) should include("You have a year on your National Insurance record where you did not contribute enough. You only need to fill this year to get the most you can.")
          contentAsString(result) should include("The most you can get by filling this year in your record is")
        }
      }

      "the future proof config is set to true" should {
          "show new personal max text when there are multiple/single year" in {
          val result = statePensionController.show()(authenticatedFakeRequest(mockUserIdFillGapsMultiple))
          contentAsString(result) should include("You have shortfalls in your National Insurance record that you can fill and make count towards your State Pension.")
          contentAsString(result) should include("The most you can increase your forecast to is")
        }
      }
    }

    "GET /signout" should {
      "redirect to the questionnaire page when govuk done page is disabled" in {
        val result = statePensionController.signOut(fakeRequest)
        redirectLocation(result).get shouldBe "/foo"
      }

      "redirect to the feedback questionnaire page when govuk done page is enabled" in {
        val result = statePensionController.signOut(fakeRequest)
        redirectLocation(result).get shouldBe "/foo"
      }
    }

  }
}
