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
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.services.{CitizenDetailsService, NationalInsuranceService, StatePensionService}
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.nisp.views.formatting.Time
import uk.gov.hmrc.nisp.views.html.HtmlSpec
import uk.gov.hmrc.play.frontend.auth.AuthenticationProviderIds
import uk.gov.hmrc.play.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.time.DateTimeUtils.now

import scala.concurrent.Future

class StatePension_Non_MQPViewSpec extends UnitSpec with MockitoSugar with HtmlSpec with BeforeAndAfter with OneAppPerSuite {


  implicit val cachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
  lazy val fakeRequest = FakeRequest()
  val mockUserNino = TestAccountBuilder.regularNino
  val mockUserNinoExcluded = TestAccountBuilder.excludedAll
  val mockUserNinoNotFound = TestAccountBuilder.blankNino
  val json = s"test/resources/$mockUserNino.json"
  val mockUsername = "mockuser"
  val mockUserId = "/auth/oid/" + mockUsername
  val mockUserIdExcluded = "/auth/oid/mockexcludedall"
  val mockUserIdContractedOut = "/auth/oid/mockcontractedout"
  val mockUserIdBlank = "/auth/oid/mockblank"
  val mockUserIdMQP = "/auth/oid/mockmqp"
  val mockUserIdForecastOnly = "/auth/oid/mockforecastonly"
  val mockUserIdWeak = "/auth/oid/mockweak"
  val mockUserIdAbroad = "/auth/oid/mockabroad"
  val mockUserIdMQPAbroad = "/auth/oid/mockmqpabroad"
  val mockUserIdFillGapsSingle = "/auth/oid/mockfillgapssingle"
  val mockUserIdFillGapsMultiple = "/auth/oid/mockfillgapsmultiple"
  val ggSignInUrl = "http://localhost:9949/gg/sign-in?continue=http%3A%2F%2Flocalhost%3A9234%2Fcheck-your-state-pension%2Faccount&origin=nisp-frontend&accountType=individual"
  val twoFactorUrl = "http://localhost:9949/coafe/two-step-verification/register/?continue=http%3A%2F%2Flocalhost%3A9234%2Fcheck-your-state-pension%2Faccount&failure=http%3A%2F%2Flocalhost%3A9234%2Fcheck-your-state-pension%2Fnot-authorised"

  def authenticatedFakeRequest(userId: String) = FakeRequest().withSession(
    SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
    SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
    SessionKeys.userId -> userId,
    SessionKeys.authProvider -> AuthenticationProviderIds.VerifyProviderId
  )


  "Render State Pension view with forecast only" should {

    lazy val controller = new MockStatePensionController {
      override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
      override val applicationConfig: ApplicationConfig = new ApplicationConfig {
        override val assetsPrefix: String = ""
        override val reportAProblemNonJSUrl: String = ""
        override val ssoUrl: Option[String] = None
        override val betaFeedbackUnauthenticatedUrl: String = ""
        override val contactFrontendPartialBaseUrl: String = ""
        override val govUkFinishedPageUrl: String = "govukdone"
        override val showGovUkDonePage: Boolean = false
        override val analyticsHost: String = ""
        override val analyticsToken: Option[String] = None
        override val betaFeedbackUrl: String = ""
        override val reportAProblemPartialUrl: String = ""
        override val citizenAuthHost: String = ""
        override val postSignInRedirectUrl: String = ""
        override val notAuthorisedRedirectUrl: String = ""
        override val identityVerification: Boolean = false
        override val ivUpliftUrl: String = "ivuplift"
        override val ggSignInUrl: String = "ggsignin"
        override val twoFactorUrl: String = "twofactor"
        override val pertaxFrontendUrl: String = ""
        override val contactFormServiceIdentifier: String = ""
        override val breadcrumbPartialUrl: String = ""
        override val showFullNI: Boolean = false
        override val futureProofPersonalMax: Boolean = false
        override val useStatePensionAPI: Boolean = true
        override val useNationalInsuranceAPI: Boolean = true
      }
      override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
      override val statePensionService: StatePensionService = mock[StatePensionService]
      override val nationalInsuranceService: NationalInsuranceService = mock[NationalInsuranceService]
    }

    when(controller.statePensionService.getSummary(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(Right(StatePension(
        new LocalDate(2016, 4, 5),
        amounts = StatePensionAmounts(
          protectedPayment = false,
          StatePensionAmountRegular(151.71, 590.10, 7081.15),
          StatePensionAmountForecast(4, 150.71, 590.10, 7081.15),
          StatePensionAmountMaximum(4, 1, 149.71, 590.10, 7081.15),
          StatePensionAmountRegular(0, 0, 0)
        ),
        pensionAge = 67,
        new LocalDate(2020, 6, 7),
        "2019-20",
        20,
        pensionSharingOrder = false,
        currentFullWeeklyPensionAmount = 155.65
      )
      )))

    when(controller.nationalInsuranceService.getSummary(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(Right(NationalInsuranceRecord(
        qualifyingYears = 11,
        qualifyingYearsPriorTo1975 = 0,
        numberOfGaps = 1,
        numberOfGapsPayable = 1,
        new LocalDate(1954, 3, 6),
        false,
        new LocalDate(2017, 4, 5),
        List(

          NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
          NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
          NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
        )
      )
      )))

    lazy val result = controller.show()(authenticatedFakeRequest(mockUserIdForecastOnly))

    //val scenario = controller.statePensionService.getSummary(mockUserNino)(HeaderCarrier()).right.get.forecastScenario

    lazy val htmlAccountDoc = asDocument(contentAsString(result))

    "render page with heading  'Your State Pension' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h1.heading-large", "nisp.main.h1.title")
    }

    "render page with text  'You can get your State Pension on' " in {
      assertElemetsOwnMessage(htmlAccountDoc, "article.content__body>div:nth-child(2)>p", "nisp.main.basedOn")
    }
    "render page with text  '7 june 2020' " in {
      assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(2)>p:nth-child(1)>span:nth-child(1)", "7 June 2020.")
    }
    "render page with text  'Your forecast is' " in {
      val sMessage = Messages("nisp.main.caveats") + " " + Messages("nisp.is")
      assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(2)>p:nth-child(1)>span:nth-child(2)", sMessage)
    }

    "render page with text  '£150.71 a week" in {
      val sWeek = "£150.71 " + Messages("nisp.main.week")
      assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(2)>p:nth-child(2)>em", sWeek)
    }
    "render page with text  ' £590.10 a month, £7,081.15 a year '" in {
      val sForecastAmount = "£590.10 " + Messages("nisp.main.month") + ", £7,081.15 " + Messages("nisp.main.year")
      assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(2)>p:nth-child(3)", sForecastAmount)
    }
    "render page with text  ' Your forcaste '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(3)", "nisp.main.caveats")
    }
    "render page with text  ' is not a guarantee and is based on the current law '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>ul:nth-child(4)>li:nth-child(1)", "nisp.main.notAGuarantee")
    }
    "render page with text  ' is based on your National Insurance record up to 5 April 2016 '" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>ul:nth-child(4)>li:nth-child(2)", "nisp.main.isBased", "5 April 2016", null, null)
    }
    "render page with text  ' does not include any increase due to inflation '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>ul:nth-child(4)>li:nth-child(3)", "nisp.main.inflation")
    }

    "render page with Heading  ' £155.55 is the most you can get'" in {
      val sMaxCanGet = "£150.71 " + Messages("nisp.main.mostYouCanGet")
      assertEqualsValue(htmlAccountDoc, "article.content__body>h2:nth-child(5)", sMaxCanGet)
    }
    "render page with text  'You cannot improve your forecast any further.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(6)", "nisp.main.cantImprove")
    }
    "render page with text  'If you’re working you may still need to pay National Insurance contributions until 7 June 2020 as they fund other state benefits and the NHS.'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(7)", "nisp.main.context.reachMax.needToPay", "7 June 2020", null, null)
    }
    "render page with link  'View your National Insurence Record'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(8)", "nisp.main.showyourrecord")
    }
    "render page with href link  'View your National Insurence Record'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>a:nth-child(8)", "/check-your-state-pension/account/nirecord")
    }

    /*overseas message*/
    "render page with text  'As you are living or working overseas (opens in new tab), you may be entitled to a " +
      "State Pension from the country you are living or working in.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>div.panel-indent:nth-child(9)>p", "nisp.main.overseas")
    }
    /*Ends*/

    "render page with heading  'Putting of claiming'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(10)", "nisp.main.puttingOff")
    }

    "render page with text  'When you are 67, you can put off claiming your State Pension. Doing this may mean you get extra State Pension when you do come to claim it. " +
      "The extra amount, along with your State Pension, forms part of your taxable income.'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(11)", "nisp.main.puttingOff.line1", "67", null, null)
    }

    "render page with link 'More on putting off claiming (opens in new tab)'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(12)", "nisp.main.puttingOff.linkTitle")
    }
    "render page with href link 'More on putting off claiming (opens in new tab)'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>a:nth-child(12)", "https://www.gov.uk/deferring-state-pension")
    }

    /*Side bar help*/
    "render page with heading  'Get help'" in {
      assertEqualsMessage(htmlAccountDoc, "aside.sidebar >div.helpline-sidebar>h2", "nisp.nirecord.helpline.getHelp")
    }
    "render page with text  'Helpline 0345 608 0126'" in {
      assertEqualsMessage(htmlAccountDoc, "aside.sidebar >div.helpline-sidebar>p:nth-child(2)", "nisp.nirecord.helpline.number")
    }
    "render page with text  'Monday to Friday: 8am to 6pm'" in {
      assertEqualsMessage(htmlAccountDoc, "aside.sidebar >div.helpline-sidebar>p:nth-child(3)", "nisp.nirecord.helpline.openTimes")
    }
    "render page with text  'Calls cost up to 12p a minute from landlines. Calls from mobiles may cost more.'" in {
      assertEqualsMessage(htmlAccountDoc, "aside.sidebar >div.helpline-sidebar>p:nth-child(4)", "nisp.nirecord.helpline.callsCost")
    }
    "render page with help text 'Get help with this page.' " in {
      assertElementContainsText(htmlAccountDoc, "div.report-error>a#get-help-action", "Get help with this page.")
    }

  }

  "Render State Pension view with MQP : Continue Working || Less than 10 years || Fill Gaps || Full Rate" should {

    lazy val controller = new MockStatePensionController {
      override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
      override val applicationConfig: ApplicationConfig = new ApplicationConfig {
        override val assetsPrefix: String = ""
        override val reportAProblemNonJSUrl: String = ""
        override val ssoUrl: Option[String] = None
        override val betaFeedbackUnauthenticatedUrl: String = ""
        override val contactFrontendPartialBaseUrl: String = ""
        override val govUkFinishedPageUrl: String = "govukdone"
        override val showGovUkDonePage: Boolean = false
        override val analyticsHost: String = ""
        override val analyticsToken: Option[String] = None
        override val betaFeedbackUrl: String = ""
        override val reportAProblemPartialUrl: String = ""
        override val citizenAuthHost: String = ""
        override val postSignInRedirectUrl: String = ""
        override val notAuthorisedRedirectUrl: String = ""
        override val identityVerification: Boolean = false
        override val ivUpliftUrl: String = "ivuplift"
        override val ggSignInUrl: String = "ggsignin"
        override val twoFactorUrl: String = "twofactor"
        override val pertaxFrontendUrl: String = ""
        override val contactFormServiceIdentifier: String = ""
        override val breadcrumbPartialUrl: String = ""
        override val showFullNI: Boolean = false
        override val futureProofPersonalMax: Boolean = false
        override val useStatePensionAPI: Boolean = true
        override val useNationalInsuranceAPI: Boolean = true
      }
      override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
      override val statePensionService: StatePensionService = mock[StatePensionService]
      override val nationalInsuranceService: NationalInsuranceService = mock[NationalInsuranceService]
    }

    when(controller.statePensionService.getSummary(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(Right(StatePension(
        new LocalDate(2016, 4, 5),
        amounts = StatePensionAmounts(
          protectedPayment = false,
          StatePensionAmountRegular(111.71, 590.10, 7081.15),
          StatePensionAmountForecast(4, 137.86, 599.44, 7193.34),
          StatePensionAmountMaximum(4, 1, 149.71, 590.10, 7081.15),
          StatePensionAmountRegular(0, 0, 0)
        ),
        pensionAge = 67,
        new LocalDate(2020, 6, 7),
        "2019-20",
        4,
        pensionSharingOrder = false,
        currentFullWeeklyPensionAmount = 155.65
      )
      )))

    when(controller.nationalInsuranceService.getSummary(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(Right(NationalInsuranceRecord(
        qualifyingYears = 4,
        qualifyingYearsPriorTo1975 = 0,
        numberOfGaps = 2,
        numberOfGapsPayable = 2,
        new LocalDate(1954, 3, 6),
        false,
        new LocalDate(2017, 4, 5),
        List(

          NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
          NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
          NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
        )
      )
      )))

    lazy val result = controller.show()(authenticatedFakeRequest(mockUserIdForecastOnly))

    val scenario = controller.statePensionService.getSummary(mockUserNino)(HeaderCarrier()).right.get.forecastScenario


    lazy val htmlAccountDoc = asDocument(contentAsString(result))

    "render page with heading  'Your State Pension' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h1.heading-large", "nisp.main.h1.title")
    }

    "render page with text  'You can get your State Pension on' " in {
      assertElemetsOwnMessage(htmlAccountDoc, "article.content__body>div:nth-child(2)>p", "nisp.main.basedOn")
    }
    "render page with text  '7 june 2020' " in {
      assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(2)>p:nth-child(1)>span:nth-child(1)", "7 June 2020.")
    }
    "render page with text  'Your forecast is' " in {
      val sMessage = Messages("nisp.main.caveats") + " " + Messages("nisp.is")
      assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(2)>p:nth-child(1)>span:nth-child(2)", sMessage)
    }

    "render page with text  '£137.86 a week" in {
      val sWeek = "£137.86 " + Messages("nisp.main.week")
      assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(2)>p:nth-child(2)>em", sWeek)
    }
    "render page with text  ' £599.44 a month, £7,193.34 a year '" in {
      val sForecastAmount = "£599.44 " + Messages("nisp.main.month") + ", £7,193.34 " + Messages("nisp.main.year")
      assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(2)>p:nth-child(3)", sForecastAmount)
    }
    "render page with text  ' Your forcaste '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(3)", "nisp.main.caveats")
    }
    "render page with text  ' is not a guarantee and is based on the current law '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>ul:nth-child(4)>li:nth-child(1)", "nisp.main.notAGuarantee")
    }
    "render page with text  ' is based on your National Insurance record up to 5 April 2016 '" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>ul:nth-child(4)>li:nth-child(2)", "nisp.main.isBased", "5 April 2016", null, null)
    }
    "render page with text  ' assumes that you’ll contribute another 4 years '" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>ul:nth-child(4)>li:nth-child(3)", "nisp.mqp.howManyToContribute", Time.years(4), null, null)
    }

    "render page with text  ' does not include any increase due to inflation '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>ul:nth-child(4)>li:nth-child(4)", "nisp.main.inflation")
    }
    "render page with text  ' You currently have 4 years on your record and you need at least 10 years to get any State Pension. '" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(5)", "nisp.mqp.youCurrentlyHave", Time.years(4).toString(), Constants.minimumQualifyingYearsNSP.toString(), null)
    }

    "render page with Heading  ' You can improve your forecast'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(6)", "nisp.main.context.fillGaps.improve.title")
    }
    "render page with text  'You have years on your record where you did not contribute enough National Insurance and you can make up the shortfall. " +
      "This will make these years count towards your pension and may improve your forecast.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(7)", "nisp.main.context.improve.para1.plural")
    }
    "render page with link  'View gaps in your record'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(8)", "nisp.main.context.fillGaps.viewGaps")
    }
    "render page with href link  'View gaps in your record'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>a:nth-child(8)", "/check-your-state-pension/account/nirecord/gaps")
    }

    /*overseas message*/
    "render page with text  'As you are living or working overseas (opens in new tab), you may be entitled to a " +
      "State Pension from the country you are living or working in.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>div.panel-indent:nth-child(9)>p", "nisp.main.overseas")
    }
    /*Ends*/

    "render page with heading  'Putting of claiming'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(10)", "nisp.main.puttingOff")
    }

    "render page with text  'When you are 67, you can put off claiming your State Pension. Doing this may mean you get extra State Pension when you do come to claim it. " +
      "The extra amount, along with your State Pension, forms part of your taxable income.'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(11)", "nisp.main.puttingOff.line1", "67", null, null)
    }

    "render page with link 'More on putting off claiming (opens in new tab)'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(12)", "nisp.main.puttingOff.linkTitle")
    }
    "render page with href link 'More on putting off claiming (opens in new tab)'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>a:nth-child(12)", "https://www.gov.uk/deferring-state-pension")
    }

    /*Side bar help*/
    "render page with heading  'Get help'" in {
      assertEqualsMessage(htmlAccountDoc, "aside.sidebar >div.helpline-sidebar>h2", "nisp.nirecord.helpline.getHelp")
    }
    "render page with text  'Helpline 0345 608 0126'" in {
      assertEqualsMessage(htmlAccountDoc, "aside.sidebar >div.helpline-sidebar>p:nth-child(2)", "nisp.nirecord.helpline.number")
    }
    "render page with text  'Monday to Friday: 8am to 6pm'" in {
      assertEqualsMessage(htmlAccountDoc, "aside.sidebar >div.helpline-sidebar>p:nth-child(3)", "nisp.nirecord.helpline.openTimes")
    }
    "render page with text  'Calls cost up to 12p a minute from landlines. Calls from mobiles may cost more.'" in {
      assertEqualsMessage(htmlAccountDoc, "aside.sidebar >div.helpline-sidebar>p:nth-child(4)", "nisp.nirecord.helpline.callsCost")
    }
    "render page with help text 'Get help with this page.' " in {
      assertElementContainsText(htmlAccountDoc, "div.report-error>a#get-help-action", "Get help with this page.")
    }

  }

  "Render State Pension view with MQP : Continue Working || Less than 10 years || no gaps || Full Rate" should {

    lazy val controller = new MockStatePensionController {
      override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
      override val applicationConfig: ApplicationConfig = new ApplicationConfig {
        override val assetsPrefix: String = ""
        override val reportAProblemNonJSUrl: String = ""
        override val ssoUrl: Option[String] = None
        override val betaFeedbackUnauthenticatedUrl: String = ""
        override val contactFrontendPartialBaseUrl: String = ""
        override val govUkFinishedPageUrl: String = "govukdone"
        override val showGovUkDonePage: Boolean = false
        override val analyticsHost: String = ""
        override val analyticsToken: Option[String] = None
        override val betaFeedbackUrl: String = ""
        override val reportAProblemPartialUrl: String = ""
        override val citizenAuthHost: String = ""
        override val postSignInRedirectUrl: String = ""
        override val notAuthorisedRedirectUrl: String = ""
        override val identityVerification: Boolean = false
        override val ivUpliftUrl: String = "ivuplift"
        override val ggSignInUrl: String = "ggsignin"
        override val twoFactorUrl: String = "twofactor"
        override val pertaxFrontendUrl: String = ""
        override val contactFormServiceIdentifier: String = ""
        override val breadcrumbPartialUrl: String = ""
        override val showFullNI: Boolean = false
        override val futureProofPersonalMax: Boolean = false
        override val useStatePensionAPI: Boolean = true
        override val useNationalInsuranceAPI: Boolean = true
      }
      override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
      override val statePensionService: StatePensionService = mock[StatePensionService]
      override val nationalInsuranceService: NationalInsuranceService = mock[NationalInsuranceService]
    }

    when(controller.statePensionService.getSummary(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(Right(StatePension(
        new LocalDate(2016, 4, 5),
        amounts = StatePensionAmounts(
          protectedPayment = false,
          StatePensionAmountRegular(111.71, 590.10, 7081.15),
          StatePensionAmountForecast(4, 155.65, 599.44, 7193.34),
          StatePensionAmountMaximum(4, 1, 149.71, 590.10, 7081.15),
          StatePensionAmountRegular(0, 0, 0)
        ),
        pensionAge = 67,
        new LocalDate(2020, 6, 7),
        "2019-20",
        4,
        pensionSharingOrder = false,
        currentFullWeeklyPensionAmount = 155.65
      )
      )))

    when(controller.nationalInsuranceService.getSummary(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(Right(NationalInsuranceRecord(
        qualifyingYears = 4,
        qualifyingYearsPriorTo1975 = 0,
        numberOfGaps = 0,
        numberOfGapsPayable = 0,
        new LocalDate(1954, 3, 6),
        false,
        new LocalDate(2017, 4, 5),
        List(

          NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
          NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
          NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
        )
      )
      )))

    lazy val result = controller.show()(authenticatedFakeRequest(mockUserIdForecastOnly))

    val scenario = controller.statePensionService.getSummary(mockUserNino)(HeaderCarrier()).right.get.forecastScenario

    lazy val htmlAccountDoc = asDocument(contentAsString(result))

    "render page with heading  'Your State Pension' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h1.heading-large", "nisp.main.h1.title")
    }

    "render page with text  'You can get your State Pension on' " in {
      assertElemetsOwnMessage(htmlAccountDoc, "article.content__body>div:nth-child(2)>p", "nisp.main.basedOn")
    }
    "render page with text  '7 june 2020' " in {
      assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(2)>p:nth-child(1)>span:nth-child(1)", "7 June 2020.")
    }
    "render page with text  'Your forecast is' " in {
      val sMessage = Messages("nisp.main.caveats") + " " + Messages("nisp.is")
      assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(2)>p:nth-child(1)>span:nth-child(2)", sMessage)
    }

    "render page with text  '£155.65 a week" in {
      val sWeek = "£155.65 " + Messages("nisp.main.week")
      assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(2)>p:nth-child(2)>em", sWeek)
    }
    "render page with text  ' £599.44 a month, £7,193.34 a year '" in {
      val sForecastAmount = "£599.44 " + Messages("nisp.main.month") + ", £7,193.34 " + Messages("nisp.main.year")
      assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(2)>p:nth-child(3)", sForecastAmount)
    }
    "render page with text  ' Your forcaste '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(3)", "nisp.main.caveats")
    }
    "render page with text  ' is not a guarantee and is based on the current law '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>ul:nth-child(4)>li:nth-child(1)", "nisp.main.notAGuarantee")
    }
    "render page with text  ' is based on your National Insurance record up to 5 April 2016 '" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>ul:nth-child(4)>li:nth-child(2)", "nisp.main.isBased", "5 April 2016", null, null)
    }
    "render page with text  ' assumes that you’ll contribute another 4 years '" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>ul:nth-child(4)>li:nth-child(3)", "nisp.mqp.howManyToContribute", Time.years(4), null, null)
    }

    "render page with text  ' does not include any increase due to inflation '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>ul:nth-child(4)>li:nth-child(4)", "nisp.main.inflation")
    }
    "render page with text  ' You currently have 4 years on your record and you need at least 10 years to get any State Pension. '" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(5)", "nisp.mqp.youCurrentlyHave", Time.years(4).toString(), Constants.minimumQualifyingYearsNSP.toString(), null)
    }

    "render page with Heading  '£155.65 is the most you can get'" in {
      val sMessgae = "£155.65 " + Messages("nisp.main.mostYouCanGet")
      assertEqualsValue(htmlAccountDoc, "article.content__body>h3:nth-child(6)", sMessgae)
    }

    "render page with text  'You cannot improve your forecast any further.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(7)", "nisp.main.context.willReach")
    }
    "render page with text  'If you’re working you may still need to pay National Insurance contributions until 7 June 2020 as they fund other state benefits and the NHS.'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(8)", "nisp.main.context.reachMax.needToPay", "7 June 2020", null, null)
    }
    "render page with link  'View your National Insurence Record'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(9)", "nisp.main.showyourrecord")
    }
    "render page with href link  'View your National Insurence Record'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>a:nth-child(9)", "/check-your-state-pension/account/nirecord")
    }

    /*overseas message*/
    "render page with text  'As you are living or working overseas (opens in new tab), you may be entitled to a " +
      "State Pension from the country you are living or working in.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>div.panel-indent:nth-child(10)>p", "nisp.main.overseas")
    }
    /*Ends*/
    "render page with heading  'Putting of claiming'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(11)", "nisp.main.puttingOff")
    }

    "render page with text  'When you are 67, you can put off claiming your State Pension. Doing this may mean you get extra State Pension when you do come to claim it. " +
      "The extra amount, along with your State Pension, forms part of your taxable income.'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(12)", "nisp.main.puttingOff.line1", "67", null, null)
    }

    "render page with link 'More on putting off claiming (opens in new tab)'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(13)", "nisp.main.puttingOff.linkTitle")
    }
    "render page with href link 'More on putting off claiming (opens in new tab)'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>a:nth-child(13)", "https://www.gov.uk/deferring-state-pension")
    }


  }

  "Render State Pension view with MQP : Continue Working || 0 Qualify Years || has fillable Gaps ||  Personal Max" should {

    lazy val controller = new MockStatePensionController {
      override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
      override val applicationConfig: ApplicationConfig = new ApplicationConfig {
        override val assetsPrefix: String = ""
        override val reportAProblemNonJSUrl: String = ""
        override val ssoUrl: Option[String] = None
        override val betaFeedbackUnauthenticatedUrl: String = ""
        override val contactFrontendPartialBaseUrl: String = ""
        override val govUkFinishedPageUrl: String = "govukdone"
        override val showGovUkDonePage: Boolean = false
        override val analyticsHost: String = ""
        override val analyticsToken: Option[String] = None
        override val betaFeedbackUrl: String = ""
        override val reportAProblemPartialUrl: String = ""
        override val citizenAuthHost: String = ""
        override val postSignInRedirectUrl: String = ""
        override val notAuthorisedRedirectUrl: String = ""
        override val identityVerification: Boolean = false
        override val ivUpliftUrl: String = "ivuplift"
        override val ggSignInUrl: String = "ggsignin"
        override val twoFactorUrl: String = "twofactor"
        override val pertaxFrontendUrl: String = ""
        override val contactFormServiceIdentifier: String = ""
        override val breadcrumbPartialUrl: String = ""
        override val showFullNI: Boolean = false
        override val futureProofPersonalMax: Boolean = false
        override val useStatePensionAPI: Boolean = true
        override val useNationalInsuranceAPI: Boolean = true
      }
      override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
      override val statePensionService: StatePensionService = mock[StatePensionService]
      override val nationalInsuranceService: NationalInsuranceService = mock[NationalInsuranceService]
    }

    when(controller.statePensionService.getSummary(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(Right(StatePension(
        new LocalDate(2016, 4, 5),
        amounts = StatePensionAmounts(
          protectedPayment = false,
          StatePensionAmountRegular(111.71, 590.10, 7081.15),
          StatePensionAmountForecast(4, 155.65, 599.44, 7193.34),
          StatePensionAmountMaximum(4, 1, 149.71, 590.10, 7081.15),
          StatePensionAmountRegular(0, 0, 0)
        ),
        pensionAge = 67,
        new LocalDate(2020, 6, 7),
        "2019-20",
        0,
        pensionSharingOrder = false,
        currentFullWeeklyPensionAmount = 155.65
      )
      )))

    when(controller.nationalInsuranceService.getSummary(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(Right(NationalInsuranceRecord(
        qualifyingYears = 0,
        qualifyingYearsPriorTo1975 = 0,
        numberOfGaps = 1,
        numberOfGapsPayable = 1,
        new LocalDate(1954, 3, 6),
        false,
        new LocalDate(2017, 4, 5),
        List(

          NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
          NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
          NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
        )
      )
      )))

    lazy val result = controller.show()(authenticatedFakeRequest(mockUserIdForecastOnly))

    val scenario = controller.statePensionService.getSummary(mockUserNino)(HeaderCarrier()).right.get.forecastScenario


    lazy val htmlAccountDoc = asDocument(contentAsString(result))

    "render page with heading  'Your State Pension' " in {

      assertEqualsMessage(htmlAccountDoc, "article.content__body>h1.heading-large", "nisp.main.h1.title")
    }

    "render page with text  'You can get your State Pension on' " in {
      assertElemetsOwnMessage(htmlAccountDoc, "article.content__body>div:nth-child(2)>p", "nisp.main.basedOn")
    }
    "render page with text  '7 june 2020' " in {
      assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(2)>p:nth-child(1)>span:nth-child(1)", "7 June 2020.")
    }
    "render page with text  'Your forecast is' " in {
      val sMessage = Messages("nisp.main.caveats") + " " + Messages("nisp.is")
      assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(2)>p:nth-child(1)>span:nth-child(2)", sMessage)
    }

    "render page with text  '£155.65 a week" in {
      val sWeek = "£155.65 " + Messages("nisp.main.week")
      assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(2)>p:nth-child(2)>em", sWeek)
    }
    "render page with text  ' £599.44 a month, £7,193.34 a year '" in {
      val sForecastAmount = "£599.44 " + Messages("nisp.main.month") + ", £7,193.34 " + Messages("nisp.main.year")
      assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(2)>p:nth-child(3)", sForecastAmount)
    }
    "render page with text  ' Your forcaste '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(3)", "nisp.main.caveats")
    }
    "render page with text  ' is not a guarantee and is based on the current law '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>ul:nth-child(4)>li:nth-child(1)", "nisp.main.notAGuarantee")
    }
    "render page with text  ' is based on your National Insurance record up to 5 April 2016 '" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>ul:nth-child(4)>li:nth-child(2)", "nisp.main.isBased", "5 April 2016", null, null)
    }
    "render page with text  ' assumes that you’ll contribute another 4 years '" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>ul:nth-child(4)>li:nth-child(3)", "nisp.mqp.howManyToContribute", Time.years(4), null, null)
    }

    "render page with text  ' does not include any increase due to inflation '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>ul:nth-child(4)>li:nth-child(4)", "nisp.main.inflation")
    }
    "render page with text  'You do not have any years on your record and you need at least 10 years to get any State Pension. '" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(5)", "nisp.mqp.youCurrentlyHaveZero", Constants.minimumQualifyingYearsNSP.toString(), null, null)
    }

    "render page with Heading  '£155.65 is the most you can get'" in {
      val sMessgae = "£155.65 " + Messages("nisp.main.mostYouCanGet")
      assertEqualsValue(htmlAccountDoc, "article.content__body>h3:nth-child(6)", sMessgae)
    }

    "render page with text  'You cannot improve your forecast any further, unless you choose to put off claimimg'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(7)", "nisp.main.context.willReach")
    }
    "render page with text  'If you’re working you may still need to pay National Insurance contributions until 7 June 2020 as they fund other state benefits and the NHS.'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(8)", "nisp.main.context.reachMax.needToPay", "7 June 2020", null, null)
    }
    "render page with link  'View your National Insurence Record'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(9)", "nisp.main.showyourrecord")
    }
    "render page with href link  'View your National Insurence Record'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>a:nth-child(9)", "/check-your-state-pension/account/nirecord")
    }

    /*overseas message*/
    "render page with text  'As you are living or working overseas (opens in new tab), you may be entitled to a " +
      "State Pension from the country you are living or working in.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>div.panel-indent:nth-child(10)>p", "nisp.main.overseas")
    }
    /*Ends*/
    "render page with heading  'Putting of claiming'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(11)", "nisp.main.puttingOff")
    }

    "render page with text  'When you are 67, you can put off claiming your State Pension. Doing this may mean you get extra State Pension when you do come to claim it. " +
      "The extra amount, along with your State Pension, forms part of your taxable income.'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(12)", "nisp.main.puttingOff.line1", "67", null, null)
    }

    "render page with link 'More on putting off claiming (opens in new tab)'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(13)", "nisp.main.puttingOff.linkTitle")
    }
    "render page with href link 'More on putting off claiming (opens in new tab)'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>a:nth-child(13)", "https://www.gov.uk/deferring-state-pension")
    }
  }

  "Render State Pension view with MQP : Continue Working || 9 Qualify Years || cant fill gaps ||  Personal Max" should {

    lazy val controller = new MockStatePensionController {
      override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
      override val applicationConfig: ApplicationConfig = new ApplicationConfig {
        override val assetsPrefix: String = ""
        override val reportAProblemNonJSUrl: String = ""
        override val ssoUrl: Option[String] = None
        override val betaFeedbackUnauthenticatedUrl: String = ""
        override val contactFrontendPartialBaseUrl: String = ""
        override val govUkFinishedPageUrl: String = "govukdone"
        override val showGovUkDonePage: Boolean = false
        override val analyticsHost: String = ""
        override val analyticsToken: Option[String] = None
        override val betaFeedbackUrl: String = ""
        override val reportAProblemPartialUrl: String = ""
        override val citizenAuthHost: String = ""
        override val postSignInRedirectUrl: String = ""
        override val notAuthorisedRedirectUrl: String = ""
        override val identityVerification: Boolean = false
        override val ivUpliftUrl: String = "ivuplift"
        override val ggSignInUrl: String = "ggsignin"
        override val twoFactorUrl: String = "twofactor"
        override val pertaxFrontendUrl: String = ""
        override val contactFormServiceIdentifier: String = ""
        override val breadcrumbPartialUrl: String = ""
        override val showFullNI: Boolean = false
        override val futureProofPersonalMax: Boolean = false
        override val useStatePensionAPI: Boolean = true
        override val useNationalInsuranceAPI: Boolean = true
      }
      override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
      override val statePensionService: StatePensionService = mock[StatePensionService]
      override val nationalInsuranceService: NationalInsuranceService = mock[NationalInsuranceService]
    }

    when(controller.statePensionService.getSummary(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(Right(StatePension(
        new LocalDate(2016, 4, 5),
        amounts = StatePensionAmounts(
          protectedPayment = false,
          StatePensionAmountRegular(111.71, 590.10, 7081.15),
          StatePensionAmountForecast(4, 155.65, 599.44, 7193.34),
          StatePensionAmountMaximum(4, 1, 149.71, 590.10, 7081.15),
          StatePensionAmountRegular(0, 0, 0)
        ),
        pensionAge = 67,
        new LocalDate(2020, 6, 7),
        "2019-20",
        9,
        pensionSharingOrder = false,
        currentFullWeeklyPensionAmount = 155.65
      )
      )))

    when(controller.nationalInsuranceService.getSummary(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(Right(NationalInsuranceRecord(
        qualifyingYears = 9,
        qualifyingYearsPriorTo1975 = 0,
        numberOfGaps = 0,
        numberOfGapsPayable = 0,
        new LocalDate(1954, 3, 6),
        false,
        new LocalDate(2017, 4, 5),
        List(

          NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
          NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
          NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
        )
      )
      )))

    lazy val result = controller.show()(authenticatedFakeRequest(mockUserIdForecastOnly))

    val scenario = controller.statePensionService.getSummary(mockUserNino)(HeaderCarrier()).right.get.forecastScenario


    lazy val htmlAccountDoc = asDocument(contentAsString(result))

    "render page with heading  'Your State Pension' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h1.heading-large", "nisp.main.h1.title")
    }

    "render page with text  'You can get your State Pension on' " in {
      assertElemetsOwnMessage(htmlAccountDoc, "article.content__body>div:nth-child(2)>p", "nisp.main.basedOn")
    }
    "render page with text  '7 june 2020' " in {
      assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(2)>p:nth-child(1)>span:nth-child(1)", "7 June 2020.")
    }
    "render page with text  'Your forecast is' " in {
      val sMessage = Messages("nisp.main.caveats") + " " + Messages("nisp.is")
      assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(2)>p:nth-child(1)>span:nth-child(2)", sMessage)
    }

    "render page with text  '£155.65 a week" in {
      val sWeek = "£155.65 " + Messages("nisp.main.week")
      assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(2)>p:nth-child(2)>em", sWeek)
    }
    "render page with text  ' £599.44 a month, £7,193.34 a year '" in {
      val sForecastAmount = "£599.44 " + Messages("nisp.main.month") + ", £7,193.34 " + Messages("nisp.main.year")
      assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(2)>p:nth-child(3)", sForecastAmount)
    }
    "render page with text  ' Your forcaste '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(3)", "nisp.main.caveats")
    }
    "render page with text  ' is not a guarantee and is based on the current law '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>ul:nth-child(4)>li:nth-child(1)", "nisp.main.notAGuarantee")
    }
    "render page with text  ' is based on your National Insurance record up to 5 April 2016 '" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>ul:nth-child(4)>li:nth-child(2)", "nisp.main.isBased", "5 April 2016", null, null)
    }
    "render page with text  ' assumes that you’ll contribute another 4 years '" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>ul:nth-child(4)>li:nth-child(3)", "nisp.mqp.howManyToContribute", Time.years(4), null, null)
    }

    "render page with text  ' does not include any increase due to inflation '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>ul:nth-child(4)>li:nth-child(4)", "nisp.main.inflation")
    }

    "render page with text  ' You currently have 9 years on your record and you need at least 10 years to get any State Pension. '" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(5)", "nisp.mqp.youCurrentlyHave", Time.years(9).toString(), Constants.minimumQualifyingYearsNSP.toString(), null)
    }

    "render page with Heading  '£155.65 is the most you can get'" in {
      val sMessgae = "£155.65 " + Messages("nisp.main.mostYouCanGet")
      assertEqualsValue(htmlAccountDoc, "article.content__body>h3:nth-child(6)", sMessgae)
    }

    "render page with text  'You cannot improve your forecast any further, unless you choose to put off claimimg'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(7)", "nisp.main.context.willReach")
    }
    "render page with text  'If you’re working you may still need to pay National Insurance contributions until 7 June 2020 as they fund other state benefits and the NHS.'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(8)", "nisp.main.context.reachMax.needToPay", "7 June 2020", null, null)
    }
    "render page with link  'View your National Insurence Record'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(9)", "nisp.main.showyourrecord")
    }
    "render page with href link  'View your National Insurence Record'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>a:nth-child(9)", "/check-your-state-pension/account/nirecord")
    }

    /*overseas message*/
    "render page with text  'As you are living or working overseas (opens in new tab), you may be entitled to a " +
      "State Pension from the country you are living or working in.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>div.panel-indent:nth-child(10)>p", "nisp.main.overseas")
    }
    /*Ends*/
    "render page with heading  'Putting of claiming'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(11)", "nisp.main.puttingOff")
    }

    "render page with text  'When you are 67, you can put off claiming your State Pension. Doing this may mean you get extra State Pension when you do come to claim it. " +
      "The extra amount, along with your State Pension, forms part of your taxable income.'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(12)", "nisp.main.puttingOff.line1", "67", null, null)
    }

    "render page with link 'More on putting off claiming (opens in new tab)'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(13)", "nisp.main.puttingOff.linkTitle")
    }
    "render page with href link 'More on putting off claiming (opens in new tab)'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>a:nth-child(13)", "https://www.gov.uk/deferring-state-pension")
    }
  }

  "Render State Pension view with MQP : No years to contrib || Less than 10 years || No Gaps || Cant get pension" should {

    lazy val controller = new MockStatePensionController {
      override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
      override val applicationConfig: ApplicationConfig = new ApplicationConfig {
        override val assetsPrefix: String = ""
        override val reportAProblemNonJSUrl: String = ""
        override val ssoUrl: Option[String] = None
        override val betaFeedbackUnauthenticatedUrl: String = ""
        override val contactFrontendPartialBaseUrl: String = ""
        override val govUkFinishedPageUrl: String = "govukdone"
        override val showGovUkDonePage: Boolean = false
        override val analyticsHost: String = ""
        override val analyticsToken: Option[String] = None
        override val betaFeedbackUrl: String = ""
        override val reportAProblemPartialUrl: String = ""
        override val citizenAuthHost: String = ""
        override val postSignInRedirectUrl: String = ""
        override val notAuthorisedRedirectUrl: String = ""
        override val identityVerification: Boolean = false
        override val ivUpliftUrl: String = "ivuplift"
        override val ggSignInUrl: String = "ggsignin"
        override val twoFactorUrl: String = "twofactor"
        override val pertaxFrontendUrl: String = ""
        override val contactFormServiceIdentifier: String = ""
        override val breadcrumbPartialUrl: String = ""
        override val showFullNI: Boolean = false
        override val futureProofPersonalMax: Boolean = false
        override val useStatePensionAPI: Boolean = true
        override val useNationalInsuranceAPI: Boolean = true
      }
      override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
      override val statePensionService: StatePensionService = mock[StatePensionService]
      override val nationalInsuranceService: NationalInsuranceService = mock[NationalInsuranceService]
    }

    when(controller.statePensionService.getSummary(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(Right(StatePension(
        new LocalDate(2016, 4, 5),
        amounts = StatePensionAmounts(
          protectedPayment = false,
          StatePensionAmountRegular(0, 0, 0),
          StatePensionAmountForecast(0, 0, 0, 0),
          StatePensionAmountMaximum(0, 0, 0, 0, 0),
          StatePensionAmountRegular(0, 0, 0)
        ),
        pensionAge = 67,
        new LocalDate(2017, 5, 4),
        "2016-17",
        4,
        pensionSharingOrder = false,
        currentFullWeeklyPensionAmount = 155.65
      )
      )))

    when(controller.nationalInsuranceService.getSummary(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(Right(NationalInsuranceRecord(
        qualifyingYears = 4,
        qualifyingYearsPriorTo1975 = 0,
        numberOfGaps = 0,
        numberOfGapsPayable = 0,
        new LocalDate(1954, 3, 6),
        false,
        new LocalDate(2017, 4, 5),
        List(

          NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
          NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
          NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
        )
      )
      )))

    lazy val result = controller.show()(authenticatedFakeRequest(mockUserIdForecastOnly))

    val scenario = controller.statePensionService.getSummary(mockUserNino)(HeaderCarrier()).right.get.forecastScenario

    lazy val htmlAccountDoc = asDocument(contentAsString(result))

    "render page with heading  'Your State Pension' " in {

      assertEqualsMessage(htmlAccountDoc, "article.content__body>h1.heading-large", "nisp.main.h1.title")
    }

    "render page with text  'You’ll reach State Pension age on ' " in {
      val sMessage = "You’ll reach State Pension age on";
      assertElemetsOwnText(htmlAccountDoc, "article.content__body>div:nth-child(2)>p", sMessage)
    }
    "render page with text  'You’ll reach State Pension age on 4 May 2018. ' " in {
      val sMessage = "4 May 2017.";
      assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(2)>p>span", sMessage)
    }

    "render page with text  'By this time, you will not be able to get the 10 years needed on your National Insurance record to get any State Pension.' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>div:nth-child(2)>p:nth-child(2)", "nisp.mqp.notPossible")
    }

    "render page with text  'What you can do next" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(3)", "nisp.mqp.doNext")
    }

    /*overseas message*/
    "render page with text  'As you are living or working overseas (opens in new tab), you may be entitled to a " +
      "State Pension from the country you are living or working in.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(4)", "nisp.main.overseas")
    }
    /*Ends*/
    "render page with text ' You do not have any years on your record that do not count towards your pension. '" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(5)", "nisp.mqp.years.dontCount.zero")
    }
    "render page with link  'View your National Insurence Record'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(6)>a", "nisp.main.showyourrecord")
    }
    "render page with href link  'View your National Insurence Record'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>p:nth-child(6)>a", "/check-your-state-pension/account/nirecord")
    }

    "render page with text  'After State Pension age, 4 May 2017 you no longer pay National Insurance contributions.'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(7)", "nisp.mqp.afterSpa", "4 May 2017", null, null)
    }

    "render page with link 'What else you can do'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(8)", "nisp.mqp.whatElse")
    }
    "render page with link 'You may be eligible for Pension Credit (opens in new tab)  if your retirement income is low.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(9)", "nisp.mqp.pensionCredit")
    }
    "render page with href link 'You may be eligible for Pension Credit (opens in new tab)  if your retirement income is low'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>p:nth-child(9)>a", "https://www.gov.uk/pension-credit/overview")
    }
    "render page with link 'Contact the Money Advice Service (opens in new tab)  for free impartial advice.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(10)", "nisp.mqp.moneyAdvice")
    }
    "render page with href link 'Contact the Money Advice Service (opens in new tab)  for free impartial advice.'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>p:nth-child(10)>a", "https://www.moneyadviceservice.org.uk/en")
    }

  }

  "Render State Pension view with MQP : No years to contrib || Less than 10 years || has fillable Gaps || Personal Max" should {

    lazy val controller = new MockStatePensionController {
      override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService
      override val applicationConfig: ApplicationConfig = new ApplicationConfig {
        override val assetsPrefix: String = ""
        override val reportAProblemNonJSUrl: String = ""
        override val ssoUrl: Option[String] = None
        override val betaFeedbackUnauthenticatedUrl: String = ""
        override val contactFrontendPartialBaseUrl: String = ""
        override val govUkFinishedPageUrl: String = "govukdone"
        override val showGovUkDonePage: Boolean = false
        override val analyticsHost: String = ""
        override val analyticsToken: Option[String] = None
        override val betaFeedbackUrl: String = ""
        override val reportAProblemPartialUrl: String = ""
        override val citizenAuthHost: String = ""
        override val postSignInRedirectUrl: String = ""
        override val notAuthorisedRedirectUrl: String = ""
        override val identityVerification: Boolean = false
        override val ivUpliftUrl: String = "ivuplift"
        override val ggSignInUrl: String = "ggsignin"
        override val twoFactorUrl: String = "twofactor"
        override val pertaxFrontendUrl: String = ""
        override val contactFormServiceIdentifier: String = ""
        override val breadcrumbPartialUrl: String = ""
        override val showFullNI: Boolean = false
        override val futureProofPersonalMax: Boolean = false
        override val useStatePensionAPI: Boolean = true
        override val useNationalInsuranceAPI: Boolean = true
      }
      override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
      override val statePensionService: StatePensionService = mock[StatePensionService]
      override val nationalInsuranceService: NationalInsuranceService = mock[NationalInsuranceService]
    }

    when(controller.statePensionService.getSummary(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(Right(StatePension(
        new LocalDate(2016, 4, 5),
        amounts = StatePensionAmounts(
          protectedPayment = false,
          StatePensionAmountRegular(0, 0, 0),
          StatePensionAmountForecast(0, 0, 0, 0),
          StatePensionAmountMaximum(2, 2, 12, 0, 0),
          StatePensionAmountRegular(0, 0, 0)
        ),
        pensionAge = 67,
        new LocalDate(2018, 5, 4),
        "2017-18",
        4,
        pensionSharingOrder = false,
        currentFullWeeklyPensionAmount = 155.65
      )
      )))

    when(controller.nationalInsuranceService.getSummary(Matchers.any())(Matchers.any()))
      .thenReturn(Future.successful(Right(NationalInsuranceRecord(
        qualifyingYears = 4,
        qualifyingYearsPriorTo1975 = 0,
        numberOfGaps = 2,
        numberOfGapsPayable = 2,
        new LocalDate(1954, 3, 6),
        false,
        new LocalDate(2017, 4, 5),
        List(

          NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
          NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
          NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
        )
      )
      )))

    lazy val result = controller.show()(authenticatedFakeRequest(mockUserIdForecastOnly))

    val scenario = controller.statePensionService.getSummary(mockUserNino)(HeaderCarrier()).right.get.forecastScenario

    lazy val htmlAccountDoc = asDocument(contentAsString(result))

    "render page with heading  'Your State Pension' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h1.heading-large", "nisp.main.h1.title")
    }

    "render page with text  'You’ll reach State Pension age on ' " in {
      val sMessage = "You’ll reach State Pension age on";
      assertElemetsOwnText(htmlAccountDoc, "article.content__body>div:nth-child(2)>p", sMessage)
    }
    "render page with text  'You’ll reach State Pension age on 4 May 2018. ' " in {
      val sMessage = "4 May 2018.";
      assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(2)>p>span", sMessage)
    }

    "render page with text  'By this time, you will not have the 10 years needed on your National Insurance record to get any State Pension.' " in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>div:nth-child(2)>p:nth-child(2)", "nisp.mqp.possible")
    }

    "render page with text  'What you can do next" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(3)", "nisp.mqp.doNext")
    }

    /*overseas message*/
    "render page with text  'As you are living or working overseas (opens in new tab), you may be entitled to a " +
      "State Pension from the country you are living or working in.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(4)", "nisp.main.overseas")
    }
    /*Ends*/
    "render page with text '  You also have 2 years on your record which do not count towards your pension because you did not contribute enough National Insurance." +
      " Filling some of these years may get you some State Pension.  '" in {
      val sMessage = Messages("nisp.mqp.years.dontCount.plural", Time.years(2).toString()) + " " + Messages("nisp.mqp.filling.may.plural")
      assertEqualsValue(htmlAccountDoc, "article.content__body>p:nth-child(5)", sMessage)
    }
    "render page with link  'Gaps in your record and the cost of filling them'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(6)>a", "nisp.main.context.fillGaps.viewGapsAndCost")
    }
    "render page with href link  'View your National Insurence Record'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>p:nth-child(6)>a", "/check-your-state-pension/account/nirecord/gaps")
    }

    "render page with text  'After State Pension age, 4 May 2017 you no longer pay National Insurance contributions.'" in {
      assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(7)", "nisp.mqp.afterSpa", "4 May 2018", null, null)
    }

    "render page with link 'What else you can do'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(8)", "nisp.mqp.whatElse")
    }
    "render page with link 'You may be eligible for Pension Credit (opens in new tab)  if your retirement income is low.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(9)", "nisp.mqp.pensionCredit")
    }
    "render page with href link 'You may be eligible for Pension Credit (opens in new tab)  if your retirement income is low'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>p:nth-child(9)>a", "https://www.gov.uk/pension-credit/overview")
    }
    "render page with link 'Contact the Money Advice Service (opens in new tab)  for free impartial advice.'" in {
      assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(10)", "nisp.mqp.moneyAdvice")
    }
    "render page with href link 'Contact the Money Advice Service (opens in new tab)  for free impartial advice.'" in {
      assertLinkHasValue(htmlAccountDoc, "article.content__body>p:nth-child(10)>a", "https://www.moneyadviceservice.org.uk/en")
    }
  }
}
