/*
 * Copyright 2018 HM Revenue & Customs
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
import org.joda.time.LocalDate
import org.scalatestplus.play.OneAppPerSuite
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.services.{CitizenDetailsService, MetricsService, StatePensionService}
import uk.gov.hmrc.play.frontend.auth.AuthenticationProviderIds
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.time.DateTimeUtils._
import scala.concurrent.Future
import uk.gov.hmrc.http.SessionKeys

class ExclusionControllerSpec extends UnitSpec with OneAppPerSuite {
  val fakeRequest = FakeRequest()

  private def authId(username: String): String = s"/auth/oid/$username"

  val mockUserId = authId("mockuser")

  val mockUserIdExcludedAll = authId("mockexcludedall")
  val mockUserIdExcludedAllButDead = authId("mockexcludedallbutdead")
  val mockUserIdExcludedAllButDeadMCI = authId("mockexcludedallbutdeadmci")
  val mockUserIdExcludedDissonanceIomMwrreAbroad = authId("mockexcludeddissonanceiommwrreabroad")
  val mockUserIdExcludedIomMwrreAbroad = authId("mockexcludediommwrreabroad")
  val mockUserIdExcludedMwrreAbroad = authId("mockexcludedmwrreabroad")
  val mockUserIdExcludedMwrre = authId("mockexcludedmwrre")
  val mockUserIdExcludedAbroad = authId("mockexcludedabroad")

  val mockUserIdSPAUnderConsiderationExcludedAmountDis = authId("mockspaunderconsiderationexclusionamountdis")
  val mockUserIdSPAUnderConsiderationExcludedIoM = authId("mockspaunderconsiderationexclusioniom")
  val mockUserIdSPAUnderConsiderationExcludedMwrre = authId("mockspaunderconsiderationexclusionmwree")
  val mockUserIdSPAUnderConsiderationExcludedOverSpa = authId("mockspaunderconsiderationexclusionoverspa")
  val mockUserIdSPAUnderConsiderationExcludedMultiple = authId("mockspaunderconsiderationexclusionmultiple")
  val mockUserIdSPAUnderConsiderationExcludedNoFlag = authId("mockspaunderconsiderationexclusionnoflag")

  val deadMessaging = "Please contact HMRC National Insurance helpline on 0300 200 3500."
  val mciMessaging = "We need to talk to you about an MCI error before you sign in."
  val postSPAMessaging = "If you have not already started <a href=\"https://www.gov.uk/claim-state-pension-online\" rel=\"external\" data-journey-click=\"checkmystatepension:external:claimstatepension\">claiming your State Pension</a>, you can <a href=\"https://www.gov.uk/deferring-state-pension\" rel=\"external\" data-journey-click=\"checkmystatepension:external:deferstatepension\" target=\"_blank\">put off claiming your State Pension (opens in new tab)</a> and this may mean you get extra State Pension when you do want to claim it."
  val dissonanceMessaging = "We’re unable to calculate your State Pension forecast at the moment and we’re working on fixing this."
  val isleOfManMessagingSP = "We’re unable to calculate your State Pension, as the Isle of Man Government is currently undertaking a review of its Retirement Pension scheme."
  val isleOfManMessagingNI = "We’re unable to show your National Insurance record as you have contributions from the Isle of Man."
  val mwrreMessagingSP = "We’re unable to calculate your State Pension forecast as you have <a href=\"https://www.gov.uk/reduced-national-insurance-married-women\" rel=\"external\" target=\"_blank\" data-journey-click=\"checkmystatepension:external:mwrre\">paid a reduced rate of National Insurance as a married woman (opens in new tab)"
  val mwrreMessagingNI = "We’re currently unable to show your National Insurance Record as you have <a href=\"https://www.gov.uk/reduced-national-insurance-married-women\" rel=\"external\" target=\"_blank\" data-journey-click=\"checkmystatepension:external:mwrre\">paid a reduced rate of National Insurance as a married woman (opens in new tab)</a>."
  val abroadMessaging = "We’re unable to calculate your UK State Pension forecast as you’ve lived or worked abroad."
  val spaUnderConsiderationMessaging = "Proposed change to your State Pension age"

  "GET /exclusion" should {

    "return redirect to account page for non-excluded user" in {
      val result = MockExclusionController.showSP()(fakeRequest.withSession(
        SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
        SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
        SessionKeys.userId -> mockUserId,
        SessionKeys.authProvider -> AuthenticationProviderIds.VerifyProviderId
      ))

      redirectLocation(result) shouldBe Some("/check-your-state-pension/account")
    }

    "Exclusion Controller" when {

      def generateSPRequest(userId: String): Future[Result] = {
        MockExclusionController.showSP()(fakeRequest.withSession(
          SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
          SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
          SessionKeys.userId -> userId,
          SessionKeys.authProvider -> AuthenticationProviderIds.VerifyProviderId
        ))
      }

      def generateNIRequest(userId: String): Future[Result] = {
        MockExclusionController.showNI()(fakeRequest.withSession(
          SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
          SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
          SessionKeys.userId -> userId,
          SessionKeys.authProvider -> AuthenticationProviderIds.VerifyProviderId
        ))
      }

      "The User has every exclusion" should {
        "return only the Dead Exclusion on /exclusion" in {
          val result = generateSPRequest(mockUserIdExcludedAll)
          redirectLocation(result) shouldBe None
          contentAsString(result) should include (deadMessaging)
          contentAsString(result) should not include mciMessaging
          contentAsString(result) should not include postSPAMessaging
          contentAsString(result) should not include dissonanceMessaging
          contentAsString(result) should not include isleOfManMessagingSP
          contentAsString(result) should not include mwrreMessagingSP
          contentAsString(result) should not include abroadMessaging
        }

        "return only the Dead Exclusion on /exclusionni" in {
          val result = generateNIRequest(mockUserIdExcludedAll)
          redirectLocation(result) shouldBe None
          contentAsString(result) should include (deadMessaging)
          contentAsString(result) should not include mciMessaging
          contentAsString(result) should not include isleOfManMessagingNI
          contentAsString(result) should not include mwrreMessagingNI
        }
      }

      "The User has every exclusion except Dead" should {
        "return only the MCI Exclusion on /exclusion" in {
          val result = generateSPRequest(mockUserIdExcludedAllButDead)
          redirectLocation(result) shouldBe None
          contentAsString(result) should not include deadMessaging
          contentAsString(result) should include (mciMessaging)
          contentAsString(result) should not include postSPAMessaging
          contentAsString(result) should not include dissonanceMessaging
          contentAsString(result) should not include isleOfManMessagingSP
          contentAsString(result) should not include mwrreMessagingSP
          contentAsString(result) should not include abroadMessaging
        }

        "return only the MCI Exclusion on /exclusionni" in {
          val result = generateNIRequest(mockUserIdExcludedAllButDead)
          redirectLocation(result) shouldBe None
          contentAsString(result) should not include deadMessaging
          contentAsString(result) should include (mciMessaging)
          contentAsString(result) should not include isleOfManMessagingNI
          contentAsString(result) should not include mwrreMessagingNI
        }
      }

      "The User has every exclusion except Dead and MCI" should {
        "return only the Post SPA Exclusion on /exclusion" in {
          val result = generateSPRequest(mockUserIdExcludedAllButDeadMCI)
          redirectLocation(result) shouldBe None
          contentAsString(result) should not include deadMessaging
          contentAsString(result) should not include mciMessaging
          contentAsString(result) should include (postSPAMessaging)
          contentAsString(result) should not include dissonanceMessaging
          contentAsString(result) should not include isleOfManMessagingSP
          contentAsString(result) should not include mwrreMessagingSP
          contentAsString(result) should not include abroadMessaging
        }

        "return only the Isle of Man Exclusion on /exclusionni" in {
          val result = generateNIRequest(mockUserIdExcludedAllButDeadMCI)
          redirectLocation(result) shouldBe None
          contentAsString(result) should not include deadMessaging
          contentAsString(result) should not include mciMessaging
          contentAsString(result) should include (isleOfManMessagingNI)
          contentAsString(result) should not include mwrreMessagingNI
        }
      }

      "The User has every exclusion except Dead, MCI and Post SPA" should {
        "return only the Amount Dissonance Exclusion on /exclusion" in {
          val result = generateSPRequest(mockUserIdExcludedDissonanceIomMwrreAbroad)
          redirectLocation(result) shouldBe None
          contentAsString(result) should not include deadMessaging
          contentAsString(result) should not include mciMessaging
          contentAsString(result) should not include postSPAMessaging
          contentAsString(result) should include (dissonanceMessaging)
          contentAsString(result) should not include isleOfManMessagingSP
          contentAsString(result) should not include mwrreMessagingSP
          contentAsString(result) should not include abroadMessaging
        }

        "return only the Isle of Man Exclusion on /exclusionni" in {
          val result = generateNIRequest(mockUserIdExcludedDissonanceIomMwrreAbroad)
          redirectLocation(result) shouldBe None
          contentAsString(result) should not include deadMessaging
          contentAsString(result) should not include mciMessaging
          contentAsString(result) should include (isleOfManMessagingNI)
          contentAsString(result) should not include mwrreMessagingNI
        }
      }

      "The User has the Isle of Man, MWRRE and Abroad exclusions" should {
        "return only the Isle of Man Exclusion on /exclusion" in {
          val result = generateSPRequest(mockUserIdExcludedIomMwrreAbroad)
          redirectLocation(result) shouldBe None
          contentAsString(result) should not include deadMessaging
          contentAsString(result) should not include mciMessaging
          contentAsString(result) should not include postSPAMessaging
          contentAsString(result) should not include dissonanceMessaging
          contentAsString(result) should include (isleOfManMessagingSP)
          contentAsString(result) should not include mwrreMessagingSP
          contentAsString(result) should not include abroadMessaging
        }

        "return only the Isle of Man Exclusion on /exclusionni" in {
          val result = generateNIRequest(mockUserIdExcludedIomMwrreAbroad)
          redirectLocation(result) shouldBe None
          contentAsString(result) should not include deadMessaging
          contentAsString(result) should not include mciMessaging
          contentAsString(result) should include (isleOfManMessagingNI)
          contentAsString(result) should not include mwrreMessagingNI
        }
      }

      "The User has MWRRE and Abroad exclusions" should {
        "return only the MWREE Exclusion on /exclusion" in {
          val result = generateSPRequest(mockUserIdExcludedMwrreAbroad)
          redirectLocation(result) shouldBe None
          contentAsString(result) should not include deadMessaging
          contentAsString(result) should not include mciMessaging
          contentAsString(result) should not include postSPAMessaging
          contentAsString(result) should not include dissonanceMessaging
          contentAsString(result) should not include isleOfManMessagingSP
          contentAsString(result) should include (mwrreMessagingSP)
          contentAsString(result) should not include abroadMessaging
        }

        "return only the MWRRE Exclusion on /exclusionni" in {
          val result = generateNIRequest(mockUserIdExcludedMwrreAbroad)
          redirectLocation(result) shouldBe None
          contentAsString(result) should not include deadMessaging
          contentAsString(result) should not include mciMessaging
          contentAsString(result) should not include isleOfManMessagingNI
          contentAsString(result) should include (mwrreMessagingNI)
        }

      }

      "The User has MWRRE exclusion" should {

        "return only the MWRRE Exclusion on /exclusion" in {

          val result = MockExclusionController.showSP()(fakeRequest.withSession(
              SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
              SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
              SessionKeys.userId -> mockUserIdExcludedMwrre,
              SessionKeys.authProvider -> AuthenticationProviderIds.VerifyProviderId
            ))
          redirectLocation(result) shouldBe None
          contentAsString(result) should not include deadMessaging
          contentAsString(result) should not include mciMessaging
          contentAsString(result) should not include isleOfManMessagingSP
          contentAsString(result) should include (mwrreMessagingSP)
        }

        "return only the MWRRE Exclusion on /exclusionni" in {
          val result = generateNIRequest(mockUserIdExcludedMwrre)
          redirectLocation(result) shouldBe None
          contentAsString(result) should not include deadMessaging
          contentAsString(result) should not include mciMessaging
          contentAsString(result) should not include isleOfManMessagingNI
          contentAsString(result) should include (mwrreMessagingNI)
        }
      }

      "The User has the Abroad only should" should {
        "return only the Abroad Exclusion on /exclusion" in {
          val result = generateSPRequest(mockUserIdExcludedAbroad)
          redirectLocation(result) shouldBe None
          contentAsString(result) should not include deadMessaging
          contentAsString(result) should not include mciMessaging
          contentAsString(result) should not include postSPAMessaging
          contentAsString(result) should not include dissonanceMessaging
          contentAsString(result) should not include isleOfManMessagingSP
          contentAsString(result) should not include mwrreMessagingSP
          contentAsString(result) should include (abroadMessaging)
        }

        "they should be redirected from /exclusionni to /nirecord" in {
          val result = generateNIRequest(mockUserIdExcludedAbroad)
          redirectLocation(result) shouldBe Some("/check-your-state-pension/account/nirecord/gaps")

        }
      }

      "The User has SPA under consideration flag and Amount Dis exclusion" should {
        "return with SPA under consideration message" in {
          val result = generateSPRequest(mockUserIdSPAUnderConsiderationExcludedAmountDis)
          redirectLocation(result) shouldBe None
          contentAsString(result) should include (spaUnderConsiderationMessaging)
        }
      }

      "The User has SPA under consideration flag and IoM exclusion" should {
        "return with SPA under consideration message" in {
          val result = generateSPRequest(mockUserIdSPAUnderConsiderationExcludedIoM)
          redirectLocation(result) shouldBe None
          contentAsString(result) should include (spaUnderConsiderationMessaging)
        }
      }

      "The User has SPA under consideration flag and Mwrre exclusion" should {
        "return with no SPA under consideration message" in {
          val result = generateSPRequest(mockUserIdSPAUnderConsiderationExcludedMwrre)
          redirectLocation(result) shouldBe None
          contentAsString(result) should not include spaUnderConsiderationMessaging
        }
      }

      "The User has SPA under consideration flag and Over Spa exclusion" should {
        "return with no SPA under consideration message" in {
          val result = generateSPRequest(mockUserIdSPAUnderConsiderationExcludedOverSpa)
          redirectLocation(result) shouldBe None
          contentAsString(result) should not include spaUnderConsiderationMessaging
        }
      }

      "The User has SPA under consideration flag and Multiple exclusions with Over SPA first" should {
        "return with no SPA under consideration message" in {
          val result = generateSPRequest(mockUserIdSPAUnderConsiderationExcludedMultiple)
          redirectLocation(result) shouldBe None
          contentAsString(result) should not include spaUnderConsiderationMessaging
        }
      }

      "The User has no SPA under consideration flag and exclusion" should {
        "return with no SPA under consideration message" in {
          val result = generateSPRequest(mockUserIdSPAUnderConsiderationExcludedNoFlag)
          redirectLocation(result) shouldBe None
          contentAsString(result) should not include spaUnderConsiderationMessaging
        }
      }

    }
  }
}
