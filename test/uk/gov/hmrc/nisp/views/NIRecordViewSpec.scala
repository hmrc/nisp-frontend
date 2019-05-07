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

package uk.gov.hmrc.nisp.views

import java.util.UUID

import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatest._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.mvc.Cookie
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, _}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.builders.NationalInsuranceTaxYearBuilder
import uk.gov.hmrc.nisp.config.wiring.NispFormPartialRetriever
import uk.gov.hmrc.nisp.controllers.connectors.CustomAuditConnector
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.models.enums.Exclusion
import uk.gov.hmrc.nisp.models.{NationalInsuranceRecord, StatePensionExclusionFiltered}
import uk.gov.hmrc.nisp.services.{CitizenDetailsService, MetricsService, NationalInsuranceService, StatePensionService}
import uk.gov.hmrc.nisp.utils.{Constants, MockTemplateRenderer}
import uk.gov.hmrc.play.frontend.auth.AuthenticationProviderIds
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.language.LanguageUtils._
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.time.DateTimeUtils.now

import scala.concurrent.Future

class NIRecordViewSpec extends HtmlSpec with MockitoSugar with BeforeAndAfter {

  implicit val cachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
  implicit val formPartialRetriever: uk.gov.hmrc.play.partials.FormPartialRetriever = NispFormPartialRetriever
  implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer

  val mockUsername = "mockuser"
  val mockUserId = "/auth/oid/" + mockUsername
  val mockAbroadUserId = "/auth/oid/mockabroad"

  val urMockUsername = "showurbanner"
  val urMockUserId = "/auth/oid/" + urMockUsername

  val noUrMockUsername = "hideurbanner"
  val noUrMockUserId = "/auth/oid/" + noUrMockUsername

  implicit lazy val fakeRequest = FakeRequest()

  def authenticatedFakeRequest(userId: String) = fakeRequest.withSession(
    SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
    SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
    SessionKeys.userId -> userId,
    SessionKeys.authProvider -> AuthenticationProviderIds.VerifyProviderId
  )

  "Render Ni Record UR banner" should {
    lazy val controller = new MockNIRecordController {
      override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
      override val customAuditConnector: CustomAuditConnector = MockCustomAuditConnector
      override val sessionCache: SessionCache = MockSessionCache
      override val showFullNI = true
      override val currentDate = new LocalDate(2016, 9, 9)
      override protected def authConnector: AuthConnector = MockAuthConnector
      override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
      override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
      override val metricsService: MetricsService = MockMetricsService
    }

    lazy val urResult = controller.showFull(authenticatedFakeRequest(urMockUserId).withCookies(lanCookie))
    lazy val urHtmlAccountDoc = asDocument(contentAsString(urResult))

    "render UR banner on page before no thanks is clicked" in {
      val urBanner =  urHtmlAccountDoc.getElementsByClass("full-width-banner__title")
      val urBannerHref =  urHtmlAccountDoc.getElementById("fullWidthBannerLink")
      val urDismissedText = urHtmlAccountDoc.getElementById("fullWidthBannerDismissText")
      assert(urBanner.text() == Messages("nisp.home.banner.recruitment.title"))
      assert(urBannerHref.text() == Messages("nisp.home.banner.recruitment.linkURL"))
      assert(urDismissedText.text() == Messages("nisp.home.banner.recruitment.reject"))
      assert(urHtmlAccountDoc.getElementById("full-width-banner") != null)
    }

    "not render the UR banner" in {
      val request = authenticatedFakeRequest(urMockUserId).withCookies(new Cookie("cysp-nisp-urBannerHide", "9999"))
      val result = controller.showFull(request)
      val doc = asDocument(contentAsString(result))
      assert(doc.getElementById("full-width-banner") == null)
    }

    "render for nino: CL928713A" in {
      val urBanner =  urHtmlAccountDoc.getElementsByClass("full-width-banner__title")
      assert(urBanner.text() == Messages("nisp.home.banner.recruitment.title"))
      assert(urHtmlAccountDoc.getElementById("full-width-banner") != null)
    }

    "not render for nino: HT009413A" in {
      val request = authenticatedFakeRequest(noUrMockUserId).withCookies(new Cookie("cysp-nisp-urBannerHide", "9999"))
      val result = controller.showFull(request)
      val doc = asDocument(contentAsString(result))
      assert(doc.getElementById("full-width-banner") == null)
    }

  }

  "Render Ni Record to view all the years" should {
    lazy val controller = new MockNIRecordController {
      override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
      override val customAuditConnector: CustomAuditConnector = MockCustomAuditConnector
      override val sessionCache: SessionCache = MockSessionCache
      override val showFullNI = true
      override val currentDate = new LocalDate(2016, 9, 9)
      override protected def authConnector: AuthConnector = MockAuthConnector
      override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
      override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
      override val metricsService: MetricsService = MockMetricsService
    }

    lazy val result = controller.showFull(authenticatedFakeRequest(mockUserId).withCookies(lanCookie))
    lazy val htmlAccountDoc = asDocument(contentAsString(result))



    /*Check side border :summary */

    "render page with qualifying years text '4 years of full contributions'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "ul.list-bullet li:nth-child(1)", "nisp.nirecord.summary.fullContributions", "28")
    }

    "render page with text '4 years to contribute before 5 April 2018 '" in {
      assertContainsDynamicMessage(htmlAccountDoc, "ul.list-bullet li:nth-child(2)", "nisp.nirecord.summary.yearsRemaining", "4", "2018")
    }

    /*Ends here*/
    "render with correct page title" in {
      assertElementContainsText(htmlAccountDoc, "head>title" ,messages("nisp.nirecord.heading") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
    }

    "render page with Gaps  heading  Your National Insurance record " in {
      assertEqualsMessage(htmlAccountDoc, "h1.titleWithPageInfo", "nisp.nirecord.heading")
    }

    "render page with name " in {
      assertEqualsValue(htmlAccountDoc, ".page-info", "AHMED BRENNAN")
    }

    "render page with national insurence number " in {
      assertElemetsOwnMessage(htmlAccountDoc, "article.content__body>p:nth-child(3)", "nisp.national.insurance.number")
    }

    "render page with link 'View gaps only'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p.panel-indent>a", "nisp.nirecord.showgaps")
    }

    "render page with link href 'View gaps only'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>p.panel-indent>a", "/check-your-state-pension/account/nirecord/gaps")
    }
    "render page with text  'your record for this year is not available'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl>dt:nth-child(1)>div.ni-wrapper>div.inactive", "nisp.nirecord.unavailableyear")
    }

    "render page with text  'year is not full'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl>dt:nth-child(2)>div>div.ni-notfull", "nisp.nirecord.gap")
    }
    "render page with link 'View details'" in {

      assertContainsNextedValue(htmlAccountDoc, "article.content__body>dl>dt:nth-child(2)>div>div.ni-action>a.view-details", "nisp.nirecord.gap.viewdetails", "2013-14")
    }

    "render page with text  'You did not make any contributions this year '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl>dd:nth-child(3)>div.contributions-wrapper>p.contributions-header", "nisp.nirecord.youdidnotmakeanycontrib")
    }

    "render page with text 'Find out more about gaps in your account'" in {
      assertContainsNextedValue(htmlAccountDoc, "article.content__body>dl>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(2)", "nisp.nirecord.gap.findoutmoreabout", "/check-your-state-pension/account/nirecord/gapsandhowtocheck")
    }

    "render page with link href 'gaps in your record and how to check them'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>dl>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(2)>a", "/check-your-state-pension/account/nirecord/gapsandhowtocheck")
    }

    "render page with text  'You can make up the shortfall'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl>dd:nth-child(3)>div.contributions-wrapper>p.contributions-header:nth-child(3)", "nisp.nirecord.gap.youcanmakeupshortfall")
    }
    "render page with text  'Pay a voluntary contribution of £530 by 5 April 2023. This shortfall may increase after 5 April 2019.'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>dl>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(4)", "nisp.nirecord.gap.payvoluntarycontrib", "&pound;704.60", Dates.formatDate(new LocalDate(2023, 4, 5)), Dates.formatDate(new LocalDate(2019, 4, 5)))
    }
    "render page with text  'Find out more about...'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>dl>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(5)", "nisp.nirecord.gap.findoutmore", "/check-your-state-pension/account/nirecord/voluntarycontribs")
    }
    "render page with text  ' FUll years'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl>dt:nth-child(10)>div>div.ni-notfull", "nisp.nirecord.fullyear")
    }

    "render page with text  'You have contributions from '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl>dd:nth-child(11)>div.contributions-wrapper>p.contributions-header", "nisp.nirecord.yourcontributionfrom")
    }

    "render page with text 'Paid employment £ 4,259.60'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>dl>dd:nth-child(11)>div.contributions-wrapper>p:nth-child(3)", "nisp.nirecord.gap.paidemployment", "£4,259.60")
    }

    /*check for medical credit year*/

    "render page with text  'full year'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl>dt:nth-child(50)>div>div.ni-notfull", "nisp.nirecord.fullyear")
    }

    "render page with text 'you have contributions from '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl>dd:nth-child(51)>div.contributions-wrapper>p:nth-child(1)", "nisp.nirecord.yourcontributionfrom")
    }

    "render page with text 'National insurence credits 52 weeks'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>dl>dd:nth-child(51)>div.contributions-wrapper>p:nth-child(2)", "nisp.nirecord.gap.whenyouareclaiming.plural", "52")
    }
    "render page with text 'These may have been added to your record if you were ill/disabled...'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl>dd:nth-child(51)>div.contributions-wrapper>p:nth-child(3)", "nisp.nirecord.gap.whenyouareclaiming.info.plural")
    }
    /*ends here*/

    "render page with link  'view gaps only'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p>a", "nisp.nirecord.showgaps")
    }
    "render page with href link  'view gaps years'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>p>a", "/check-your-state-pension/account/nirecord/gaps")
    }
    "render page with link  'back'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p.backlink>a", "nisp.back")
    }
    "render page with href link  'back'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>p.backlink>a", "javascript:window.history.back();")
    }
  }

  "Render Ni Record view Gaps Only" should {

    lazy val controller = new MockNIRecordController {
      override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
      override val customAuditConnector: CustomAuditConnector = MockCustomAuditConnector
      override val sessionCache: SessionCache = MockSessionCache
      override val showFullNI = true
      override val currentDate = new LocalDate(2016, 9, 9)
      override protected def authConnector: AuthConnector = MockAuthConnector
      override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
      override val metricsService: MetricsService = MockMetricsService
    }

    lazy val result = controller.showGaps(authenticatedFakeRequest(mockUserId).withCookies(lanCookie))
    lazy val htmlAccountDoc = asDocument(contentAsString(result))

    /*Check side border :summary */

    "render page with qualifying years text '28 years of full contributions'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "ul.list-bullet li:nth-child(1)", "nisp.nirecord.summary.fullContributions", "28")
    }

    "render page with text '4 years to contribute before 5 April 2018 '" in {
      assertContainsDynamicMessage(htmlAccountDoc, "ul.list-bullet li:nth-child(2)", "nisp.nirecord.summary.yearsRemaining", "4", "2018")
    }

    "render page with text '10 years when you did not contribute enough'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "ul.list-bullet li:nth-child(3)", "nisp.nirecord.summary.gaps", "10")
    }

    /*Ends here*/

    "render with correct page title" in {
      assertElementContainsText(htmlAccountDoc, "head>title" ,messages("nisp.nirecord.heading") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
    }
    "render page with Gaps  heading  Your National Insurance record " in {
      assertEqualsMessage(htmlAccountDoc, "h1.titleWithPageInfo", "nisp.nirecord.heading")
    }

    "render page with name " in {
      assertEqualsValue(htmlAccountDoc, ".page-info", "AHMED BRENNAN")
    }

    "render page with national insurence number " in {
      assertElemetsOwnMessage(htmlAccountDoc, "article.content__body>p:nth-child(3)", "nisp.national.insurance.number")
    }

      "render page with link 'View all years'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p.panel-indent>a", "nisp.nirecord.showfull")
    }

    "render page with link href 'View all years'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>p.panel-indent>a", "/check-your-state-pension/account/nirecord")
    }
    "render page with text  'your record for this year is not available yet'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl>dt:nth-child(1)>div.ni-wrapper>div.inactive", "nisp.nirecord.unavailableyear")
    }

    "render page with text  'year is not full'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl>dt:nth-child(2)>div>div.ni-notfull", "nisp.nirecord.gap")
    }
    "render page with link 'View details'" in {
      assertContainsNextedValue(htmlAccountDoc, "article.content__body>dl>dt:nth-child(2)>div>div.ni-action>a.view-details", "nisp.nirecord.gap.viewdetails", "2013-14")
    }

    "render page with text  'You did not make any contributions this year '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl>dd:nth-child(3)>div.contributions-wrapper>p.contributions-header", "nisp.nirecord.youdidnotmakeanycontrib")
    }

    "render page with text 'Find out more about gaps in your account'" in {
      assertContainsNextedValue(htmlAccountDoc, "article.content__body>dl>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(2)", "nisp.nirecord.gap.findoutmoreabout", "/check-your-state-pension/account/nirecord/gapsandhowtocheck")
    }

    "render page with text  'You can make up the shortfall'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl>dd:nth-child(3)>div.contributions-wrapper>p.contributions-header:nth-child(3)", "nisp.nirecord.gap.youcanmakeupshortfall")
    }
    "render page with text  'Pay a voluntary contribution of figure out how to do it...'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>dl>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(4)", "nisp.nirecord.gap.payvoluntarycontrib", "&pound;704.60", Dates.formatDate(new LocalDate(2023, 4, 5)), Dates.formatDate(new LocalDate(2019, 4, 5)))
    }
    "render page with text  'Find out more about...'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>dl>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(5)", "nisp.nirecord.gap.findoutmore", "/check-your-state-pension/account/nirecord/voluntarycontribs")
    }
    "render page with text  ' year is not full'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl>dt:nth-child(10)>div>div.ni-notfull", "nisp.nirecord.gap")
    }

    "render page with text  'You did not make any contributions this year for toolate to pay '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl>dd:nth-child(11)>div.contributions-wrapper>p.contributions-header", "nisp.nirecord.youdidnotmakeanycontrib")
    }

    "render page with text 'Find out more about for toolate to pay'" in {
      assertContainsNextedValue(htmlAccountDoc, "article.content__body>dl>dd:nth-child(11)>div.contributions-wrapper>p:nth-child(2)", "nisp.nirecord.gap.findoutmoreabout", "/check-your-state-pension/account/nirecord/gapsandhowtocheck")
      //assertContainsNextedValue(htmlAccountDoc, "article.content__body>dl>dt:nth-child()>div>div.ni-action>a.view-details", "nisp.nirecord.gap.viewdetails", "2013-14")
    }

    "render page with text  'It’s too late to pay for this year. You can usually only pay for the last 6 years.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl>dd:nth-child(11)>div.contributions-wrapper>p.panel-indent:nth-child(3)", "nisp.nirecord.gap.latePaymentMessage")
    }
    "render page with link  'view all years'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p.panel-indent>a", "nisp.nirecord.showfull")
    }
    "render page with href link  'view all years'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>p.panel-indent>a", "/check-your-state-pension/account/nirecord")
    }
    "render page with link  'back'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p.backlink>a", "nisp.back")
    }
    "render page with href link  'back'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>p.backlink>a", "javascript:window.history.back();")
    }
  }


  "Render Ni Record view With HRP Message" should {

    lazy val result = html.nirecordGapsAndHowToCheckThem(true);

    lazy val htmlAccountDoc = asDocument(contentAsString(result))

    "render with correct page title" in {
      assertElementContainsText(htmlAccountDoc, "head>title" ,messages("nisp.nirecord.gapsinyourrecord.heading") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
    }
    "render page with heading 'Gaps in your record and how to check them'" in {
      assertEqualsMessage(htmlAccountDoc, "h1.heading-large", "nisp.nirecord.gapsinyourrecord.heading")
    }
    "render page with text 'In most cases, you will have a gap in your record as you did not contribute enough National Insurance.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(2)", "nisp.nirecord.gapsinyourrecord.title.message")
    }
    "render page with text 'This could be because you were:.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(3)", "nisp.nirecord.gapsinyourrecord.listheader")
    }
    "render page with text 'in paid employment and had low earnings'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>ul.list-bullet:nth-child(4)>li:nth-child(1)", "nisp.nirecord.gapsinyourrecord.line1")
    }
    "render page with text 'unemployed and not claiming benefit'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>ul.list-bullet:nth-child(4)>li:nth-child(2)", "nisp.nirecord.gapsinyourrecord.line2")
    }
    "render page with text 'self-employed but did not pay contributions because of small profits'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>ul.list-bullet:nth-child(4)>li:nth-child(3)", "nisp.nirecord.gapsinyourrecord.line3")
    }
    "render page with text 'living abroad'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>ul.list-bullet:nth-child(4)>li:nth-child(4)", "nisp.nirecord.gapsinyourrecord.line4")
    }
    "render page with text 'living or working in the Isle of Man'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>ul.list-bullet:nth-child(4)>li:nth-child(5)", "nisp.nirecord.gapsinyourrecord.line5")
    }
    "render page with text 'How to check your record'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(5)", "nisp.nirecord.gapsinyourrecord.howtocheckrecord")
    }
    "render page with text 'Paid employment'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h3:nth-child(6)", "nisp.nirecord.gapsinyourrecord.paidemployment.title")
    }
    "render page with text 'Check the contributions you made against P60s from your employers.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(7)", "nisp.nirecord.gapsinyourrecord.paidemployment.desc")
    }
    "render page with text 'If you do not have P60s'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>details:nth-child(8)>summary>span", "nisp.nirecord.gapsinyourrecord.donothavep60.title")
    }
    "render page with text 'You can get a replacement P60 from your employer. Alternatively, you can find your National Insurance contributions on your payslips.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>details:nth-child(8)>div.panel-indent>p", "nisp.nirecord.gapsinyourrecord.donothavep60.desc")
    }
    "render page with text 'Self-employment and voluntary contributions.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h3:nth-child(9)", "nisp.nirecord.gapsinyourrecord.selfemployment.title")
    }
    "render page with text 'Check the contributions you made against your personal accounts. For instance if you made payments by cheque or through your bank.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(10)", "nisp.nirecord.gapsinyourrecord.selfemployment.desc")
    }
    "render page with text 'If you have evidence that your record is wrong'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(11)", "nisp.nirecord.gapsinyourrecord.ifyouhaveevidence.title")
    }
    "render page with text 'You may be able to correct your record. Send copies of the necessary evidence with a covering letter to:'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(12)", "nisp.nirecord.gapsinyourrecord.ifyouhaveevidence.line1")
    }
    "render page with text 'Individuals Caseworker National Insurance contributions and Employers Office HM Revenue and Customs BX9 1AN:'" in {
      assertContainsChildWithMessage(htmlAccountDoc, "article.content__body>p:nth-child(13)", "nisp.nirecord.gapsinyourrecord.ifyouhaveevidence.addr1", "nisp.nirecord.gapsinyourrecord.ifyouhaveevidence.addr2", "nisp.nirecord.gapsinyourrecord.ifyouhaveevidence.addr3", "nisp.nirecord.gapsinyourrecord.ifyouhaveevidence.addr4")
    }
    "render page with text 'National Insurance credits'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(14)", "nisp.nirecord.gapsinyourrecord.nationalinsurancecredits.title")
    }
    "render page with text 'If you were claiming benefits because you were unable to work, unemployed or caring for someone full time...'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(15)", "nisp.nirecord.gapsinyourrecord.nationalinsurancecredits.desc")
    }
    "render page with text 'Home Responsibilities Protection (HRP) is only available for'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>div:nth-child(16)>p", "nisp.nirecord.gapsinyourrecord.nationalinsurancecredits.hrp", null)
    }
    "render page with link 'Home Responsibilities Protection (HRP) is only available foMore on National Insurance credits (opens in new tab)r'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(17)>a", "nisp.nirecord.gapsinyourrecord.nationalinsurancecredits.link")
    }
    "render page with href link More on National Insurance credits (opens in new tab) " in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>p:nth-child(17)>a", "https://www.gov.uk/national-insurance-credits/eligibility")
    }
    "render page with  link back " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>div:nth-child(18)>a", "nisp.back")
    }
    "render page with href link back " in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>div:nth-child(18)>a", "/check-your-state-pension/account/nirecord/gaps")
    }
  }


  "Render Ni Record without With HRP Message" should {


    lazy val result = html.nirecordGapsAndHowToCheckThem(false);

    lazy val htmlAccountDoc = asDocument(contentAsString(result))

    "render with correct page title" in {
      assertElementContainsText(htmlAccountDoc, "head>title" ,messages("nisp.nirecord.gapsinyourrecord.heading") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
    }
    "render page with heading 'Gaps in your record and how to check them'" in {
      assertEqualsMessage(htmlAccountDoc, "h1.heading-large", "nisp.nirecord.gapsinyourrecord.heading")
    }
    "render page with text 'In most cases, you will have a gap in your record as you did not contribute enough National Insurance.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(2)", "nisp.nirecord.gapsinyourrecord.title.message")
    }
    "render page with text 'This could be because you were:.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(3)", "nisp.nirecord.gapsinyourrecord.listheader")
    }
    "render page with text 'in paid employment and had low earnings'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>ul.list-bullet:nth-child(4)>li:nth-child(1)", "nisp.nirecord.gapsinyourrecord.line1")
    }
    "render page with text 'unemployed and not claiming benefit'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>ul.list-bullet:nth-child(4)>li:nth-child(2)", "nisp.nirecord.gapsinyourrecord.line2")
    }
    "render page with text 'self-employed but did not pay contributions because of small profits'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>ul.list-bullet:nth-child(4)>li:nth-child(3)", "nisp.nirecord.gapsinyourrecord.line3")
    }
    "render page with text 'living abroad'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>ul.list-bullet:nth-child(4)>li:nth-child(4)", "nisp.nirecord.gapsinyourrecord.line4")
    }
    "render page with text 'living or working in the Isle of Man'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>ul.list-bullet:nth-child(4)>li:nth-child(5)", "nisp.nirecord.gapsinyourrecord.line5")
    }
    "render page with text 'How to check your record'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(5)", "nisp.nirecord.gapsinyourrecord.howtocheckrecord")
    }
    "render page with text 'Paid employment'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h3:nth-child(6)", "nisp.nirecord.gapsinyourrecord.paidemployment.title")
    }
    "render page with text 'Check the contributions you made against P60s from your employers.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(7)", "nisp.nirecord.gapsinyourrecord.paidemployment.desc")
    }
    "render page with text 'If you do not have P60s'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>details:nth-child(8)>summary>span", "nisp.nirecord.gapsinyourrecord.donothavep60.title")
    }
    "render page with text 'You can get a replacement P60 from your employer. Alternatively, you can find your National Insurance contributions on your payslips.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>details:nth-child(8)>div.panel-indent>p", "nisp.nirecord.gapsinyourrecord.donothavep60.desc")
    }
    "render page with text 'Self-employment and voluntary contributions.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h3:nth-child(9)", "nisp.nirecord.gapsinyourrecord.selfemployment.title")
    }
    "render page with text 'Check the contributions you made against your personal accounts. For instance if you made payments by cheque or through your bank.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(10)", "nisp.nirecord.gapsinyourrecord.selfemployment.desc")
    }
    "render page with text 'If you have evidence that your record is wrong'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(11)", "nisp.nirecord.gapsinyourrecord.ifyouhaveevidence.title")
    }
    "render page with text 'You may be able to correct your record. Send copies of the necessary evidence with a covering letter to:'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(12)", "nisp.nirecord.gapsinyourrecord.ifyouhaveevidence.line1")
    }
    "render page with text 'Individuals Caseworker National Insurance contributions and Employers Office HM Revenue and Customs BX9 1AN:'" in {
      assertContainsChildWithMessage(htmlAccountDoc, "article.content__body>p:nth-child(13)", "nisp.nirecord.gapsinyourrecord.ifyouhaveevidence.addr1", "nisp.nirecord.gapsinyourrecord.ifyouhaveevidence.addr2", "nisp.nirecord.gapsinyourrecord.ifyouhaveevidence.addr3", "nisp.nirecord.gapsinyourrecord.ifyouhaveevidence.addr4")
    }
    "render page with text 'National Insurance credits'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(14)", "nisp.nirecord.gapsinyourrecord.nationalinsurancecredits.title")
    }
    "render page with text 'If you were claiming benefits because you were unable to work, unemployed or caring for someone full time...'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(15)", "nisp.nirecord.gapsinyourrecord.nationalinsurancecredits.desc")
    }
    "render page with link 'More on National Insurance credits (opens in new tab)'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(16)>a", "nisp.nirecord.gapsinyourrecord.nationalinsurancecredits.link")
    }
    "render page with href link More on National Insurance credits (opens in new tab) " in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>p:nth-child(16)>a", "https://www.gov.uk/national-insurance-credits/eligibility")
    }
    "render page with  link back " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>div:nth-child(17)>a", "nisp.back")
    }
    "render page with href link back " in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>div:nth-child(17)>a", "/check-your-state-pension/account/nirecord/gaps")
    }
  }


  "Render Ni Record without gap and has pre75years" should {
    lazy val controller = new MockNIRecordController {
      override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
      override val customAuditConnector: CustomAuditConnector = MockCustomAuditConnector
      override val sessionCache: SessionCache = MockSessionCache
      override val showFullNI = true
      override val currentDate = new LocalDate(2016, 9, 9)
      override protected def authConnector: AuthConnector = MockAuthConnector
      override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
      override val metricsService: MetricsService = MockMetricsService
      override val nationalInsuranceService: NationalInsuranceService = mock[NationalInsuranceService]
      override val statePensionService: StatePensionService = mock[StatePensionService]
    }

    when(controller.nationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(Right(NationalInsuranceRecord(
        qualifyingYears = 1,
        qualifyingYearsPriorTo1975 = 5,
        numberOfGaps = 0,
        numberOfGapsPayable = 0,
        Some(new LocalDate(1954, 3, 6)),
        false,
        new LocalDate(2016, 4, 5),
        List(
          NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
          NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
          NationalInsuranceTaxYearBuilder("2013-14", qualifying = false, payable = true, underInvestigation = false)
        ),
        reducedRateElection = false
      )
      )))

    when(controller.statePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(Left(StatePensionExclusionFiltered(
        Exclusion.AmountDissonance,
        Some(66),
        Some(new LocalDate(2020, 3, 6))
      )
      )))

    lazy val result = controller.showFull(authenticatedFakeRequest(mockUserId).withCookies(lanCookie))
    lazy val htmlAccountDoc = asDocument(contentAsString(result))

    /*Check side border :summary */

    "render page with text '1 year of full contribution'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "ul.list-bullet li:nth-child(1)", "nisp.nirecord.summary.fullContributions.single", "1")
    }

    "render with correct page title" in {
      assertElementContainsText(htmlAccountDoc, "head>title" ,messages("nisp.nirecord.title") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
    }

    "render page with qualifying years text '1 year of full contribution'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "ul.list-bullet li:nth-child(1)", "nisp.nirecord.summary.fullContributions.single", "1")
    }

    "render page with text 'you do not have any gaps in your record.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(6)", "nisp.nirecord.youdonothaveanygaps")
    }
    "render page with text 'pre75 years'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl.accordion>dt:nth-child(10)>div>div.ni-years", "nisp.nirecord.pre75Years")
    }
    "render page with text 'you have 5  qualifying years pre 1975'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>dl.accordion>dt:nth-child(10)>div>div.ni-full", "nisp.nirecord.pre75QualifyingYears", "5")
    }
    "render page with link  'back'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p.backlink>a", "nisp.back")
    }
    "render page with href link  'back'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>p.backlink>a", "javascript:window.history.back();")
    }
  }


  "Render Ni Record without gap and has gaps pre75years" should {
    lazy val controller = new MockNIRecordController {
      override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
      override val customAuditConnector: CustomAuditConnector = MockCustomAuditConnector
      override val sessionCache: SessionCache = MockSessionCache
      override val showFullNI = true
      override val currentDate = new LocalDate(2016, 9, 9)
      override protected def authConnector: AuthConnector = MockAuthConnector
      override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
      override val metricsService: MetricsService = MockMetricsService
      override val nationalInsuranceService: NationalInsuranceService = mock[NationalInsuranceService]
      override val statePensionService: StatePensionService = mock[StatePensionService]
    }

    when(controller.nationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(Right(NationalInsuranceRecord(
        qualifyingYears = 2,
        qualifyingYearsPriorTo1975 = 0,
        numberOfGaps = 1,
        numberOfGapsPayable = 1,
        Some(new LocalDate(1954, 3, 6)),
        false,
        new LocalDate(2015, 4, 5),
        List(
          NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
          NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
          NationalInsuranceTaxYearBuilder("2013-14", qualifying = false, payable = true, underInvestigation = false)
        ),
        reducedRateElection = false
      )
      )))

    when(controller.statePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(Left(StatePensionExclusionFiltered(
        Exclusion.AmountDissonance,
        Some(66),
        Some(new LocalDate(2020, 3, 6))
      )
      )))

    lazy val result = controller.showFull(authenticatedFakeRequest(mockUserId).withCookies(lanCookie))
    lazy val htmlAccountDoc = asDocument(contentAsString(result))

    /*Check side border :summary */
    "render page with text  you have" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(4)", "nisp.nirecord.summary.youhave")
    }

    "render page with qualifying years text '2 year of full contributions'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "ul.list-bullet li:nth-child(1)", "nisp.nirecord.summary.fullContributions", "2")
    }

    "render page with text 1 years when you did not contribute enough" in {
      assertContainsDynamicMessage(htmlAccountDoc, "ul.list-bullet li:nth-child(2)", "nisp.nirecord.summary.gaps.single", "1")
    }

    "render with correct page title" in {
      assertElementContainsText(htmlAccountDoc, "head>title" ,messages("nisp.nirecord.title") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
    }

    "render page with qualifying years text '2 years of full contributions'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "ul.list-bullet li:nth-child(1)", "nisp.nirecord.summary.fullContributions", "2")
    }

    "render page with text '1 year you did not contribute enough" in {
      assertContainsDynamicMessage(htmlAccountDoc, "ul.list-bullet li:nth-child(2)", "nisp.nirecord.summary.gaps.single", "1")
    }

    "render page with text 'pre75 years'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl.accordion>dt:nth-child(10)>div>div.ni-years", "nisp.nirecord.pre75Years")
    }
    "render page with text 'Our records show you do not have any full years up to 5 April 1975'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl.accordion>dt:nth-child(10)>div>div.ni-full", "nisp.nirecord.pre75QualifyingYearsZero")
    }
    "render page with link  'back'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p.backlink>a", "nisp.back")
    }
    "render page with href link 'back'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>p.backlink>a", "javascript:window.history.back();")
    }
  }


  "Render Ni Record without gap and has gaps pre75years with Years to contribute " should {
    lazy val controller = new MockNIRecordController {
      override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
      override val customAuditConnector: CustomAuditConnector = MockCustomAuditConnector
      override val sessionCache: SessionCache = MockSessionCache
      override val showFullNI = true
      override val currentDate = new LocalDate(2016, 9, 9)
      override protected def authConnector: AuthConnector = MockAuthConnector
      override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
      override val metricsService: MetricsService = MockMetricsService
      override val nationalInsuranceService: NationalInsuranceService = mock[NationalInsuranceService]
      override val statePensionService: StatePensionService = mock[StatePensionService]
    }

    when(controller.nationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(Right(NationalInsuranceRecord(
        qualifyingYears = 2,
        qualifyingYearsPriorTo1975 = 0,
        numberOfGaps = 1,
        numberOfGapsPayable = 1,
        Some(new LocalDate(1954, 3, 6)),
        false,
        new LocalDate(2017, 4, 5),
        List(
          NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = true),
          NationalInsuranceTaxYearBuilder("2014-15", qualifying = true, underInvestigation = false),
          NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
        ),
        reducedRateElection = false
      )
      )))

    when(controller.statePensionService.yearsToContributeUntilPensionAge(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(1)

    when(controller.statePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(Left(StatePensionExclusionFiltered(
        Exclusion.AmountDissonance,
        Some(66),
        Some(new LocalDate(2018, 3, 6))
      )
      )))

    lazy val result = controller.showFull(authenticatedFakeRequest(mockUserId).withCookies(lanCookie))
    lazy val htmlAccountDoc = asDocument(contentAsString(result))

    /*Check side border :summary */

    "render page with text  you have" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(4)", "nisp.nirecord.summary.youhave")
    }

    "render page with qualifying years text '2 year of full contributions'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "ul.list-bullet li:nth-child(1)", "nisp.nirecord.summary.fullContributions", "2")
    }

    "render page with text  1 year to contribute before 5 April 2017'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "ul.list-bullet li:nth-child(2)", "nisp.nirecord.summary.yearsRemaining.single", "1", "2017")
    }

    "render page with text 1 years when you did not contribute enough" in {
      assertContainsDynamicMessage(htmlAccountDoc, "ul.list-bullet li:nth-child(3)", "nisp.nirecord.summary.gaps.single", "1")
    }

    /* Under investigation*/

    "render page with text  'full year for under investigation'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl>dt:nth-child(3)>div>div.ni-notfull", "nisp.nirecord.fullyear")
    }

    "render page with text 'we are checking this year to see if it counts towards your pension. We will update your records '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl>dd:nth-child(4)>div.contributions-wrapper>p:nth-child(1)", "nisp.nirecord.gap.underInvestigation")
    }

    /*Ends here*/

    "render page with text  'full year'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl>dt:nth-child(5)>div>div.ni-notfull", "nisp.nirecord.fullyear")
    }

    "render page with text 'you have contributions from '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl>dd:nth-child(6)>div.contributions-wrapper>p:nth-child(1)", "nisp.nirecord.yourcontributionfrom")
    }

    "render page with text 'paid employment : £12,345.67'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>dl>dd:nth-child(6)>div.contributions-wrapper>p:nth-child(3)", "nisp.nirecord.gap.paidemployment", "£12,345.67")
    }
    "render page with text 'self- employment : 10 weeks'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>dl>dd:nth-child(6)>div.contributions-wrapper>p:nth-child(4)", "nisp.nirecord.gap.selfemployed.plural", "10")
    }
    "render page with text 'Voluntary: : 8 weeks'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>dl>dd:nth-child(6)>div.contributions-wrapper>p:nth-child(5)", "nisp.nirecord.gap.voluntary.plural", "8")
    }

    "render page with text 'National Insurance credits: : 12 weeks'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>dl>dd:nth-child(6)>div.contributions-wrapper>p:nth-child(7)", "nisp.nirecord.gap.whenyouareclaiming.plural", "12")
    }
    "render page with text 'These may have been added to your record if you were ill/disabled, unemployed, caring for someone full-time or on jury service.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl>dd:nth-child(6)>div.contributions-wrapper>p:nth-child(8)", "nisp.nirecord.gap.whenyouareclaiming.info.plural")

    }
    "render page with link  'back'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p.backlink>a", "nisp.back")
    }
    "render page with href link 'back'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>p.backlink>a", "javascript:window.history.back();")
    }
  }


  "Render Ni Record with Single weeks in self ,contribution and paid -and a abroad User" should {
    lazy val controller = new MockNIRecordController {
      override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
      override val customAuditConnector: CustomAuditConnector = MockCustomAuditConnector
      override val sessionCache: SessionCache = MockSessionCache
      override val showFullNI = true
      override val currentDate = new LocalDate(2016, 9, 9)
      override protected def authConnector: AuthConnector = MockAuthConnector
      override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
      override val metricsService: MetricsService = MockMetricsService
      override val nationalInsuranceService: NationalInsuranceService = mock[NationalInsuranceService]
      override val statePensionService: StatePensionService = mock[StatePensionService]
    }

    when(controller.nationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(Right(NationalInsuranceRecord(
        qualifyingYears = 2,
        qualifyingYearsPriorTo1975 = 0,
        numberOfGaps = 1,
        numberOfGapsPayable = 1,
        Some(new LocalDate(1954, 3, 6)),
        false,
        new LocalDate(2017, 4, 5),
        List(
          NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = true),
          NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
          NationalInsuranceTaxYearBuilder("2013-14", qualifying = false, underInvestigation = false) /*payable = true*/
        ),
        reducedRateElection = false
      )
      )))

    when(controller.statePensionService.yearsToContributeUntilPensionAge(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(1)

    when(controller.statePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(Left(StatePensionExclusionFiltered(
        Exclusion.AmountDissonance,
        Some(66),
        Some(new LocalDate(2018, 3, 6))
      )
      )))

    lazy val result = controller.showFull(authenticatedFakeRequest(mockAbroadUserId).withCookies(lanCookie))
    lazy val htmlAccountDoc = asDocument(contentAsString(result))

    "render with correct page title" in {
      assertElementContainsText(htmlAccountDoc, "head>title" ,messages("nisp.nirecord.heading.uk") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
    }
    "render page with heading your UK National Insurance Record " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h1", "nisp.nirecord.heading.uk")
    }

    /*Check side border :summary */

    "render page with text  you have" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(4)", "nisp.nirecord.summary.youhave")
    }

    "render page with qualifying years text '2 year of full contributions'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "ul.list-bullet li:nth-child(1)", "nisp.nirecord.summary.fullContributions", "2")
    }

    "render page with text  1 year to contribute before 5 April 2017'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "ul.list-bullet li:nth-child(2)", "nisp.nirecord.summary.yearsRemaining.single", "1", "2017")
    }

    "render page with text 1 years when you did not contribute enough" in {
      assertContainsDynamicMessage(htmlAccountDoc, "ul.list-bullet li:nth-child(3)", "nisp.nirecord.summary.gaps.single", "1")
    }

    "render page with text  'year full year'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl>dt:nth-child(5)>div>div.ni-notfull", "nisp.nirecord.gap")
    }

    "render page with text 'you have contributions from '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl>dd:nth-child(6)>div.contributions-wrapper>p:nth-child(1)", "nisp.nirecord.yourcontributionfrom")
    }

    "render page with text 'self- employment : 1 week'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>dl>dd:nth-child(6)>div.contributions-wrapper>p:nth-child(4)", "nisp.nirecord.gap.selfemployed.singular", "1")
    }
    "render page with text 'Voluntary: : 1 week'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>dl>dd:nth-child(6)>div.contributions-wrapper>p:nth-child(5)", "nisp.nirecord.gap.voluntary.singular", "1")
    }

    "render page with text 'National Insurance credits: : 1 week'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>dl>dd:nth-child(6)>div.contributions-wrapper>p:nth-child(7)", "nisp.nirecord.gap.whenyouareclaiming.singular", "1")
    }
    "render page with text 'These may have been added to your record if you were ill/disabled, unemployed, caring for someone full-time or on jury service.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl>dd:nth-child(6)>div.contributions-wrapper>p:nth-child(8)", "nisp.nirecord.gap.whenyouareclaiming.info.singular")
    }
  }


}
