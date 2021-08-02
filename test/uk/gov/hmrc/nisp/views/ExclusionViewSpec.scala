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
import java.time.LocalDate

import org.scalatest.mockito.MockitoSugar
import play.api.test.{FakeRequest, Injecting}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.{LoginTimes, Name}
import uk.gov.hmrc.nisp.controllers.auth.{AuthDetails, AuthenticatedRequest, ExcludedAuthenticatedRequest, NispAuthedUser}
import uk.gov.hmrc.nisp.fixtures.NispAuthedUserFixture
import uk.gov.hmrc.nisp.helpers.{FakeCachedStaticHtmlPartialRetriever, FakePartialRetriever, FakeTemplateRenderer, TestAccountBuilder}
import uk.gov.hmrc.nisp.models.{Exclusion, UserName}
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.nisp.utils.LanguageHelper.langUtils.Dates
import uk.gov.hmrc.nisp.views.html.{excluded_dead, excluded_mci, excluded_sp}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer

class ExclusionViewSpec extends HtmlSpec with MockitoSugar with Injecting {

  implicit val cachedStaticHtmlPartialRetriever = FakeCachedStaticHtmlPartialRetriever
  implicit val templateRenderer: TemplateRenderer = FakeTemplateRenderer
  implicit val fakeRequest = AuthenticatedRequest(FakeRequest(), TestAccountBuilder.nispAuthedUser,
    AuthDetails(ConfidenceLevel.L200, Some("GovernmentGateway"), LoginTimes(DateTime.now(), None)))
  implicit val user: NispAuthedUser = NispAuthedUserFixture.user(TestAccountBuilder.regularNino)


  "Exclusion Dead" should {
    lazy val sResult = inject[excluded_dead]
    lazy val htmlAccountDoc = asDocument(sResult(Exclusion.Dead, Some(65)).toString)

    "not render the UR banner" in {
      val urBanner = htmlAccountDoc.getElementById("full-width-banner")
      assert(urBanner == null)
    }


    "render with correct page title" in {
      assertElementContainsText(htmlAccountDoc, "head>title" ,messages("nisp.excluded.title") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
    }

    "render page with heading  You are unable to use this service " in {

      assertEqualsMessage(htmlAccountDoc, "article.content__body>h1", "nisp.excluded.title")
    }
    "render page with message  'Please contact HMRC National Insurance helpline on 0300 200 3500.' " in {

      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(2)", "nisp.excluded.dead")
    }
    "render page with message  'opening times' " in {

      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(3)", "nisp.excluded.dead.openingtime")
    }
    "render page with message  '8am to 8pm, Monday to Friday' " in {

      assertEqualsMessage(htmlAccountDoc, "article.content__body>ul:nth-child(4)>li:nth-child(1)", "nisp.excluded.dead.openingtime.weekdays")
    }
    "render page with message  '8am to 4pm, Saturday' " in {

      assertEqualsMessage(htmlAccountDoc, "article.content__body>ul:nth-child(4)>li:nth-child(2)", "nisp.excluded.dead.openingtime.saturday")
    }
    "render page with message  'Closed Sundays and bank holidays' " in {

      assertEqualsMessage(htmlAccountDoc, "article.content__body>ul:nth-child(4)>li:nth-child(3)", "nisp.excluded.dead.openingtime.holidays")
    }
  }

  "Exclusion Isle of Man : Can't see NI Record: State Pension Age under consideration - no flag" should {

    lazy val sResult = inject[excluded_sp]
    lazy val htmlAccountDoc = asDocument(sResult(Exclusion.IsleOfMan, Some(40), Some(LocalDate.of(2019, 9, 6)), false, None).toString)

    "render the UR banner" in {
      assert(htmlAccountDoc.getElementsByClass("full-width-banner__title") != null)
    }

    "render with correct page title" in {
      assertElementContainsText(htmlAccountDoc, "head>title" ,messages("nisp.main.h1.title") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
    }


    "render page with heading  'Your State Pension'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h1", "nisp.main.h1.title")

    }
    "render page with heading  'You’ll reach State Pension age on 6 sep 2019' " in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>h2.heading-medium", "nisp.excluded.willReach", Dates.formatDate(LocalDate.of(2019, 9, 6)), null, null)

    }

    "render page with message  We’re unable to calculate your State Pension, as the Isle of Man Government is currently undertaking a review of its Retirement Pension scheme. It will not be adopting the new State Pension reforms." in {
      assertEqualsMessage(htmlAccountDoc, "div.panel-indent>p:nth-child(1)", "nisp.excluded.isleOfMan.sp.line1")
    }

    "render page with message 'For more information about the Retirement Pension scheme, visit' " in {
      assertEqualsMessage(htmlAccountDoc, "div.panel-indent>p:nth-child(2)", "nisp.excluded.isleOfMan.sp.line2")
    }

    "render page with message 'In the meantime, you can contact the Future Pension centre to get and estimate of your State Pension ,bases on your current National Insurence record....' " in {
      assertEqualsMessage(htmlAccountDoc, "div.panel-indent>p:nth-child(3)", "nisp.excluded.contactFuturePensionCentre")
    }

    "render page with message 'To get a copy of your National Insurence record so far,contact the National Insurence helpline ....' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p", "nisp.excluded.contactNationalInsuranceHelplineIom")
    }

    //No state pension age under consideration message
    "not render page with heading  'Proposed change to your State Pension age'" in {
      assertPageDoesNotContainMessage(htmlAccountDoc, ("nisp.spa.under.consideration.title"))
    }

    "not render page with text  'Youll reach State Pension age on 6 Sep 2015. Under government proposals this may increase by up to a year.'" in {
      assertPageDoesNotContainDynamicMessage(htmlAccountDoc, "nisp.spa.under.consideration.detail", Dates.formatDate(LocalDate.of(2015, 9, 6)))
    }
  }

  "Exclusion Isle of Man : Can't see NI Record: State Pension Age under consideration - false" should {

    lazy val sResult = inject[excluded_sp]
    lazy val htmlAccountDoc = asDocument(sResult(Exclusion.IsleOfMan, Some(40), Some(LocalDate.of(2019, 9, 6)), false, Some(false)).toString)

    "render the UR banner" in {
      assert(htmlAccountDoc.getElementsByClass("full-width-banner__title") != null)
    }

    "render with correct page title" in {
      assertElementContainsText(htmlAccountDoc, "head>title" ,messages("nisp.main.h1.title") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
    }

    "render page with heading  'Your State Pension'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h1", "nisp.main.h1.title")

    }
    "render page with heading  'You’ll reach State Pension age on 6 sep 2019' " in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>h2.heading-medium", "nisp.excluded.willReach", Dates.formatDate(LocalDate.of(2019, 9, 6)), null, null)

    }

    "render page with message  We’re unable to calculate your State Pension, as the Isle of Man Government is currently undertaking a review of its Retirement Pension scheme. It will not be adopting the new State Pension reforms." in {
      assertEqualsMessage(htmlAccountDoc, "div.panel-indent>p:nth-child(1)", "nisp.excluded.isleOfMan.sp.line1")
    }

    "render page with message 'For more information about the Retirement Pension scheme, visit' " in {
      assertEqualsMessage(htmlAccountDoc, "div.panel-indent>p:nth-child(2)", "nisp.excluded.isleOfMan.sp.line2")
    }

    "render page with message 'In the meantime, you can contact the Future Pension centre to get and estimate of your State Pension ,bases on your current National Insurence record....' " in {
      assertEqualsMessage(htmlAccountDoc, "div.panel-indent>p:nth-child(3)", "nisp.excluded.contactFuturePensionCentre")
    }

    "render page with message 'To get a copy of your National Insurence record so far,contact the National Insurence helpline ....' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p", "nisp.excluded.contactNationalInsuranceHelplineIom")
    }

    //No state pension age under consideration message
    "not render page with heading  'Proposed change to your State Pension age'" in {
      assertPageDoesNotContainMessage(htmlAccountDoc, ("nisp.spa.under.consideration.title"))
    }

    "not render page with text  'Youll reach State Pension age on 6 Sep 2015. Under government proposals this may increase by up to a year.'" in {
      assertPageDoesNotContainDynamicMessage(htmlAccountDoc, "nisp.spa.under.consideration.detail", Dates.formatDate(LocalDate.of(2015, 9, 6)))
    }
  }

  "Exclusion Isle of Man : Can't see NI Record: State Pension Age under consideration - true" should {

    lazy val sResult = inject[excluded_sp]
    lazy val htmlAccountDoc = asDocument(sResult(Exclusion.IsleOfMan, Some(40), Some(LocalDate.of(2019, 9, 6)), false, Some(true)).toString)

    "render the UR banner" in {
      assert(htmlAccountDoc.getElementsByClass("full-width-banner__title") != null)
    }

    "render with correct page title" in {
      assertElementContainsText(htmlAccountDoc, "head>title" ,messages("nisp.main.h1.title") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
    }

    "render page with heading  'Your State Pension'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h1", "nisp.main.h1.title")

    }
    "render page with heading  'You’ll reach State Pension age on 6 Sep 2019' " in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>h2.heading-medium", "nisp.excluded.willReach", Dates.formatDate(LocalDate.of(2019, 9, 6)), null, null)

    }

    "render page with message  We’re unable to calculate your State Pension, as the Isle of Man Government is currently undertaking a review of its Retirement Pension scheme. It will not be adopting the new State Pension reforms." in {
      assertEqualsMessage(htmlAccountDoc, "div.panel-indent>p:nth-child(1)", "nisp.excluded.isleOfMan.sp.line1")
    }

    "render page with message 'For more information about the Retirement Pension scheme, visit' " in {
      assertEqualsMessage(htmlAccountDoc, "div.panel-indent>p:nth-child(2)", "nisp.excluded.isleOfMan.sp.line2")
    }

    "render page with message 'In the meantime, you can contact the Future Pension centre to get and estimate of your State Pension ,bases on your current National Insurence record....' " in {
      assertEqualsMessage(htmlAccountDoc, "div.panel-indent>p:nth-child(3)", "nisp.excluded.contactFuturePensionCentre")
    }

    "render page with message 'To get a copy of your National Insurence record so far,contact the National Insurence helpline ....' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(4)", "nisp.excluded.contactNationalInsuranceHelplineIom")
    }

    //state pension age under consideration message
    "render page with heading  'Proposed change to your State Pension age'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(5)", "nisp.spa.under.consideration.title")
    }

    "render page with text  'Youll reach State Pension age on 6 Sep 2019. Under government proposals this may increase by up to a year.'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(6)", "nisp.spa.under.consideration.detail", Dates.formatDate(LocalDate.of(2019, 9, 6)))
    }
  }

  "Exclusion Manual Correspondence Indicator(MCI)" should {

    implicit val authDetails = AuthDetails(ConfidenceLevel.L200, Some("GovernmentGateway"), LoginTimes(DateTime.now, None))

    lazy val sResult = inject[excluded_mci]

    lazy val htmlAccountDoc = asDocument(sResult(Exclusion.ManualCorrespondenceIndicator, Some(40)).toString)

    "render with correct page title" in {
      assertElementContainsText(htmlAccountDoc, "head>title" ,messages("nisp.excluded.mci.title") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
    }
    "not render the UR banner" in {
      assert(htmlAccountDoc.getElementById("full-width-banner") == null)
    }

    "render page with heading  'You cannot access your account right now'" in {
      assertEqualsMessage(htmlAccountDoc, "h1.heading-large", "nisp.excluded.mci.title")

    }
    "render page with text  'We need to speak to you before you can log in to the service.' " in {
      assertEqualsMessage(htmlAccountDoc, "p.lede", "nisp.excluded.mci.info")

    }

    "render page with text 'How to contact us'" in {
      assertEqualsMessage(htmlAccountDoc, "h2.heading-medium", "nisp.excluded.mci.howToFix")
    }

    "render page with message 'Telephone: 0300 200 3300' " in {
      assertEqualsMessage(htmlAccountDoc, "ul.list-bullet>li:nth-child(1)", "nisp.excluded.mci.howToFix.message1")
    }
    "render page with message 'Textphone: 0300 200 3319'" in {
      assertEqualsMessage(htmlAccountDoc, "ul.list-bullet> li:nth-child(2)", "nisp.excluded.mci.howToFix.message2")
    }

    "render page with message 'Outside UK: +44 135 535 9022'" in {
      assertEqualsMessage(htmlAccountDoc, "ul.list-bullet> li:nth-child(3)", "nisp.excluded.mci.howToFix.message3")
    }

    "render page with message 'Phone lines are open:'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(5)", "nisp.excluded.mci.howToContact")
    }

    "render page with message '8am to 8pm, Monday to Friday'" in {
      assertEqualsMessage(htmlAccountDoc, "article>ul.list-bullet:nth-child(6)> li:nth-child(1)", "nisp.excluded.mci.howToContact.weekdays")
    }

    "render page with message '8am to 4pm Saturdays'" in {
      assertEqualsMessage(htmlAccountDoc, "article>ul.list-bullet:nth-child(6)> li:nth-child(2)", "nisp.excluded.mci.howToContact.saturday")
    }

    "render page with message '9am to 5pm Sundays'" in {
      assertEqualsMessage(htmlAccountDoc, "article>ul.list-bullet:nth-child(6)> li:nth-child(3)", "nisp.excluded.mci.howToContact.sunday")
    }
    "render page with message 'Closed bank holidays.'" in {
      assertEqualsMessage(htmlAccountDoc, "article>p:nth-child(7)", "nisp.excluded.mci.howToContact.bankholiday")
    }

    "render page with message 'Find out about call charges (opens in a new window)'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(8)", "nisp.excluded.mci.howToContact.link")
    }
  }

  "Exclusion Post State Pension Age: State Pension Age under consideration - no flag" should {

    lazy val sResult = inject[excluded_sp]
    lazy val htmlAccountDoc = asDocument(sResult(Exclusion.PostStatePensionAge, Some(70), Some(LocalDate.of(2015, 9, 6)), true, None).toString)

    "render the UR banner" in {
      assert(htmlAccountDoc.getElementsByClass("full-width-banner__title") != null)
    }

    "render with correct page title" in {
      assertElementContainsText(htmlAccountDoc, "head>title" ,messages("nisp.main.h1.title") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
    }

    "render page with heading  'Your State Pension'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h1", "nisp.main.h1.title")
    }

    "render page with heading  You reached State Pension age on 6 September 2015 when you were 70 " in {

      assertContainsDynamicMessage(htmlAccountDoc, "h2.heading-medium", "nisp.excluded.haveReached", Dates.formatDate(LocalDate.of(2015, 9, 6)), (70).toString, null)
    }
    "render page with message  'if you have not already started claiming your state pension you can putoff...' " in {

      assertEqualsMessage(htmlAccountDoc, "div.panel-indent>p:nth-child(1)", "nisp.excluded.spa")
    }
    "render page with help message 'See a record of the National Insurance contributions which count towards your State Pension ' " in {

      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(4)", "nisp.excluded.niRecordIntro")
    }
    "render page with help message 'View your National Insurance record' " in {

      assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(5)", "nisp.main.showyourrecord")
    }

    //No state pension age under consideration message
    "not render page with heading  'Proposed change to your State Pension age'" in {
      assertPageDoesNotContainMessage(htmlAccountDoc, ("nisp.spa.under.consideration.title"))
    }

    "not render page with text  'Youll reach State Pension age on 6 Sep 2015. Under government proposals this may increase by up to a year.'" in {
      assertPageDoesNotContainDynamicMessage(htmlAccountDoc, "nisp.spa.under.consideration.detail", Dates.formatDate(LocalDate.of(2015, 9, 6)))
    }
  }

  "Exclusion Post State Pension Age: State Pension Age under consideration - false" should {

    lazy val sResult = inject[excluded_sp]
    lazy val htmlAccountDoc = asDocument(sResult.apply(Exclusion.PostStatePensionAge, Some(70), Some(LocalDate.of(2015, 9, 6)), true, Some(false)).toString)

    "render the UR banner" in {
      assert(htmlAccountDoc.getElementsByClass("full-width-banner__title") != null)
    }

    "render with correct page title" in {
      assertElementContainsText(htmlAccountDoc, "head>title" ,messages("nisp.main.h1.title") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
    }
    "render page with heading  'Your State Pension'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h1", "nisp.main.h1.title")
    }

    "render page with heading  You reached State Pension age on 6 September 2015 when you were 70 " in {

      assertContainsDynamicMessage(htmlAccountDoc, "h2.heading-medium", "nisp.excluded.haveReached", Dates.formatDate(LocalDate.of(2015, 9, 6)), (70).toString, null)
    }
    "render page with message  'if you have not already started claiming your state pension you can putoff...' " in {

      assertEqualsMessage(htmlAccountDoc, "div.panel-indent>p:nth-child(1)", "nisp.excluded.spa")
    }
    "render page with help message 'See a record of the National Insurance contributions which count towards your State Pension ' " in {

      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(4)", "nisp.excluded.niRecordIntro")
    }
    "render page with help message 'View your National Insurance record' " in {

      assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(5)", "nisp.main.showyourrecord")
    }

    //No state pension age under consideration message
    "not render page with heading  'Proposed change to your State Pension age'" in {
      assertPageDoesNotContainMessage(htmlAccountDoc, ("nisp.spa.under.consideration.title"))
    }

    "not render page with text  'Youll reach State Pension age on 6 Sep 2015. Under government proposals this may increase by up to a year.'" in {
      assertPageDoesNotContainDynamicMessage(htmlAccountDoc, "nisp.spa.under.consideration.detail", Dates.formatDate(LocalDate.of(2015, 9, 6)))
    }
  }

  "Exclusion Post State Pension Age: State Pension Age under consideration - true" should {

    lazy val sResult = inject[excluded_sp]
    lazy val htmlAccountDoc = asDocument(sResult(Exclusion.PostStatePensionAge, Some(70), Some(LocalDate.of(2015, 9, 6)), true, Some(true)).toString)

    "render the UR banner" in {
      assert(htmlAccountDoc.getElementsByClass("full-width-banner__title") != null)
    }
    "render with correct page title" in {
      assertElementContainsText(htmlAccountDoc, "head>title" ,messages("nisp.main.h1.title") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
    }
    "render page with heading  'Your State Pension'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h1", "nisp.main.h1.title")
    }

    "render page with heading  You reached State Pension age on 6 September 2015 when you were 70 " in {

      assertContainsDynamicMessage(htmlAccountDoc, "h2.heading-medium", "nisp.excluded.haveReached", Dates.formatDate(LocalDate.of(2015, 9, 6)), (70).toString, null)
    }
    "render page with message  'if you have not already started claiming your state pension you can putoff...' " in {

      assertEqualsMessage(htmlAccountDoc, "div.panel-indent>p:nth-child(1)", "nisp.excluded.spa")
    }
    "render page with help message 'See a record of the National Insurance contributions which count towards your State Pension ' " in {

      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(4)", "nisp.excluded.niRecordIntro")
    }
    "render page with help message 'View your National Insurance record' " in {

      assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(5)", "nisp.main.showyourrecord")
    }

    //No state pension age under consideration message
    "not render page with heading  'Proposed change to your State Pension age'" in {
      assertPageDoesNotContainMessage(htmlAccountDoc, ("nisp.spa.under.consideration.title"))
    }

    "not render page with text  'Youll reach State Pension age on 6 Sep 2015. Under government proposals this may increase by up to a year.'" in {
      assertPageDoesNotContainDynamicMessage(htmlAccountDoc, "nisp.spa.under.consideration.detail", Dates.formatDate(LocalDate.of(2015, 9, 6)))
    }
  }

  "Exclusion Amount Dissonance: State Pension Age under consideration - no flag" should {

    lazy val sResult = inject[excluded_sp]
    lazy val htmlAccountDoc = asDocument(sResult(Exclusion.AmountDissonance, Some(70), Some(LocalDate.of(2015, 9, 6)), true, None).toString)

    "render with correct page title" in {
      assertElementContainsText(htmlAccountDoc, "head>title" ,messages("nisp.main.h1.title") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
    }
    "render page with heading  'Your State Pension'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h1", "nisp.main.h1.title")
    }

    "render page with text  You will reach your  State Pension age on 6 September 2015" in {

      assertContainsDynamicMessage(htmlAccountDoc, "h2.heading-medium", "nisp.excluded.willReach", Dates.formatDate(LocalDate.of(2015, 9, 6)), null, null)
    }
    "render page with text  We’re unable to calculate your State Pension forecast at the moment and we’re working on fixing this." in {

      assertEqualsMessage(htmlAccountDoc, "div.panel-indent>p:nth-child(1)", "nisp.excluded.amountdissonance")
    }
    "render page with text  In the meantime, you can contact the Future Pension Centre (opens in new tab) to get an estimate of your State Pension, based on your current National Insurance record." in {

      assertEqualsMessage(htmlAccountDoc, "div.panel-indent>p:nth-child(2)", "nisp.excluded.contactFuturePensionCentre")
    }
    "render page with text  See a record of the National Insurance contributions which count towards your State Pension and check for any gaps." in {

      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(4)", "nisp.excluded.niRecordIntro")
    }
    "render page with text View your National Insurance record for Dissonance" in {

      assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(5)", "nisp.main.showyourrecord")
    }

    //No state pension age under consideration message
    "not render page with heading  'Proposed change to your State Pension age'" in {
      assertPageDoesNotContainMessage(htmlAccountDoc, ("nisp.spa.under.consideration.title"))
    }

    "not render page with text  'Youll reach State Pension age on 6 Sep 2015. Under government proposals this may increase by up to a year.'" in {
      assertPageDoesNotContainDynamicMessage(htmlAccountDoc, "nisp.spa.under.consideration.detail", Dates.formatDate(LocalDate.of(2015, 9, 6)))
    }
  }

  "Exclusion Amount Dissonance: State Pension Age under consideration - false" should {

    lazy val sResult = inject[excluded_sp]
    lazy val htmlAccountDoc = asDocument(sResult(Exclusion.AmountDissonance, Some(70), Some(LocalDate.of(2015, 9, 6)), true, Some(false)).toString)

    "render with correct page title" in {
      assertElementContainsText(htmlAccountDoc, "head>title" ,messages("nisp.main.h1.title") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
    }
    "render page with heading  'Your State Pension'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h1", "nisp.main.h1.title")
    }

    "render page with text  You will reach your  State Pension age on 6 September 2015" in {

      assertContainsDynamicMessage(htmlAccountDoc, "h2.heading-medium", "nisp.excluded.willReach", Dates.formatDate(LocalDate.of(2015, 9, 6)), null, null)
    }
    "render page with text  We’re unable to calculate your State Pension forecast at the moment and we’re working on fixing this." in {

      assertEqualsMessage(htmlAccountDoc, "div.panel-indent>p:nth-child(1)", "nisp.excluded.amountdissonance")
    }
    "render page with text  In the meantime, you can contact the Future Pension Centre (opens in new tab) to get an estimate of your State Pension, based on your current National Insurance record." in {

      assertEqualsMessage(htmlAccountDoc, "div.panel-indent>p:nth-child(2)", "nisp.excluded.contactFuturePensionCentre")
    }
    "render page with text  See a record of the National Insurance contributions which count towards your State Pension and check for any gaps." in {

      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(4)", "nisp.excluded.niRecordIntro")
    }
    "render page with text View your National Insurance record for Dissonance" in {

      assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(5)", "nisp.main.showyourrecord")
    }

    //No state pension age under consideration message
    "not render page with heading  'Proposed change to your State Pension age'" in {
      assertPageDoesNotContainMessage(htmlAccountDoc, ("nisp.spa.under.consideration.title"))
    }

    "not render page with text  'Youll reach State Pension age on 6 Sep 2015. Under government proposals this may increase by up to a year.'" in {
      assertPageDoesNotContainDynamicMessage(htmlAccountDoc, "nisp.spa.under.consideration.detail", Dates.formatDate(LocalDate.of(2015, 9, 6)))
    }
  }

  "Exclusion Amount Dissonance: State Pension Age under consideration - true" should {

    lazy val sResult = inject[excluded_sp]
    lazy val htmlAccountDoc = asDocument(sResult(Exclusion.AmountDissonance, Some(70), Some(LocalDate.of(2015, 9, 6)), true, Some(true)).toString)

    "render with correct page title" in {
      assertElementContainsText(htmlAccountDoc, "head>title" ,messages("nisp.main.h1.title") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
    }
    "render page with heading  'Your State Pension'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h1", "nisp.main.h1.title")
    }

    "render page with text  You will reach your  State Pension age on 6 September 2015" in {

      assertContainsDynamicMessage(htmlAccountDoc, "h2.heading-medium", "nisp.excluded.willReach", Dates.formatDate(LocalDate.of(2015, 9, 6)), null, null)
    }
    "render page with text  We’re unable to calculate your State Pension forecast at the moment and we’re working on fixing this." in {

      assertEqualsMessage(htmlAccountDoc, "div.panel-indent>p:nth-child(1)", "nisp.excluded.amountdissonance")
    }
    "render page with text  In the meantime, you can contact the Future Pension Centre (opens in new tab) to get an estimate of your State Pension, based on your current National Insurance record." in {

      assertEqualsMessage(htmlAccountDoc, "div.panel-indent>p:nth-child(2)", "nisp.excluded.contactFuturePensionCentre")
    }
    "render page with text  See a record of the National Insurance contributions which count towards your State Pension and check for any gaps." in {

      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(4)", "nisp.excluded.niRecordIntro")
    }
    "render page with text View your National Insurance record for Dissonance" in {

      assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(5)", "nisp.main.showyourrecord")
    }

    //state pension age under consideration message
    "render page with heading  'Proposed change to your State Pension age'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(6)", "nisp.spa.under.consideration.title")
    }

    "render page with text  'Youll reach State Pension age on 6 Sep 2015. Under government proposals this may increase by up to a year.'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(7)", "nisp.spa.under.consideration.detail", Dates.formatDate(LocalDate.of(2015, 9, 6)))
    }
  }

  "Exclusion Married Women: State Pension Age under consideration - no flag" should {

    lazy val sResult = inject[excluded_sp]
    lazy val htmlAccountDoc = asDocument(sResult(Exclusion.MarriedWomenReducedRateElection, Some(60), Some(LocalDate.of(2015, 9, 6)), false, None).toString)

    "render with correct page title" in {
      assertElementContainsText(htmlAccountDoc, "head>title" ,messages("nisp.main.h1.title") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
    }
    "render page with heading  'Your State Pension'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h1", "nisp.main.h1.title")
    }

    "render page with heading  'You’ll reach State Pension age on' " in {
      val sDate = Dates.formatDate(LocalDate.of(2015, 9, 6)).toString()
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>h2.heading-medium", "nisp.excluded.willReach", sDate, null, null)
    }

    "render page with text - You will reach your  State Pension age on 6 September 2015" in {
      assertContainsDynamicMessage(htmlAccountDoc, "h2.heading-medium", "nisp.excluded.willReach", Dates.formatDate(LocalDate.of(2015, 9, 6)), null, null)
    }

    "render page with text  We’re unable to calculate your State Pension forecast as you have paid a reduced rate of National Insurance as a married woman (opens in new tab)." in {

      assertEqualsMessage(htmlAccountDoc, "div.panel-indent>p:nth-child(1)", "nisp.excluded.mwrre.sp")
    }

    "render page with text  In the meantime, you can contact the Future Pension Centre (opens in new tab) to get an estimate of your State Pension, based on your current National Insurance record." in {

      assertEqualsMessage(htmlAccountDoc, "div.panel-indent>p:nth-child(2)", "nisp.excluded.contactFuturePensionCentre")
    }

    "render page with message -To get a copy of your National Insurence record so far,contact the National Insurence helpline ...." in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p", "nisp.excluded.contactNationalInsuranceHelpline")
    }

    "render page with text - Help us improve this service" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(5)", "nisp.excluded.mwrre.improve")
    }

    "render page with text If you would like to take part in any future research so we can find out what people in your situation would like to know, please leave..." in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(6)", "nisp.excluded.mwrre.futureResearch")
    }

    "render page with link having  text sign out and leave feedback " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(7)", "nisp.excluded.mwrre.signOut")
    }

    //No state pension age under consideration message
    "not render page with heading  'Proposed change to your State Pension age'" in {
      assertPageDoesNotContainMessage(htmlAccountDoc, ("nisp.spa.under.consideration.title"))
    }

    "not render page with text  'Youll reach State Pension age on 6 Sep 2015. Under government proposals this may increase by up to a year.'" in {
      assertPageDoesNotContainDynamicMessage(htmlAccountDoc, "nisp.spa.under.consideration.detail", Dates.formatDate(LocalDate.of(2015, 9, 6)))
    }
  }

  "Exclusion Married Women: State Pension Age under consideration - false" should {

    lazy val sResult = inject[excluded_sp]
    lazy val htmlAccountDoc = asDocument(sResult(Exclusion.MarriedWomenReducedRateElection, Some(60), Some(LocalDate.of(2015, 9, 6)), false, Some(false)).toString)

    "render with correct page title" in {
      assertElementContainsText(htmlAccountDoc, "head>title" ,messages("nisp.main.h1.title") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
    }
    "render page with heading  'Your State Pension'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h1", "nisp.main.h1.title")
    }

    "render page with heading  'You’ll reach State Pension age on' " in {
      val sDate = Dates.formatDate(LocalDate.of(2015, 9, 6)).toString()
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>h2.heading-medium", "nisp.excluded.willReach", sDate, null, null)
    }

    "render page with text - You will reach your  State Pension age on 6 September 2015" in {
      assertContainsDynamicMessage(htmlAccountDoc, "h2.heading-medium", "nisp.excluded.willReach", Dates.formatDate(LocalDate.of(2015, 9, 6)), null, null)
    }

    "render page with text  We’re unable to calculate your State Pension forecast as you have paid a reduced rate of National Insurance as a married woman (opens in new tab)." in {
      assertEqualsMessage(htmlAccountDoc, "div.panel-indent>p:nth-child(1)", "nisp.excluded.mwrre.sp")
    }

    "render page with text  In the meantime, you can contact the Future Pension Centre (opens in new tab) to get an estimate of your State Pension, based on your current National Insurance record." in {
      assertEqualsMessage(htmlAccountDoc, "div.panel-indent>p:nth-child(2)", "nisp.excluded.contactFuturePensionCentre")
    }

    "render page with message -To get a copy of your National Insurence record so far,contact the National Insurence helpline ...." in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p", "nisp.excluded.contactNationalInsuranceHelpline")
    }

    "render page with text - Help us improve this service" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(5)", "nisp.excluded.mwrre.improve")
    }
    "render page with text If you would like to take part in any future research so we can find out what people in your situation would like to know, please leave..." in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(6)", "nisp.excluded.mwrre.futureResearch")
    }

    "render page with link having  text sign out and leave feedback " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(7)", "nisp.excluded.mwrre.signOut")
    }

    //No state pension age under consideration message
    "not render page with heading  'Proposed change to your State Pension age'" in {
      assertPageDoesNotContainMessage(htmlAccountDoc, ("nisp.spa.under.consideration.title"))
    }

    "not render page with text  'Youll reach State Pension age on 6 Sep 2015. Under government proposals this may increase by up to a year.'" in {
      assertPageDoesNotContainDynamicMessage(htmlAccountDoc, "nisp.spa.under.consideration.detail", Dates.formatDate(LocalDate.of(2015, 9, 6)))
    }
  }

  "Exclusion Married Women: State Pension Age under consideration - true" should {

    lazy val sResult = inject[excluded_sp]
    lazy val htmlAccountDoc = asDocument(sResult(Exclusion.MarriedWomenReducedRateElection, Some(60), Some(LocalDate.of(2015, 9, 6)), false, Some(true)).toString)

    "render with correct page title" in {
      assertElementContainsText(htmlAccountDoc, "head>title" ,messages("nisp.main.h1.title") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
    }
    "render page with heading  'Your State Pension'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h1", "nisp.main.h1.title")
    }

    "render page with heading  'You’ll reach State Pension age on' " in {
      val sDate = Dates.formatDate(LocalDate.of(2015, 9, 6)).toString()
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>h2.heading-medium", "nisp.excluded.willReach", sDate, null, null)
    }

    "render page with text - You will reach your  State Pension age on 6 September 2015" in {
      assertContainsDynamicMessage(htmlAccountDoc, "h2.heading-medium", "nisp.excluded.willReach", Dates.formatDate(LocalDate.of(2015, 9, 6)), null, null)
    }

    "render page with text  We’re unable to calculate your State Pension forecast as you have paid a reduced rate of National Insurance as a married woman (opens in new tab)." in {
      assertEqualsMessage(htmlAccountDoc, "div.panel-indent>p:nth-child(1)", "nisp.excluded.mwrre.sp")
    }

    "render page with text  In the meantime, you can contact the Future Pension Centre (opens in new tab) to get an estimate of your State Pension, based on your current National Insurance record." in {
      assertEqualsMessage(htmlAccountDoc, "div.panel-indent>p:nth-child(2)", "nisp.excluded.contactFuturePensionCentre")
    }

    "render page with message -To get a copy of your National Insurence record so far,contact the National Insurence helpline ...." in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p", "nisp.excluded.contactNationalInsuranceHelpline")
    }

    "render page with text - Help us improve this service" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(5)", "nisp.excluded.mwrre.improve")
    }

    "render page with text If you would like to take part in any future research so we can find out what people in your situation would like to know, please leave..." in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(6)", "nisp.excluded.mwrre.futureResearch")
    }

    "render page with link having  text sign out and leave feedback " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(7)", "nisp.excluded.mwrre.signOut")
    }

    //No state pension age under consideration message
    "not render page with heading  'Proposed change to your State Pension age'" in {
      assertPageDoesNotContainMessage(htmlAccountDoc, ("nisp.spa.under.consideration.title"))
    }

    "not render page with text  'Youll reach State Pension age on 6 Sep 2015. Under government proposals this may increase by up to a year.'" in {
      assertPageDoesNotContainDynamicMessage(htmlAccountDoc, "nisp.spa.under.consideration.detail", Dates.formatDate(LocalDate.of(2015, 9, 6)))
    }
  }

}
