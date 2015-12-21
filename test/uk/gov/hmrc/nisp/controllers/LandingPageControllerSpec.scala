/*
 * Copyright 2015 HM Revenue & Customs
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

import org.joda.time.LocalDateTime
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.http._
import uk.gov.hmrc.nisp.services.NpsAvailabilityChecker
import uk.gov.hmrc.play.test.UnitSpec

class LandingPageControllerSpec extends UnitSpec with OneAppPerSuite {

  val fakeRequest = FakeRequest("GET", "/")

  def testLandingPageController(testNow: LocalDateTime): LandingController = new LandingController {
    override val npsAvailabilityChecker: NpsAvailabilityChecker = new NpsAvailabilityChecker {
      override def now: LocalDateTime = testNow
    }
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
      val headingText = Messages("nisp.landing.summary")
      contentAsString(result) should include (headingText)
    }

    "have a start button" in {
      val result = LandingController.show(fakeRequest)
      val buttonText = Messages("nisp.start")
      contentAsString(result) should include (s"$buttonText</a>")
    }
  }

  "GET /guidancenotes" should {
    "return guidance notes page" in {
      val result = LandingController.showGuidanceNotes(fakeRequest)
      contentAsString(result) should include ("The information provided is based on the rules of the new State Pension, which starts on 6 April 2016.")
    }
  }

  "GET /pre-verify" should {
    "return pre-verify page" in {
      val result = LandingController.showPreVerify(fakeRequest)
      contentAsString(result) should include ("Verify your identity")
    }
  }

  "GET /service-unavailable" should {
    "return service unavailable page" in {
      val result = LandingController.showNpsUnavailable(fakeRequest)
      contentAsString(result) should include ("The service is unavailable due to maintenance")
    }
  }
}
