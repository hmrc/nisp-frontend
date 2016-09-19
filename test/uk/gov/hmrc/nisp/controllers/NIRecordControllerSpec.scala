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

import org.joda.time.LocalDate
import org.scalatestplus.play.OneAppPerSuite
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.connectors.NispConnector
import uk.gov.hmrc.nisp.controllers.connectors.CustomAuditConnector
import uk.gov.hmrc.nisp.services.{CitizenDetailsService}
import uk.gov.hmrc.play.frontend.auth.AuthenticationProviderIds
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.SessionKeys
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.time.DateTimeUtils._
import uk.gov.hmrc.nisp.helpers._

class NIRecordControllerSpec extends UnitSpec with OneAppPerSuite {
  val mockUserId = "/auth/oid/mockuser"
  val mockFullUserId = "/auth/oid/mockfulluser"
  val mockBlankUserId = "/auth/oid/mockblank"
  val mockUserIdExcluded = "/auth/oid/mockexcluded"
  val mockUserIdHRP = "/auth/oid/mockhomeresponsibilitiesprotection"
  val mockUserWithGaps ="/auth/oid/mockfillgapsmultiple"

  val ggSignInUrl = s"http://localhost:9949/auth-login-stub/gg-sign-in?continue=http%3A%2F%2Flocalhost%3A9234%2Fcheck-your-state-pension%2Faccount&origin=nisp-frontend&accountType=individual"

  lazy val fakeRequest = FakeRequest()
  def authenticatedFakeRequest(userId: String) = FakeRequest().withSession(
    SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
    SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
    SessionKeys.userId -> userId,
    SessionKeys.authProvider -> AuthenticationProviderIds.VerifyProviderId
  )

  "GET /account/nirecord/gaps (gaps)" should {
    "return redirect for unauthenticated user" in {
      val result = MockNIRecordController.showGaps(fakeRequest)
      redirectLocation(result) shouldBe Some(ggSignInUrl)
    }

    "return gaps page for user with gaps" in {
      val result = MockNIRecordController.showGaps(authenticatedFakeRequest(mockUserId))
      contentAsString(result) should include ("Years which are not full")
    }

    "return full page for user without gaps" in {
      val result = MockNIRecordController.showGaps(authenticatedFakeRequest(mockFullUserId))
      redirectLocation(result) shouldBe Some("/check-your-state-pension/account/nirecord")
    }

    "return error page for blank response NINO" in {
      intercept[RuntimeException] {
        val result = MockNIRecordController.showGaps(authenticatedFakeRequest(mockBlankUserId))
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "redirect to exclusion for excluded user" in {
      val result =  MockNIRecordController.showGaps(fakeRequest.withSession(
        SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
        SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
        SessionKeys.userId -> mockUserIdExcluded,
        SessionKeys.authProvider -> AuthenticationProviderIds.VerifyProviderId
      ))
      redirectLocation(result) shouldBe Some("/check-your-state-pension/exclusionni")
    }
  }

  "GET /account/nirecord (full)" should {
    "return redirect for unauthenticated user" in {
      val result = MockNIRecordController.showFull(fakeRequest)
      redirectLocation(result) shouldBe Some(ggSignInUrl)
    }

    "return gaps page for user with gaps" in {
      val result = MockNIRecordController.showFull(authenticatedFakeRequest(mockUserId))
      contentAsString(result) should include ("All years.")
    }

    "return full page for user without gaps" in {
      val result = MockNIRecordController.showFull(authenticatedFakeRequest(mockFullUserId))
      contentAsString(result) should include ("You do not have any gaps in your record.")
    }

    "redirect to exclusion for excluded user" in {
      val result =  MockNIRecordController.showFull(fakeRequest.withSession(
        SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
        SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
        SessionKeys.userId -> mockUserIdExcluded,
        SessionKeys.authProvider -> AuthenticationProviderIds.VerifyProviderId
      ))
      redirectLocation(result) shouldBe Some("/check-your-state-pension/exclusionni")
    }
  }

  "GET /account/nirecord/gapsandhowtocheck" should {
    "return redirect for unauthenticated user" in {
      val result = MockNIRecordController.showGapsAndHowToCheckThem(fakeRequest)
      redirectLocation(result) shouldBe Some(ggSignInUrl)
    }

    "return how to check page for authenticated user" in {
      val result = MockNIRecordController.showGapsAndHowToCheckThem(authenticatedFakeRequest(mockUserId))
      contentAsString(result) should include ("Gaps in your record and how to check them")
    }
    "return hrp message for hrp user" in {
      val result = MockNIRecordController.showGapsAndHowToCheckThem(authenticatedFakeRequest(mockUserIdHRP))
      contentAsString(result) should include ("Home Responsibilities Protection (HRP) is only available for <strong>full</strong> tax years, from 6 April to 5 April, between 1978 and 2010.")
    }
    "do not return hrp message for non hrp user" in {
      val result = MockNIRecordController.showGapsAndHowToCheckThem(authenticatedFakeRequest(mockUserId))
      contentAsString(result) should not include
        "Home Responsibilities Protection (HRP) is only available for <strong>full</strong> tax years, from 6 April to 5 April, between 1978 and 2010."
    }
  }

  "GET /account/nirecord/voluntarycontribs" should {
    "return redirect for unauthenticated user" in {
      val result = MockNIRecordController.showVoluntaryContributions(fakeRequest)
      redirectLocation(result) shouldBe Some(ggSignInUrl)
    }

    "return how to check page for authenticated user" in {
      val result = MockNIRecordController.showVoluntaryContributions(authenticatedFakeRequest(mockUserId))
      contentAsString(result) should include ("Voluntary contributions")
    }
  }

  "GET /account/nirecord (full)" should {
    "return NI record page with details for full years - when showFullNI is true" in {
      val controller = new MockNIRecordController {
        override val nispConnector: NispConnector = MockNispConnector
        override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
        override val customAuditConnector: CustomAuditConnector = MockCustomAuditConnector
        override val sessionCache: SessionCache = MockSessionCache
        override val showFullNI = true
        override val currentDate = new LocalDate(2016,9,9)

        override protected def authConnector: AuthConnector = MockAuthConnector

      override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
      }
      val result = controller.showFull(authenticatedFakeRequest(mockFullUserId))
      contentAsString(result) should include("52 weeks")
    }

    "return NI record page with no details for full years - when showFullNI is false" in {
      val controller = new MockNIRecordController {
        override val nispConnector: NispConnector = MockNispConnector
        override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
        override val customAuditConnector: CustomAuditConnector = MockCustomAuditConnector
        override val sessionCache: SessionCache = MockSessionCache
        override val currentDate = new LocalDate(2016,9,9)

        override protected def authConnector: AuthConnector = MockAuthConnector
        override val showFullNI = false
        override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
      }
      val result = controller.showFull(authenticatedFakeRequest(mockFullUserId))
      contentAsString(result) shouldNot include("52 weeks")
    }
  }

  "GET /account/nirecord (Gaps)" should {
    "return NI record page - gap details should not show shortfall may increase messages - if current date is after 5 April 2019" in {
      val controller = new MockNIRecordController {
        override val nispConnector: NispConnector = MockNispConnector
        override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
        override val customAuditConnector: CustomAuditConnector = MockCustomAuditConnector
        override val sessionCache: SessionCache = MockSessionCache
        override val showFullNI = false
        override val currentDate = new LocalDate(2019,4,6)
        override protected def authConnector: AuthConnector = MockAuthConnector
        override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
      }
      val result = controller.showGaps(authenticatedFakeRequest(mockUserWithGaps))
      contentAsString(result) should not include("shortfall may increase")
    }

    "return NI record page - gap details should show shortfall may increase messages - if current date is before 5 April 2019" in {
      val controller = new MockNIRecordController {
        override val nispConnector: NispConnector = MockNispConnector
        override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
        override val customAuditConnector: CustomAuditConnector = MockCustomAuditConnector
        override val sessionCache: SessionCache = MockSessionCache
        override val showFullNI = false
        override val currentDate = new LocalDate(2019,4,4)
        override protected def authConnector: AuthConnector = MockAuthConnector
        override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
      }
      val result = controller.showGaps(authenticatedFakeRequest(mockUserWithGaps))
      contentAsString(result) should include("shortfall may increase")
    }

    "return NI record page - gap details should show shortfall may increase messages - if current date is same 5 April 2019" in {
      val controller = new MockNIRecordController {
        override val nispConnector: NispConnector = MockNispConnector
        override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
        override val customAuditConnector: CustomAuditConnector = MockCustomAuditConnector
        override val sessionCache: SessionCache = MockSessionCache
        override val showFullNI = false
        override val currentDate = new LocalDate(2019,4,5)
        override protected def authConnector: AuthConnector = MockAuthConnector
        override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
      }
      val result = controller.showGaps(authenticatedFakeRequest(mockUserWithGaps))
      contentAsString(result) should include("shortfall may increase")
    }
  }

}
