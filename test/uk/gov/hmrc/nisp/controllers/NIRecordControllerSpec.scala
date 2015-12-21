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

import java.util.UUID

import org.scalatestplus.play.OneAppPerSuite
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.nisp.helpers.MockNIRecordController
import uk.gov.hmrc.play.http.SessionKeys
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.time.DateTimeUtils._

class NIRecordControllerSpec extends UnitSpec with OneAppPerSuite {
  val mockUserId = "/auth/oid/mockuser"
  val mockFullUserId = "/auth/oid/mockfulluser"
  val mockBlankUserId = "/auth/oid/mockblank"

  lazy val fakeRequest = FakeRequest()
  def authenticatedFakeRequest(userId: String) = FakeRequest().withSession(
    SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
    SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
    SessionKeys.userId -> userId
  )

  "GET /account/nirecord/gaps (gaps)" should {
    "return redirect for unauthenticated user" in {
      val result = MockNIRecordController.showGaps(fakeRequest)
      redirectLocation(result) shouldBe Some("http://localhost:9029/ida/login")
    }

    "return gaps page for user with gaps" in {
      val result = MockNIRecordController.showGaps(authenticatedFakeRequest(mockUserId))
      contentAsString(result) should include ("Years which are not full")
    }

    "return full page for user without gaps" in {
      val result = MockNIRecordController.showGaps(authenticatedFakeRequest(mockFullUserId))
      redirectLocation(result) shouldBe Some("/checkmystatepension/account/nirecord")
    }

    "return error page for blank response NINO" in {
      intercept[RuntimeException] {
        val result = MockNIRecordController.showGaps(authenticatedFakeRequest(mockBlankUserId))
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  "GET /account/nirecord (full)" should {
    "return redirect for unauthenticated user" in {
      val result = MockNIRecordController.showFull(fakeRequest)
      redirectLocation(result) shouldBe Some("http://localhost:9029/ida/login")
    }

    "return gaps page for user with gaps" in {
      val result = MockNIRecordController.showFull(authenticatedFakeRequest(mockUserId))
      contentAsString(result) should include ("All years.")
    }

    "return full page for user without gaps" in {
      val result = MockNIRecordController.showFull(authenticatedFakeRequest(mockFullUserId))
      contentAsString(result) should include ("All years.")
    }
  }

  "GET /account/nirecord/gapsandhowtocheck" should {
    "return redirect for unauthenticated user" in {
      val result = MockNIRecordController.showGapsAndHowToCheckThem(fakeRequest)
      redirectLocation(result) shouldBe Some("http://localhost:9029/ida/login")
    }

    "return how to check page for authenticated user" in {
      val result = MockNIRecordController.showGapsAndHowToCheckThem(authenticatedFakeRequest(mockUserId))
      contentAsString(result) should include ("Gaps in your record and how to check them")
    }
  }

  "GET /account/nirecord/voluntarycontribs" should {
    "return redirect for unauthenticated user" in {
      val result = MockNIRecordController.showVoluntaryContributions(fakeRequest)
      redirectLocation(result) shouldBe Some("http://localhost:9029/ida/login")
    }

    "return how to check page for authenticated user" in {
      val result = MockNIRecordController.showVoluntaryContributions(authenticatedFakeRequest(mockUserId))
      contentAsString(result) should include ("Voluntary contributions")
    }
  }
}
