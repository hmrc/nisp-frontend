/*
 * Copyright 2022 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.{any => mockAny, eq => mockEQ}
import org.mockito.Mockito.{reset, when}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.contentAsString
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.LoginTimes
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.nisp.builders.NationalInsuranceTaxYearBuilder
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.NIRecordController
import uk.gov.hmrc.nisp.controllers.auth.{AuthAction, AuthDetails, AuthenticatedRequest, NispAuthedUser}
import uk.gov.hmrc.nisp.controllers.pertax.PertaxHelper
import uk.gov.hmrc.nisp.fixtures.NispAuthedUserFixture
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.services.{NationalInsuranceService, StatePensionService}
import uk.gov.hmrc.nisp.utils.{Constants, DateProvider}
import uk.gov.hmrc.nisp.views.html.nirecordGapsAndHowToCheckThem
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.language.LanguageUtils
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}
import uk.gov.hmrc.renderer.TemplateRenderer

import java.time.{Instant, LocalDate}
import java.util.UUID
import scala.concurrent.Future

class NIRecordViewSpec extends HtmlSpec with Injecting {

  implicit val cachedRetriever: CachedStaticHtmlPartialRetriever = FakeCachedStaticHtmlPartialRetriever
  implicit val templateRenderer: TemplateRenderer                = FakeTemplateRenderer

  val authDetails                                   = AuthDetails(ConfidenceLevel.L200, LoginTimes(Instant.now(), None))
  implicit val user: NispAuthedUser                 = NispAuthedUserFixture.user(TestAccountBuilder.regularNino)
  implicit val abroadUser: NispAuthedUser           = NispAuthedUserFixture.user(TestAccountBuilder.abroadNino)
  implicit val fakeRequest: AuthenticatedRequest[_] = AuthenticatedRequest(FakeRequest(), user, authDetails)
  implicit val abroadRequest: AuthenticatedRequest[_] = AuthenticatedRequest(FakeRequest(), abroadUser, authDetails)

  val mockAuditConnector: AuditConnector                     = mock[AuditConnector]
  val mockNationalInsuranceService: NationalInsuranceService = mock[NationalInsuranceService]
  val mockStatePensionService: StatePensionService           = mock[StatePensionService]
  val mockAppConfig: ApplicationConfig                       = mock[ApplicationConfig]
  val mockPertaxHelper: PertaxHelper                         = mock[PertaxHelper]
  val mockDateProvider: DateProvider                         = mock[DateProvider]

  lazy val langUtils = inject[LanguageUtils]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[AuditConnector].toInstance(mockAuditConnector),
      bind[AuthAction].to[FakeAuthAction],
      bind[NationalInsuranceService].toInstance(mockNationalInsuranceService),
      bind[StatePensionService].toInstance(mockStatePensionService),
      bind[ApplicationConfig].toInstance(mockAppConfig),
      bind[PertaxHelper].toInstance(mockPertaxHelper),
      bind[CachedStaticHtmlPartialRetriever].toInstance(cachedRetriever),
      bind[FormPartialRetriever].to[FakePartialRetriever],
      bind[TemplateRenderer].toInstance(templateRenderer),
      bind[DateProvider].toInstance(mockDateProvider)
    )
    .build()

  lazy val controller = inject[NIRecordController]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(
      mockNationalInsuranceService,
      mockStatePensionService,
      mockAppConfig,
      mockPertaxHelper,
      mockAuditConnector,
      mockDateProvider
    )
    mockSetup
    when(mockAppConfig.pertaxFrontendUrl).thenReturn("/pert")
    when(mockAppConfig.reportAProblemNonJSUrl).thenReturn("/reportAProblem")
    when(mockAppConfig.contactFormServiceIdentifier).thenReturn("/id")
  }

  def generateFakeRequest = FakeRequest().withSession(
    SessionKeys.sessionId            -> s"session-${UUID.randomUUID()}",
    SessionKeys.lastRequestTimestamp -> LocalDate.now.toEpochDay.toString
  )

  val nIRecordRegular = TestAccountBuilder
    .jsonResponseByType[NationalInsuranceRecord](TestAccountBuilder.regularNino, "national-insurance-record")

  def mockSetup = {
    when(mockDateProvider.currentDate).thenReturn(LocalDate.of(2016, 9, 9))
    when(mockAppConfig.showFullNI).thenReturn(true)

    when(mockStatePensionService.yearsToContributeUntilPensionAge(mockEQ(LocalDate.of(2014, 4, 5)), mockEQ(2017)))
      .thenReturn(4)

    when(mockStatePensionService.getSummary(mockAny())(mockAny()))
      .thenReturn(Future.successful(Right(Right(StatePension(
        LocalDate.of(2015, 4, 5),
        amounts = StatePensionAmounts(
          protectedPayment = false,
          StatePensionAmountRegular(133.41, 580.1, 6961.14),
          StatePensionAmountForecast(3, 146.76, 638.14, 7657.73),
          StatePensionAmountMaximum(3, 2, 155.65, 676.8, 8121.59),
          StatePensionAmountRegular(0, 0, 0)
        ),
        pensionAge = 64,
        LocalDate.of(2018, 7, 6),
        "2017-18",
        30,
        pensionSharingOrder = false,
        currentFullWeeklyPensionAmount = 155.65,
        reducedRateElection = false,
        statePensionAgeUnderConsideration = false
      )
      ))))

    val niRecord = nIRecordRegular.copy(qualifyingYearsPriorTo1975 = -3)
    when(mockNationalInsuranceService.getSummary(mockAny())(mockAny())).
      thenReturn(Future.successful(Right(Right(niRecord))))

    when(mockAppConfig.showUrBanner).thenReturn(true)
    when(mockAppConfig.urBannerUrl).thenReturn(
      "https://signup.take-part-in-research.service.gov.uk/?utm_campaign=checkyourstatepensionPTA&utm_source=Other&utm_medium=other&t=HMRC&id=183"
    )
  }

  "Render Ni Record to view all the years" should {
    /*Check side border :summary */

    lazy val doc = asDocument(contentAsString(controller.showFull(generateFakeRequest)))

    "render page with qualifying years text '4 years of full contributions'" in {
      assertContainsDynamicMessage(
        doc,
        "[data-spec='nirecordpage__ul__qualifying_years__li1']",
        "nisp.nirecord.summary.fullContributions",
        "28"
      )
    }

    "render page with text '4 years to contribute before 5 April 2018 '" in {
      assertContainsDynamicMessage(
        doc,
        "[data-spec='nirecordpage__ul__qualifying_years__li2']",
        "nisp.nirecord.summary.yearsRemaining",
        "4",
        "2018"
      )
    }

    /*Ends here*/
    "render with correct page title" in {
      assertElementContainsText(
        doc,
        "head > title",
        messages("nisp.nirecord.heading")
          + Constants.titleSplitter
          + messages("nisp.title.extension")
          + Constants.titleSplitter
          + messages("nisp.gov-uk")
      )
    }

    "render page with Gaps  heading  Your National Insurance record " in {
      assertEqualsMessage(
        doc,
        "[data-spec='nirecordpage__pageheading'] [data-component='nisp_page_heading__h1']",
        "nisp.nirecord.heading"
      )
    }

    "render page with name " in {
      assertEqualsValue(
        doc,
        "[data-spec='nirecordpage__pageheading'] [data-aria-hidden]",
        "AHMED BRENNAN"
      )
    }

    "render page with national Insurance number " in {
      assertElementsOwnMessage(
        doc,
        "[data-spec='nirecordpage__details__nino'] .govuk-details__summary-text",
        "nisp.show.nino"
      )
    }

    "render page with link 'View gaps only'" in {
      assertEqualsMessage(
        doc,
        "[data-spec='nirecordpage__inset_text2__link']",
        "nisp.nirecord.showgaps"
      )
    }

    "render page with link href 'View gaps only'" in {
      assertLinkHasValue(
        doc,
        "[data-spec='nirecordpage__inset_text2__link']",
        "/check-your-state-pension/account/nirecord/gaps"
      )
    }

    "render page with text 'your record for this year is not available'" in {
      assertEqualsMessage(
        doc,
        ".govuk-grid-column-two-thirds>dl>dt:nth-child(1)>div.ni-wrapper>div.inactive",
        "nisp.nirecord.unavailableyear"
      )
    }

    "render page with text 'year is not full'" in {
      assertEqualsMessage(
        doc,
        ".govuk-grid-column-two-thirds>dl>dt:nth-child(2)>div>div.ni-notfull",
        "nisp.nirecord.gap"
      )
    }

    "render page with link 'View details'" in {
      assertContainsExpectedValue(
        doc,
        ".govuk-grid-column-two-thirds>dl>dt:nth-child(2)>div>div.ni-action>a.view-details",
        "nisp.nirecord.gap.viewdetails",
        "2013-14"
      )
    }

    "render page with text 'You did not make any contributions this year '" in {
      assertEqualsMessage(
        doc,
        ".govuk-grid-column-two-thirds>dl>dd:nth-child(3)>div.contributions-wrapper>p.contributions-header",
        "nisp.nirecord.youdidnotmakeanycontrib"
      )
    }

    "render page with text 'Find out more about gaps in your account'" in {
      assertContainsExpectedValue(
        doc,
        ".govuk-grid-column-two-thirds>dl>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(2)",
        "nisp.nirecord.gap.findoutmoreabout",
        "/check-your-state-pension/account/nirecord/gapsandhowtocheck"
      )
    }

    "render page with link href 'gaps in your record and how to check them'" in {
      assertLinkHasValue(
        doc,
        ".govuk-grid-column-two-thirds>dl>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(2)>a",
        "/check-your-state-pension/account/nirecord/gapsandhowtocheck"
      )
    }

    "render page with text 'You can make up the shortfall'" in {
      assertEqualsMessage(
        doc,
        ".govuk-grid-column-two-thirds>dl>dd:nth-child(3)>div.contributions-wrapper>p.contributions-header:nth-child(3)",
        "nisp.nirecord.gap.youcanmakeupshortfall"
      )
    }

    "render page with text 'Pay a voluntary contribution of £530 by 5 April 2023. This shortfall may increase after 5 April 2019.'" in {
      assertContainsDynamicMessage(
        doc,
        ".govuk-grid-column-two-thirds>dl>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(4)",
        "nisp.nirecord.gap.payvoluntarycontrib",
        "&pound;704.60",
        langUtils.Dates.formatDate(LocalDate.of(2023, 4, 5)),
        langUtils.Dates.formatDate(LocalDate.of(2019, 4, 5))
      )
    }

    "render page with text 'Find out more about...'" in {
      assertContainsDynamicMessage(
        doc,
        ".govuk-grid-column-two-thirds>dl>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(5)",
        "nisp.nirecord.gap.findoutmore",
        "/check-your-state-pension/account/nirecord/voluntarycontribs"
      )
    }

    "render page with text ' FUll years'" in {
      assertEqualsMessage(
        doc,
        ".govuk-grid-column-two-thirds>dl>dt:nth-child(10)>div>div.ni-notfull",
        "nisp.nirecord.fullyear"
      )
    }

    "render page with text 'You have contributions from '" in {
      assertEqualsMessage(
        doc,
        ".govuk-grid-column-two-thirds>dl>dd:nth-child(11)>div.contributions-wrapper>p.contributions-header",
        "nisp.nirecord.yourcontributionfrom"
      )
    }

    "render page with text 'Paid employment £ 1,149.98'" in {
      assertContainsDynamicMessageUsingClass(
        doc,
        "payment-message",
        "nisp.nirecord.gap.paidemployment",
        "£1,149.98"
      )
    }

    "render page with text 'full year'" in {
      assertEqualsMessage(
        doc,
        ".govuk-grid-column-two-thirds>dl>dt:nth-child(50)>div>div.ni-notfull",
        "nisp.nirecord.fullyear"
      )
    }

    "render page with text 'you have contributions from '" in {
      assertEqualsMessage(
        doc,
        ".govuk-grid-column-two-thirds>dl>dd:nth-child(51)>div.contributions-wrapper>p:nth-child(1)",
        "nisp.nirecord.yourcontributionfrom"
      )
    }

    "render page with text 'National Insurance credits 52 weeks'" in {
      assertContainsDynamicMessage(
        doc,
        ".govuk-grid-column-two-thirds>dl>dd:nth-child(51)>div.contributions-wrapper>p:nth-child(2)",
        "nisp.nirecord.gap.whenyouareclaiming.plural",
        "52"
      )
    }

    "render page with text 'These may have been added to your record if you were ill/disabled...'" in {
      assertEqualsMessage(
        doc,
        ".govuk-grid-column-two-thirds>dl>dd:nth-child(51)>div.contributions-wrapper>p:nth-child(3)",
        "nisp.nirecord.gap.whenyouareclaiming.info.plural"
      )
    }

    "render page with link 'view gaps only'" in {
      assertEqualsMessage(
        doc,
        "[data-spec='nirecordpage__inset_text2__link']",
        "nisp.nirecord.showgaps"
      )
    }

    "render page with href link 'view gaps years'" in {
      assertLinkHasValue(
        doc,
        "[data-spec='nirecordpage__inset_text2__link']",
        "/check-your-state-pension/account/nirecord/gaps"
      )
    }

    "render page with link 'back'" in {
      assertEqualsMessage(
        doc,
        "[data-spec='nirecordpage__backlink_l']",
        "nisp.back"
      )
    }

    "render page with href link 'back'" in {
      assertLinkHasValue(
        doc,
        "[data-spec='nirecordpage__backlink_l']",
        "javascript:window.history.back();"
      )
    }

    "render page with print link" in {
      assertEqualsMessage(
        doc,
        "[data-spec='nirecordpage__printlink_l']",
        "nisp.print.this.ni.record"
      )
    }
  }

  "Render Ni Record view Gaps Only" should {

    lazy val doc = asDocument(contentAsString(controller.showGaps(generateFakeRequest)))

    /*Check side border :summary */

    "render page with qualifying years text '28 years of full contributions'" in {
      assertContainsDynamicMessage(
        doc,
        "[data-spec='nirecordpage__ul__qualifying_years__li1']",
        "nisp.nirecord.summary.fullContributions",
        "28"
      )
    }

    "render page with text '4 years to contribute before 5 April 2018 '" in {
      assertContainsDynamicMessage(
        doc,
        "[data-spec='nirecordpage__ul__qualifying_years__li2']",
        "nisp.nirecord.summary.yearsRemaining",
        "4",
        "2018"
      )
    }

    "render page with text '10 years when you did not contribute enough'" in {
      assertContainsDynamicMessage(
        doc,
        "[data-spec='nirecordpage__ul__qualifying_years__li3']",
        "nisp.nirecord.summary.gaps",
        "10"
      )
    }

    /*Ends here*/

    "render with correct page title" in {
      assertElementContainsText(
        doc,
        "head > title",
        messages("nisp.nirecord.heading")
          + Constants.titleSplitter
          + messages("nisp.title.extension")
          + Constants.titleSplitter
          + messages("nisp.gov-uk")
      )
    }

    "render page with Gaps  heading  Your National Insurance record " in {
      assertEqualsMessage(
        doc,
        "[data-spec='nirecordpage__pageheading'] [data-component='nisp_page_heading__h1']",
        "nisp.nirecord.heading"
      )
    }

    "render page with name " in {
      assertEqualsValue(
        doc,
        "[data-spec='nirecordpage__pageheading'] [data-aria-hidden]",
        "AHMED BRENNAN"
      )
    }

    "render page with national Insurance number " in {
      assertElementsOwnMessage(
        doc,
        ".govuk-details .govuk-details__summary-text",
        "nisp.show.nino"
      )
    }

    "render page with link 'View all years'" in {
      assertEqualsMessage(
        doc,
        "[data-spec='nirecordpage__inset_text3__link']",
        "nisp.nirecord.showfull"
      )
    }

    "render page with link href 'View all years'" in {
      assertLinkHasValue(
        doc,
        "[data-spec='nirecordpage__inset_text3__link']",
        "/check-your-state-pension/account/nirecord"
      )
    }

    "render page with text 'your record for this year is not available yet'" in {
      assertEqualsMessage(
        doc,
        ".govuk-grid-column-two-thirds>dl>dt:nth-child(1)>div.ni-wrapper>div.inactive",
        "nisp.nirecord.unavailableyear"
      )
    }

    "render page with text 'year is not full'" in {
      assertEqualsMessage(
        doc,
        ".govuk-grid-column-two-thirds>dl>dt:nth-child(2)>div>div.ni-notfull",
        "nisp.nirecord.gap"
      )
    }

    "render page with link 'View details'" in {
      assertContainsExpectedValue(
        doc,
        ".govuk-grid-column-two-thirds>dl>dt:nth-child(2)>div>div.ni-action>a.view-details",
        "nisp.nirecord.gap.viewdetails",
        "2013-14"
      )
    }

    "render page with text 'You did not make any contributions this year '" in {
      assertEqualsMessage(
        doc,
        ".govuk-grid-column-two-thirds>dl>dd:nth-child(3)>div.contributions-wrapper>p.contributions-header",
        "nisp.nirecord.youdidnotmakeanycontrib"
      )
    }

    "render page with text 'Find out more about gaps in your account'" in {
      assertContainsExpectedValue(
        doc,
        ".govuk-grid-column-two-thirds>dl>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(2)",
        "nisp.nirecord.gap.findoutmoreabout",
        "/check-your-state-pension/account/nirecord/gapsandhowtocheck"
      )
    }

    "render page with text 'You can make up the shortfall'" in {
      assertEqualsMessage(
        doc,
        ".govuk-grid-column-two-thirds>dl>dd:nth-child(3)>div.contributions-wrapper>p.contributions-header:nth-child(3)",
        "nisp.nirecord.gap.youcanmakeupshortfall"
      )
    }

    "render page with text 'Pay a voluntary contribution of figure out how to do it...'" in {
      assertContainsDynamicMessage(
        doc,
        ".govuk-grid-column-two-thirds>dl>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(4)",
        "nisp.nirecord.gap.payvoluntarycontrib",
        "&pound;704.60",
        langUtils.Dates.formatDate(LocalDate.of(2023, 4, 5)),
        langUtils.Dates.formatDate(LocalDate.of(2019, 4, 5))
      )
    }

    "render page with text 'Find out more about...'" in {
      assertContainsDynamicMessage(
        doc,
        ".govuk-grid-column-two-thirds>dl>dd:nth-child(3)>div.contributions-wrapper>p:nth-child(5)",
        "nisp.nirecord.gap.findoutmore",
        "/check-your-state-pension/account/nirecord/voluntarycontribs"
      )
    }

    "render page with text ' year is not full'" in {
      assertEqualsMessage(
        doc,
        ".govuk-grid-column-two-thirds>dl>dt:nth-child(10)>div>div.ni-notfull",
        "nisp.nirecord.gap"
      )
    }

    "render page with text 'You did not make any contributions this year for too late to pay '" in {
      assertEqualsMessage(
        doc,
        ".govuk-grid-column-two-thirds>dl>dd:nth-child(11)>div.contributions-wrapper>p.contributions-header",
        "nisp.nirecord.youdidnotmakeanycontrib"
      )
    }

    "render page with text 'Find out more about for too late to pay'" in {
      assertContainsExpectedValue(
        doc,
        ".govuk-grid-column-two-thirds>dl>dd:nth-child(11)>div.contributions-wrapper>p:nth-child(2)",
        "nisp.nirecord.gap.findoutmoreabout",
        "/check-your-state-pension/account/nirecord/gapsandhowtocheck"
      )
    }

    "render page with text 'It’s too late to pay for this year. You can usually only pay for the last 6 years.'" in {
      assertEqualsMessage(
        doc,
        ".govuk-grid-column-two-thirds>dl>dd:nth-child(11)>div.contributions-wrapper>p.panel-indent:nth-child(3)",
        "nisp.nirecord.gap.latePaymentMessage"
      )
    }

    "render page with link 'view all years'" in {
      assertEqualsMessage(
        doc,
        "[data-spec='nirecordpage__inset_text3__link']",
        "nisp.nirecord.showfull"
      )
    }

    "render page with href link 'view all years'" in {
      assertLinkHasValue(
        doc,
        "[data-spec='nirecordpage__inset_text3__link']",
        "/check-your-state-pension/account/nirecord"
      )
    }

    "render page with link 'back'" in {
      assertEqualsMessage(
        doc,
        "[data-spec='nirecordpage__backlink_l']",
        "nisp.back"
      )
    }

    "render page with href link 'back'" in {
      assertLinkHasValue(
        doc,
        "[data-spec='nirecordpage__backlink_l']",
        "javascript:window.history.back();"
      )
    }

    "render page with print link" in {
      assertEqualsMessage(
        doc,
        "[data-spec='nirecordpage__printlink_l']",
        "nisp.print.this.ni.record"
      )
    }
  }

  "Render Ni Record view With HRP Message" should {
    lazy val withHRPResult = inject[nirecordGapsAndHowToCheckThem]
    lazy val withHRPDoc    = asDocument(withHRPResult(true)(request = fakeRequest, messages = messages, user = user).toString)

    "render with correct page title" in {
      assertElementContainsText(
        withHRPDoc,
        "head > title",
        messages("nisp.nirecord.gapsinyourrecord.heading")
          + Constants.titleSplitter
          + messages("nisp.title.extension")
          + Constants.titleSplitter
          + messages("nisp.gov-uk")
      )
    }

    "render page with heading 'Gaps in your record and how to check them'" in {
      assertEqualsMessage(
        withHRPDoc,
        "[data-spec='ni_record_gaps_and_how_to_check_them__h1']",
        "nisp.nirecord.gapsinyourrecord.heading"
      )
    }

    "render page with text 'In most cases, you will have a gap in your record as you did not contribute enough National Insurance.'" in {
      assertEqualsMessage(
        withHRPDoc,
        "[data-spec='ni_record_gaps_and_how_to_check_them__p1']",
        "nisp.nirecord.gapsinyourrecord.title.message"
      )
    }

    "render page with text 'This could be because you were:.'" in {
      assertEqualsMessage(
        withHRPDoc,
        "[data-spec='ni_record_gaps_and_how_to_check_them__p2']",
        "nisp.nirecord.gapsinyourrecord.listheader"
      )
    }

    "render page with text 'in paid employment and had low earnings'" in {
      assertEqualsMessage(
        withHRPDoc,
        "[data-spec='ni_record_gaps_and_how_to_check_them__ul__li1']",
        "nisp.nirecord.gapsinyourrecord.line1"
      )
    }

    "render page with text 'unemployed and not claiming benefit'" in {
      assertEqualsMessage(
        withHRPDoc,
        "[data-spec='ni_record_gaps_and_how_to_check_them__ul__li2']",
        "nisp.nirecord.gapsinyourrecord.line2"
      )
    }

    "render page with text 'self-employed but did not pay contributions because of small profits'" in {
      assertEqualsMessage(
        withHRPDoc,
        "[data-spec='ni_record_gaps_and_how_to_check_them__ul__li3']",
        "nisp.nirecord.gapsinyourrecord.line3"
      )
    }

    "render page with text 'living abroad'" in {
      assertEqualsMessage(
        withHRPDoc,
        "[data-spec='ni_record_gaps_and_how_to_check_them__ul__li4']",
        "nisp.nirecord.gapsinyourrecord.line4"
      )
    }

    "render page with text 'living or working in the Isle of Man'" in {
      assertEqualsMessage(
        withHRPDoc,
        "[data-spec='ni_record_gaps_and_how_to_check_them__ul__li5']",
        "nisp.nirecord.gapsinyourrecord.line5"
      )
    }

    "render page with text 'How to check your record'" in {
      assertEqualsMessage(
        withHRPDoc,
        "[data-spec='ni_record_gaps_and_how_to_check_them__h2_1']",
        "nisp.nirecord.gapsinyourrecord.howtocheckrecord"
      )
    }

    "render page with text 'Paid employment'" in {
      assertEqualsMessage(
        withHRPDoc,
        "[data-spec='ni_record_gaps_and_how_to_check_them__h3_1']",
        "nisp.nirecord.gapsinyourrecord.paidemployment.title"
      )
    }

    "render page with text 'Check the contributions you made against P60s from your employers.'" in {
      assertEqualsMessage(
        withHRPDoc,
        "[data-spec='ni_record_gaps_and_how_to_check_them__p3']",
        "nisp.nirecord.gapsinyourrecord.paidemployment.desc"
      )
    }

    "render page with text 'If you do not have P60s'" in {
      assertEqualsMessage(
        withHRPDoc,
        "[data-spec='ni_record_gaps_and_how_to_check_them__details__do_not_have_p60'] .govuk-details__summary-text",
        "nisp.nirecord.gapsinyourrecord.donothavep60.title"
      )
    }

    "render page with text 'You can get a replacement P60 from your employer. Alternatively, you can find your National Insurance contributions on your payslips.'" in {
      assertEqualsMessage(
        withHRPDoc,
        "#main-content > div > div > details > div",
        "nisp.nirecord.gapsinyourrecord.donothavep60.desc"
      )
    }

    "render page with text 'Self-employment and voluntary contributions.'" in {
      assertEqualsMessage(
        withHRPDoc,
        "#main-content > div > div > h3:nth-child(9)",
        "nisp.nirecord.gapsinyourrecord.selfemployment.title"
      )
    }

    "render page with text 'Check the contributions you made against your personal accounts. For instance if you made payments by cheque or through your bank.'" in {
      assertEqualsMessage(
        withHRPDoc,
        "#main-content > div > div > p:nth-child(10)",
        "nisp.nirecord.gapsinyourrecord.selfemployment.desc"
      )
    }

    "render page with text 'If you have evidence that your record is wrong'" in {
      assertEqualsMessage(
        withHRPDoc,
        "#main-content > div > div > h2:nth-child(11)",
        "nisp.nirecord.gapsinyourrecord.ifyouhaveevidence.title"
      )
    }

    "render page with text 'You may be able to correct your record. Send copies of the necessary evidence with a covering letter to:'" in {
      assertEqualsMessage(
        withHRPDoc,
        "#main-content > div > div > p:nth-child(12)",
        "nisp.nirecord.gapsinyourrecord.ifyouhaveevidence.line1"
      )
    }

    "render page with text 'Individuals Caseworker National Insurance contributions and Employers Office HM Revenue and Customs BX9 1AN:'" in {
      assertContainsChildWithMessage(
        withHRPDoc,
        "#main-content > div > div > p:nth-child(13)",
        "nisp.nirecord.gapsinyourrecord.ifyouhaveevidence.addr1",
        "nisp.nirecord.gapsinyourrecord.ifyouhaveevidence.addr2",
        "nisp.nirecord.gapsinyourrecord.ifyouhaveevidence.addr3",
        "nisp.nirecord.gapsinyourrecord.ifyouhaveevidence.addr4"
      )
    }

    "render page with text 'National Insurance credits'" in {
      assertEqualsMessage(
        withHRPDoc,
        "#main-content > div > div > h2:nth-child(14)",
        "nisp.nirecord.gapsinyourrecord.nationalinsurancecredits.title"
      )
    }

    "render page with text 'If you were claiming benefits because you were unable to work, unemployed or caring for someone full time...'" in {
      assertEqualsMessage(
        withHRPDoc,
        "#main-content > div > div > p:nth-child(15)",
        "nisp.nirecord.gapsinyourrecord.nationalinsurancecredits.desc"
      )
    }

    "render page with text 'Home Responsibilities Protection (HRP) is only available for'" in {
      assertContainsDynamicMessage(
        withHRPDoc,
        "#main-content > div > div > div:nth-child(16)",
        "nisp.nirecord.gapsinyourrecord.nationalinsurancecredits.hrp",
        null
      )
    }

    "render page with link 'Home Responsibilities Protection (HRP) is only available foMore on National Insurance credits (opens in new tab)r'" in {
      assertEqualsMessage(
        withHRPDoc,
        "#main-content > div > div > p:nth-child(17) > a",
        "nisp.nirecord.gapsinyourrecord.nationalinsurancecredits.link"
      )
    }

    "render page with href link More on National Insurance credits (opens in new tab) " in {
      assertLinkHasValue(
        withHRPDoc,
        "#main-content > div > div > p:nth-child(17) > a",
        "https://www.gov.uk/national-insurance-credits/eligibility"
      )
    }

    "render page with  link back " in {
      assertEqualsMessage(
        withHRPDoc,
        "#main-content > div > div > p:nth-child(18) > a",
        "nisp.back"
      )
    }

    "render page with href link back " in {
      assertLinkHasValue(
        withHRPDoc,
        "#main-content > div > div > p:nth-child(18) > a",
        "/check-your-state-pension/account/nirecord/gaps"
      )
    }
  }

  "Render Ni Record without HRP Message" should {
    lazy val withoutHRPresult = inject[nirecordGapsAndHowToCheckThem]
    lazy val withoutHRPDoc    = asDocument(withoutHRPresult(false)(request = fakeRequest, messages = messages, user = user).toString)

    "render with correct page title" in {
      assertElementContainsText(
        withoutHRPDoc,
        "head > title",
        messages("nisp.nirecord.gapsinyourrecord.heading")
          + Constants.titleSplitter
          + messages("nisp.title.extension")
          + Constants.titleSplitter
          + messages("nisp.gov-uk")
      )
    }

    "render page with heading 'Gaps in your record and how to check them'" in {
      assertEqualsMessage(
        withoutHRPDoc,
        "[data-spec='ni_record_gaps_and_how_to_check_them__h1']",
        "nisp.nirecord.gapsinyourrecord.heading"
      )
    }

    "render page with text 'In most cases, you will have a gap in your record as you did not contribute enough National Insurance.'" in {
      assertEqualsMessage(
        withoutHRPDoc,
        "[data-spec='ni_record_gaps_and_how_to_check_them__p1']",
        "nisp.nirecord.gapsinyourrecord.title.message"
      )
    }

    "render page with text 'This could be because you were:.'" in {
      assertEqualsMessage(
        withoutHRPDoc,
        "[data-spec='ni_record_gaps_and_how_to_check_them__p2']",
        "nisp.nirecord.gapsinyourrecord.listheader"
      )
    }

    "render page with text 'in paid employment and had low earnings'" in {
      assertEqualsMessage(
        withoutHRPDoc,
        "#main-content > div > div > ul:nth-child(4) > li:nth-child(1)",
        "nisp.nirecord.gapsinyourrecord.line1"
      )
    }

    "render page with text 'unemployed and not claiming benefit'" in {
      assertEqualsMessage(
        withoutHRPDoc,
        "#main-content > div > div > ul:nth-child(4) > li:nth-child(2)",
        "nisp.nirecord.gapsinyourrecord.line2"
      )
    }

    "render page with text 'self-employed but did not pay contributions because of small profits'" in {
      assertEqualsMessage(
        withoutHRPDoc,
        "#main-content > div > div > ul:nth-child(4) > li:nth-child(3)",
        "nisp.nirecord.gapsinyourrecord.line3"
      )
    }

    "render page with text 'living abroad'" in {
      assertEqualsMessage(
        withoutHRPDoc,
        "#main-content > div > div > ul:nth-child(4) > li:nth-child(4)",
        "nisp.nirecord.gapsinyourrecord.line4"
      )
    }

    "render page with text 'living or working in the Isle of Man'" in {
      assertEqualsMessage(
        withoutHRPDoc,
        "#main-content > div > div > ul:nth-child(4) > li:nth-child(5)",
        "nisp.nirecord.gapsinyourrecord.line5"
      )
    }

    "render page with text 'How to check your record'" in {
      assertEqualsMessage(
        withoutHRPDoc,
        "[data-spec='ni_record_gaps_and_how_to_check_them__h2_1']",
        "nisp.nirecord.gapsinyourrecord.howtocheckrecord"
      )
    }

    "render page with text 'Paid employment'" in {
      assertEqualsMessage(
        withoutHRPDoc,
        "[data-spec='ni_record_gaps_and_how_to_check_them__h3_1']",
        "nisp.nirecord.gapsinyourrecord.paidemployment.title"
      )
    }

    "render page with text 'Check the contributions you made against P60s from your employers.'" in {
      assertEqualsMessage(
        withoutHRPDoc,
        "[data-spec='ni_record_gaps_and_how_to_check_them__p3']",
        "nisp.nirecord.gapsinyourrecord.paidemployment.desc"
      )
    }

    "render page with text 'If you do not have P60s'" in {
      assertEqualsMessage(
        withoutHRPDoc,
        "[data-spec='ni_record_gaps_and_how_to_check_them__details__do_not_have_p60'] .govuk-details__summary-text",
        "nisp.nirecord.gapsinyourrecord.donothavep60.title"
      )
    }

    "render page with text 'You can get a replacement P60 from your employer. Alternatively, you can find your National Insurance contributions on your payslips.'" in {
      assertEqualsMessage(
        withoutHRPDoc,
        "[data-spec='ni_record_gaps_and_how_to_check_them__details__do_not_have_p60'] .govuk-details__text",
        "nisp.nirecord.gapsinyourrecord.donothavep60.desc"
      )
    }

    "render page with text 'Self-employment and voluntary contributions.'" in {
      assertEqualsMessage(
        withoutHRPDoc,
        "[data-spec='ni_record_gaps_and_how_to_check_them__h3_2']",
        "nisp.nirecord.gapsinyourrecord.selfemployment.title"
      )
    }

    "render page with text 'Check the contributions you made against your personal accounts. For instance if you made payments by cheque or through your bank.'" in {
      assertEqualsMessage(
        withoutHRPDoc,
        "[data-spec='ni_record_gaps_and_how_to_check_them__p4']",
        "nisp.nirecord.gapsinyourrecord.selfemployment.desc"
      )
    }

    "render page with text 'If you have evidence that your record is wrong'" in {
      assertEqualsMessage(
        withoutHRPDoc,
        "[data-spec='ni_record_gaps_and_how_to_check_them__h2_2']",
        "nisp.nirecord.gapsinyourrecord.ifyouhaveevidence.title"
      )
    }

    "render page with text 'You may be able to correct your record. Send copies of the necessary evidence with a covering letter to:'" in {
      assertEqualsMessage(
        withoutHRPDoc,
        "[data-spec='ni_record_gaps_and_how_to_check_them__p5']",
        "nisp.nirecord.gapsinyourrecord.ifyouhaveevidence.line1"
      )
    }

    "render page with text 'Individuals Caseworker National Insurance contributions and Employers Office HM Revenue and Customs BX9 1AN:'" in {
      assertContainsChildWithMessage(
        withoutHRPDoc,
        "[data-spec='ni_record_gaps_and_how_to_check_them__p6']",
        "nisp.nirecord.gapsinyourrecord.ifyouhaveevidence.addr1",
        "nisp.nirecord.gapsinyourrecord.ifyouhaveevidence.addr2",
        "nisp.nirecord.gapsinyourrecord.ifyouhaveevidence.addr3",
        "nisp.nirecord.gapsinyourrecord.ifyouhaveevidence.addr4"
      )
    }

    "render page with text 'National Insurance credits'" in {
      assertEqualsMessage(
        withoutHRPDoc,
        "[data-spec='ni_record_gaps_and_how_to_check_them__h2_3']",
        "nisp.nirecord.gapsinyourrecord.nationalinsurancecredits.title"
      )
    }

    "render page with text 'If you were claiming benefits because you were unable to work, unemployed or caring for someone full time...'" in {
      assertEqualsMessage(
        withoutHRPDoc,
        "[data-spec='ni_record_gaps_and_how_to_check_them__p7']",
        "nisp.nirecord.gapsinyourrecord.nationalinsurancecredits.desc"
      )
    }

    "render page with link 'More on National Insurance credits (opens in new tab)'" in {
      assertEqualsMessage(
        withoutHRPDoc,
        "[data-spec='ni_record_gaps_and_how_to_check_them__link1']",
        "nisp.nirecord.gapsinyourrecord.nationalinsurancecredits.link"
      )
    }

    "render page with href link More on National Insurance credits (opens in new tab) " in {
      assertLinkHasValue(
        withoutHRPDoc,
        "[data-spec='ni_record_gaps_and_how_to_check_them__link1']",
        "https://www.gov.uk/national-insurance-credits/eligibility"
      )
    }

    "render page with  link back " in {
      assertEqualsMessage(
        withoutHRPDoc,
        "[data-spec='ni_record_gaps_and_how_to_check_them__link2']",
        "nisp.back"
      )
    }

    "render page with href link back " in {
      assertLinkHasValue(
        withoutHRPDoc,
        "[data-spec='ni_record_gaps_and_how_to_check_them__link2']",
        "/check-your-state-pension/account/nirecord/gaps"
      )
    }
  }

  "Render Ni Record without gap and has pre75years" should {
    lazy val doc = asDocument(contentAsString(controller.showFull(generateFakeRequest)))

    def mockSetup = {
      when(mockDateProvider.currentDate).thenReturn(LocalDate.of(2016, 9, 9))
      when(mockAppConfig.showFullNI).thenReturn(true)

      when(mockNationalInsuranceService.getSummary(mockAny())(mockAny()))
        .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
          qualifyingYears = 1,
          qualifyingYearsPriorTo1975 = 5,
          numberOfGaps = 0,
          numberOfGapsPayable = 0,
          Some(LocalDate.of(1954, 3, 6)),
          false,
          LocalDate.of(2016, 4, 5),
          List(
            NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
            NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
            NationalInsuranceTaxYearBuilder("2013-14", qualifying = false, payable = true, underInvestigation = false)
          ),
          reducedRateElection = false
        )
        ))))

      when(mockStatePensionService.getSummary(mockAny())(mockAny()))
        .thenReturn(Future.successful(Right(Left(StatePensionExclusionFiltered(
          Exclusion.AmountDissonance,
          Some(66),
          Some(LocalDate.of(2020, 3, 6))
        )
        ))))
    }

    /*Check side border :summary */

    "render page with text '1 year of full contribution'" in {
      mockSetup
      assertContainsDynamicMessage(
        doc,
        "[data-spec='nirecordpage__ul__qualifying_years__li1']",
        "nisp.nirecord.summary.fullContributions.single",
        "1"
      )
    }

    "render with correct page title" in {
      mockSetup
      assertElementContainsText(
        doc,
        "head > title",
        messages("nisp.nirecord.title")
          + Constants.titleSplitter
          + messages("nisp.title.extension")
          + Constants.titleSplitter
          + messages("nisp.gov-uk")
      )
    }

    "render page with qualifying years text '1 year of full contribution'" in {
      mockSetup
      assertContainsDynamicMessage(
        doc,
        "[data-spec='nirecordpage__ul__qualifying_years__li1']",
        "nisp.nirecord.summary.fullContributions.single",
        "1"
      )
    }

    "render page with text 'you do not have any gaps in your record.'" in {
      mockSetup
      assertEqualsMessage(
        doc,
        "[data-spec='nirecordpage__inset_text1']",
        "nisp.nirecord.youdonothaveanygaps"
      )
    }

    "render page with text 'pre75 years'" in {
      mockSetup
      assertEqualsMessage(
        doc,
        ".govuk-grid-column-two-thirds>dl.accordion>dt:nth-child(10)>div>div.ni-years",
        "nisp.nirecord.pre75Years"
      )
    }

    "render page with text 'you have 5  qualifying years pre 1975'" in {
      mockSetup
      assertContainsDynamicMessage(
        doc,
        ".govuk-grid-column-two-thirds>dl.accordion>dt:nth-child(10)>div>div.ni-full",
        "nisp.nirecord.pre75QualifyingYears",
        "5"
      )
    }

    "render page with link 'back'" in {
      mockSetup
      assertEqualsMessage(
        doc,
        "[data-spec='nirecordpage__backlink_l']",
        "nisp.back"
      )
    }

    "render page with href link 'back'" in {
      mockSetup
      assertLinkHasValue(
        doc,
        "[data-spec='nirecordpage__backlink_l']",
        "javascript:window.history.back();"
      )
    }

    "render page with print link" in {
      mockSetup
      assertEqualsMessage(
        doc,
        "[data-spec='nirecordpage__printlink_l']",
        "nisp.print.this.ni.record"
      )
    }
  }

  "Render Ni Record without gap and has gaps pre75years" should {
    lazy val doc = asDocument(contentAsString(controller.showFull(generateFakeRequest)))

    def mockSetup = {
      when(mockDateProvider.currentDate).thenReturn(LocalDate.of(2016, 9, 9))
      when(mockAppConfig.showFullNI).thenReturn(true)

      when(mockNationalInsuranceService.getSummary(mockAny())(mockAny()))
        .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
          qualifyingYears = 2,
          qualifyingYearsPriorTo1975 = 0,
          numberOfGaps = 1,
          numberOfGapsPayable = 1,
          Some(LocalDate.of(1954, 3, 6)),
          false,
          LocalDate.of(2015, 4, 5),
          List(
            NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
            NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
            NationalInsuranceTaxYearBuilder("2013-14", qualifying = false, payable = true, underInvestigation = false)
          ),
          reducedRateElection = false
        )
        ))))

      when(mockStatePensionService.getSummary(mockAny())(mockAny()))
        .thenReturn(Future.successful(Right(Left(StatePensionExclusionFiltered(
          Exclusion.AmountDissonance,
          Some(66),
          Some(LocalDate.of(2020, 3, 6))
        )
        ))))
    }

    /*Check side border :summary */
    "render page with text  you have" in {
      mockSetup
      assertEqualsMessage(
        doc,
        "[data-spec='nirecordpage__p1']",
        "nisp.nirecord.summary.youhave"
      )
    }

    "render page with qualifying years text '2 year of full contributions'" in {
      mockSetup
      assertContainsDynamicMessage(
        doc,
        "ul.govuk-list li:nth-child(1)",
        "nisp.nirecord.summary.fullContributions",
        "2"
      )
    }

    "render page with text 1 years when you did not contribute enough" in {
      mockSetup
      assertContainsDynamicMessage(
        doc,
        "ul.govuk-list li:nth-child(2)",
        "nisp.nirecord.summary.gaps.single",
        "1"
      )
    }

    "render with correct page title" in {
      mockSetup
      assertElementContainsText(
        doc,
        "head > title",
        messages("nisp.nirecord.title")
        + Constants.titleSplitter
        + messages("nisp.title.extension")
        + Constants.titleSplitter
        + messages("nisp.gov-uk")
      )
    }

    "render page with qualifying years text '2 years of full contributions'" in {
      mockSetup
      assertContainsDynamicMessage(
        doc,
        "ul.govuk-list li:nth-child(1)",
        "nisp.nirecord.summary.fullContributions",
        "2"
      )
    }

    "render page with text '1 year you did not contribute enough" in {
      mockSetup
      assertContainsDynamicMessage(
        doc,
        "ul.govuk-list li:nth-child(2)",
        "nisp.nirecord.summary.gaps.single",
        "1"
      )
    }

    "render page with text 'pre75 years'" in {
      mockSetup
      assertEqualsMessage(
        doc,
        ".govuk-grid-column-two-thirds>dl.accordion>dt:nth-child(10)>div>div.ni-years",
        "nisp.nirecord.pre75Years"
      )
    }

    "render page with text 'Our records show you do not have any full years up to 5 April 1975'" in {
      mockSetup
      assertEqualsMessage(
        doc,
        ".govuk-grid-column-two-thirds>dl.accordion>dt:nth-child(10)>div>div.ni-full",
        "nisp.nirecord.pre75QualifyingYearsZero"
      )
    }

    "render page with link 'back'" in {
      mockSetup
      assertEqualsMessage(
        doc,
        "[data-spec='nirecordpage__backlink_l']",
        "nisp.back"
      )
    }

    "render page with href link 'back'" in {
      mockSetup
      assertLinkHasValue(
        doc,
        "[data-spec='nirecordpage__backlink_l']",
        "javascript:window.history.back();"
      )
    }

    "render page with print link" in {
      mockSetup
      assertEqualsMessage(
        doc,
        "[data-spec='nirecordpage__printlink_l']",
        "nisp.print.this.ni.record"
      )
    }
  }

  "Render Ni Record without gap and has gaps pre75years with Years to contribute " should {
    lazy val doc = asDocument(contentAsString(controller.showFull(generateFakeRequest)))

    def mockSetup = {
      when(mockDateProvider.currentDate).thenReturn(LocalDate.of(2016, 9, 9))
      when(mockAppConfig.showFullNI).thenReturn(true)

      when(mockNationalInsuranceService.getSummary(mockAny())(mockAny()))
        .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
          qualifyingYears = 2,
          qualifyingYearsPriorTo1975 = 0,
          numberOfGaps = 1,
          numberOfGapsPayable = 1,
          Some(LocalDate.of(1954, 3, 6)),
          false,
          LocalDate.of(2017, 4, 5),
          List(
            NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = true),
            NationalInsuranceTaxYearBuilder("2014-15", qualifying = true, underInvestigation = false),
            NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
          ),
          reducedRateElection = false
        )
        ))))

      when(mockStatePensionService.yearsToContributeUntilPensionAge(mockAny(), mockAny()))
        .thenReturn(1)

      when(mockStatePensionService.getSummary(mockAny())(mockAny()))
        .thenReturn(Future.successful(Right(Left(StatePensionExclusionFiltered(
          Exclusion.AmountDissonance,
          Some(66),
          Some(LocalDate.of(2018, 3, 6))
        )
        ))))
    }

    /*Check side border :summary */

    "render page with text  you have" in {
      mockSetup
      assertEqualsMessage(
        doc,
        "[data-spec='nirecordpage__p1']",
        "nisp.nirecord.summary.youhave"
      )
    }

    "render page with qualifying years text '2 year of full contributions'" in {
      mockSetup
      assertContainsDynamicMessage(
        doc,
        "ul.govuk-list li:nth-child(1)",
        "nisp.nirecord.summary.fullContributions",
        "2"
      )
    }

    "render page with text  1 year to contribute before 5 April 2017'" in {
      mockSetup
      assertContainsDynamicMessage(
        doc,
        "ul.govuk-list li:nth-child(2)",
        "nisp.nirecord.summary.yearsRemaining.single",
        "1",
        "2017"
      )
    }

    "render page with text 1 years when you did not contribute enough" in {
      mockSetup
      assertContainsDynamicMessage(
        doc,
        "ul.govuk-list li:nth-child(3)",
        "nisp.nirecord.summary.gaps.single",
        "1"
      )
    }

    /* Under investigation*/

    "render page with text 'full year for under investigation'" in {
      mockSetup
      assertEqualsMessage(
        doc,
        ".govuk-grid-column-two-thirds>dl>dt:nth-child(3)>div>div.ni-notfull",
        "nisp.nirecord.fullyear"
      )
    }

    "render page with text 'we are checking this year to see if it counts towards your pension. We will update your records '" in {
      mockSetup
      assertEqualsMessage(
        doc,
        ".govuk-grid-column-two-thirds>dl>dd:nth-child(4)>div.contributions-wrapper>p:nth-child(1)",
        "nisp.nirecord.gap.underInvestigation"
      )
    }

    /*Ends here*/

    "render page with text 'full year'" in {
      mockSetup
      assertEqualsMessage(
        doc,
        ".govuk-grid-column-two-thirds>dl>dt:nth-child(5)>div>div.ni-notfull",
        "nisp.nirecord.fullyear"
      )
    }

    "render page with text 'you have contributions from '" in {
      mockSetup
      assertEqualsMessage(
        doc,
        ".govuk-grid-column-two-thirds>dl>dd:nth-child(6)>div.contributions-wrapper>p:nth-child(1)",
        "nisp.nirecord.yourcontributionfrom"
      )
    }

    "render page with text 'paid employment : £12,345.67'" in {
      mockSetup
      assertContainsDynamicMessageUsingClass(
        doc,
        "payment-message",
        "nisp.nirecord.gap.paidemployment",
        "£12,345.67"
      )
    }

    "render page with text 'self- employment : 10 weeks'" in {
      mockSetup
      assertContainsDynamicMessageUsingClass(
        doc,
        "national-insurance-gap-self-employed",
        "nisp.nirecord.gap.selfemployed.plural",
        "10"
      )
    }

    "render page with text 'Voluntary: : 8 weeks'" in {
      mockSetup
      assertContainsDynamicMessageUsingClass(
        doc,
        "national-insurance-voluntary-plural",
        "nisp.nirecord.gap.voluntary.plural",
        "8"
      )
    }

    "render page with text 'National Insurance credits: : 12 weeks'" in {
      mockSetup
      assertContainsDynamicMessageUsingClass(
        doc,
        "other-credits",
        "nisp.nirecord.gap.whenyouareclaiming.plural",
        "12"
      )
    }

    "render page with text 'These may have been added to your record if you were ill/disabled, unemployed, caring for someone full-time or on jury service.'" in {
      mockSetup
      assertContainsDynamicMessageUsingClass(
        doc,
        "other-credits-reason",
        "nisp.nirecord.gap.whenyouareclaiming.info.plural"
      )
    }

    "render page with link 'back'" in {
      mockSetup
      assertEqualsMessage(
        doc,
        "[data-spec='nirecordpage__backlink_l']",
        "nisp.back"
      )
    }

    "render page with href link 'back'" in {
      mockSetup
      assertLinkHasValue(
        doc,
        "[data-spec='nirecordpage__backlink_l']",
        "javascript:window.history.back();"
      )
    }

    "render page with print link" in {
      mockSetup
      assertEqualsMessage(
        doc,
        "[data-spec='nirecordpage__printlink_l']",
        "nisp.print.this.ni.record"
      )
    }
  }

  "Render Ni Record with Single weeks in self, contribution and paid - and an abroad User" should {

    val abroadUserInjector = GuiceApplicationBuilder()
      .overrides(
        bind[StatePensionService].toInstance(mockStatePensionService),
        bind[NationalInsuranceService].toInstance(mockNationalInsuranceService),
        bind[AuditConnector].toInstance(mockAuditConnector),
        bind[ApplicationConfig].toInstance(mockAppConfig),
        bind[PertaxHelper].toInstance(mockPertaxHelper),
        bind[AuthAction].to[FakeAuthActionWithNino],
        bind[NinoContainer].toInstance(AbroadNinoContainer),
        bind[DateProvider].toInstance(mockDateProvider)
      )
      .build()
      .injector

    val abroadUserController = abroadUserInjector.instanceOf[NIRecordController]

    lazy val doc           = asDocument(contentAsString(controller.showFull(generateFakeRequest)))
    lazy val abroadUserDoc = asDocument(contentAsString(abroadUserController.showFull(generateFakeRequest)))

    def mockSetup = {
      when(mockNationalInsuranceService.getSummary(mockAny())(mockAny()))
        .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
          qualifyingYears = 2,
          qualifyingYearsPriorTo1975 = 0,
          numberOfGaps = 1,
          numberOfGapsPayable = 1,
          Some(LocalDate.of(1954, 3, 6)),
          false,
          LocalDate.of(2017, 4, 5),
          List(
            NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = true),
            NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
            NationalInsuranceTaxYearBuilder("2013-14", qualifying = false, underInvestigation = false) /*payable = true*/
          ),
          reducedRateElection = false
        )
        ))))

      when(mockStatePensionService.yearsToContributeUntilPensionAge(mockAny(), mockAny()))
        .thenReturn(1)

      when(mockStatePensionService.getSummary(mockAny())(mockAny()))
        .thenReturn(Future.successful(Right(Left(StatePensionExclusionFiltered(
          Exclusion.AmountDissonance,
          Some(66),
          Some(LocalDate.of(2018, 3, 6))
        )
        ))))
    }

    "render with correct page title" in {
      mockSetup
      assertElementContainsText(
        abroadUserDoc,
        "head > title",
        messages("nisp.nirecord.heading.uk")
          + Constants.titleSplitter
          + messages("nisp.title.extension")
          + Constants.titleSplitter
          + messages("nisp.gov-uk")
      )
    }

    "render page with heading your UK National Insurance Record " in {
      mockSetup
      assertEqualsMessage(
        abroadUserDoc,
        "[data-spec='nirecordpage__pageheading'] [data-component='nisp_page_heading__h1']",
        "nisp.nirecord.heading.uk"
      )
    }

    /*Check side border :summary */

    "render page with text  you have" in {
      mockSetup
      assertEqualsMessage(
        doc,
        "[data-spec='nirecordpage__p1']",
        "nisp.nirecord.summary.youhave"
      )
    }

    "render page with qualifying years text '2 year of full contributions'" in {
      mockSetup
      assertContainsDynamicMessage(
        doc,
        "ul.govuk-list li:nth-child(1)",
        "nisp.nirecord.summary.fullContributions",
        "2"
      )
    }

    "render page with text  1 year to contribute before 5 April 2017'" in {
      mockSetup
      assertContainsDynamicMessage(
        doc,
        "ul.govuk-list li:nth-child(2)",
        "nisp.nirecord.summary.yearsRemaining.single",
        "1",
        "2017"
      )
    }

    "render page with text 1 years when you did not contribute enough" in {
      mockSetup
      assertContainsDynamicMessage(
        doc,
        "ul.govuk-list li:nth-child(3)",
        "nisp.nirecord.summary.gaps.single",
        "1"
      )
    }

    "render page with text 'year full year'" in {
      mockSetup
      assertEqualsMessage(
        doc,
        ".govuk-grid-column-two-thirds>dl>dt:nth-child(5)>div>div.ni-notfull",
        "nisp.nirecord.gap"
      )
    }

    "render page with text 'you have contributions from '" in {
      mockSetup
      assertEqualsMessage(
        doc,
        ".govuk-grid-column-two-thirds>dl>dd:nth-child(6)>div.contributions-wrapper>p:nth-child(1)",
        "nisp.nirecord.yourcontributionfrom"
      )
    }

    "render page with text 'self-employment : 1 week'" in {
      mockSetup
      assertContainsDynamicMessageUsingClass(
        doc,
        "national-insurance-gap-self-employed",
        "nisp.nirecord.gap.selfemployed.singular",
        "1"
      )
    }

    "render page with text 'Voluntary: : 1 week'" in {
      mockSetup
      assertContainsDynamicMessageUsingClass(
        doc,
        "national-insurance-voluntary",
        "nisp.nirecord.gap.voluntary.singular",
        "1"
      )
    }

    "render page with text 'National Insurance credits: : 1 week'" in {
      mockSetup
      assertContainsDynamicMessageUsingClass(
        doc,
        "other-credits",
        "nisp.nirecord.gap.whenyouareclaiming.singular",
        "1"
      )
    }

    "render page with text 'These may have been added to your record if you were ill/disabled, unemployed, caring for someone full-time or on jury service.'" in {
      mockSetup
      assertContainsDynamicMessageUsingClass(
        doc,
        "other-reasons",
        "nisp.nirecord.gap.whenyouareclaiming.info.singular"
      )
    }

    "render page with print link" in {
      mockSetup
      assertEqualsMessage(
        doc,
        "[data-spec='nirecordpage__printlink_l']",
        "nisp.print.this.ni.record"
      )
    }
  }
}
