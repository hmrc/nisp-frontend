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

import org.scalatestplus.play.OneAppPerSuite
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.nisp.config.wiring.NispCachedStaticHtmlPartialRetriever
import uk.gov.hmrc.nisp.controllers.connectors.CustomAuditConnector
import uk.gov.hmrc.nisp.helpers.{MockCachedStaticHtmlPartialRetriever, MockCustomAuditConnector, MockAccountController}
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec

class QuestionnaireControllerSpec extends UnitSpec with OneAppPerSuite {
  val fakeRequest = FakeRequest("GET", "/")

  val testQuestionnaireController: QuestionnaireController = new QuestionnaireController {
    override val customAuditConnector: CustomAuditConnector = MockCustomAuditConnector
    override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
  }

  "GET /questionnaire" should {
    "return questionnaire page" in {
      val result = testQuestionnaireController.show(fakeRequest)
      contentAsString(result).contains("signed out of your account") shouldBe true
      contentAsString(result).contains("Give us feedback to help us improve this service.") shouldBe true
    }
  }

  "POST /questionnaire" should {
    "return thank you page for submitted form" in {
      val result = testQuestionnaireController.submit(fakeRequest.withFormUrlEncodedBody(
        ("easytouse", "2"),
        ("useitbyyourself", "2"),
        ("likelytouse", "2"),
        ("likelytoseek", "2"),
        ("recommend", "1"),
        ("satisfied", "2"),
        ("takepart", "1"),
        ("research", "0"),
        ("email", "testuser@gmail.com"),
        ("nextsteps", "nextsteps1"),
        ("name", "test"),
        ("nino", uk.gov.hmrc.nisp.helpers.TestAccountBuilder.randomNino.nino)
      ))
      redirectLocation(result) shouldBe Some("/check-your-state-pension/finished")
    }

    "return an error page for selecting 'yes' to research but not specifying email address" in {
      val result = testQuestionnaireController.submit(fakeRequest.withFormUrlEncodedBody(
        ("easytouse", "2"),
        ("useitbyyourself", "2"),
        ("likelytouse", "2"),
        ("research", "0"),
        ("email", ""),
        ("name", "test"),
        ("nino", uk.gov.hmrc.nisp.helpers.TestAccountBuilder.randomNino.nino)
      ))
      contentAsString(result).contains("Error summary")
    }
    
     "return an error page for selecting 'yes' to research and specifying wrong email format address" in {
      val result = testQuestionnaireController.submit(fakeRequest.withFormUrlEncodedBody(
        ("easytouse", "2"),
        ("useitbyyourself", "2"),
        ("likelytouse", "2"),
        ("research", "0"),
        ("email", "testuser@"),
        ("name", "test"),
        ("nino", uk.gov.hmrc.nisp.helpers.TestAccountBuilder.randomNino.nino)
      ))
      contentAsString(result).contains("Error summary")
    }
  }

  "GET /finished" should {
    "return 200" in {
      val result = testQuestionnaireController.showFinished(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return HTML" in {
      val result = testQuestionnaireController.showFinished(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }
  }

}
