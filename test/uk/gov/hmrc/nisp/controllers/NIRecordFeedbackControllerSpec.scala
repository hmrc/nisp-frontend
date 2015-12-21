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
import uk.gov.hmrc.nisp.helpers.MockNIRecordFeedbackController
import uk.gov.hmrc.play.http.SessionKeys
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.time.DateTimeUtils._

class NIRecordFeedbackControllerSpec extends UnitSpec with OneAppPerSuite {
  val fakeRequest = FakeRequest()
  val mockUserId = "/auth/oid/mockuser"
  val mockBlankUserId = "/auth/oid/mockblank"

  "GET /account/nirecord/feedback" should {
    "redirect to NIRecordFeedback page when user click on Give us your feedback link " in {
      val result = MockNIRecordFeedbackController.show()(fakeRequest.withSession(
        SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
        SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
        SessionKeys.userId -> mockUserId
      ))
      status(result) shouldBe Status.OK
    }

    "show error page if bad niresponse" in {
      val exception = intercept[RuntimeException] {
        val result = MockNIRecordFeedbackController.show()(fakeRequest.withSession(
          SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
          SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
          SessionKeys.userId -> mockBlankUserId
        ))
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  "POST /account/nirecord/feedback" should {
    "submit NIRecordFeedback when user click submit button" in {
      val result = MockNIRecordFeedbackController.submit()(fakeRequest.withSession(
        SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
        SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
        SessionKeys.userId -> mockUserId
      ).withFormUrlEncodedBody(
        ("comments","test"),
        ("updatingRecord","updatingRecordTest")
      )
      )
      redirectLocation(result) shouldBe Some("/checkmystatepension/account/nirecord/feedback/thankyou")
    }

    "show errors when field incorrect" in {
      val result = MockNIRecordFeedbackController.submit()(fakeRequest.withSession(
        SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
        SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
        SessionKeys.userId -> mockUserId
      ).withFormUrlEncodedBody(
        ("comments","a" * 1201),
        ("hasGaps", "true"),
        ("updatingRecord","updatingRecordTest")
      )
      )
      redirectLocation(result) should not be Some("/checkmystatepension/account/nirecord/feedback/thankyou")
      contentAsString(result) should include ("Maximum length is 1,200")
    }
  }

  "GET /account/nirecord/feedback/thankyou" should {
    "redirect to NIRecordFeedback Thank you page when user submits feedback" in {
      val result = MockNIRecordFeedbackController.showThankYou()(fakeRequest.withSession(
        SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
        SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
        SessionKeys.userId -> mockUserId
      ))
      status(result) shouldBe Status.OK
    }
  }
}
