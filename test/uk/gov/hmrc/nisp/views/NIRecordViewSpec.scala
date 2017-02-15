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

import java.util.UUID

import builders.NationalInsuranceTaxYearBuilder
import org.joda.time.LocalDate
import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, _}
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.controllers.connectors.CustomAuditConnector
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.models.enums.Exclusion
import uk.gov.hmrc.nisp.models.{NationalInsuranceRecord, StatePension, StatePensionExclusion, StatePensionExclusionFiltered}
import uk.gov.hmrc.nisp.services.{CitizenDetailsService, MetricsService, NationalInsuranceService, StatePensionService}
import uk.gov.hmrc.nisp.views.html.HtmlSpec
import uk.gov.hmrc.play.frontend.auth.AuthenticationProviderIds
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.{HttpResponse, SessionKeys, Upstream4xxResponse}
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.time.DateTimeUtils.now

import scala.concurrent.Future


class NIRecordViewSpec extends UnitSpec with MockitoSugar with HtmlSpec with BeforeAndAfter with OneAppPerSuite {


  implicit val cachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever

  val mockUserNino = TestAccountBuilder.regularNino;
  val mockUserIdForecastOnly = "/auth/oid/mockforecastonly"
  val mockUsername = "mockuser"
  val mockUserId = "/auth/oid/" + mockUsername
  val mockFullUserId = "/auth/oid/mockfulluser"
  val mockAbroadUserId = "/auth/oid/mockabroad"

  lazy val fakeRequest = FakeRequest();


  def authenticatedFakeRequest(userId: String) = FakeRequest().withSession(
    SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
    SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
    SessionKeys.userId -> userId,
    SessionKeys.authProvider -> AuthenticationProviderIds.VerifyProviderId
  )


  "Render Ni Record to view all the years" should {

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


    lazy val result = controller.showFull(authenticatedFakeRequest(mockUserId))

    lazy val htmlAccountDoc = asDocument(contentAsString(result))

    /*Check side border :summary */
    "render page with heading  Summary" in {
      assertEqualsMessage(htmlAccountDoc, "div.sidebar-border>h2", "nisp.nirecord.summary.yourrecord")
    }
    "render page with number of qualifying yeras - 28" in {
      assertEqualsValue(htmlAccountDoc, "div.sidebar-border>p:nth-child(2)", "28")
    }
    "render page with text 'years of full contributions'" in {
      assertEqualsMessage(htmlAccountDoc, "div.sidebar-border>p:nth-child(3)", "nisp.nirecord.summary.fullContributions")
    }
    "render page with 4 years to contribute before 5 April 2018 '" in {
      assertEqualsValue(htmlAccountDoc, "div.sidebar-border>p:nth-child(4)", "4")
    }
    "render page with text  'years to contribute before 5 April 2017'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "div.sidebar-border>p:nth-child(5)", "nisp.nirecord.summary.yearsRemaining", "2018", null, null)
    }

    /*Ends here*/
    "render page with Gaps  heading  Your National Insurance record " in {
      assertEqualsMessage(htmlAccountDoc, "h1.heading-large", "nisp.nirecord.heading")
    }
    "render page with text 'All years.'" in {
      assertEqualsMessage(htmlAccountDoc, "p.lede", "nisp.nirecord.yournirecordallyears")
    }
    "render page with link 'View gaps only'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(4)>a", "nisp.nirecord.showgaps")
    }

    "render page with link href 'View gaps only'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>p:nth-child(4)>a", "/check-your-state-pension/account/nirecord/gaps")
    }
    "render page with text  'your record for this year is not available'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dt:nth-child(1)>div.ni-wrapper>div.inactive", "nisp.nirecord.unavailableyear")
    }

    "render page with text  'year is not full'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dt:nth-child(2)>div>div.ni-notfull", "nisp.nirecord.gap")
    }
    "render page with link 'View details'" in {

      assertContainsTextBetweenTags(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dt:nth-child(2)>div>div.ni-action>a", "View  details", "article.content__body>dl:nth-child(5)>dt:nth-child(2)>div>div.ni-action>a>span")
    }

    "render page with text  'You did not make any contributions this year '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(3)>div.contributions-wrapper>p.contributions-header", "nisp.nirecord.youdidnotmakeanycontrib")
    }

    "render page with text 'Find out more about'" in {
      assertContainsTextBetweenTags(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(2)", "Find out more about .", "article.content__body>dl:nth-child(5)>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(2)>a")
    }

    "render page with link 'gaps in your record and how to check them'" in {
      assertEqualsValue(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(2)>a", "gaps in your record and how to check them")
    }
    "render page with link href 'gaps in your record and how to check them'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(2)>a", "/check-your-state-pension/account/nirecord/gapsandhowtocheck")
    }

    "render page with text  'You can make up the shortfall'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(3)>div.contributions-wrapper>p.contributions-header:nth-child(3)", "nisp.nirecord.gap.youcanmakeupshortfall")
    }
    "render page with text  'Pay a voluntary contribution of £530 by 5 April 2023. This shortfall may increase after 5 April 2019.'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(4)", "nisp.nirecord.gap.payvoluntarycontrib", " &pound;704.60", "5 April 2023", "5 April 2019")
    }
    "render page with text  'Find out more about...'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(5)", "nisp.nirecord.gap.findoutmore", "/check-your-state-pension/account/nirecord/voluntarycontribs", null, null)
    }
    "render page with text  ' FUll years'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dt:nth-child(10)>div>div.ni-notfull", "nisp.nirecord.fullyear")
    }

    "render page with text  'You have contributions from '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(11)>div.contributions-wrapper>p.contributions-header", "nisp.nirecord.yourcontributionfrom")
    }

    "render page with text 'Paid employment £ 4,259.60'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(11)>div.contributions-wrapper>p:nth-child(3)", "nisp.nirecord.gap.paidemployment", " £4,259.60", null, null)
    }

    /*check for medical credit year*/

    "render page with text  'full year'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dt:nth-child(50)>div>div.ni-notfull", "nisp.nirecord.fullyear")
    }

    "render page with text 'you have contributions from '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(51)>div.contributions-wrapper>p:nth-child(1)", "nisp.nirecord.yourcontributionfrom")
    }

    "render page with text 'National insurence credits 52 weeks'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(51)>div.contributions-wrapper>p:nth-child(2)", "nisp.nirecord.gap.whenyouareclaiming.plural", "52", null, null)
    }
    "render page with text 'These may have been added to your record if you were ill/disabled...'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(51)>div.contributions-wrapper>p:nth-child(3)", "nisp.nirecord.gap.whenyouareclaiming.info.plural")
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
      assertLinkHasValue(htmlAccountDoc, "article.content__body>p.backlink>a", "/check-your-state-pension/account")
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

    lazy val result = controller.showGaps(authenticatedFakeRequest(mockUserId))

    lazy val htmlAccountDoc = asDocument(contentAsString(result))

    /*Check side border :summary */
    "render page with heading  Summary" in {
      assertEqualsMessage(htmlAccountDoc, "div.sidebar-border>h2", "nisp.nirecord.summary.yourrecord")
    }
    "render page with number of qualifying yeras - 28" in {
      assertEqualsValue(htmlAccountDoc, "div.sidebar-border>p:nth-child(2)", "28")
    }
    "render page with text 'years of full contributions'" in {
      assertEqualsMessage(htmlAccountDoc, "div.sidebar-border>p:nth-child(3)", "nisp.nirecord.summary.fullContributions")
    }
    "render page with 4 years to contribute before 5 April 2018 '" in {
      assertEqualsValue(htmlAccountDoc, "div.sidebar-border>p:nth-child(4)", "4")
    }
    "render page with text  'years to contribute before 5 April 2017'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "div.sidebar-border>p:nth-child(5)", "nisp.nirecord.summary.yearsRemaining", "2018", null, null)
    }
    "render page with 10 years - when you did not contribute enough" in {
      assertEqualsValue(htmlAccountDoc, "div.sidebar-border>p:nth-child(6)", "10")
    }
    "render page with text 'when you did not contribute enough'" in {
      assertEqualsMessage(htmlAccountDoc, "div.sidebar-border>p:nth-child(7)", "nisp.nirecord.summary.gaps")
    }
    /*Ends here*/
    "render page with Gaps  heading  Your National Insurance record " in {
      assertEqualsMessage(htmlAccountDoc, "h1.heading-large", "nisp.nirecord.heading")
    }

    "render page with text 'Years which are not full'" in {
      assertEqualsMessage(htmlAccountDoc, "p.lede", "nisp.nirecord.yournirecordgapyears")
    }
    "render page with link 'View all years'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(4)>a", "nisp.nirecord.showfull")
    }

    "render page with link href 'View all years'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>p:nth-child(4)>a", "/check-your-state-pension/account/nirecord")
    }
    "render page with text  'your record for this year is not available yet'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dt:nth-child(1)>div.ni-wrapper>div.inactive", "nisp.nirecord.unavailableyear")
    }

    "render page with text  'year is not full'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dt:nth-child(2)>div>div.ni-notfull", "nisp.nirecord.gap")
    }
    "render page with link 'View details'" in {

      assertContainsTextBetweenTags(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dt:nth-child(2)>div>div.ni-action>a", "View  details", "article.content__body>dl:nth-child(5)>dt:nth-child(2)>div>div.ni-action>a>span")
    }

    "render page with text  'You did not make any contributions this year '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(3)>div.contributions-wrapper>p.contributions-header", "nisp.nirecord.youdidnotmakeanycontrib")
    }

    "render page with text 'Find out more about'" in {
      assertContainsTextBetweenTags(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(2)", "Find out more about .", "article.content__body>dl:nth-child(5)>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(2)>a")
    }

    "render page with link 'gaps in your record and how to check them'" in {
      assertEqualsValue(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(2)>a", "gaps in your record and how to check them")
    }
    "render page with link href 'gaps in your record and how to check them'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(2)>a", "/check-your-state-pension/account/nirecord/gapsandhowtocheck")
    }

    "render page with text  'You can make up the shortfall'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(3)>div.contributions-wrapper>p.contributions-header:nth-child(3)", "nisp.nirecord.gap.youcanmakeupshortfall")
    }
    "render page with text  'Pay a voluntary contribution of figure out how to do it...'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(4)", "nisp.nirecord.gap.payvoluntarycontrib", " &pound;704.60", "5 April 2023", "5 April 2019")
    }
    "render page with text  'Find out more about...'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(5)", "nisp.nirecord.gap.findoutmore", "/check-your-state-pension/account/nirecord/voluntarycontribs", null, null)
    }
    "render page with text  ' year is not full'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dt:nth-child(10)>div>div.ni-notfull", "nisp.nirecord.gap")
    }

    "render page with text  'You did not make any contributions this year for toolate to pay '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(11)>div.contributions-wrapper>p.contributions-header", "nisp.nirecord.youdidnotmakeanycontrib")
    }

    "render page with text 'Find out more about for toolate to pay'" in {
      assertContainsTextBetweenTags(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(11)>div.contributions-wrapper>p:nth-child(2)", "Find out more about .", "article.content__body>dl:nth-child(5)>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(2)>a")
    }

    "render page with link 'gaps in your record and how to check them for toolate to pay'" in {
      assertEqualsValue(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(11)>div.contributions-wrapper>p:nth-child(2)>a", "gaps in your record and how to check them")
    }
    "render page with link href 'gaps in your record and how to check them for toolate to pay'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(11)>div.contributions-wrapper>p:nth-child(2)>a", "/check-your-state-pension/account/nirecord/gapsandhowtocheck")
    }

    "render page with text  'It’s too late to pay for this year. You can usually only pay for the last 6 years.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(11)>div.contributions-wrapper>p.panel-indent:nth-child(3)", "nisp.nirecord.gap.latePaymentMessage")
    }
    "render page with link  'view all years'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p>a", "nisp.nirecord.showfull")
    }
    "render page with href link  'view all years'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>p>a", "/check-your-state-pension/account/nirecord")
    }
    "render page with link  'back'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p.backlink>a", "nisp.back")
    }
    "render page with href link  'back'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>p.backlink>a", "/check-your-state-pension/account")
    }


  }
  "Render Ni Record view With HRP Message" should {

    lazy val result = html.nirecordGapsAndHowToCheckThem(true);

    lazy val htmlAccountDoc = asDocument(contentAsString(result))

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
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>div:nth-child(16)>p", "nisp.nirecord.gapsinyourrecord.nationalinsurancecredits.hrp", null, null, null)
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

    /*  lazy val controller = new MockNIRecordController {
        override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
        override val customAuditConnector: CustomAuditConnector = MockCustomAuditConnector
        override val sessionCache: SessionCache = MockSessionCache
        override val showFullNI = true
        override val currentDate = new LocalDate(2016, 9, 9)

        override protected def authConnector: AuthConnector = MockAuthConnector

        override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
        override val metricsService: MetricsService = MockMetricsService
      }*/

    lazy val result = html.nirecordGapsAndHowToCheckThem(false);

    lazy val htmlAccountDoc = asDocument(contentAsString(result))

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

    when(controller.nationalInsuranceService.getSummary(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(Right(NationalInsuranceRecord(
        qualifyingYears = 1,
        qualifyingYearsPriorTo1975 = 5,
        numberOfGaps = 0,
        numberOfGapsPayable = 0,
        new LocalDate(1954, 3, 6),
        false,
        new LocalDate(2016, 4, 5),
        List(
          NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
          NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
          NationalInsuranceTaxYearBuilder("2013-14", qualifying = false, payable = true, underInvestigation = false)

        )
      )
      )))

    when(controller.statePensionService.getSummary(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(Left(StatePensionExclusionFiltered(
        Exclusion.AmountDissonance,
        Some(66),
        Some(new LocalDate(2020, 3, 6))
      )
      )))


    lazy val result = controller.showFull(authenticatedFakeRequest(mockUserId))

    lazy val htmlAccountDoc = asDocument(contentAsString(result))

    /*Check side border :summary */
    "render page with heading  Main page Summary" in {
      assertEqualsMessage(htmlAccountDoc, "div.sidebar-mobile>h2", "nisp.nirecord.summary.yourrecord")
    }
    "render page with text  you have" in {
      assertEqualsMessage(htmlAccountDoc, "div.sidebar-mobile>p", "nisp.nirecord.summary.youhave")
    }
    "render page with text  1 year of full contributions" in {
      val contributionMessage = "1 " + Messages("nisp.nirecord.summary.fullContributions.single")
      assertEqualsValue(htmlAccountDoc, "div.sidebar-mobile>ul.list-bullet>li:nth-child(1)", contributionMessage)
    }

    "render page with heading  Summary" in {
      assertEqualsMessage(htmlAccountDoc, "div.sidebar-border>h2", "nisp.nirecord.summary.yourrecord")
    }
    "render page with number of qualifying yeras" in {
      assertEqualsValue(htmlAccountDoc, "div.sidebar-border>p:nth-child(2)", "1")
    }
    "render page with text 'years of full contributions'" in {
      assertEqualsMessage(htmlAccountDoc, "div.sidebar-border>p:nth-child(3)", "nisp.nirecord.summary.fullContributions.single")
    }
    "render page with text 'you do not have any gaps in your record.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(3)", "nisp.nirecord.youdonothaveanygaps")
    }
    "render page with text 'pre75 years'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl.accordion>dt:nth-child(10)>div>div.ni-years", "nisp.nirecord.pre75Years")
    }
    "render page with text 'you have 5  qualifying years pre 1975'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>dl.accordion>dt:nth-child(10)>div>div.ni-full", "nisp.nirecord.pre75QualifyingYears", "5", null, null)
    }
    "render page with link  'back'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p.backlink>a", "nisp.back")
    }
    "render page with href link  'back'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>p.backlink>a", "/check-your-state-pension/account")
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

    when(controller.nationalInsuranceService.getSummary(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(Right(NationalInsuranceRecord(
        qualifyingYears = 2,
        qualifyingYearsPriorTo1975 = 0,
        numberOfGaps = 1,
        numberOfGapsPayable = 1,
        new LocalDate(1954, 3, 6),
        false,
        new LocalDate(2015, 4, 5),
        List(
          NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
          NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
          NationalInsuranceTaxYearBuilder("2013-14", qualifying = false, payable = true, underInvestigation = false)
        )
      )
      )))


    when(controller.statePensionService.getSummary(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(Left(StatePensionExclusionFiltered (
        Exclusion.AmountDissonance,
        Some(66),
        Some(new LocalDate(2020, 3, 6))
      )
      )))


    lazy val result = controller.showFull(authenticatedFakeRequest(mockUserId))

    lazy val htmlAccountDoc = asDocument(contentAsString(result))

    /*Check side border :summary */
    "render page with heading  Main page Summary" in {
      assertEqualsMessage(htmlAccountDoc, "div.sidebar-mobile>h2", "nisp.nirecord.summary.yourrecord")
    }
    "render page with text  you have" in {
      assertEqualsMessage(htmlAccountDoc, "div.sidebar-mobile>p", "nisp.nirecord.summary.youhave")
    }
    "render page with text  1 year of full contributions" in {
      val contributionMessage = "2 " + Messages("nisp.nirecord.summary.fullContributions")
      assertEqualsValue(htmlAccountDoc, "div.sidebar-mobile>ul.list-bullet>li:nth-child(1)", contributionMessage)
    }
    "render page with text 2 years when you did not contribute enough " in {
      val contributionMessage = "1 " + Messages("nisp.nirecord.summary.gaps.single")
      assertEqualsValue(htmlAccountDoc, "div.sidebar-mobile>ul.list-bullet>li:nth-child(2)", contributionMessage)
    }

    "render page with heading  Summary" in {
      assertEqualsMessage(htmlAccountDoc, "div.sidebar-border>h2", "nisp.nirecord.summary.yourrecord")
    }
    "render page with number of qualifying yeras" in {
      assertEqualsValue(htmlAccountDoc, "div.sidebar-border>p:nth-child(2)", "2")
    }
    "render page with text 'years of full contributions'" in {
      assertEqualsMessage(htmlAccountDoc, "div.sidebar-border>p:nth-child(3)", "nisp.nirecord.summary.fullContributions")
    }

    "render page with text '1 year you did not contribute'" in {
      assertEqualsValue(htmlAccountDoc, "div.sidebar-border>p:nth-child(4)", "1")
    }
    "render page with text 'year when you did not contribute enough.'" in {
      assertEqualsMessage(htmlAccountDoc, "div.sidebar-border>p:nth-child(5)", "nisp.nirecord.summary.gaps.single")
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
      assertLinkHasValue(htmlAccountDoc, "article.content__body>p.backlink>a", "/check-your-state-pension/account")
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

    when(controller.nationalInsuranceService.getSummary(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(Right(NationalInsuranceRecord(
        qualifyingYears = 2,
        qualifyingYearsPriorTo1975 = 0,
        numberOfGaps = 1,
        numberOfGapsPayable = 1,
        new LocalDate(1954, 3, 6),
        false,
        new LocalDate(2017, 4, 5),
        List(

          NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = true),
          NationalInsuranceTaxYearBuilder("2014-15", qualifying = true, underInvestigation = false),
          NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
        )
      )
      )))

    when(controller.statePensionService.yearsToContributeUntilPensionAge(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(1))

    when(controller.statePensionService.getSummary(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(Left(StatePensionExclusionFiltered(
        Exclusion.AmountDissonance,
        Some(66),
        Some(new LocalDate(2018, 3, 6))
      )
      )))


    lazy val result = controller.showFull(authenticatedFakeRequest(mockUserId))

    lazy val htmlAccountDoc = asDocument(contentAsString(result))

    /*Check side border :summary */
    "render page with heading  Main page Summary" in {
      assertEqualsMessage(htmlAccountDoc, "div.sidebar-mobile>h2", "nisp.nirecord.summary.yourrecord")
    }
    "render page with text  you have" in {
      assertEqualsMessage(htmlAccountDoc, "div.sidebar-mobile>p", "nisp.nirecord.summary.youhave")
    }
    "render page with text  2 years of full contributions" in {
      val contributionMessage = "2 " + Messages("nisp.nirecord.summary.fullContributions")
      assertEqualsValue(htmlAccountDoc, "div.sidebar-mobile>ul.list-bullet>li:nth-child(1)", contributionMessage)
    }
    "render page with text  1 year to contribute before 5 April 2017" in {
      val contributionMessage = "1 " + Messages("nisp.nirecord.summary.yearsRemaining.single", "2017")
      assertEqualsValue(htmlAccountDoc, "div.sidebar-mobile>ul.list-bullet>li:nth-child(2)", contributionMessage)
    }
    "render page with text  1 year year when you did not contribute enough" in {
      val contributionMessage = "1 " + Messages("nisp.nirecord.summary.gaps.single")
      assertEqualsValue(htmlAccountDoc, "div.sidebar-mobile>ul.list-bullet>li:nth-child(3)", contributionMessage)
    }

    "render page with 1 year to contribute before 5 April 2017 '" in {
      assertEqualsValue(htmlAccountDoc, "div.sidebar-border>p:nth-child(4)", "1")
    }
    "render page with text  'year to contribute before 5 April 2017'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "div.sidebar-border>p:nth-child(5)", "nisp.nirecord.summary.yearsRemaining.single", "2017", null, null)
    }
    "render page with 1 year year when you did not contribute enough '" in {
      assertEqualsValue(htmlAccountDoc, "div.sidebar-border>p:nth-child(6)", "1")
    }
    "render page with text  'year when you did not contribute enough'" in {
      assertEqualsMessage(htmlAccountDoc, "div.sidebar-border>p:nth-child(7)", "nisp.nirecord.summary.gaps.single")
    }
    /* Under investigation*/

    "render page with text  'full year for under investigation'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dt:nth-child(3)>div>div.ni-notfull", "nisp.nirecord.fullyear")
    }

    "render page with text 'we are checking this year to see if it counts towards your pension. We will update your records '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(4)>div.contributions-wrapper>p:nth-child(1)", "nisp.nirecord.gap.underInvestigation")
    }

    /*Ends here*/


    "render page with text  'full year'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dt:nth-child(5)>div>div.ni-notfull", "nisp.nirecord.fullyear")
    }

    "render page with text 'you have contributions from '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(6)>div.contributions-wrapper>p:nth-child(1)", "nisp.nirecord.yourcontributionfrom")
    }

    "render page with text 'paid employment : £12,345.67'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(6)>div.contributions-wrapper>p:nth-child(3)", "nisp.nirecord.gap.paidemployment", " £12,345.67", null, null)
    }
    "render page with text 'self- employment : 10 weeks'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(6)>div.contributions-wrapper>p:nth-child(4)", "nisp.nirecord.gap.selfemployed.plural", "10", null, null)
    }
    "render page with text 'Voluntary: : 8 weeks'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(6)>div.contributions-wrapper>p:nth-child(5)", "nisp.nirecord.gap.voluntary.plural", "8", null, null)
    }

    "render page with text 'National Insurance credits: : 12 weeks'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(6)>div.contributions-wrapper>p:nth-child(7)", "nisp.nirecord.gap.whenyouareclaiming.plural", "12", null, null)
    }
    "render page with text 'These may have been added to your record if you were ill/disabled, unemployed, caring for someone full-time or on jury service.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(6)>div.contributions-wrapper>p:nth-child(8)", "nisp.nirecord.gap.whenyouareclaiming.info.plural")

    }
    "render page with link  'back'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p.backlink>a", "nisp.back")
    }
    "render page with href link 'back'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>p.backlink>a", "/check-your-state-pension/account")
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

    when(controller.nationalInsuranceService.getSummary(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(Right(NationalInsuranceRecord(
        qualifyingYears = 2,
        qualifyingYearsPriorTo1975 = 0,
        numberOfGaps = 1,
        numberOfGapsPayable = 1,
        new LocalDate(1954, 3, 6),
        false,
        new LocalDate(2017, 4, 5),
        List(

          NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = true),
          NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
          NationalInsuranceTaxYearBuilder("2013-14", qualifying = false, underInvestigation = false) /*payable = true*/
        )
      )
      )))

    when(controller.statePensionService.yearsToContributeUntilPensionAge(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(1))

    when(controller.statePensionService.getSummary(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(Left(StatePensionExclusionFiltered(
        Exclusion.AmountDissonance,
        Some(66),
        Some(new LocalDate(2018, 3, 6))
      )
      )))


    lazy val result = controller.showFull(authenticatedFakeRequest(mockAbroadUserId))

    lazy val htmlAccountDoc = asDocument(contentAsString(result))

    "render page with heading your UK National Insurance Record " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h1", "nisp.nirecord.heading.uk")
    }

    /*Check side border :summary */
    "render page with heading  Main page Summary" in {
      assertEqualsMessage(htmlAccountDoc, "div.sidebar-mobile>h2", "nisp.nirecord.summary.yourrecord")
    }
    "render page with text  you have" in {
      assertEqualsMessage(htmlAccountDoc, "div.sidebar-mobile>p", "nisp.nirecord.summary.youhave")
    }
    "render page with text  2 years of full contributions" in {
      val contributionMessage = "2 " + Messages("nisp.nirecord.summary.fullContributions")
      assertEqualsValue(htmlAccountDoc, "div.sidebar-mobile>ul.list-bullet>li:nth-child(1)", contributionMessage)
    }
    "render page with text  1 year to contribute before 5 April 2017" in {
      val contributionMessage = "1 " + Messages("nisp.nirecord.summary.yearsRemaining.single", "2017")
      assertEqualsValue(htmlAccountDoc, "div.sidebar-mobile>ul.list-bullet>li:nth-child(2)", contributionMessage)
    }
    "render page with text  1 year year when you did not contribute enough" in {
      val contributionMessage = "1 " + Messages("nisp.nirecord.summary.gaps.single")
      assertEqualsValue(htmlAccountDoc, "div.sidebar-mobile>ul.list-bullet>li:nth-child(3)", contributionMessage)
    }

    "render page with 1 year to contribute before 5 April 2017 '" in {
      assertEqualsValue(htmlAccountDoc, "div.sidebar-border>p:nth-child(4)", "1")
    }
    "render page with text  'year to contribute before 5 April 2017'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "div.sidebar-border>p:nth-child(5)", "nisp.nirecord.summary.yearsRemaining.single", "2017", null, null)
    }
    "render page with 1 year year when you did not contribute enough '" in {
      assertEqualsValue(htmlAccountDoc, "div.sidebar-border>p:nth-child(6)", "1")
    }
    "render page with text  'year when you did not contribute enough'" in {
      assertEqualsMessage(htmlAccountDoc, "div.sidebar-border>p:nth-child(7)", "nisp.nirecord.summary.gaps.single")
    }

    "render page with text  'year full year'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dt:nth-child(5)>div>div.ni-notfull", "nisp.nirecord.gap")
    }

    "render page with text 'you have contributions from '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(6)>div.contributions-wrapper>p:nth-child(1)", "nisp.nirecord.yourcontributionfrom")
    }

    "render page with text 'self- employment : 1 week'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(6)>div.contributions-wrapper>p:nth-child(4)", "nisp.nirecord.gap.selfemployed.singular", "1", null, null)
    }
    "render page with text 'Voluntary: : 1 week'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(6)>div.contributions-wrapper>p:nth-child(5)", "nisp.nirecord.gap.voluntary.singular", "1", null, null)
    }

    "render page with text 'National Insurance credits: : 1 week'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(6)>div.contributions-wrapper>p:nth-child(7)", "nisp.nirecord.gap.whenyouareclaiming.singular", "1", null, null)
    }
    "render page with text 'These may have been added to your record if you were ill/disabled, unemployed, caring for someone full-time or on jury service.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>dl:nth-child(5)>dd:nth-child(6)>div.contributions-wrapper>p:nth-child(8)", "nisp.nirecord.gap.whenyouareclaiming.info.singular")
    }

  }
}
