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

import org.scalatestplus.play.OneAppPerSuite
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.nisp.helpers.MockExclusionController
import uk.gov.hmrc.play.frontend.auth.AuthenticationProviderIds
import uk.gov.hmrc.play.http.SessionKeys
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.time.DateTimeUtils._

class ExclusionControllerSpec extends UnitSpec with OneAppPerSuite {
  val fakeRequest = FakeRequest()
  
  val mockUsername = "mockuser"
  val mockUserId = "/auth/oid/" + mockUsername
  val mockUserIdExcluded = "/auth/oid/mockexcluded"

  "GET /exclusion" should {
    "return exclusion page for excluded user" in {
      val result = MockExclusionController.show()(fakeRequest.withSession(
        SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
        SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
        SessionKeys.userId -> mockUserIdExcluded,
        SessionKeys.authProvider -> AuthenticationProviderIds.VerifyProviderId
      ))
      contentAsString(result).contains("You are unable to use this service") shouldBe true
    }

    "return redirect to account page for non-excluded user" in {
      val result = MockExclusionController.show()(fakeRequest.withSession(
        SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
        SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
        SessionKeys.userId -> mockUserId,
        SessionKeys.authProvider -> AuthenticationProviderIds.VerifyProviderId
      ))

      redirectLocation(result) shouldBe Some("/checkmystatepension/account")
    }
  }
}
