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

import org.scalatestplus.play.OneAppPerSuite
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.nisp.helpers.MockAccountController
import uk.gov.hmrc.play.test.UnitSpec

class QuestionnaireControllerSpec extends UnitSpec with OneAppPerSuite {
  val fakeRequest = FakeRequest("GET", "/")

  "GET /questionnaire" should {
    "return questionnaire page" in {
      val result = QuestionnaireController.show(fakeRequest)
      contentAsString(result).contains("signed out of your account") shouldBe true
      contentAsString(result).contains("Give us feedback to help us improve this service.") shouldBe true
    }
  }

  "POST /questionnaire" should {
    "return form with errors for invalid selections" in {
      val result = QuestionnaireController.submit(fakeRequest)
      contentAsString(result).contains("This field is required") shouldBe true
    }

    "return thank you page for valid form" in {
      val result = QuestionnaireController.submit(fakeRequest.withFormUrlEncodedBody(
        ("easytouse", "2"),
        ("useitbyyourself", "2"),
        ("likelytouse", "2"),
        ("likelytoseek", "2"),
        ("recommend", "1"),
        ("satisfied", "2"),
        ("takepart", "1"),
        ("nextsteps", "nextsteps1"),
        ("name", "test"),
        ("nino", "AY112233A")
      ))
      redirectLocation(result) shouldBe Some("/checkmystatepension/finished")
    }
  }

  "GET /finished" should {
    "return 200" in {
      val result = QuestionnaireController.showFinished(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return HTML" in {
      val result = QuestionnaireController.showFinished(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }
  }

}
