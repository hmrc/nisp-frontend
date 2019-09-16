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

import org.apache.commons.lang3.StringEscapeUtils
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatest._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, _}
import uk.gov.hmrc.nisp.builders.{ApplicationConfigBuilder, NationalInsuranceTaxYearBuilder}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.config.wiring.NispFormPartialRetriever
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.services.{CitizenDetailsService, NationalInsuranceService, StatePensionService}
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.nisp.views.formatting.Time
import uk.gov.hmrc.play.frontend.auth.AuthenticationProviderIds
import uk.gov.hmrc.nisp.utils.LanguageHelper.langUtils.Dates
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever
import uk.gov.hmrc.time.DateTimeUtils.now
import uk.gov.hmrc.nisp.controllers.NispFrontendController

import scala.concurrent.Future
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.nisp.controllers.auth.AuthAction

class StatePension_MQPViewSpec extends HtmlSpec with NispFrontendController with MockitoSugar with BeforeAndAfter {

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
  override implicit val formPartialRetriever: uk.gov.hmrc.play.partials.FormPartialRetriever = NispFormPartialRetriever

  val expectedMoneyServiceLink = "https://www.moneyadviceservice.org.uk/en"
  val expectedPensionCreditOverviewLink = "https://www.gov.uk/pension-credit/overview"

  def authenticatedFakeRequest(userId: String) = fakeRequest.withSession(
    SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
    SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
    SessionKeys.userId -> userId,
    SessionKeys.authProvider -> AuthenticationProviderIds.VerifyProviderId
  )

  def createStatePensionController = {
    new MockStatePensionController {
      override val authenticate: AuthAction = new MockAuthAction(TestAccountBuilder.forecastOnlyNino)
      override val applicationConfig: ApplicationConfig = ApplicationConfigBuilder()
      override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
      override val statePensionService: StatePensionService = mock[StatePensionService]
      override val nationalInsuranceService: NationalInsuranceService = mock[NationalInsuranceService]
    }
  }

  "The State Pension page" when {

    "the user is a MQP" when {

      "State Pension page with forecast only" should {

        lazy val controller = createStatePensionController
        when(controller.statePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
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
            currentFullWeeklyPensionAmount = 155.65,
            false,
            false
          )
          )))

        when(controller.nationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Right(NationalInsuranceRecord(
            qualifyingYears = 11,
            qualifyingYearsPriorTo1975 = 0,
            numberOfGaps = 1,
            numberOfGapsPayable = 1,
            Some(new LocalDate(1954, 3, 6)),
            false,
            new LocalDate(2017, 4, 5),
            List(

              NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
              NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
              NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
            ),
            reducedRateElection = false
          )
          )))

        lazy val result = controller.show()(authenticatedFakeRequest(mockUserIdForecastOnly).withCookies(lanCookie))
        lazy val htmlAccountDoc = asDocument(contentAsString(result))

        "render with correct page title" in {
          assertElementContainsText(htmlAccountDoc, "head>title" ,messages("nisp.main.h1.title") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
        }
        "render page with heading  'Your State Pension' " in {
          assertEqualsMessage(htmlAccountDoc, "article.content__body>h1.titleWithPAgeInfo", "nisp.main.h1.title")
        }

        "render page with text  'You can get your State Pension on' " in {
          assertElemetsOwnMessage(htmlAccountDoc, "article.content__body>div.highlighted-event>p", "nisp.main.basedOn")
        }
        "render page with text  '7 june 2020' " in {
          assertEqualsValue(htmlAccountDoc, "article.content__body>div.highlighted-event>p:nth-child(1)>span:nth-child(1)", Dates.formatDate(new LocalDate(2020, 6, 7)) + ".")
        }
        "render page with text  'Your forecast is' " in {
          val sMessage = Messages("nisp.main.caveats") + " " + Messages("nisp.is")
          assertEqualsValue(htmlAccountDoc, "article.content__body>div.highlighted-event>p:nth-child(1)>span:nth-child(2)", sMessage)
        }

        "render page with text  '£150.71 a week" in {
          val sWeek = "£150.71 " + Messages("nisp.main.week")
          assertEqualsValue(htmlAccountDoc, "article.content__body>div.highlighted-event>p:nth-child(2)>em", sWeek)
        }
        "render page with text  ' £590.10 a month, £7,081.15 a year '" in {
          val sForecastAmount = "£590.10 " + Messages("nisp.main.month") + ", £7,081.15 " + Messages("nisp.main.year")
          assertEqualsValue(htmlAccountDoc, "article.content__body>div.highlighted-event>p:nth-child(3)", sForecastAmount)
        }
        "render page with text  ' Your forcaste '" in {
          assertEqualsMessage(htmlAccountDoc, "article.content__body p:nth-child(5)", "nisp.main.caveats")
        }
        "render page with text  ' is not a guarantee and is based on the current law '" in {
          assertEqualsMessage(htmlAccountDoc, "article.content__body ul.list-bullet li:nth-child(1)", "nisp.main.notAGuarantee")
        }
        "render page with text  ' is based on your National Insurance record up to 5 April 2016 '" in {
          assertContainsDynamicMessage(htmlAccountDoc, "article.content__body ul.list-bullet li:nth-child(2)", "nisp.main.isBased", Dates.formatDate(new LocalDate(2016, 4, 5)))
        }
        "render page with text  ' does not include any increase due to inflation '" in {
          assertEqualsMessage(htmlAccountDoc, "article.content__body ul.list-bullet li:nth-child(3)", "nisp.main.inflation")
        }

        "render page with Heading  ' £155.55 is the most you can get'" in {
          val sMaxCanGet = "£150.71 " + Messages("nisp.main.mostYouCanGet")
          assertEqualsValue(htmlAccountDoc, "article.content__body h2", sMaxCanGet)
        }
        "render page with text  'You cannot improve your forecast any further.'" in {
          assertEqualsMessage(htmlAccountDoc, "article.content__body p:nth-child(8)", "nisp.main.cantImprove")
        }
        "render page with text  'If you’re working you may still need to pay National Insurance contributions until 7 June 2020 as they fund other state benefits and the NHS.'" in {
          assertContainsDynamicMessage(htmlAccountDoc, "article.content__body p:nth-child(9)", "nisp.main.context.reachMax.needToPay", Dates.formatDate(new LocalDate(2020, 6, 7)))
        }
        "render page with link  'View your National Insurence Record'" in {
          assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(10)", "nisp.main.showyourrecord")
        }
        "render page with href link  'View your National Insurence Record'" in {
          assertLinkHasValue(htmlAccountDoc, "article.content__body>a:nth-child(10)", "/check-your-state-pension/account/nirecord")
        }

        /*overseas message*/
        "render page with text  'As you are living or working overseas (opens in new tab), you may be entitled to a " +
          "State Pension from the country you are living or working in.'" in {
          assertEqualsMessage(htmlAccountDoc, "article.content__body div.panel-indent p", "nisp.main.overseas")
        }
        /*Ends*/

        /*Start of Non SPA Checks*/
        "NOT render page with heading 'Proposed change to your State Pension age'" in {
          assertPageDoesNotContainMessage(htmlAccountDoc, "nisp.spa.under.consideration.title")
        }

        "Not render page with text 'Youll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
          assertPageDoesNotContainDynamicMessage(htmlAccountDoc, "nisp.spa.under.consideration.detail", "7 June 2020")
        }
        /*Ends*/

        "render page with heading  'Putting of claiming'" in {
          assertEqualsMessage(htmlAccountDoc, "article.content__body h2:nth-child(12)", "nisp.main.puttingOff")
        }

        "render page with text  'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
          assertContainsDynamicMessage(htmlAccountDoc, "article.content__body p:nth-child(13)", "nisp.main.puttingOff.line1", Dates.formatDate(new LocalDate(2020, 6, 7)))
        }

        "render page with link 'More on putting off claiming (opens in new tab)'" in {
          assertEqualsMessage(htmlAccountDoc, "article.content__body a:nth-child(14)", "nisp.main.puttingOff.linkTitle")
        }
        "render page with href link 'More on putting off claiming (opens in new tab)'" in {
          assertLinkHasValue(htmlAccountDoc, "article.content__body a:nth-child(14)", "https://www.gov.uk/deferring-state-pension")
        }

        /*Side bar help*/
        "render page with heading  'Get help'" in {
          assertEqualsMessage(htmlAccountDoc, "aside.sidebar >div.helpline-sidebar>h2", "nisp.nirecord.helpline.getHelp")
        }
        "render page with text  'Helpline 0800 731 0181'" in {
          assertEqualsMessage(htmlAccountDoc, "aside.sidebar >div.helpline-sidebar>p:nth-child(2)", "nisp.nirecord.helpline.number")
        }
        "render page with text  'Textphone 0800 731 0176'" in {
          assertEqualsMessage(htmlAccountDoc, "aside.sidebar >div.helpline-sidebar>p:nth-child(3)", "nisp.nirecord.helpline.textNumber")
        }
        "render page with text  'Monday to Friday: 8am to 6pm'" in {
          assertEqualsMessage(htmlAccountDoc, "aside.sidebar >div.helpline-sidebar>p:nth-child(4)", "nisp.nirecord.helpline.openTimes")
        }
      }

      "State Pension page with forecast only: With State Pension age under consideration message" should {

        lazy val controller = createStatePensionController
        when(controller.statePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
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
            currentFullWeeklyPensionAmount = 155.65,
            reducedRateElection = false,
            statePensionAgeUnderConsideration = true
          )
          )))

        when(controller.nationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Right(NationalInsuranceRecord(
            qualifyingYears = 11,
            qualifyingYearsPriorTo1975 = 0,
            numberOfGaps = 1,
            numberOfGapsPayable = 1,
            Some(new LocalDate(1954, 3, 6)),
            false,
            new LocalDate(2017, 4, 5),
            List(

              NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
              NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
              NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
            ),
            reducedRateElection = false
          )
          )))

        lazy val result = controller.show()(authenticatedFakeRequest(mockUserIdForecastOnly).withCookies(lanCookie))


        lazy val htmlAccountDoc = asDocument(contentAsString(result))

        "render page with text  'As you are living or working overseas (opens in new tab), you may be entitled to a " +
          "State Pension from the country you are living or working in.'" in {
          assertEqualsMessage(htmlAccountDoc, "article.content__body>div.panel-indent:nth-child(11)>p", "nisp.main.overseas")
        }

        "render page with heading  'Proposed change to your State Pension age'" in {
          assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(12)", "nisp.spa.under.consideration.title")
        }

        "render page with text  'Youll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
          assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(13)", "nisp.spa.under.consideration.detail", Dates.formatDate(new LocalDate(2020, 6, 7)))
        }

        //deferral message
        "render page with heading  'Putting of claiming'" in {
          assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(14)", "nisp.main.puttingOff")
        }

        "render page with text  'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
          assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(15)", "nisp.main.puttingOff.line1", Dates.formatDate(new LocalDate(2020, 6, 7)))
        }

        "render page with link 'More on putting off claiming (opens in new tab)'" in {
          assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(16)", "nisp.main.puttingOff.linkTitle")
        }
        "render page with href link 'More on putting off claiming (opens in new tab)'" in {
          assertLinkHasValue(htmlAccountDoc, "article.content__body>a:nth-child(16)", "https://www.gov.uk/deferring-state-pension")
        }
      }

      "The scenario is continue working" when {

        "State Pension page with MQP : Continue Working || Fill Gaps || Full Rate" should {
          lazy val controller = createStatePensionController
          when(controller.statePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
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
              currentFullWeeklyPensionAmount = 155.65,
              false,
              false
            )
            )))

          when(controller.nationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(Right(NationalInsuranceRecord(
              qualifyingYears = 4,
              qualifyingYearsPriorTo1975 = 0,
              numberOfGaps = 2,
              numberOfGapsPayable = 2,
              Some(new LocalDate(1954, 3, 6)),
              false,
              new LocalDate(2017, 4, 5),
              List(

                NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
              ),
              reducedRateElection = false
            )
            )))

          lazy val result = controller.show()(authenticatedFakeRequest(mockUserIdForecastOnly).withCookies(lanCookie))

          lazy val htmlAccountDoc = asDocument(contentAsString(result))

          "render with correct page title" in {
            assertElementContainsText(htmlAccountDoc, "head>title" ,messages("nisp.main.h1.title") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
          }
          "render page with heading  'Your State Pension' " in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>h1.titleWithPAgeInfo", "nisp.main.h1.title")
          }

          "render page with text  'You can get your State Pension on' " in {
            assertElemetsOwnMessage(htmlAccountDoc, "article.content__body>div:nth-child(4)>p", "nisp.main.basedOn")
          }
          "render page with text  '7 june 2020' " in {
            assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(4)>p:nth-child(1)>span:nth-child(1)", Dates.formatDate(new LocalDate(2020, 6, 7)) + ".")
          }
          "render page with text  'Your forecast is' " in {
            val sMessage = Messages("nisp.main.caveats") + " " + Messages("nisp.is")
            assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(4)>p:nth-child(1)>span:nth-child(2)", sMessage)
          }

          "render page with text  '£137.86 a week" in {
            val sWeek = "£137.86 " + Messages("nisp.main.week")
            assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(4)>p:nth-child(2)>em", sWeek)
          }
          "render page with text  ' £599.44 a month, £7,193.34 a year '" in {
            val sForecastAmount = "£599.44 " + Messages("nisp.main.month") + ", £7,193.34 " + Messages("nisp.main.year")
            assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(4)>p:nth-child(3)", sForecastAmount)
          }
          "render page with text  ' Your forcaste '" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(5)", "nisp.main.caveats")
          }
          "render page with text  ' is not a guarantee and is based on the current law '" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>ul:nth-child(6)>li:nth-child(1)", "nisp.main.notAGuarantee")
          }
          "render page with text  ' is based on your National Insurance record up to 5 April 2016 '" in {
            assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>ul:nth-child(6)>li:nth-child(2)", "nisp.main.isBased", Dates.formatDate(new LocalDate(2016, 4, 5)))
          }
          "render page with text  ' assumes that you’ll contribute another 4 years '" in {
            assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>ul:nth-child(6)>li:nth-child(3)", "nisp.mqp.howManyToContribute", Time.years(4))
          }

          "render page with text  ' does not include any increase due to inflation '" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>ul:nth-child(6)>li:nth-child(4)", "nisp.main.inflation")
          }
          "render page with text  ' You currently have 4 years on your record and you need at least 10 years to get any State Pension. '" in {
            assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(7)", "nisp.mqp.youCurrentlyHave", Time.years(4).toString(), Constants.minimumQualifyingYearsNSP.toString(), null)
          }

          "render page with Heading  ' You can improve your forecast'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(8)", "nisp.main.context.fillGaps.improve.title")
          }
          "render page with text  'You have years on your record where you did not contribute enough National Insurance and you can make up the shortfall. " +
            "This will make these years count towards your pension and may improve your forecast.'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(9)", "nisp.main.context.improve.para1.plural")
          }
          "render page with link  'View gaps in your record'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(10)", "nisp.main.context.fillGaps.viewGaps")
          }
          "render page with href link  'View gaps in your record'" in {
            assertLinkHasValue(htmlAccountDoc, "article.content__body>a:nth-child(10)", "/check-your-state-pension/account/nirecord/gaps")
          }

          "render page with href text  'Your forecast may be different if there are any changes to your National Insurance information. There is more about this in the terms and conditions'" in {
            val sMessage = StringEscapeUtils.unescapeHtml4(Messages("nisp.legal.forecastChanges")) + " ."
            assertElemetsOwnText(htmlAccountDoc, "article.content__body>p:nth-child(11)", sMessage)
          }

          /*overseas message*/
          "render page with text 'As you are living or working overseas (opens in new tab), you may be entitled to a " +
            "State Pension from the country you are living or working in.'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>div.panel-indent:nth-child(12)>p", "nisp.main.overseas")
          }
          /*Ends*/

          /*Start of Non SPA Checks*/
          "Not render page with heading 'Proposed change to your State Pension age'" in {
            assertPageDoesNotContainMessage(htmlAccountDoc,"nisp.spa.under.consideration.title")
          }

          "Not render page with text 'Youll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
            assertPageDoesNotContainDynamicMessage(htmlAccountDoc, "nisp.spa.under.consideration.detail", Dates.formatDate(new LocalDate(2020, 6, 7)))
          }
          /*Ends*/

          "render page with heading 'Putting of claiming'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(13)", "nisp.main.puttingOff")
          }

          "render page with text 'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(14)", "nisp.main.puttingOff.line1", Dates.formatDate(new LocalDate(2020, 6, 7)))
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(15)", "nisp.main.puttingOff.linkTitle")
          }
          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            assertLinkHasValue(htmlAccountDoc, "article.content__body>a:nth-child(15)", "https://www.gov.uk/deferring-state-pension")
          }

          /*Side bar help*/
          "render page with heading 'Get help'" in {
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

        "State Pension page with MQP : Continue Working || Fill Gaps || Full Rate: With State Pension age under consideration message" should {
          lazy val controller = createStatePensionController
          when(controller.statePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
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
              currentFullWeeklyPensionAmount = 155.65,
              reducedRateElection = false,
              statePensionAgeUnderConsideration = true
            )
            )))

          when(controller.nationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(Right(NationalInsuranceRecord(
              qualifyingYears = 4,
              qualifyingYearsPriorTo1975 = 0,
              numberOfGaps = 2,
              numberOfGapsPayable = 2,
              Some(new LocalDate(1954, 3, 6)),
              false,
              new LocalDate(2017, 4, 5),
              List(

                NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
              ),
              reducedRateElection = false
            )
            )))

          lazy val result = controller.show()(authenticatedFakeRequest(mockUserIdForecastOnly).withCookies(lanCookie))

          lazy val htmlAccountDoc = asDocument(contentAsString(result))

          //overseas message
          "render page with text  'As you are living or working overseas (opens in new tab), you may be entitled to a " +
            "State Pension from the country you are living or working in.'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>div.panel-indent:nth-child(12)>p", "nisp.main.overseas")
          }

          // SPA under consideration message
          "render page with heading  'Proposed change to your State Pension age'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(13)", "nisp.spa.under.consideration.title")
          }

          "render page with text  'Youll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
            assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(14)", "nisp.spa.under.consideration.detail", Dates.formatDate(new LocalDate(2020, 6, 7)))
          }

          //deferral message
          "render page with heading  'Putting of claiming'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(15)", "nisp.main.puttingOff")
          }

          "render page with text  'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(16)", "nisp.main.puttingOff.line1", Dates.formatDate(new LocalDate(2020, 6, 7)))
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(17)", "nisp.main.puttingOff.linkTitle")
          }
          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            assertLinkHasValue(htmlAccountDoc, "article.content__body>a:nth-child(17)", "https://www.gov.uk/deferring-state-pension")
          }
        }

        "State Pension page with MQP : Continue Working || no gaps || Full Rate" should {

          lazy val controller = createStatePensionController
          when(controller.statePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
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
              currentFullWeeklyPensionAmount = 155.65,
              false,
              false
            )
            )))

          when(controller.nationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(Right(NationalInsuranceRecord(
              qualifyingYears = 4,
              qualifyingYearsPriorTo1975 = 0,
              numberOfGaps = 0,
              numberOfGapsPayable = 0,
              Some(new LocalDate(1954, 3, 6)),
              false,
              new LocalDate(2017, 4, 5),
              List(

                NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
              ),
              reducedRateElection = false
            )
            )))

          lazy val result = controller.show()(authenticatedFakeRequest(mockUserIdForecastOnly).withCookies(lanCookie))

          lazy val htmlAccountDoc = asDocument(contentAsString(result))

          "render with correct page title" in {
            assertElementContainsText(htmlAccountDoc, "head>title" ,messages("nisp.main.h1.title") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
          }
          "render page with heading  'Your State Pension' " in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>h1.titleWithPAgeInfo", "nisp.main.h1.title")
          }

          "render page with text  'You can get your State Pension on' " in {
            assertElemetsOwnMessage(htmlAccountDoc, "article.content__body>div:nth-child(4)>p", "nisp.main.basedOn")
          }
          "render page with text  '7 june 2020' " in {
            assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(4)>p:nth-child(1)>span:nth-child(1)", Dates.formatDate(new LocalDate(2020, 6, 7)) + ".")
          }
          "render page with text  'Your forecast is' " in {
            val sMessage = Messages("nisp.main.caveats") + " " + Messages("nisp.is")
            assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(4)>p:nth-child(1)>span:nth-child(2)", sMessage)
          }

          "render page with text  '£155.65 a week" in {
            val sWeek = "£155.65 " + Messages("nisp.main.week")
            assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(4)>p:nth-child(2)>em", sWeek)
          }
          "render page with text  ' £599.44 a month, £7,193.34 a year '" in {
            val sForecastAmount = "£599.44 " + Messages("nisp.main.month") + ", £7,193.34 " + Messages("nisp.main.year")
            assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(4)>p:nth-child(3)", sForecastAmount)
          }
          "render page with text  ' Your forcaste '" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(5)", "nisp.main.caveats")
          }
          "render page with text  ' is not a guarantee and is based on the current law '" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>ul:nth-child(6)>li:nth-child(1)", "nisp.main.notAGuarantee")
          }
          "render page with text  ' is based on your National Insurance record up to 5 April 2016 '" in {
            assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>ul:nth-child(6)>li:nth-child(2)", "nisp.main.isBased", Dates.formatDate(new LocalDate(2016, 4, 5)))
          }
          "render page with text  ' assumes that you’ll contribute another 4 years '" in {
            assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>ul:nth-child(6)>li:nth-child(3)", "nisp.mqp.howManyToContribute", Time.years(4))
          }

          "render page with text  ' does not include any increase due to inflation '" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>ul:nth-child(6)>li:nth-child(4)", "nisp.main.inflation")
          }
          "render page with text  ' You currently have 4 years on your record and you need at least 10 years to get any State Pension. '" in {
            assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(7)", "nisp.mqp.youCurrentlyHave", Time.years(4).toString(), Constants.minimumQualifyingYearsNSP.toString(), null)
          }

          "render page with Heading  '£155.65 is the most you can get'" in {
            val sMessgae = "£155.65 " + StringEscapeUtils.unescapeHtml4(Messages("nisp.main.mostYouCanGet"))
            assertEqualsValue(htmlAccountDoc, "article.content__body>h3:nth-child(8)", sMessgae)
          }

          "render page with text  'You cannot improve your forecast any further.'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(9)", "nisp.main.context.willReach")
          }
          "render page with text  'If you’re working you may still need to pay National Insurance contributions until 7 June 2020 as they fund other state benefits and the NHS.'" in {
            assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(10)", "nisp.main.context.reachMax.needToPay", Dates.formatDate(new LocalDate(2020, 6, 7)))
          }
          "render page with link  'View your National Insurence Record'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(11)", "nisp.main.showyourrecord")
          }
          "render page with href link 'View your National Insurence Record'" in {
            assertLinkHasValue(htmlAccountDoc, "article.content__body>a:nth-child(11)", "/check-your-state-pension/account/nirecord")
          }

          "render page with href text 'Your forecast may be different if there are any changes to your National Insurance information. There is more about this in the terms and conditions'" in {
            val sMessage = StringEscapeUtils.unescapeHtml4(Messages("nisp.legal.forecastChanges")) + " ."
            assertElemetsOwnText(htmlAccountDoc, "article.content__body>p:nth-child(12)", sMessage)
          }

          /*overseas message*/
          "render page with text 'As you are living or working overseas (opens in new tab), you may be entitled to a " +
            "State Pension from the country you are living or working in.'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>div.panel-indent:nth-child(13)>p", "nisp.main.overseas")
          }

          "Not render page with heading 'Proposed change to your State Pension age'" in {
            assertPageDoesNotContainMessage(htmlAccountDoc, "nisp.spa.under.consideration.title")
          }

          "Not render page with text 'Youll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
            assertPageDoesNotContainDynamicMessage(htmlAccountDoc, "nisp.spa.under.consideration.detail", Dates.formatDate(new LocalDate(2020, 6, 7)))
          }

          "render page with heading 'Putting of claiming'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(14)", "nisp.main.puttingOff")
          }

          "render page with text 'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(15)", "nisp.main.puttingOff.line1", Dates.formatDate(new LocalDate(2020, 6, 7)))
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(16)", "nisp.main.puttingOff.linkTitle")
          }
          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            assertLinkHasValue(htmlAccountDoc, "article.content__body>a:nth-child(16)", "https://www.gov.uk/deferring-state-pension")
          }
        }

        "State Pension page with MQP : Continue Working || no gaps || Full Rate: With State Pension age under consideration message" should {

          lazy val controller = createStatePensionController
          when(controller.statePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
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
              currentFullWeeklyPensionAmount = 155.65,
              reducedRateElection = false,
              statePensionAgeUnderConsideration = true
            )
            )))

          when(controller.nationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(Right(NationalInsuranceRecord(
              qualifyingYears = 4,
              qualifyingYearsPriorTo1975 = 0,
              numberOfGaps = 0,
              numberOfGapsPayable = 0,
              Some(new LocalDate(1954, 3, 6)),
              false,
              new LocalDate(2017, 4, 5),
              List(

                NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
              ),
              reducedRateElection = false
            )
            )))

          lazy val result = controller.show()(authenticatedFakeRequest(mockUserIdForecastOnly).withCookies(lanCookie))

          lazy val htmlAccountDoc = asDocument(contentAsString(result))

          //overseas message
          "render page with text  'As you are living or working overseas (opens in new tab), you may be entitled to a " +
            "State Pension from the country you are living or working in.'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>div.panel-indent:nth-child(13)>p", "nisp.main.overseas")
          }

          "render page with heading  'Proposed change to your State Pension age'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(14)", "nisp.spa.under.consideration.title")
          }

          "render page with text  'Youll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
            assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(15)", "nisp.spa.under.consideration.detail", Dates.formatDate(new LocalDate(2020, 6, 7)))
          }

          "render page with heading  'Putting of claiming'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(16)", "nisp.main.puttingOff")
          }

          "render page with text  'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(17)", "nisp.main.puttingOff.line1", Dates.formatDate(new LocalDate(2020, 6, 7)))
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(18)", "nisp.main.puttingOff.linkTitle")
          }
          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            assertLinkHasValue(htmlAccountDoc, "article.content__body>a:nth-child(18)", "https://www.gov.uk/deferring-state-pension")
          }
        }

        "State Pension page with MQP : Continue Working || 0 Qualify Years || has fillable Gaps ||  Personal Max" should {

          lazy val controller = createStatePensionController
          when(controller.statePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
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
              currentFullWeeklyPensionAmount = 155.65,
              false,
              false
            )
            )))

          when(controller.nationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(Right(NationalInsuranceRecord(
              qualifyingYears = 0,
              qualifyingYearsPriorTo1975 = 0,
              numberOfGaps = 1,
              numberOfGapsPayable = 1,
              Some(new LocalDate(1954, 3, 6)),
              false,
              new LocalDate(2017, 4, 5),
              List(

                NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
              ),
              reducedRateElection = false
            )
            )))

          lazy val result = controller.show()(authenticatedFakeRequest(mockUserIdForecastOnly).withCookies(lanCookie))

          lazy val htmlAccountDoc = asDocument(contentAsString(result))

          "render with correct page title" in {
            assertElementContainsText(htmlAccountDoc, "head>title" ,messages("nisp.main.h1.title") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
          }
          "render page with heading  'Your State Pension' " in {

            assertEqualsMessage(htmlAccountDoc, "article.content__body>h1.titleWithPAgeInfo", "nisp.main.h1.title")
          }

          "render page with text  'You can get your State Pension on' " in {
            assertElemetsOwnMessage(htmlAccountDoc, "article.content__body>div:nth-child(4)>p", "nisp.main.basedOn")
          }
          "render page with text  '7 june 2020' " in {
            assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(4)>p:nth-child(1)>span:nth-child(1)", Dates.formatDate(new LocalDate(2020, 6, 7)) + ".")
          }
          "render page with text  'Your forecast is' " in {
            val sMessage = Messages("nisp.main.caveats") + " " + Messages("nisp.is")
            assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(4)>p:nth-child(1)>span:nth-child(2)", sMessage)
          }

          "render page with text  '£155.65 a week" in {
            val sWeek = "£155.65 " + Messages("nisp.main.week")
            assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(4)>p:nth-child(2)>em", sWeek)
          }
          "render page with text  ' £599.44 a month, £7,193.34 a year '" in {
            val sForecastAmount = "£599.44 " + Messages("nisp.main.month") + ", £7,193.34 " + Messages("nisp.main.year")
            assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(4)>p:nth-child(3)", sForecastAmount)
          }
          "render page with text  ' Your forcaste '" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(5)", "nisp.main.caveats")
          }
          "render page with text  ' is not a guarantee and is based on the current law '" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>ul:nth-child(6)>li:nth-child(1)", "nisp.main.notAGuarantee")
          }
          "render page with text  ' is based on your National Insurance record up to 5 April 2016 '" in {
            assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>ul:nth-child(6)>li:nth-child(2)", "nisp.main.isBased", Dates.formatDate(new LocalDate(2016, 4, 5)))
          }
          "render page with text  ' assumes that you’ll contribute another 4 years '" in {
            assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>ul:nth-child(6)>li:nth-child(3)", "nisp.mqp.howManyToContribute", Time.years(4))
          }

          "render page with text  ' does not include any increase due to inflation '" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>ul:nth-child(6)>li:nth-child(4)", "nisp.main.inflation")
          }
          "render page with text  'You do not have any years on your record and you need at least 10 years to get any State Pension. '" in {
            assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(7)", "nisp.mqp.youCurrentlyHaveZero", Constants.minimumQualifyingYearsNSP.toString())
          }

          "render page with Heading  '£155.65 is the most you can get'" in {
            val sMessgae = "£155.65 " + Messages("nisp.main.mostYouCanGet")
            assertEqualsValue(htmlAccountDoc, "article.content__body>h3:nth-child(8)", sMessgae)
          }

          "render page with text  'You cannot improve your forecast any further, unless you choose to put off claimimg'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(9)", "nisp.main.context.willReach")
          }
          "render page with text  'If you’re working you may still need to pay National Insurance contributions until 7 June 2020 as they fund other state benefits and the NHS.'" in {
            assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(10)", "nisp.main.context.reachMax.needToPay", Dates.formatDate(new LocalDate(2020, 6, 7)))
          }
          "render page with link  'View your National Insurence Record'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(11)", "nisp.main.showyourrecord")
          }
          "render page with href link  'View your National Insurence Record'" in {
            assertLinkHasValue(htmlAccountDoc, "article.content__body>a:nth-child(11)", "/check-your-state-pension/account/nirecord")
          }

          "render page with href text  'Your forecast may be different if there are any changes to your National Insurance information. There is more about this in the terms and conditions'" in {
            val sMessage = StringEscapeUtils.unescapeHtml4(Messages("nisp.legal.forecastChanges")) + " ."
            assertElemetsOwnText(htmlAccountDoc, "article.content__body>p:nth-child(12)", sMessage)
          }

          /*overseas message*/
          "render page with text 'As you are living or working overseas (opens in new tab), you may be entitled to a " +
            "State Pension from the country you are living or working in.'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>div.panel-indent:nth-child(13)>p", "nisp.main.overseas")
          }
          /*Ends*/

          /*Start of Non SPA Checks*/
          "Not render page with heading 'Proposed change to your State Pension age'" in {
            assertPageDoesNotContainMessage(htmlAccountDoc, "nisp.spa.under.consideration.title")
          }

          "Not render page with text 'Youll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
            assertPageDoesNotContainDynamicMessage(htmlAccountDoc, "nisp.spa.under.consideration.detail", Dates.formatDate(new LocalDate(2020, 6, 7)))
          }
          /*Ends*/

          "render page with heading 'Putting of claiming'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(14)", "nisp.main.puttingOff")
          }

          "render page with text 'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(15)", "nisp.main.puttingOff.line1", Dates.formatDate(new LocalDate(2020, 6, 7)))
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(16)", "nisp.main.puttingOff.linkTitle")
          }
          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            assertLinkHasValue(htmlAccountDoc, "article.content__body>a:nth-child(16)", "https://www.gov.uk/deferring-state-pension")
          }
        }

        "State Pension page with MQP : Continue Working || 0 Qualify Years || has fillable Gaps ||  Personal Max: With State Pension age under consideration message" should {

          lazy val controller = createStatePensionController
          when(controller.statePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
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
              currentFullWeeklyPensionAmount = 155.65,
              reducedRateElection = false,
              statePensionAgeUnderConsideration = true
            )
            )))

          when(controller.nationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(Right(NationalInsuranceRecord(
              qualifyingYears = 0,
              qualifyingYearsPriorTo1975 = 0,
              numberOfGaps = 1,
              numberOfGapsPayable = 1,
              Some(new LocalDate(1954, 3, 6)),
              false,
              new LocalDate(2017, 4, 5),
              List(

                NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
              ),
              reducedRateElection = false
            )
            )))

          lazy val result = controller.show()(authenticatedFakeRequest(mockUserIdForecastOnly).withCookies(lanCookie))

          lazy val htmlAccountDoc = asDocument(contentAsString(result))

          //overseas message
          "render page with text  'As you are living or working overseas (opens in new tab), you may be entitled to a " +
            "State Pension from the country you are living or working in.'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>div.panel-indent:nth-child(13)>p", "nisp.main.overseas")
          }

          "render page with heading  'Proposed change to your State Pension age'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(14)", "nisp.spa.under.consideration.title")
          }

          "render page with text  'Youll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
            assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(15)", "nisp.spa.under.consideration.detail", Dates.formatDate(new LocalDate(2020, 6, 7)))
          }

          "render page with heading  'Putting of claiming'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(16)", "nisp.main.puttingOff")
          }

          "render page with text  'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(17)", "nisp.main.puttingOff.line1", Dates.formatDate(new LocalDate(2020, 6, 7)))
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(18)", "nisp.main.puttingOff.linkTitle")
          }
          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            assertLinkHasValue(htmlAccountDoc, "article.content__body>a:nth-child(18)", "https://www.gov.uk/deferring-state-pension")
          }
        }

        "State Pension page with MQP : Continue Working || 9 Qualify Years || cant fill gaps ||  Personal Max" should {

          lazy val controller = createStatePensionController
          when(controller.statePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
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
              currentFullWeeklyPensionAmount = 155.65,
              false,
              false
            )
            )))

          when(controller.nationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(Right(NationalInsuranceRecord(
              qualifyingYears = 9,
              qualifyingYearsPriorTo1975 = 0,
              numberOfGaps = 0,
              numberOfGapsPayable = 0,
              Some(new LocalDate(1954, 3, 6)),
              false,
              new LocalDate(2017, 4, 5),
              List(
                NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
              ),
              reducedRateElection = false
            )
            )))

          lazy val result = controller.show()(authenticatedFakeRequest(mockUserIdForecastOnly).withCookies(lanCookie))

          lazy val htmlAccountDoc = asDocument(contentAsString(result))

          "render with correct page title" in {
            assertElementContainsText(htmlAccountDoc, "head>title" ,messages("nisp.main.h1.title") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
          }
          "render page with heading  'Your State Pension' " in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>h1.titleWithPAgeInfo", "nisp.main.h1.title")
          }

          "render page with text  'You can get your State Pension on' " in {
            assertElemetsOwnMessage(htmlAccountDoc, "article.content__body>div:nth-child(4)>p", "nisp.main.basedOn")
          }
          "render page with text  '7 june 2020' " in {
            assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(4)>p:nth-child(1)>span:nth-child(1)", Dates.formatDate(new LocalDate(2020, 6, 7)) + ".")
          }
          "render page with text  'Your forecast is' " in {
            val sMessage = Messages("nisp.main.caveats") + " " + Messages("nisp.is")
            assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(4)>p:nth-child(1)>span:nth-child(2)", sMessage)
          }

          "render page with text  '£155.65 a week" in {
            val sWeek = "£155.65 " + Messages("nisp.main.week")
            assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(4)>p:nth-child(2)>em", sWeek)
          }
          "render page with text  ' £599.44 a month, £7,193.34 a year '" in {
            val sForecastAmount = "£599.44 " + Messages("nisp.main.month") + ", £7,193.34 " + Messages("nisp.main.year")
            assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(4)>p:nth-child(3)", sForecastAmount)
          }
          "render page with text  ' Your forcaste '" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(5)", "nisp.main.caveats")
          }
          "render page with text  ' is not a guarantee and is based on the current law '" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>ul:nth-child(6)>li:nth-child(1)", "nisp.main.notAGuarantee")
          }
          "render page with text  ' is based on your National Insurance record up to 5 April 2016 '" in {
            assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>ul:nth-child(6)>li:nth-child(2)", "nisp.main.isBased", Dates.formatDate(new LocalDate(2016, 4, 5)))
          }
          "render page with text  ' assumes that you’ll contribute another 4 years '" in {
            assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>ul:nth-child(6)>li:nth-child(3)", "nisp.mqp.howManyToContribute", Time.years(4))
          }

          "render page with text  ' does not include any increase due to inflation '" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>ul:nth-child(6)>li:nth-child(4)", "nisp.main.inflation")
          }

          "render page with text  ' You currently have 9 years on your record and you need at least 10 years to get any State Pension. '" in {
            assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(7)", "nisp.mqp.youCurrentlyHave", Time.years(9).toString(), Constants.minimumQualifyingYearsNSP.toString(), null)
          }

          "render page with Heading  '£155.65 is the most you can get'" in {
            val sMessgae = "£155.65 " + Messages("nisp.main.mostYouCanGet")
            assertEqualsValue(htmlAccountDoc, "article.content__body>h3:nth-child(8)", sMessgae)
          }

          "render page with text  'You cannot improve your forecast any further, unless you choose to put off claimimg'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(9)", "nisp.main.context.willReach")
          }
          "render page with text  'If you’re working you may still need to pay National Insurance contributions until 7 June 2020 as they fund other state benefits and the NHS.'" in {
            assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(10)", "nisp.main.context.reachMax.needToPay", Dates.formatDate(new LocalDate(2020, 6, 7)))
          }
          "render page with link  'View your National Insurence Record'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(11)", "nisp.main.showyourrecord")
          }
          "render page with href link 'View your National Insurence Record'" in {
            assertLinkHasValue(htmlAccountDoc, "article.content__body>a:nth-child(11)", "/check-your-state-pension/account/nirecord")
          }

          "render page with href text 'Your forecast may be different if there are any changes to your National Insurance information. There is more about this in the terms and conditions''" in {
            val sMessage = StringEscapeUtils.unescapeHtml4(Messages("nisp.legal.forecastChanges")) + " ."
            assertElemetsOwnText(htmlAccountDoc, "article.content__body>p:nth-child(12)", sMessage)
          }

          /*overseas message*/
          "render page with text 'As you are living or working overseas (opens in new tab), you may be entitled to a " +
            "State Pension from the country you are living or working in.'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>div.panel-indent:nth-child(13)>p", "nisp.main.overseas")
          }
          /*Ends*/

          /*Start of Non SPA Checks*/
          "Not render page with heading 'Proposed change to your State Pension age'" in {
            assertPageDoesNotContainMessage(htmlAccountDoc, "nisp.spa.under.consideration.title")
          }

          "Not render page with text 'Youll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
            assertPageDoesNotContainDynamicMessage(htmlAccountDoc, "nisp.spa.under.consideration.detail", Dates.formatDate(new LocalDate(2020, 6, 7)))
          }

          "render page with heading  'Putting of claiming'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(14)", "nisp.main.puttingOff")
          }

          "render page with text  'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(15)", "nisp.main.puttingOff.line1", Dates.formatDate(new LocalDate(2020, 6, 7)))
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(16)", "nisp.main.puttingOff.linkTitle")
          }
          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            assertLinkHasValue(htmlAccountDoc, "article.content__body>a:nth-child(16)", "https://www.gov.uk/deferring-state-pension")
          }
        }

        "State Pension page with MQP : Continue Working || 9 Qualify Years || cant fill gaps ||  Personal Max: With State Pension age under consideration message" should {

          lazy val controller = createStatePensionController
          when(controller.statePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
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
              currentFullWeeklyPensionAmount = 155.65,
              reducedRateElection = false,
              statePensionAgeUnderConsideration = true
            )
            )))

          when(controller.nationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(Right(NationalInsuranceRecord(
              qualifyingYears = 9,
              qualifyingYearsPriorTo1975 = 0,
              numberOfGaps = 0,
              numberOfGapsPayable = 0,
              Some(new LocalDate(1954, 3, 6)),
              false,
              new LocalDate(2017, 4, 5),
              List(
                NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
              ),
              reducedRateElection = false
            )
            )))

          lazy val result = controller.show()(authenticatedFakeRequest(mockUserIdForecastOnly).withCookies(lanCookie))

          lazy val htmlAccountDoc = asDocument(contentAsString(result))

          //overseas message
          "render page with text  'As you are living or working overseas (opens in new tab), you may be entitled to a " +
            "State Pension from the country you are living or working in.'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>div.panel-indent:nth-child(13)>p", "nisp.main.overseas")
          }

          "render page with heading  'Proposed change to your State Pension age'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(14)", "nisp.spa.under.consideration.title")
          }

          "render page with text  'Youll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
            assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(15)", "nisp.spa.under.consideration.detail", Dates.formatDate(new LocalDate(2020, 6, 7)))
          }

          "render page with heading  'Putting of claiming'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(16)", "nisp.main.puttingOff")
          }

          "render page with text  'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(17)", "nisp.main.puttingOff.line1", Dates.formatDate(new LocalDate(2020, 6, 7)))
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>a:nth-child(18)", "nisp.main.puttingOff.linkTitle")
          }
          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            assertLinkHasValue(htmlAccountDoc, "article.content__body>a:nth-child(18)", "https://www.gov.uk/deferring-state-pension")
          }
        }
      }

      "The scenario is No years to contribute" when {

        "State Pension page with MQP :  No Gaps || Cant get pension" should {

          lazy val controller = createStatePensionController

          when(controller.statePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
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
              currentFullWeeklyPensionAmount = 155.65,
              false,
              false
            )
            )))

          when(controller.nationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(Right(NationalInsuranceRecord(
              qualifyingYears = 4,
              qualifyingYearsPriorTo1975 = 0,
              numberOfGaps = 0,
              numberOfGapsPayable = 0,
              Some(new LocalDate(1954, 3, 6)),
              false,
              new LocalDate(2017, 4, 5),
              List(

                NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
              ),
              reducedRateElection = false
            )
            )))

          lazy val result = controller.show()(authenticatedFakeRequest(mockUserIdForecastOnly).withCookies(lanCookie))

          lazy val htmlAccountDoc = asDocument(contentAsString(result))

          "render with correct page title" in {
            assertElementContainsText(htmlAccountDoc, "head>title" ,messages("nisp.main.h1.title") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
          }
          "render page with heading  'Your State Pension' " in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>h1.heading-large", "nisp.main.h1.title")
          }

          "render page with text  'You’ll reach State Pension age on ' " in {
            val sMessage = Messages("nisp.main.description.mqp")
            assertElemetsOwnText(htmlAccountDoc, "article.content__body>div:nth-child(2)>p", sMessage)
          }
          "render page with text  'You’ll reach State Pension age on 4 May 2017. ' " in {
            val sMessage = Dates.formatDate(new LocalDate(2017, 5, 4)) + "."
            assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(2)>p>span", sMessage)
          }

          "render page with text 'By this time, you will not be able to get the 10 years needed on your National Insurance record to get any State Pension.' " in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>div:nth-child(2)>p:nth-child(2)", "nisp.mqp.notPossible")
          }

          "render page with text 'What you can do next" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(3)", "nisp.mqp.doNext")
          }

          /*overseas message*/
          "render page with text 'As you are living or working overseas (opens in new tab), you may be entitled to a " +
            "State Pension from the country you are living or working in.'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(4)", "nisp.main.overseas")
          }
          /*Ends*/

          /*Start of Non SPA Checks*/
          "Not render page with heading 'Proposed change to your State Pension age'" in {
            assertPageDoesNotContainMessage(htmlAccountDoc, "nisp.spa.under.consideration.title")
          }

          "Not render page with text 'Youll reach State Pension age on 4 May 2017. Under government proposals this may increase by up to a year.'" in {
            assertPageDoesNotContainDynamicMessage(htmlAccountDoc, "nisp.spa.under.consideration.detail", Dates.formatDate(new LocalDate(2017, 5, 4)))
          }
          /*Ends*/

          "render page with text 'You do not have any years on your record that do not count towards your pension.'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(5)", "nisp.mqp.years.dontCount.zero")
          }
          "render page with link  'View your National Insurence Record'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(6)>a", "nisp.main.showyourrecord")
          }
          "render page with href link  'View your National Insurence Record'" in {
            assertLinkHasValue(htmlAccountDoc, "article.content__body>p:nth-child(6)>a", "/check-your-state-pension/account/nirecord")
          }

          "render page with text  'After State Pension age, 4 May 2017 you no longer pay National Insurance contributions.'" in {
            assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(7)", "nisp.mqp.afterSpa", Dates.formatDate(new LocalDate(2017, 5, 4)))
          }

          "render page with text  'These details may be different if there are any changes to your National Insurance information. There is more about this in the terms and conditions'" in {
            val sMessage = StringEscapeUtils.unescapeHtml4(Messages("nisp.legal.mqp.forecastChanges")) + " ."
            assertElemetsOwnText(htmlAccountDoc, "article.content__body>p:nth-child(8)", sMessage)
          }
          "render page with href text  'These details may be different if there are any changes to your National Insurance information. There is more about this in the terms and conditions'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(8)>a", "nisp.legal.termsAndCondition")
          }
          "render page with href link  'These details may be different if there are any changes to your National Insurance information. There is more about this in the terms and conditions'" in {
            assertLinkHasValue(htmlAccountDoc, "article.content__body>p:nth-child(8)>a", "/check-your-state-pension/terms-and-conditions?showBackLink=true")
          }

          "render page with link 'What else you can do'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(9)", "nisp.mqp.whatElse")
          }
          "render page with link 'You may be eligible for Pension Credit (opens in new tab)  if your retirement income is low.'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(10)", "nisp.mqp.pensionCredit")
          }
          "render page with href link 'You may be eligible for Pension Credit (opens in new tab)  if your retirement income is low'" in {
            assertLinkHasValue(htmlAccountDoc, "article.content__body>p:nth-child(10)>a", expectedPensionCreditOverviewLink)
          }
          "render page with link 'Contact the Money Advice Service (opens in new tab)  for free impartial advice.'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(11)", "nisp.mqp.moneyAdvice")
          }
          "render page with href link 'Contact the Money Advice Service (opens in new tab)  for free impartial advice.'" in {
            assertLinkHasValue(htmlAccountDoc, "article.content__body>p:nth-child(11)>a", expectedMoneyServiceLink)
          }

        }

        "State Pension page with MQP :  No Gaps || Cant get pension: With State Pension age under consideration message" should {

          lazy val controller = createStatePensionController

          when(controller.statePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
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
              currentFullWeeklyPensionAmount = 155.65,
              reducedRateElection = false,
              statePensionAgeUnderConsideration = true
            )
            )))

          when(controller.nationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(Right(NationalInsuranceRecord(
              qualifyingYears = 4,
              qualifyingYearsPriorTo1975 = 0,
              numberOfGaps = 0,
              numberOfGapsPayable = 0,
              Some(new LocalDate(1954, 3, 6)),
              false,
              new LocalDate(2017, 4, 5),
              List(

                NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
              ),
              reducedRateElection = false
            )
            )))

          lazy val result = controller.show()(authenticatedFakeRequest(mockUserIdForecastOnly).withCookies(lanCookie))

          lazy val htmlAccountDoc = asDocument(contentAsString(result))

          "render page with link 'Contact the Money Advice Service (opens in new tab)  for free impartial advice.'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(11)", "nisp.mqp.moneyAdvice")
          }
          "render page with href link 'Contact the Money Advice Service (opens in new tab)  for free impartial advice.'" in {
            assertLinkHasValue(htmlAccountDoc, "article.content__body>p:nth-child(11)>a", expectedMoneyServiceLink)
          }

          // SPA under consideration message
          "render page with heading  'Proposed change to your State Pension age'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(12)", "nisp.spa.under.consideration.title")
          }

          "render page with text  'Youll reach State Pension age on 4 May 2017. Under government proposals this may increase by up to a year.'" in {
            assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(13)", "nisp.spa.under.consideration.detail", Dates.formatDate(new LocalDate(2017, 5, 4)))
          }
        }

        "State Pension page with MQP :  has fillable Gaps || Personal Max" should {

          lazy val controller = createStatePensionController

          when(controller.statePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
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
              currentFullWeeklyPensionAmount = 155.65,
              false,
              false
            )
            )))

          when(controller.nationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(Right(NationalInsuranceRecord(
              qualifyingYears = 4,
              qualifyingYearsPriorTo1975 = 0,
              numberOfGaps = 2,
              numberOfGapsPayable = 2,
              Some(new LocalDate(1954, 3, 6)),
              false,
              new LocalDate(2017, 4, 5),
              List(

                NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
              ),
              reducedRateElection = false
            )
            )))
          lazy val result = controller.show()(authenticatedFakeRequest(mockUserIdForecastOnly).withCookies(lanCookie))

          lazy val htmlAccountDoc = asDocument(contentAsString(result))

          "render with correct page title" in {
            assertElementContainsText(htmlAccountDoc, "head>title" ,messages("nisp.main.h1.title") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
          }
          "render page with heading  'Your State Pension' " in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>h1.heading-large", "nisp.main.h1.title")
          }

          "render page with text  'You’ll reach State Pension age on ' " in {
            val sMessage = Messages("nisp.main.description.mqp")
            assertElemetsOwnText(htmlAccountDoc, "article.content__body>div:nth-child(2)>p", sMessage)
          }
          "render page with text  'You’ll reach State Pension age on 4 May 2018. ' " in {
            val sMessage = Dates.formatDate(new LocalDate(2018, 5, 4)) + "."
            assertEqualsValue(htmlAccountDoc, "article.content__body>div:nth-child(2)>p>span", sMessage)
          }

          "render page with text 'By this time, you will not have the 10 years needed on your National Insurance record to get any State Pension.' " in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>div:nth-child(2)>p:nth-child(2)", "nisp.mqp.possible")
          }

          "render page with text 'What you can do next" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(3)", "nisp.mqp.doNext")
          }

          /*overseas message*/
          "render page with text 'As you are living or working overseas (opens in new tab), you may be entitled to a " +
            "State Pension from the country you are living or working in.'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(4)", "nisp.main.overseas")
          }
          /*Ends*/

          /*Start of Non SPA Checks*/
          "Not render page with heading 'Proposed change to your State Pension age'" in {
            assertPageDoesNotContainMessage(htmlAccountDoc, "nisp.spa.under.consideration.title")
          }

          "Not render page with text 'Youll reach State Pension age on 4 May 2018. Under government proposals this may increase by up to a year.'" in {
            assertPageDoesNotContainDynamicMessage(htmlAccountDoc, "nisp.spa.under.consideration.detail", Dates.formatDate(new LocalDate(2018, 5, 4)))
          }
          /*Ends*/

          "render page with text 'You also have 2 years on your record which do not count towards your pension because you did not contribute enough National Insurance." +
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

          "render page with text  'After State Pension age, 4 May 2018 you no longer pay National Insurance contributions.'" in {
            assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(7)", "nisp.mqp.afterSpa", Dates.formatDate(new LocalDate(2018, 5, 4)))
          }

          "render page with href text  'These details may be different if there are any changes to your National Insurance information. There is more about this in the terms and conditions'" in {
            val sMessage = StringEscapeUtils.unescapeHtml4(Messages("nisp.legal.mqp.forecastChanges")) + " ."
            assertElemetsOwnText(htmlAccountDoc, "article.content__body>p:nth-child(8)", sMessage)
          }

          "render page with link 'What else you can do'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(9)", "nisp.mqp.whatElse")
          }
          "render page with link 'You may be eligible for Pension Credit (opens in new tab)  if your retirement income is low.'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(10)", "nisp.mqp.pensionCredit")
          }
          "render page with href link 'You may be eligible for Pension Credit (opens in new tab)  if your retirement income is low'" in {
            assertLinkHasValue(htmlAccountDoc, "article.content__body>p:nth-child(10)>a", expectedPensionCreditOverviewLink)
          }
          "render page with link 'Contact the Money Advice Service (opens in new tab)  for free impartial advice.'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(11)", "nisp.mqp.moneyAdvice")
          }
          "render page with href link 'Contact the Money Advice Service (opens in new tab)  for free impartial advice.'" in {
            assertLinkHasValue(htmlAccountDoc, "article.content__body>p:nth-child(11)>a", expectedMoneyServiceLink)
          }
        }

        "State Pension page with MQP :  has fillable Gaps || Personal Max: With State Pension age under consideration message" should {

          lazy val controller = createStatePensionController

          when(controller.statePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
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
              currentFullWeeklyPensionAmount = 155.65,
              reducedRateElection = false,
              statePensionAgeUnderConsideration = true
            )
            )))

          when(controller.nationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(Right(NationalInsuranceRecord(
              qualifyingYears = 4,
              qualifyingYearsPriorTo1975 = 0,
              numberOfGaps = 2,
              numberOfGapsPayable = 2,
              Some(new LocalDate(1954, 3, 6)),
              false,
              new LocalDate(2017, 4, 5),
              List(

                NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
              ),
              reducedRateElection = false
            )
            )))
          lazy val result = controller.show()(authenticatedFakeRequest(mockUserIdForecastOnly).withCookies(lanCookie))

          lazy val htmlAccountDoc = asDocument(contentAsString(result))

          "render page with link 'Contact the Money Advice Service (opens in new tab)  for free impartial advice.'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>p:nth-child(11)", "nisp.mqp.moneyAdvice")
          }
          "render page with href link 'Contact the Money Advice Service (opens in new tab)  for free impartial advice.'" in {
            assertLinkHasValue(htmlAccountDoc, "article.content__body>p:nth-child(11)>a", expectedMoneyServiceLink)
          }

          // SPA under consideration message
          "render page with heading  'Proposed change to your State Pension age'" in {
            assertEqualsMessage(htmlAccountDoc, "article.content__body>h2:nth-child(12)", "nisp.spa.under.consideration.title")
          }

          "render page with text  'Youll reach State Pension age on 4 May 2018. Under government proposals this may increase by up to a year.'" in {
            assertContainsDynamicMessage(htmlAccountDoc, "article.content__body>p:nth-child(13)", "nisp.spa.under.consideration.detail", Dates.formatDate(new LocalDate(2018, 5, 4)))
          }
        }
      }
    }
  }
}
