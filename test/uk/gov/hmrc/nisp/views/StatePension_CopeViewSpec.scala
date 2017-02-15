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

package uk.gov.hmrc.nisp.views

import org.joda.time.LocalDate
import org.scalatest._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.models.enums.Exclusion
import uk.gov.hmrc.nisp.views.html.HtmlSpec
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.play.views.formatting.Dates

class StatePension_CopeViewSpec extends UnitSpec with MockitoSugar with HtmlSpec with BeforeAndAfter with OneAppPerSuite {


  implicit val cachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever

  val mockUserNino = TestAccountBuilder.regularNino;
  val mockUserIdForecastOnly =  "/auth/oid/mockforecastonly"
  val mockUsername = "mockuser"
  val mockUserId = "/auth/oid/" + mockUsername

  lazy val fakeRequest = FakeRequest();

  "State Pension - Contracted out View" should {

    lazy val sResult = html.statepension_cope(99.54,true)
    lazy val htmlAccountDoc = asDocument(contentAsString(sResult))

    "render page with heading  you were contracted out " in {
      assertEqualsMessage(htmlAccountDoc ,"article.content__body>h1.heading-large" , "nisp.cope.youWereContractedOut")
    }
    "render page with text  'In the past you’ve been part of one or more contracted out pension schemes, such as workplace or personal pension schemes.' " in {
      assertEqualsMessage(htmlAccountDoc ,"article.content__body>p:nth-child(2)" , "nisp.cope.inThePast")
    }
    "render page with text 'when you were contracted out:'" in {
      assertEqualsMessage(htmlAccountDoc ,"article.content__body>p:nth-child(3)" , "nisp.cope.why")
    }
    "render page with text 'you and your employers paid lower rate National Insurance contributions; or'" in {
      assertEqualsMessage(htmlAccountDoc ,"article.content__body>ul:nth-child(4)>li:nth-child(1)" , "nisp.cope.why.bullet1")
    }
    "render page with text 'some of your National Insurance contributions were paid into your private pension schemes instead'" in {
      assertEqualsMessage(htmlAccountDoc ,"article.content__body>ul:nth-child(4)>li:nth-child(2)" , "nisp.cope.why.bullet2")
    }
    "render page with heading 'Contracted Out Pension Equivalent (COPE)'" in {
      assertEqualsMessage(htmlAccountDoc ,"article.content__body>h2:nth-child(5)" , "nisp.cope.title2")
    }
    "render page with heading 'Your workplace or personal pension scheme should include an amount of pension which will, in most cases, be equal to the additional State Pension you would have been paid. " +
      "We call this amount your Contracted Out Pension Equivalent (COPE). Your COPE estimate is shown below.'" in {
      assertEqualsMessage(htmlAccountDoc ,"article.content__body>p:nth-child(6)" , "nisp.cope.definition")
    }
    "render page with heading 'The COPE amount is paid as part of your other pension schemes, not by the government. " +
      "The total amount of pension paid by your workplace or personal pension scheme will depend on the scheme and on any investment choices.'" in {
      assertEqualsMessage(htmlAccountDoc ,"article.content__body>p:nth-child(7)" , "nisp.cope.workplace")
    }
    /*"render page with heading 'your cope estimate is'" in {
      assertContainsDynamicMessage(htmlAccountDoc ,"article.content__body>p:nth-child(7)" , "nisp.cope.table.estimate.title" , )
    }*/
  }
}
