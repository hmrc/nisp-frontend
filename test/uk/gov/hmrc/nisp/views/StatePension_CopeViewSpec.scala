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

import org.apache.commons.lang3.StringEscapeUtils
import org.joda.time.{DateTime, LocalDate}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.Application
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.contentAsString
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.{LoginTimes, Name}
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.builders.NationalInsuranceTaxYearBuilder
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.StatePensionController
import uk.gov.hmrc.nisp.controllers.auth.{AuthAction, AuthDetails, AuthenticatedRequest, NispAuthedUser}
import uk.gov.hmrc.nisp.controllers.pertax.PertaxHelper
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.services.{MetricsService, NationalInsuranceService, StatePensionService}
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.nisp.utils.LanguageHelper.langUtils.Dates
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}
import uk.gov.hmrc.renderer.TemplateRenderer

import scala.concurrent.Future

class StatePension_CopeViewSpec extends HtmlSpec with MockitoSugar with
  ScalaFutures with Injecting {

  val mockUserNino = TestAccountBuilder.regularNino
  val mockUserNinoExcluded = TestAccountBuilder.excludedAll
  val mockUserNinoNotFound = TestAccountBuilder.blankNino

  implicit val user: NispAuthedUser = NispAuthedUser(mockUserNino, LocalDate.now(), UserName(Name(None, None)), None, None, false)
  val authDetails = AuthDetails(ConfidenceLevel.L200, None, LoginTimes(DateTime.now(), None))

  implicit val fakeRequest = AuthenticatedRequest(FakeRequest(), user, authDetails)

  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val mockNationalInsuranceService: NationalInsuranceService = mock[NationalInsuranceService]
  val mockStatePensionService: StatePensionService = mock[StatePensionService]
  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  val mockPertaxHelper: PertaxHelper = mock[PertaxHelper]
  val mockMetricsService: MetricsService = mock[MetricsService]
  val mockSessionCache: SessionCache = mock[SessionCache]

  implicit val cachedRetriever: CachedStaticHtmlPartialRetriever = FakeCachedStaticHtmlPartialRetriever
  implicit val formPartialRetriever: FormPartialRetriever = FakePartialRetriever
  implicit val templateRenderer: TemplateRenderer = FakeTemplateRenderer

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockStatePensionService, mockNationalInsuranceService, mockAuditConnector, mockAppConfig, mockPertaxHelper)
    when(mockPertaxHelper.isFromPertax(ArgumentMatchers.any())).thenReturn(Future.successful(false))
  }

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[AuthAction].to[FakeAuthAction],
      bind[StatePensionService].toInstance(mockStatePensionService),
      bind[NationalInsuranceService].toInstance(mockNationalInsuranceService),
      bind[AuditConnector].toInstance(mockAuditConnector),
      bind[ApplicationConfig].toInstance(mockAppConfig),
      bind[PertaxHelper].toInstance(mockPertaxHelper),
      bind[CachedStaticHtmlPartialRetriever].toInstance(cachedRetriever),
      bind[FormPartialRetriever].toInstance(formPartialRetriever),
      bind[TemplateRenderer].toInstance(templateRenderer)
    ).build()

  val statePensionController = inject[StatePensionController]

  "Render State Pension view with Contracted out User" should {

    def mockSetup = {

      when(mockStatePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Right(StatePension(
          new LocalDate(2014, 4, 5),
          StatePensionAmounts(
            false,
            StatePensionAmountRegular(46.38, 201.67, 2420.04),
            StatePensionAmountForecast(3, 155.55, 622.35, 76022.24),
            StatePensionAmountMaximum(3, 0, 155.55, 622.35, 76022.24),
            StatePensionAmountRegular(50, 217.41, 2608.93))
          ,64, new LocalDate(2021, 7, 18), "2017-18", 30, false, 155.65, false, false)
        )))

      when(mockNationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Right(NationalInsuranceRecord(
          qualifyingYears = 11,
          qualifyingYearsPriorTo1975 = 0,
          numberOfGaps = 2,
          numberOfGapsPayable = 2,
          Some(new LocalDate(1954, 3, 6)),
          false,
          new LocalDate(2017, 4, 5),
          List(

            NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
            NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
            NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false)
          ),
          reducedRateElection = false
        )
        )))

    }

    lazy val result = statePensionController.show()(AuthenticatedRequest(FakeRequest().withCookies(lanCookie), user, authDetails))
    lazy val htmlAccountDoc = asDocument(contentAsString(result))

    "render with correct page title" in {
      mockSetup
      assertElementContainsText(htmlAccountDoc, "head>title" ,messages("nisp.main.h1.title") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
    }
    "render page with heading  'Your State Pension' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h1.titleWithPageInfo", "nisp.main.h1.title")
    }

    "render page with text  'You can get your State Pension on' " in {
      assertElemetsOwnMessage(htmlAccountDoc, "article.content__body>div:nth-child(4)>p", "nisp.main.basedOn")
    }
    "render page with text  '18 july 2012' " in {
      assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(4)>p:nth-child(1)>span:nth-child(1)", Dates.formatDate(new LocalDate(2021, 7, 18)) + ".")
    }
    "render page with text  'Your forecast is' " in {
      val sMessage = Messages("nisp.main.caveats") + " " + Messages("nisp.is")
      assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(4)>p:nth-child(1)>span:nth-child(2)", sMessage)
    }

    "render page with text  '£155.55 a week" in {
      val sWeek = "£155.55 " + Messages("nisp.main.week")
      assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(4)>p:nth-child(2)>em", sWeek)
    }
    "render page with text  ' £622.35 a month, £76,022.24 a year '" in {
      val sForecastAmount = "£622.35 " + Messages("nisp.main.month") + ", £76,022.24 " + Messages("nisp.main.year")
      assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(4)>p:nth-child(3)", sForecastAmount)
    }
    "render page with text  ' Your forcaste '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(5)", "nisp.main.caveats")
    }
    "render page with text  ' is not a guarantee and is based on the current law '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>ul:nth-child(6)>li:nth-child(1)", "nisp.main.notAGuarantee")
    }
    "render page with text  ' does not include any increase due to inflation '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>ul:nth-child(6)>li:nth-child(2)", "nisp.main.inflation")
    }
    "render page with Heading  'You need to continue to contribute National Insurance to reach your forecast'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(7)", "nisp.main.continueContribute")
    }
    "render page with Heading  'Estimate based on your National Insurance record up to 5 April 2014'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>div:nth-child(8)>span:nth-child(1)", "nisp.main.chart.lastprocessed.title", "2014")
    }
    "render page with Heading  '£46.38 a week'" in {
      val sWeek = "£46.38 " + Messages("nisp.main.week")
      assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(8)>div>div>span>span", sWeek)
    }
    "render page with Heading '£155.55 is the most you can get'" in {
      val sMaxCanGet = "£155.55 " + StringEscapeUtils.unescapeHtml4(Messages("nisp.main.mostYouCanGet"))
      assertEqualsValue(htmlAccountDoc, "article.content__body>h2:nth-child(10)", sMaxCanGet)
    }
    "render page with text  'You cannot improve your forecast any further, unless you choose to put off claiming.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(11)", "nisp.main.context.willReach")
    }
    "render page with text 'If you’re working you may still need to pay National Insurance contributions until 18 " +
      "July 2021 as they fund other state benefits and the NHS.'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(12)", "nisp.main.context.reachMax.needToPay", Dates.formatDate(new LocalDate(2021, 7, 18)))
    }
    "render page with link  'View your National Insurence Record'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(13)", "nisp.main.showyourrecord")
    }
    "render page with href link  'View your National Insurence Record'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>a:nth-child(13)", "/check-your-state-pension/account/nirecord")
    }

    /*Contracting out affects*/
    "render page with text  'You’ve been in a contracted-out pension scheme'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(15)", "nisp.cope.title1")
    }
    "render page with text  'Like most people, you were contracted out of part of the State Pension.'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(16)", "nisp.cope.likeMostPeople", "/check-your-state-pension/account/cope")
    }
    /*Ends*/

    "render page with heading  'Putting of claiming'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(17)", "nisp.main.puttingOff")
    }

    "render page with text  'You can put off claiming your State Pension from 18 July 2021. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(18)", "nisp.main.puttingOff.line1", Dates.formatDate(new LocalDate(2021, 7, 18)))
    }

    "render page with link 'More on putting off claiming (opens in new tab)'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(19)", "nisp.main.puttingOff.linkTitle")
    }
    "render page with href link 'More on putting off claiming (opens in new tab)'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>a:nth-child(19)", "https://www.gov.uk/deferring-state-pension")
    }

    /*Side bar help*/
    "render page with heading  'Get help'" in {
      assertEqualsMessage(htmlAccountDoc, "aside.sidebar >div.helpline-sidebar>h2", "nisp.nirecord.helpline.getHelp")
    }
    "render page with text 'Helpline 0800 731 0181'" in {
      assertEqualsMessage(htmlAccountDoc, "aside.sidebar >div.helpline-sidebar>p:nth-child(2)", "nisp.nirecord.helpline.number")
    }
    "render page with text 'Textphone 0800 731 0176'" in {
      assertEqualsMessage(htmlAccountDoc, "aside.sidebar >div.helpline-sidebar>p:nth-child(3)", "nisp.nirecord.helpline.textNumber")
    }
    "render page with text 'Monday to Friday: 8am to 6pm'" in {
      assertEqualsMessage(htmlAccountDoc, "aside.sidebar >div.helpline-sidebar>p:nth-child(4)", "nisp.nirecord.helpline.openTimes")
    }
  }

  "Render Contracted Out View" should {

    lazy val sResult = html.statepension_cope(99.54, true)
    lazy val htmlAccountDoc = asDocument(contentAsString(sResult))

    "render with correct page title" in {
      assertElementContainsText(htmlAccountDoc, "head>title" ,messages("nisp.cope.youWereContractedOut") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
    }
    "render page with heading you were contracted out " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h1.heading-large", "nisp.cope.youWereContractedOut")
    }
    "render page with text 'In the past you’ve been part of one or more contracted out pension schemes, such as workplace or personal pension schemes.' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(2)", "nisp.cope.inThePast")
    }
    "render page with text 'when you were contracted out:'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(3)", "nisp.cope.why")
    }
    "render page with text 'you and your employers paid lower rate National Insurance contributions, or'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>ul:nth-child(4)>li:nth-child(1)", "nisp.cope.why.bullet1")
    }
    "render page with text 'some of your National Insurance contributions were paid into another pension scheme, such as a personal or stakeholder pension'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>ul:nth-child(4)>li:nth-child(2)", "nisp.cope.why.bullet2")
    }
    "render page with text 'The amount of additional State Pension you would have been paid if you had not been contracted out is known as the Contracted Out Pension Equivalent (COPE).'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(5)", "nisp.cope.copeequivalent")
    }
    "render page with text 'Contracted Out Pension Equivalent (COPE)'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(6)", "nisp.cope.title2")
    }
    "render page with test 'your cope estimate is'" in {
      assertElemetsOwnMessage(htmlAccountDoc, "article.content__body>p:nth-child(7)", "nisp.cope.table.estimate.title", ".")
    }
    "render page with test 'your cope estimate is : £99.54 a week'" in {
      val sWeekMessage = "£99.54 " + Messages("nisp.main.chart.week")
      assertEqualsValue(htmlAccountDoc, "article.content__body>p:nth-child(7)>span", sWeekMessage)
    }
    "render page with text 'This will not affect your State Pension forecast. The COPE amount is paid as part of your other pension schemes, not by the government.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(8)", "nisp.cope.definition")
    }
    "render page with text 'In most cases the private pension scheme you were contracted out to:'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(9)", "nisp.cope.definition.mostcases")
    }
    "render page with text 'will include an amount equal to the COPE amount'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>ul:nth-child(10)>li:nth-child(1)", "nisp.cope.definition.mostcases.bullet1")
    }
    "render page with text 'may not individually identify the COPE amount'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>ul:nth-child(10)>li:nth-child(2)", "nisp.cope.definition.mostcases.bullet2")
    }
    "render page with text 'The total amount of pension paid by your workplace or personal pension schemes will depend on the scheme and on any investment choices.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(11)", "nisp.cope.workplace")
    }

    "render page with link 'Find out more about COPE and contracting out'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(12)", "nisp.main.cope.linkTitle")
    }
    "render page with href link 'Find out more about COPE and contracting out'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>p:nth-child(12)>a", "https://www.gov.uk/government/publications/state-pension-fact-sheets/contracting-out-and-why-we-may-have-included-a-contracted-out-pension-equivalent-cope-amount-when-you-used-the-online-service")
    }
    "render page with link 'Back'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(13)>a", "nisp.back")
    }
    "render page with href link 'Back'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>p:nth-child(13)>a", "/check-your-state-pension/account")
    }

    /*Side bar help*/
    "render page with heading  'Get help'" in {
      assertEqualsMessage(htmlAccountDoc, "aside.sidebar >div.helpline-sidebar>h2", "nisp.nirecord.helpline.getHelp")
    }
    "render page with text 'Helpline 0800 731 0181'" in {
      assertEqualsMessage(htmlAccountDoc, "aside.sidebar >div.helpline-sidebar>p:nth-child(2)", "nisp.nirecord.helpline.number")
    }
    "render page with text 'Textphone 0800 731 0176'" in {
      assertEqualsMessage(htmlAccountDoc, "aside.sidebar >div.helpline-sidebar>p:nth-child(3)", "nisp.nirecord.helpline.textNumber")
    }
    "render page with text 'Monday to Friday: 8am to 6pm'" in {
      assertEqualsMessage(htmlAccountDoc, "aside.sidebar >div.helpline-sidebar>p:nth-child(4)", "nisp.nirecord.helpline.openTimes")
    }
  }
}
