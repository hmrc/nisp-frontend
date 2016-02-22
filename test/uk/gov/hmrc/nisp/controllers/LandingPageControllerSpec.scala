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
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.http._
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.helpers.MockCitizenDetailsService
import uk.gov.hmrc.nisp.services.{CitizenDetailsService, NpsAvailabilityChecker}
import uk.gov.hmrc.play.frontend.auth.AuthenticationProviderIds
import uk.gov.hmrc.play.http.SessionKeys
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.time.DateTimeUtils._

class LandingPageControllerSpec extends UnitSpec with OneAppPerSuite {

  val fakeRequest = FakeRequest("GET", "/")

  def testLandingPageController(testNow: LocalDateTime): LandingController = new LandingController {
    override val npsAvailabilityChecker: NpsAvailabilityChecker = new NpsAvailabilityChecker {
      override def now: LocalDateTime = testNow
    }
    override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
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
      val result = LandingController.showNotAuthorised(fakeRequest)
      contentAsString(result) should include ("We were unable to confirm your identity")
    }
  }
}
