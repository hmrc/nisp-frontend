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

import org.joda.time.LocalDate
import org.scalatestplus.play.OneAppPerSuite
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.connectors.NispConnector
import uk.gov.hmrc.nisp.controllers.connectors.CustomAuditConnector
import uk.gov.hmrc.nisp.services.{CitizenDetailsService, MetricsService}
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
  val mockUserIdExcluded = "/auth/oid/mockexcludedall"
  val mockUserIdHRP = "/auth/oid/mockhomeresponsibilitiesprotection"
  val mockUserWithGaps = "/auth/oid/mockfillgapsmultiple"
  val mockNoQualifyingYearsUserId = "/auth/oid/mocknoqualifyingyears"
  val mockBackendNotFoundUserId = "/auth/oid/mockbackendnotfound"

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
      contentAsString(result) should include("Years which are not full")
    }

    "return full page for user without gaps" in {
      val result = MockNIRecordController.showGaps(authenticatedFakeRequest(mockFullUserId))
      redirectLocation(result) shouldBe Some("/check-your-state-pension/account/nirecord")
    }

    "return error page for blank response NINO" in {
      val result = MockNIRecordController.showGaps(authenticatedFakeRequest(mockBlankUserId))
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return 500 when backend 404" in {
      val result = MockNIRecordController.showGaps(authenticatedFakeRequest(mockBackendNotFoundUserId))
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "redirect to exclusion for excluded user" in {
      val result = MockNIRecordController.showGaps(fakeRequest.withSession(
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
      contentAsString(result) should include("All years.")
    }

    "return full page for user without gaps" in {
      val result = MockNIRecordController.showFull(authenticatedFakeRequest(mockFullUserId))
      contentAsString(result) should include("You do not have any gaps in your record.")
    }

    "redirect to exclusion for excluded user" in {
      val result = MockNIRecordController.showFull(fakeRequest.withSession(
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
      contentAsString(result) should include("Gaps in your record and how to check them")
    }
    "return hrp message for hrp user" in {
      val result = MockNIRecordController.showGapsAndHowToCheckThem(authenticatedFakeRequest(mockUserIdHRP))
      contentAsString(result) should include("Home Responsibilities Protection (HRP) is only available for <strong>full</strong> tax years, from 6 April to 5 April, between 1978 and 2010.")
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
      contentAsString(result) should include("Voluntary contributions")
    }
  }

  "GET /account/nirecord (full)" should {
    "return NI record page with details for full years - when showFullNI is true" in {
      val controller = new MockNIRecordController {
        override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
        override val customAuditConnector: CustomAuditConnector = MockCustomAuditConnector
        override val sessionCache: SessionCache = MockSessionCache
        override val showFullNI = true
        override val currentDate = new LocalDate(2016, 9, 9)

        override protected def authConnector: AuthConnector = MockAuthConnector

        override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
        override val metricsService: MetricsService = MockMetricsService
      }
      val result = controller.showFull(authenticatedFakeRequest(mockFullUserId))
      contentAsString(result) should include("52 weeks")
    }

    "return NI record page with no details for full years - when showFullNI is false" in {
      val controller = new MockNIRecordController {
        override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
        override val customAuditConnector: CustomAuditConnector = MockCustomAuditConnector
        override val sessionCache: SessionCache = MockSessionCache
        override val currentDate = new LocalDate(2016, 9, 9)

        override protected def authConnector: AuthConnector = MockAuthConnector

        override val showFullNI = false
        override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
        override val metricsService: MetricsService = MockMetricsService
      }
      val result = controller.showFull(authenticatedFakeRequest(mockFullUserId))
      contentAsString(result) shouldNot include("52 weeks")
    }

    "return NI record when numner of qualifying years is 0" in {
      // Unit test for bug NISP-2436
      val controller = new MockNIRecordController {
        override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
        override val customAuditConnector: CustomAuditConnector = MockCustomAuditConnector
        override val sessionCache: SessionCache = MockSessionCache
        override val showFullNI = true
        override val currentDate = new LocalDate(2016, 9, 9)

        override protected def authConnector: AuthConnector = MockAuthConnector

        override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
        override val metricsService: MetricsService = MockMetricsService
      }
      val result = controller.showFull(authenticatedFakeRequest(mockNoQualifyingYearsUserId))
      result.header.status shouldBe 200
    }
  }

  "GET /account/nirecord (Gaps)" should {
    "return NI record page - gap details should not show shortfall may increase messages - if current date is after 5 April 2019" in {
      val controller = new MockNIRecordController {
        override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
        override val customAuditConnector: CustomAuditConnector = MockCustomAuditConnector
        override val sessionCache: SessionCache = MockSessionCache
        override val showFullNI = false
        override val currentDate = new LocalDate(2019, 4, 6)

        override protected def authConnector: AuthConnector = MockAuthConnector

        override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
        override val metricsService: MetricsService = MockMetricsService
      }
      val result = controller.showGaps(authenticatedFakeRequest(mockUserWithGaps))
      contentAsString(result) should not include ("shortfall may increase")
    }

    "return NI record page - gap details should show shortfall may increase messages - if current date is before 5 April 2019" in {
      val controller = new MockNIRecordController {
        override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
        override val customAuditConnector: CustomAuditConnector = MockCustomAuditConnector
        override val sessionCache: SessionCache = MockSessionCache
        override val showFullNI = false
        override val currentDate = new LocalDate(2019, 4, 4)

        override protected def authConnector: AuthConnector = MockAuthConnector

        override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
        override val metricsService: MetricsService = MockMetricsService
      }
      val result = controller.showGaps(authenticatedFakeRequest(mockUserWithGaps))
      contentAsString(result) should include("shortfall may increase")
    }

    "return NI record page - gap details should show shortfall may increase messages - if current date is same 5 April 2019" in {
      val controller = new MockNIRecordController {
        override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
        override val customAuditConnector: CustomAuditConnector = MockCustomAuditConnector
        override val sessionCache: SessionCache = MockSessionCache
        override val showFullNI = false
        override val currentDate = new LocalDate(2019, 4, 5)

        override protected def authConnector: AuthConnector = MockAuthConnector

        override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
        override val metricsService: MetricsService = MockMetricsService
      }
      val result = controller.showGaps(authenticatedFakeRequest(mockUserWithGaps))
      contentAsString(result) should include("shortfall may increase")
    }
  }

  "showPre75Years" when {
    "the date of entry is the sixteenth birthday" should {
      "return true for 5th April 1975" in {
        val date = new LocalDate(1975, 4, 5)
        MockNIRecordController.showPre1975Years(date, Some(date.minusYears(16))) shouldBe true
      }

      "return false for 6th April 1975" in {
        val date = new LocalDate(1975, 4, 6)
        MockNIRecordController.showPre1975Years(date, Some(date.minusYears(16))) shouldBe false
      }
    }

    "the date of entry and sixteenth are different" should {
      "return true for 16th: 5th April 1970, Date of entry: 5th April 1975" in {
        val dob = new LocalDate(1970, 4, 5).minusYears(16)
        val entry = new LocalDate(1975, 4, 5)
        MockNIRecordController.showPre1975Years(entry, Some(dob)) shouldBe true
      }

      "return true for 16th: 5th April 1970, Date of entry: 6th April 1975" in {
        val dob = new LocalDate(1970, 4, 5).minusYears(16)
        val entry = new LocalDate(1975, 4, 6)
        MockNIRecordController.showPre1975Years(entry, Some(dob)) shouldBe false
      }

      "return true for 16th: 5th April 1975, Date of entry: 5th April 1970" in {
        val dob = new LocalDate(1975, 4, 5).minusYears(16)
        val entry = new LocalDate(1970, 4, 5)
        MockNIRecordController.showPre1975Years(entry, Some(dob)) shouldBe true
      }

      "return true for 16th: 6th April 1975, Date of entry: 5th April 1970" in {
        val dob = new LocalDate(1975, 4, 6).minusYears(16)
        val entry = new LocalDate(1970, 4, 5)
        MockNIRecordController.showPre1975Years(entry, Some(dob)) shouldBe false
      }

      "return false for 16th: 10th July 1983, Date of Entry: 16th October 1977" in {
        val dob = new LocalDate(1983, 7, 10).minusYears(16)
        val entry = new LocalDate(1977, 10, 16)
        MockNIRecordController.showPre1975Years(entry, Some(dob)) shouldBe false
      }

    }

    "the date of birth is unavailable" should {
      "return true for date of entry 5th April 1975" in {
        val date = new LocalDate(1975, 4, 5)
        MockNIRecordController.showPre1975Years(date, None) shouldBe true
      }

      "return false for date of entry  6th April 1975" in {
        val date = new LocalDate(1975, 4, 6)
        MockNIRecordController.showPre1975Years(date, None) shouldBe false
      }
    }
  }

  "generateTableList" when {
    "start and end are the same" should {
      "return a list wuith one string of that year" in {
        val start = "2015-16"
        val end = "2015-16"
        MockNIRecordController.generateTableList(start, end) shouldBe List("2015-16")
      }
    }

    "the start is less then the end" should {
      "throw an illegal argument exception" in {
        val start = "2014-15"
        val end = "2015-16"
        val caught = intercept[IllegalArgumentException] {
          MockNIRecordController.generateTableList(start, end)
        }
        caught shouldBe a[IllegalArgumentException]
      }
    }

    "the inputs are not dates" should {
      "throw exception for start" in {
        val start = "hello"
        val end = "2014-15"
        val caught = intercept[IllegalArgumentException] {
          MockNIRecordController.generateTableList(start, end)
        }
        caught shouldBe a[IllegalArgumentException]
      }

      "throw exception for end" in {
        val start = "2015-16"
        val end = "hello"
        val caught = intercept[IllegalArgumentException] {
          MockNIRecordController.generateTableList(start, end)
        }
        caught shouldBe a[IllegalArgumentException]
      }

    }

    "the end is greater than the start" should {
      "return a list of two adjacent dates" in {
        val start = "2016-17"
        val end = "2015-16"
        MockNIRecordController.generateTableList(start, end) shouldBe Seq("2016-17", "2015-16")
      }

      "return a list of three dates" in {
        val start = "2016-17"
        val end = "2014-15"
        MockNIRecordController.generateTableList(start, end) shouldBe Seq("2016-17", "2015-16", "2014-15")
      }

      "return a full NI Record" in {
        val start = "2016-17"
        val end = "1975-76"
        MockNIRecordController.generateTableList(start, end) shouldBe Seq(
          "2016-17",
          "2015-16",
          "2014-15",
          "2013-14",
          "2012-13",
          "2011-12",
          "2010-11",
          "2009-10",
          "2008-09",
          "2007-08",
          "2006-07",
          "2005-06",
          "2004-05",
          "2003-04",
          "2002-03",
          "2001-02",
          "2000-01",
          "1999-00",
          "1998-99",
          "1997-98",
          "1996-97",
          "1995-96",
          "1994-95",
          "1993-94",
          "1992-93",
          "1991-92",
          "1990-91",
          "1989-90",
          "1988-89",
          "1987-88",
          "1986-87",
          "1985-86",
          "1984-85",
          "1983-84",
          "1982-83",
          "1981-82",
          "1980-81",
          "1979-80",
          "1978-79",
          "1977-78",
          "1976-77",
          "1975-76"
        )
      }
    }
  }

}
