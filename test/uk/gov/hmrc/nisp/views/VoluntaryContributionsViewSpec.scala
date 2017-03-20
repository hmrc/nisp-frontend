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

import org.scalatest._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.nisp.helpers.{LanguageToggle, _}
import uk.gov.hmrc.nisp.views.html.HtmlSpec
import uk.gov.hmrc.play.test.UnitSpec


class VoluntaryContributionsViewSpec extends UnitSpec with MockitoSugar with HtmlSpec with BeforeAndAfter with OneAppPerSuite {


  implicit val cachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever

  val mockUserNino = TestAccountBuilder.regularNino;
  val mockUserIdForecastOnly = "/auth/oid/mockforecastonly"
  val mockUsername = "mockuser"
  val mockUserId = "/auth/oid/" + mockUsername

  lazy val fakeRequest = FakeRequest();
  implicit override val lang =  LanguageToggle.getLanguageCode



  "Voluntary contributions view" should {

    lazy val sResult = html.nirecordVoluntaryContributions()
    lazy val htmlAccountDoc = asDocument(contentAsString(sResult))

    "render page with heading  Voluntary contributions " in {

      assertEqualsMessage(htmlAccountDoc, "h1.heading-large", "nisp.nirecord.voluntarycontributions.heading")
    }
    "render page with text  Before considering paying voluntary contributions you should" in {
      assertEqualsMessage(htmlAccountDoc, "p.lede", "nisp.nirecord.voluntarycontributions.title.message")
    }
    "render page with text  '1.  Check if you may get extra pension income because you' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > h2.heading-medium:nth-child(3)", "nisp.nirecord.voluntarycontributions.h2title.1")
    }
    "render page with text  'inherit or increase it from a partner' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > details:nth-child(4)>summary>span.summary", "nisp.nirecord.voluntarycontributions.h2title.1.linktitle1")
    }
    "render page with text  'Your amount may change if you:' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > details:nth-child(4)>div.panel-indent>p", "nisp.nirecord.voluntarycontributions.h2title.1.linktitle1.heading")
    }
    "render page with text  'are widowed, divorced or have dissolved your civil partnership' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > details:nth-child(4)>div.panel-indent>ul.list-bullet>li:nth-child(1)", "nisp.nirecord.voluntarycontributions.h2title.1.linktitle1.message1")
    }
    "render page with text  'paid married women’s reduced rate contributions' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > details:nth-child(4)>div.panel-indent>ul.list-bullet>li:nth-child(2)", "nisp.nirecord.voluntarycontributions.h2title.1.linktitle1.message2")
    }

    "render page with text  'lived or worked overseas' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > details:nth-child(5)>summary>span.summary", "nisp.nirecord.voluntarycontributions.h2title.1.linktitle2")
    }
    "render page with text  'You may be entitled to a State Pension from the country you lived or worked in. Contact the pension service of the country you were in to find out if you are eligible.' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > details:nth-child(5)>div.panel-indent>p", "nisp.nirecord.voluntarycontributions.h2title.1.linktitle2.message")
    }

    "render page with text  'lived or worked in the isle of man' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > details:nth-child(6)>summary>span.summary", "nisp.nirecord.voluntarycontributions.h2title.1.linktitle3")
    }
    "render page with text  'You may be entitled to some Retirement Pension from the Isle of Man. For more information about the Retirement Pension scheme, ' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > details:nth-child(6)>div.panel-indent>p", "nisp.nirecord.voluntarycontributions.h2title.1.linktitle3.message1")
    }
    "render page with text  '2.  Seek guidance or financial advice' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > h2.heading-medium:nth-child(7)", "nisp.nirecord.voluntarycontributions.h2title.2")
    }
    "render page with text  'Paying voluntary contributions may not be your best option when planning for your retirement." in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > p:nth-child(8)", "nisp.nirecord.voluntarycontributions.h2title.2.message")
    }
    "render page with text  'Where you can get guidance and advice." in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > details:nth-child(9)>summary>span.summary", "nisp.nirecord.voluntarycontributions.h2title.2.help.title")
    }
    "render page with text  'You can seek professional financial advice. For free impartial guidance (not on GOV.UK):'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > details:nth-child(9)>div>p", "nisp.nirecord.voluntarycontributions.h2title.2.help.message")
    }
    "render page with text  'Money Advice Service (opens in new tab)'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > details:nth-child(9)>div>ul.list-bullet>li:nth-child(1)", "nisp.nirecord.voluntarycontributions.h2title.2.help.link1")
    }
    "render page with text  ' Pension wise (opens in new tab)'" in {
      assertElemetsOwnMessage(htmlAccountDoc, "article.content__body > details:nth-child(9)>div>ul.list-bullet>li:nth-child(2)", "nisp.nirecord.voluntarycontributions.h2title.2.help.link2.message")
    }
    "render page with link  'Pension wise (opens in new tab)'' " in {
      assertContainsMessageBetweenTags(htmlAccountDoc, "article.content__body >details:nth-child(9)>div>ul.list-bullet>li:nth-child(2)", "nisp.nirecord.voluntarycontributions.h2title.2.help.link2.message","details:nth-child(9)>div>ul.list-bullet>li:nth-child(2)>a")
    }
    "render page with text  'Citizens Advice (opens in new tab)'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > details:nth-child(9)>div>ul.list-bullet>li:nth-child(3)", "nisp.nirecord.voluntarycontributions.h2title.2.help.link3")
    }
    "render page with text  '3.  Find out how paying voluntary contributions affects your State Pension ' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > h2.heading-medium:nth-child(10)", "nisp.nirecord.voluntarycontributions.h2title.3")
    }
    "render page with text  'to find out more about how filling gaps can affect your pension.' " in {
      assertContainsMessageBetweenTags(htmlAccountDoc, "article.content__body >p:nth-child(11)", "nisp.nirecord.voluntarycontributions.h2title.3.message","article.content__body >p:nth-child(11)>a")
    }

    "render page with link  'to find out more about how filling gaps can affect your pension.' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body >p:nth-child(11)>a", "nisp.nirecord.voluntarycontributions.h2title.3.link1")
    }
    "render page with text  'back' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > div.backlink:nth-child(12)>a", "nisp.back")
    }

    "render page with href value of link 'GOV.IM – Retirement Pension (opens in new window)'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body > details:nth-child(6)>div.panel-indent>p>a", "https://www.gov.im/categories/benefits-and-financial-support/social-security-benefits/retirement-and-pensions/retirement-pension/")
    }

    "render page with href value of link 'Money advice service'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body > details:nth-child(9)>div>ul.list-bullet>li:nth-child(1)>a", "https://www.moneyadviceservice.org.uk/en")

    }
    "render page with href value of link 'Pension wise'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body > details:nth-child(9)>div>ul.list-bullet>li:nth-child(2)>a", "https://www.pensionwise.gov.uk/")
    }
    "render page with href value of link 'Citizen advice'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body > details:nth-child(9)>div>ul.list-bullet>li:nth-child(3)>a", "https://www.citizensadvice.org.uk/")

    }

    "render page with href value of link 'Contact the future pension Center'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body >p:nth-child(11)>a", "https://www.gov.uk/future-pension-centre")
    }

    "render page with href value of link 'back'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body > div.backlink:nth-child(12)>a", "/check-your-state-pension/account/nirecord/gaps")
    }

  }

}
