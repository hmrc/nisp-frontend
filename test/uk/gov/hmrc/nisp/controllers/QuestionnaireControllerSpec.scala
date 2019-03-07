/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.nisp.controllers.connectors.CustomAuditConnector
import uk.gov.hmrc.nisp.helpers.{MockCachedStaticHtmlPartialRetriever, MockCustomAuditConnector, TestAccountBuilder}
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.nisp.utils.MockTemplateRenderer
import uk.gov.hmrc.renderer.TemplateRenderer

class QuestionnaireControllerSpec extends UnitSpec with OneAppPerSuite {
  val fakeRequest = FakeRequest("GET", "/")

  val testQuestionnaireController: QuestionnaireController = new QuestionnaireController {
    override val customAuditConnector: CustomAuditConnector = MockCustomAuditConnector
    override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
    override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
  }

  "GET /questionnaire" should {
    "return questionnaire page" in {
      val result = testQuestionnaireController.show(fakeRequest)
      contentAsString(result).contains("What did you think of this service?") shouldBe true
      contentAsString(result).contains("Give us feedback to help us improve this service.") shouldBe true
      contentAsString(result).contains("What you can do next")
      contentAsString(result).contains("Pension Wise - understanding your pension options")
      contentAsString(result).contains("Planning your retirement income")
      contentAsString(result).contains("Defer your State Pension")
      contentAsString(result).contains("Contact the Pension Service")
      contentAsString(result).contains("More on State Pensions")
      contentAsString(result).contains("http://gov.uk/browse/working/state-pension")
      contentAsString(result).contains("https://gov.uk/contact-pension-service")
      contentAsString(result).contains("http://gov.uk/deferring-state-pension")
      contentAsString(result).contains("https://gov.uk/plan-retirement-income")
      contentAsString(result).contains("https://pensionwise.gov.uk")

    }
  }

  "POST /questionnaire" should {
    "return thank you page for submitted form" in {
      val result = testQuestionnaireController.submit(fakeRequest.withFormUrlEncodedBody(
        ("easytouse", "2"),
        ("useitbyyourself", "2"),
        ("likelytouse", "2"),
        ("satisfied", "2"),
        ("understanding", "1"),
        ("whatWillYouDoNext", "4"),
        ("otherFollowUp", ""),
        ("improve", "Lorem ipsum dolor sit amet, consectetur adipiscing elit."),
        ("research", "0"),
        ("email", "testuser@gmail.com"),
        ("name", "test"),
        ("nino", TestAccountBuilder.randomNino.nino)
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
        ("nino", TestAccountBuilder.randomNino.nino)
      ))
      contentAsString(result).contains("Error summary")
    }

    "return an error page for selecting [Other] to [What Will You Do Next], but not specifying any text" in {
      val result = testQuestionnaireController.submit(fakeRequest.withFormUrlEncodedBody(
        ("easytouse", "2"),
        ("useitbyyourself", "2"),
        ("likelytouse", "2"),
        ("whatWillYouDoNext", "9"),
        ("otherFollowUp", ""),
        ("research", "0"),
        ("email", ""),
        ("name", "test"),
        ("nino", TestAccountBuilder.randomNino.nino)
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
        ("nino", TestAccountBuilder.randomNino.nino)
      ))
      contentAsString(result).contains("Error summary")
    }

    "return a thank you page for entrying 1199 characters in improve field" in {
      val result = testQuestionnaireController.submit(fakeRequest.withFormUrlEncodedBody(
        ("email", ""),
        ("name", "test"),
        ("nino", TestAccountBuilder.randomNino.nino),
        ("improve", "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin nec maximus turpis. Integer sollicitudin, ante sed finibus tincidunt, orci sem volutpat arcu, non rutrum mi nisl vitae neque. Donec vehicula, ante nec tempor condimentum, augue dolor congue mauris, a cursus sem nisl ut ligula. Quisque rhoncus bibendum metus, ac sollicitudin mauris placerat vitae. Curabitur facilisis ante et pharetra bibendum. Vestibulum sed justo nec leo porta tempor iaculis nec ex. Proin blandit tincidunt vulputate. Ut finibus metus mi. Pellentesque non volutpat lacus. Phasellus sollicitudin magna tortor, a viverra justo aliquet a. Cras nulla diam, blandit sed nulla non, convallis feugiat velit. Suspendisse diam ex, molestie at euismod a, auctor vitae purus. Aenean non consequat neque. Suspendisse et suscipit tortor. Sed eget dictum mi.\n\nPraesent ac odio non nulla tempus faucibus. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Proin ut lectus ac dolor vestibulum vulputate. Donec feugiat ac est sed pharetra. Mauris metus erat, bibendum sed velit nec, venenatis semper ipsum. Vivamus vitae magna nec eros porta egestas in gravida quam. Phasellus maximus posue")
      ))
      redirectLocation(result) shouldBe Some("/check-your-state-pension/finished")
    }
    "return a thank you page for entrying 254 characters in [Please state] field" in {
      val result = testQuestionnaireController.submit(fakeRequest.withFormUrlEncodedBody(
        ("email", ""),
        ("name", "test"),
        ("nino", TestAccountBuilder.randomNino.nino),
        ("whatWillYouDoNext", "8"),
        ("otherFollowUp", "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec et enim pulvinar, lobortis lectus blandit, consectetur risus. Fusce malesuada elit a tellus efficitur, nec suscipit lorem maximus. Ut at odio quam. Maecenas at dolor ut lectus bibendum metus.")
      ))
      redirectLocation(result) shouldBe Some("/check-your-state-pension/finished")
    }

     "return an error page for entrying 256 characters in [Please state] field" in {
      val result = testQuestionnaireController.submit(fakeRequest.withFormUrlEncodedBody(
        ("email", ""),
        ("name", "test"),
        ("nino", TestAccountBuilder.randomNino.nino),
        ("whatWillYouDoNext", "8"),
        ("otherFollowUp", "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec et enim pulvinar, lobortis lectus blandit, consectetur risus. Fusce malesuada elit a tellus efficitur, nec suscipit lorem maximus. Ut at odio quam. Maecenas at dolor ut lectus bibendum a metus donec.")
      ))
      contentAsString(result).contains("Error summary")
    }
    "return an error page for entrying 1203 characters in improve field" in {
      val result = testQuestionnaireController.submit(fakeRequest.withFormUrlEncodedBody(
        ("email", ""),
        ("improve", "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin nec maximus turpis. Integer sollicitudin, ante sed finibus tincidunt, orci sem volutpat arcu, non rutrum mi nisl vitae neque. Donec vehicula, ante nec tempor condimentum, augue dolor congue mauris, a cursus sem nisl ut ligula. Quisque rhoncus bibendum metus, ac sollicitudin mauris placerat vitae. Curabitur facilisis ante et pharetra bibendum. Vestibulum sed justo nec leo porta tempor iaculis nec ex. Proin blandit tincidunt vulputate. Ut finibus metus mi. Pellentesque non volutpat lacus. Phasellus sollicitudin magna tortor, a viverra justo aliquet a. Cras nulla diam, blandit sed nulla non, convallis feugiat velit. Suspendisse diam ex, molestie at euismod a, auctor vitae purus. Aenean non consequat neque. Suspendisse et suscipit tortor. Sed eget dictum mi.\n\nPraesent ac odio non nulla tempus faucibus. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Proin ut lectus ac dolor vestibulum vulputate. Donec feugiat ac est sed pharetra. Mauris metus erat, bibendum sed velit nec, venenatis semper ipsum. Vivamus vitae magna nec eros porta egestas in gravida quam. Phasellus maximus posuere.eee")
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
