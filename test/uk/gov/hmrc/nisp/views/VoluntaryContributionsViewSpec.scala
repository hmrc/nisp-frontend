/*
 * Copyright 2021 HM Revenue & Customs
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

import org.joda.time.DateTime
import org.scalatest._
import org.scalatest.mockito.MockitoSugar
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.LoginTimes
import uk.gov.hmrc.nisp.controllers.auth.{AuthDetails, AuthenticatedRequest}
import uk.gov.hmrc.nisp.fixtures.NispAuthedUserFixture
import uk.gov.hmrc.nisp.helpers.{FakeCachedStaticHtmlPartialRetriever, FakePartialRetriever, FakeTemplateRenderer, TestAccountBuilder}
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.nisp.views.html.nirecordVoluntaryContributions
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}
import play.api.inject.bind
import uk.gov.hmrc.renderer.TemplateRenderer

class VoluntaryContributionsViewSpec extends HtmlSpec with MockitoSugar with BeforeAndAfter with Injecting {

  implicit val cachedStaticHtmlPartialRetriever = FakeCachedStaticHtmlPartialRetriever
  implicit val user = NispAuthedUserFixture.user(TestAccountBuilder.regularNino)

  val authDetails = AuthDetails(ConfidenceLevel.L200, None, LoginTimes(DateTime.now(), None))

  implicit val fakeRequest = AuthenticatedRequest(FakeRequest(), user, authDetails)

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[FormPartialRetriever].to[FakePartialRetriever],
      bind[CachedStaticHtmlPartialRetriever].toInstance(FakeCachedStaticHtmlPartialRetriever),
      bind[TemplateRenderer].to(FakeTemplateRenderer)
    ).build()

  val expectedMoneyServiceLink = "https://www.moneyadviceservice.org.uk/en"
  val expectedCitizensAdviceLink = "https://www.citizensadvice.org.uk/"

  "Voluntary contributions view" should {
    lazy val sResult: nirecordVoluntaryContributions = inject[nirecordVoluntaryContributions]
    lazy val htmlAccountDoc = asDocument(sResult.apply().toString())


    "render with correct page title" in {
      assertElementContainsText(htmlAccountDoc, "head>title" , messages("nisp.nirecord.voluntarycontributions.heading") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
    }
    "render page with heading  Voluntary contributions " in {
      assertEqualsMessage(htmlAccountDoc, "h1.heading-large", "nisp.nirecord.voluntarycontributions.heading")
    }
    "render page with text  Before considering paying voluntary contributions you should" in {
      assertEqualsMessage(htmlAccountDoc, "p.lede", "nisp.nirecord.voluntarycontributions.title.message")
    }

    "render page with text  '1.  Find out how paying voluntary contributions affects your State Pension ' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > h2.heading-medium:nth-child(3)", "nisp.nirecord.voluntarycontributions.h2title.1")
    }
    "render page with text  'You can call the Future Pension Centre on 0800 731 0181 to find out more about how filling gaps can affect your State Pension forecast.' " in {
      assertElemetsOwnMessage(htmlAccountDoc, "article.content__body >p:nth-child(4)", "nisp.nirecord.voluntarycontributions.h2title.1.message", ".")
    }

    "render page with link  'yoyr state pension forecaste.' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body >p:nth-child(4)>a", "nisp.nirecord.voluntarycontributions.h2title.1.link1")
    }
    "render page with link href 'yoyr state pension forecaste.' " in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body >p:nth-child(4)>a", "/check-your-state-pension/account")
    }

    "render page with text  '2.  Seek guidance or financial advice' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > h2.heading-medium:nth-child(5)", "nisp.nirecord.voluntarycontributions.h2title.2")
    }
    "render page with text  'Paying voluntary contributions may not be your best option when planning for your retirement." in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > p:nth-child(6)", "nisp.nirecord.voluntarycontributions.h2title.2.message")
    }
    "render page with text  'Where you can get guidance and advice." in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > details:nth-child(7)>summary>span.summary", "nisp.nirecord.voluntarycontributions.h2title.2.help.title")
    }
    "render page with text  'You can seek professional financial advice. For free impartial guidance (not on GOV.UK):'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > details:nth-child(7)>div>p", "nisp.nirecord.voluntarycontributions.h2title.2.help.message")
    }
    "render page with text  'Money Advice Service (opens in new tab)'" in {
      assertEqualsMessage(htmlAccountDoc, "#moneyAdviceLink", "nisp.nirecord.voluntarycontributions.h2title.2.help.link1")
    }
    "render page with text ' Pension wise (opens in new tab)'" in {
      assertElemetsOwnMessage(htmlAccountDoc, "#pensionWiseLink", "nisp.nirecord.voluntarycontributions.h2title.2.help.link2")
    }
    "render page with link  'Pension wise (opens in new tab)'' " in {
      assertContainsMessageBetweenTags(htmlAccountDoc, "#pensionAdviceText", "nisp.nirecord.voluntarycontributions.h2title.2.help.link2.message", "article.content__body > details:nth-child(7)>div>ul.list-bullet>li:nth-child(3)>a")
    }
    "render page with text  'Citizens Advice (opens in new tab)'" in {
      assertEqualsMessage(htmlAccountDoc, "#citizenAdviceLink", "nisp.nirecord.voluntarycontributions.h2title.2.help.link3")
    }


    "render page with text  '3. Check if you may get extra pension income because you' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body >h2.heading-medium:nth-child(8)", "nisp.nirecord.voluntarycontributions.h2title.3")
    }
    "render page with text  'inherit or increase it from a partner' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > details:nth-child(9)>summary>span.summary", "nisp.nirecord.voluntarycontributions.h2title.3.linktitle1")
    }
    "render page with text  'Your amount may change if you:' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > details:nth-child(9)>div.panel-indent>p", "nisp.nirecord.voluntarycontributions.h2title.3.linktitle1.heading")
    }
    "render page with text  'are widowed, divorced or have dissolved your civil partnership' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > details:nth-child(9)>div.panel-indent>ul.list-bullet>li:nth-child(1)", "nisp.nirecord.voluntarycontributions.h2title.3.linktitle1.message1")
    }
    "render page with text  'paid married women’s reduced rate contributions' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > details:nth-child(9)>div.panel-indent>ul.list-bullet>li:nth-child(2)", "nisp.nirecord.voluntarycontributions.h2title.3.linktitle1.message2")
    }

    "render page with text  'lived or worked overseas' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > details:nth-child(10)>summary>span.summary", "nisp.nirecord.voluntarycontributions.h2title.3.linktitle2")
    }
    "render page with text  'You may be entitled to a State Pension from the country you lived or worked in. Contact the pension service of the country you were in to find out if you are eligible.' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > details:nth-child(10)>div.panel-indent>p", "nisp.nirecord.voluntarycontributions.h2title.3.linktitle2.message")
    }

    "render page with text  'lived or worked in the isle of man' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > details:nth-child(11)>summary>span.summary", "nisp.nirecord.voluntarycontributions.h2title.3.linktitle3")
    }
    "render page with text  'You may be entitled to some Retirement Pension from the Isle of Man. For more information about the Retirement Pension scheme, ' " in {
      assertEqualsMessage(htmlAccountDoc, "#IOM-message", "nisp.nirecord.voluntarycontributions.h2title.3.linktitle3.message1")
    }

    "render page with text  'back' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body > div.backlink:nth-child(12)>a", "nisp.back")
    }

    "render page with href value of link 'GOV.IM – Retirement Pension (opens in new window)'" in {
      assertLinkHasValue(htmlAccountDoc, "#nir-external-link", "https://www.gov.im/categories/benefits-and-financial-support/social-security-benefits/retirement-and-pensions/retirement-pension/")
    }

    "render page with href value of link 'Money advice service'" in {
      assertLinkHasValue(htmlAccountDoc, "#moneyAdviceLink", expectedMoneyServiceLink)

    }
    "render page with href value of link 'Pension wise'" in {
      assertLinkHasValue(htmlAccountDoc, "#pensionWiseLink", "https://www.pensionwise.gov.uk/")
    }
    "render page with href value of link 'Citizen advice'" in {
      assertLinkHasValue(htmlAccountDoc, "#citizenAdviceLink", expectedCitizensAdviceLink)

    }

    "render page with href value of link 'back'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body > div.backlink:nth-child(12)>a", "/check-your-state-pension/account/nirecord/gaps")
    }

  }

}
