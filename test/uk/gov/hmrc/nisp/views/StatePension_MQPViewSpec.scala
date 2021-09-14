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

import java.util.UUID

import org.apache.commons.lang3.StringEscapeUtils
import java.time.LocalDate
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.contentAsString
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.nisp.builders.NationalInsuranceTaxYearBuilder
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.StatePensionController
import uk.gov.hmrc.nisp.controllers.auth.AuthAction
import uk.gov.hmrc.nisp.controllers.pertax.PertaxHelper
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.services.{NationalInsuranceService, StatePensionService}
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.nisp.utils.LanguageHelper.langUtils.Dates
import uk.gov.hmrc.nisp.views.formatting.Time
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}
import uk.gov.hmrc.renderer.TemplateRenderer

import scala.concurrent.Future

class StatePension_MQPViewSpec extends HtmlSpec with MockitoSugar with Injecting {

  val expectedMoneyServiceLink = "https://www.moneyadviceservice.org.uk/en"
  val expectedPensionCreditOverviewLink = "https://www.gov.uk/pension-credit/overview"

  def generateFakeRequest = FakeRequest().withSession(
    SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
    SessionKeys.lastRequestTimestamp -> LocalDate.now.toEpochDay.toString
  )

  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val mockNationalInsuranceService: NationalInsuranceService = mock[NationalInsuranceService]
  val mockStatePensionService: StatePensionService = mock[StatePensionService]
  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  val mockPertaxHelper: PertaxHelper = mock[PertaxHelper]

  val standardInjector = GuiceApplicationBuilder()
    .overrides(
      bind[AuthAction].to[FakeAuthAction],
      bind[StatePensionService].toInstance(mockStatePensionService),
      bind[NationalInsuranceService].toInstance(mockNationalInsuranceService),
      bind[AuditConnector].toInstance(mockAuditConnector),
      bind[ApplicationConfig].toInstance(mockAppConfig),
      bind[PertaxHelper].toInstance(mockPertaxHelper),
      bind[CachedStaticHtmlPartialRetriever].toInstance(FakeCachedStaticHtmlPartialRetriever),
      bind[FormPartialRetriever].to[FakePartialRetriever],
      bind[TemplateRenderer].toInstance(FakeTemplateRenderer)
    )
    .build()
    .injector

  val abroadUserInjector = GuiceApplicationBuilder()
    .overrides(
      bind[StatePensionService].toInstance(mockStatePensionService),
      bind[NationalInsuranceService].toInstance(mockNationalInsuranceService),
      bind[AuditConnector].toInstance(mockAuditConnector),
      bind[ApplicationConfig].toInstance(mockAppConfig),
      bind[PertaxHelper].toInstance(mockPertaxHelper),
      bind[CachedStaticHtmlPartialRetriever].toInstance(FakeCachedStaticHtmlPartialRetriever),
      bind[FormPartialRetriever].to[FakePartialRetriever],
      bind[TemplateRenderer].toInstance(FakeTemplateRenderer),
      bind[AuthAction].to[FakeAuthActionWithNino],
      bind[NinoContainer].toInstance(AbroadNinoContainer)
    )
    .build()
    .injector

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockStatePensionService, mockNationalInsuranceService, mockAuditConnector, mockAppConfig, mockPertaxHelper)
    when(mockPertaxHelper.isFromPertax(ArgumentMatchers.any())).thenReturn(Future.successful(false))
  }

  lazy val controller = standardInjector.instanceOf[StatePensionController]
  lazy val abroadUserController = abroadUserInjector.instanceOf[StatePensionController]

  "The State Pension page" when {

    "the user is a MQP" when {

      "State Pension page with forecast only" should {

        lazy val nonForeignDoc =
          asDocument(contentAsString(controller.show()(generateFakeRequest)))

        lazy val foreignDoc =
          asDocument(contentAsString(abroadUserController.show()(generateFakeRequest)))

        def mockSetup = {
          when(mockStatePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(Right(StatePension(
              LocalDate.of(2016, 4, 5),
              amounts = StatePensionAmounts(
                protectedPayment = false,
                StatePensionAmountRegular(151.71, 590.10, 7081.15),
                StatePensionAmountForecast(4, 150.71, 590.10, 7081.15),
                StatePensionAmountMaximum(4, 1, 149.71, 590.10, 7081.15),
                StatePensionAmountRegular(0, 0, 0)
              ),
              pensionAge = 67,
              LocalDate.of(2020, 6, 7),
              "2019-20",
              20,
              pensionSharingOrder = false,
              currentFullWeeklyPensionAmount = 155.65,
              false,
              false
            )
            )))

          when(mockNationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(Right(NationalInsuranceRecord(
              qualifyingYears = 11,
              qualifyingYearsPriorTo1975 = 0,
              numberOfGaps = 1,
              numberOfGapsPayable = 1,
              Some(LocalDate.of(1954, 3, 6)),
              false,
              LocalDate.of(2017, 4, 5),
              List(

                NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
              ),
              reducedRateElection = false
            )
            )))
        }

        "render with correct page title" in {
          mockSetup
          assertElementContainsText(nonForeignDoc, "head>title" ,messages("nisp.main.h1.title") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
        }
        "render page with heading  'Your State Pension' " in {
          mockSetup
          assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>h1.titleWithPAgeInfo", "nisp.main.h1.title")
        }

        "render page with text  'You can get your State Pension on' " in {
          mockSetup
          assertElemetsOwnMessage(nonForeignDoc, "article.content__body>div>div>div.highlighted-event>p", "nisp.main.basedOn")
        }
        "render page with text  '7 june 2020' " in {
          mockSetup
          assertEqualsValue(nonForeignDoc, "article.content__body>div>div>div.highlighted-event>p:nth-child(1)>span:nth-child(1)", Dates.formatDate(LocalDate.of(2020, 6, 7)) + ".")
        }
        "render page with text  'Your forecast is' " in {
          mockSetup
          val sMessage = Messages("nisp.main.caveats") + " " + Messages("nisp.is")
          assertEqualsValue(nonForeignDoc, "article.content__body>div>div>div.highlighted-event>p:nth-child(1)>span:nth-child(2)", sMessage)
        }

        "render page with text  '£150.71 a week" in {
          mockSetup
          val sWeek = "£150.71 " + Messages("nisp.main.week")
          assertEqualsValue(nonForeignDoc, "article.content__body>div>div>div.highlighted-event>p:nth-child(2)>em", sWeek)
        }
        "render page with text  ' £590.10 a month, £7,081.15 a year '" in {
          mockSetup
          val sForecastAmount = "£590.10 " + Messages("nisp.main.month") + ", £7,081.15 " + Messages("nisp.main.year")
          assertEqualsValue(nonForeignDoc, "article.content__body>div>div>div.highlighted-event>p:nth-child(3)", sForecastAmount)
        }
        "render page with text  ' Your forcaste '" in {
          mockSetup
          assertEqualsMessage(nonForeignDoc, "article.content__body p:nth-child(5)", "nisp.main.caveats")
        }
        "render page with text  ' is not a guarantee and is based on the current law '" in {
          mockSetup
          assertEqualsMessage(nonForeignDoc, "article.content__body ul.list-bullet li:nth-child(1)", "nisp.main.notAGuarantee")
        }
        "render page with text  ' is based on your National Insurance record up to 5 April 2016 '" in {
          mockSetup
          assertContainsDynamicMessage(nonForeignDoc, "article.content__body ul.list-bullet li:nth-child(2)", "nisp.main.isBased", Dates.formatDate(LocalDate.of(2016, 4, 5)))
        }
        "render page with text  ' does not include any increase due to inflation '" in {
          mockSetup
          assertEqualsMessage(nonForeignDoc, "article.content__body ul.list-bullet li:nth-child(3)", "nisp.main.inflation")
        }

        "render page with Heading  ' £155.55 is the most you can get'" in {
          mockSetup
          val sMaxCanGet = "£150.71 " + Messages("nisp.main.mostYouCanGet")
          assertEqualsValue(nonForeignDoc, "article.content__body h2", sMaxCanGet)
        }
        "render page with text  'You cannot improve your forecast any further.'" in {
          mockSetup
          assertEqualsMessage(nonForeignDoc, "article.content__body p:nth-child(8)", "nisp.main.cantImprove")
        }
        "render page with text  'If you’re working you may still need to pay National Insurance contributions until 7 June 2020 as they fund other state benefits and the NHS.'" in {
          mockSetup
          assertContainsDynamicMessage(foreignDoc, "article.content__body p:nth-child(9)", "nisp.main.context.reachMax.needToPay", Dates.formatDate(LocalDate.of(2020, 6, 7)))
        }
        "render page with link  'View your National Insurence Record'" in {
          mockSetup
          assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>a:nth-child(10)", "nisp.main.showyourrecord")
        }
        "render page with href link  'View your National Insurence Record'" in {
          mockSetup
          assertLinkHasValue(foreignDoc, "article.content__body>div>div>a:nth-child(10)", "/check-your-state-pension/account/nirecord")
        }

        /*overseas message*/
        "render page with text  'As you are living or working overseas (opens in new tab), you may be entitled to a " +
          "State Pension from the country you are living or working in.'" in {
          mockSetup
          assertEqualsMessage(foreignDoc, "article.content__body div.panel-indent p", "nisp.main.overseas")
        }
        /*Ends*/

        /*Start of Non SPA Checks*/
        "NOT render page with heading 'Proposed change to your State Pension age'" in {
          mockSetup
          assertPageDoesNotContainMessage(nonForeignDoc, "nisp.spa.under.consideration.title")
        }

        "Not render page with text 'Youll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
          mockSetup
          assertPageDoesNotContainDynamicMessage(foreignDoc, "nisp.spa.under.consideration.detail", "7 June 2020")
        }
        /*Ends*/

        "render page with heading  'Putting of claiming'" in {
          mockSetup
          assertEqualsMessage(foreignDoc, "article.content__body h2:nth-child(12)", "nisp.main.puttingOff")
        }

        "render page with text  'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
          mockSetup
          assertContainsDynamicMessage(foreignDoc, "article.content__body p:nth-child(13)", "nisp.main.puttingOff.line1", Dates.formatDate(LocalDate.of(2020, 6, 7)))
        }

        "render page with link 'More on putting off claiming (opens in new tab)'" in {
          mockSetup
          assertEqualsMessage(foreignDoc, "article.content__body a:nth-child(14)", "nisp.main.puttingOff.linkTitle")
        }
        "render page with href link 'More on putting off claiming (opens in new tab)'" in {
          mockSetup
          assertLinkHasValue(foreignDoc, "article.content__body a:nth-child(14)", "https://www.gov.uk/deferring-state-pension")
        }

        /*Side bar help*/
        "render page with heading  'Get help'" in {
          mockSetup
          assertEqualsMessage(nonForeignDoc, ".helpline-sidebar>h2", "nisp.nirecord.helpline.getHelp")
        }
        "render page with text  'Helpline 0800 731 0181'" in {
          mockSetup
          assertEqualsMessage(nonForeignDoc, ".helpline-sidebar>p:nth-child(2)", "nisp.nirecord.helpline.number")
        }
        "render page with text  'Textphone 0800 731 0176'" in {
          mockSetup
          assertEqualsMessage(nonForeignDoc, ".helpline-sidebar>p:nth-child(3)", "nisp.nirecord.helpline.textNumber")
        }
        "render page with text  'Monday to Friday: 8am to 6pm'" in {
          mockSetup
          assertEqualsMessage(nonForeignDoc, ".helpline-sidebar>p:nth-child(4)", "nisp.nirecord.helpline.openTimes")
        }
      }

      "State Pension page with forecast only: With State Pension age under consideration message" should {

        def mockSetup = {
          when(mockStatePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(Right(StatePension(
              LocalDate.of(2016, 4, 5),
              amounts = StatePensionAmounts(
                protectedPayment = false,
                StatePensionAmountRegular(151.71, 590.10, 7081.15),
                StatePensionAmountForecast(4, 150.71, 590.10, 7081.15),
                StatePensionAmountMaximum(4, 1, 149.71, 590.10, 7081.15),
                StatePensionAmountRegular(0, 0, 0)
              ),
              pensionAge = 67,
              LocalDate.of(2020, 6, 7),
              "2019-20",
              20,
              pensionSharingOrder = false,
              currentFullWeeklyPensionAmount = 155.65,
              reducedRateElection = false,
              statePensionAgeUnderConsideration = true
            )
            )))

          when(mockNationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(Right(NationalInsuranceRecord(
              qualifyingYears = 11,
              qualifyingYearsPriorTo1975 = 0,
              numberOfGaps = 1,
              numberOfGapsPayable = 1,
              Some(LocalDate.of(1954, 3, 6)),
              false,
              LocalDate.of(2017, 4, 5),
              List(

                NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
              ),
              reducedRateElection = false
            )
            )))
        }

        lazy val nonForeignDoc =
          asDocument(contentAsString(controller.show()(FakeRequest())))

        lazy val foreignDoc =
          asDocument(contentAsString(abroadUserController.show()(FakeRequest())))

        "render page with text  'As you are living or working overseas (opens in new tab), you may be entitled to a " +
          "State Pension from the country you are living or working in.'" in {
          mockSetup
          assertEqualsMessage(foreignDoc, "article.content__body>div>div>div.panel-indent:nth-child(11)>p", "nisp.main.overseas")
        }

        "render page with heading  'Proposed change to your State Pension age'" in {
          mockSetup
          assertEqualsMessage(foreignDoc, "article.content__body>div>div>h2:nth-child(12)", "nisp.spa.under.consideration.title")
        }

        "render page with text  'Youll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
          mockSetup
          assertContainsDynamicMessage(foreignDoc, "article.content__body>div>div>p:nth-child(13)", "nisp.spa.under.consideration.detail", Dates.formatDate(LocalDate.of(2020, 6, 7)))
        }

        //deferral message
        "render page with heading  'Putting of claiming'" in {
          mockSetup
          assertEqualsMessage(foreignDoc, "article.content__body>div>div>h2:nth-child(14)", "nisp.main.puttingOff")
        }

        "render page with text  'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
          mockSetup
          assertContainsDynamicMessage(foreignDoc, "article.content__body>div>div>p:nth-child(15)", "nisp.main.puttingOff.line1", Dates.formatDate(LocalDate.of(2020, 6, 7)))
        }

        "render page with link 'More on putting off claiming (opens in new tab)'" in {
          mockSetup
          assertEqualsMessage(foreignDoc, "article.content__body>div>div>a:nth-child(16)", "nisp.main.puttingOff.linkTitle")
        }
        "render page with href link 'More on putting off claiming (opens in new tab)'" in {
          mockSetup
          assertLinkHasValue(foreignDoc, "article.content__body>div>div>a:nth-child(16)", "https://www.gov.uk/deferring-state-pension")
        }
      }

      "The scenario is continue working" when {

        "State Pension page with MQP : Continue Working || Fill Gaps || Full Rate" should {

          def mockSetup = {
            when(mockStatePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(Right(StatePension(
                LocalDate.of(2016, 4, 5),
                amounts = StatePensionAmounts(
                  protectedPayment = false,
                  StatePensionAmountRegular(111.71, 590.10, 7081.15),
                  StatePensionAmountForecast(4, 137.86, 599.44, 7193.34),
                  StatePensionAmountMaximum(4, 1, 149.71, 590.10, 7081.15),
                  StatePensionAmountRegular(0, 0, 0)
                ),
                pensionAge = 67,
                LocalDate.of(2020, 6, 7),
                "2019-20",
                4,
                pensionSharingOrder = false,
                currentFullWeeklyPensionAmount = 155.65,
                false,
                false
              )
              )))

            when(mockNationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(Right(NationalInsuranceRecord(
                qualifyingYears = 4,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 2,
                numberOfGapsPayable = 2,
                Some(LocalDate.of(1954, 3, 6)),
                false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
                ),
                reducedRateElection = false
              )
              )))
          }

          lazy val nonForeignDoc =
            asDocument(contentAsString(controller.show()(FakeRequest())))

          lazy val foreignDoc =
            asDocument(contentAsString(abroadUserController.show()(FakeRequest())))

          "render with correct page title" in {
            mockSetup
            assertElementContainsText(nonForeignDoc, "head>title" ,messages("nisp.main.h1.title") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
          }
          "render page with heading  'Your State Pension' " in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>h1.titleWithPAgeInfo", "nisp.main.h1.title")
          }

          "render page with text  'You can get your State Pension on' " in {
            mockSetup
            assertElemetsOwnMessage(nonForeignDoc, "article.content__body>div>div>div:nth-child(4)>p", "nisp.main.basedOn")
          }
          "render page with text  '7 june 2020' " in {
            mockSetup
            assertEqualsValue(nonForeignDoc, "article.content__body>div>div>div:nth-child(4)>p:nth-child(1)>span:nth-child(1)", Dates.formatDate(LocalDate.of(2020, 6, 7)) + ".")
          }
          "render page with text  'Your forecast is' " in {
            mockSetup
            val sMessage = Messages("nisp.main.caveats") + " " + Messages("nisp.is")
            assertEqualsValue(nonForeignDoc, "article.content__body>div>div>div:nth-child(4)>p:nth-child(1)>span:nth-child(2)", sMessage)
          }

          "render page with text  '£137.86 a week" in {
            mockSetup
            val sWeek = "£137.86 " + Messages("nisp.main.week")
            assertEqualsValue(nonForeignDoc, "article.content__body>div>div>div:nth-child(4)>p:nth-child(2)>em", sWeek)
          }
          "render page with text  ' £599.44 a month, £7,193.34 a year '" in {
            mockSetup
            val sForecastAmount = "£599.44 " + Messages("nisp.main.month") + ", £7,193.34 " + Messages("nisp.main.year")
            assertEqualsValue(nonForeignDoc, "article.content__body>div>div>div:nth-child(4)>p:nth-child(3)", sForecastAmount)
          }
          "render page with text  ' Your forcaste '" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(5)", "nisp.main.caveats")
          }
          "render page with text  ' is not a guarantee and is based on the current law '" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>ul:nth-child(6)>li:nth-child(1)", "nisp.main.notAGuarantee")
          }
          "render page with text  ' is based on your National Insurance record up to 5 April 2016 '" in {
            mockSetup
            assertContainsDynamicMessage(nonForeignDoc, "article.content__body>div>div>ul:nth-child(6)>li:nth-child(2)", "nisp.main.isBased", Dates.formatDate(LocalDate.of(2016, 4, 5)))
          }
          "render page with text  ' assumes that you’ll contribute another 4 years '" in {
            mockSetup
            assertContainsDynamicMessage(nonForeignDoc, "article.content__body>div>div>ul:nth-child(6)>li:nth-child(3)", "nisp.mqp.howManyToContribute", Time.years(4))
          }

          "render page with text  ' does not include any increase due to inflation '" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>ul:nth-child(6)>li:nth-child(4)", "nisp.main.inflation")
          }
          "render page with text  ' You currently have 4 years on your record and you need at least 10 years to get any State Pension. '" in {
            mockSetup
            assertContainsDynamicMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(7)", "nisp.mqp.youCurrentlyHave", Time.years(4).toString(), Constants.minimumQualifyingYearsNSP.toString(), null)
          }

          "render page with Heading  ' You can improve your forecast'" in {
            mockSetup
            assertEqualsMessage(foreignDoc, "article.content__body>div>div>h2:nth-child(8)", "nisp.main.context.fillGaps.improve.title")
          }
          "render page with text  'You have years on your record where you did not contribute enough National Insurance and you can make up the shortfall. " +
            "This will make these years count towards your pension and may improve your forecast.'" in {
            mockSetup
            assertEqualsMessage(foreignDoc, "article.content__body>div>div>p:nth-child(9)", "nisp.main.context.improve.para1.plural")
          }
          "render page with link  'View gaps in your record'" in {
            mockSetup
            assertEqualsMessage(foreignDoc, "article.content__body>div>div>a:nth-child(10)", "nisp.main.context.fillGaps.viewGaps")
          }
          "render page with href link  'View gaps in your record'" in {
            mockSetup
            assertLinkHasValue(foreignDoc, "article.content__body>div>div>a:nth-child(10)", "/check-your-state-pension/account/nirecord/gaps")
          }

          "render page with href text  'Your forecast may be different if there are any changes to your National Insurance information. There is more about this in the terms and conditions'" in {
            mockSetup
            val sMessage = StringEscapeUtils.unescapeHtml4(Messages("nisp.legal.forecastChanges")) + " ."
            assertElemetsOwnText(foreignDoc, "article.content__body>div>div>p:nth-child(11)", sMessage)
          }

          /*overseas message*/
          "render page with text 'As you are living or working overseas (opens in new tab), you may be entitled to a " +
            "State Pension from the country you are living or working in.'" in {
            mockSetup
            assertEqualsMessage(foreignDoc, "article.content__body>div>div>div.panel-indent:nth-child(12)>p", "nisp.main.overseas")
          }
          /*Ends*/

          /*Start of Non SPA Checks*/
          "Not render page with heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertPageDoesNotContainMessage(nonForeignDoc,"nisp.spa.under.consideration.title")
          }

          "Not render page with text 'Youll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertPageDoesNotContainDynamicMessage(nonForeignDoc, "nisp.spa.under.consideration.detail", Dates.formatDate(LocalDate.of(2020, 6, 7)))
          }
          /*Ends*/

          "render page with heading 'Putting of claiming'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>h2:nth-child(13)", "nisp.main.puttingOff")
          }

          "render page with text 'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            mockSetup
            assertContainsDynamicMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(14)", "nisp.main.puttingOff.line1", Dates.formatDate(LocalDate.of(2020, 6, 7)))
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>a:nth-child(15)", "nisp.main.puttingOff.linkTitle")
          }
          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertLinkHasValue(nonForeignDoc, "article.content__body>div>div>a:nth-child(15)", "https://www.gov.uk/deferring-state-pension")
          }

          /*Side bar help*/
          "render page with heading 'Get help'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, ".helpline-sidebar>h2", "nisp.nirecord.helpline.getHelp")
          }
          "render page with text 'Helpline 0800 731 0181'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, ".helpline-sidebar>p:nth-child(2)", "nisp.nirecord.helpline.number")
          }
          "render page with text 'Textphone 0800 731 0176'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, ".helpline-sidebar>p:nth-child(3)", "nisp.nirecord.helpline.textNumber")
          }
          "render page with text 'Monday to Friday: 8am to 6pm'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, ".helpline-sidebar>p:nth-child(4)", "nisp.nirecord.helpline.openTimes")
          }
        }

        "State Pension page with MQP : Continue Working || Fill Gaps || Full Rate: With State Pension age under consideration message" should {

          def mockSetup = {
            when(mockStatePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(Right(StatePension(
                LocalDate.of(2016, 4, 5),
                amounts = StatePensionAmounts(
                  protectedPayment = false,
                  StatePensionAmountRegular(111.71, 590.10, 7081.15),
                  StatePensionAmountForecast(4, 137.86, 599.44, 7193.34),
                  StatePensionAmountMaximum(4, 1, 149.71, 590.10, 7081.15),
                  StatePensionAmountRegular(0, 0, 0)
                ),
                pensionAge = 67,
                LocalDate.of(2020, 6, 7),
                "2019-20",
                4,
                pensionSharingOrder = false,
                currentFullWeeklyPensionAmount = 155.65,
                reducedRateElection = false,
                statePensionAgeUnderConsideration = true
              )
              )))

            when(mockNationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(Right(NationalInsuranceRecord(
                qualifyingYears = 4,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 2,
                numberOfGapsPayable = 2,
                Some(LocalDate.of(1954, 3, 6)),
                false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
                ),
                reducedRateElection = false
              )
              )))
          }

          lazy val nonForeignDoc =
            asDocument(contentAsString(controller.show()(FakeRequest())))

          lazy val foreignDoc =
            asDocument(contentAsString(abroadUserController.show()(FakeRequest())))

          //overseas message
          "render page with text  'As you are living or working overseas (opens in new tab), you may be entitled to a " +
            "State Pension from the country you are living or working in.'" in {
            mockSetup
            assertEqualsMessage(foreignDoc, "article.content__body>div>div>div.panel-indent:nth-child(12)>p", "nisp.main.overseas")
          }

          // SPA under consideration message
          "render page with heading  'Proposed change to your State Pension age'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>h2:nth-child(13)", "nisp.spa.under.consideration.title")
          }

          "render page with text  'Youll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertContainsDynamicMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(14)", "nisp.spa.under.consideration.detail", Dates.formatDate(LocalDate.of(2020, 6, 7)))
          }

          //deferral message
          "render page with heading  'Putting of claiming'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>h2:nth-child(15)", "nisp.main.puttingOff")
          }

          "render page with text  'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            mockSetup
            assertContainsDynamicMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(16)", "nisp.main.puttingOff.line1", Dates.formatDate(LocalDate.of(2020, 6, 7)))
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>a:nth-child(17)", "nisp.main.puttingOff.linkTitle")
          }
          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertLinkHasValue(nonForeignDoc, "article.content__body>div>div>a:nth-child(17)", "https://www.gov.uk/deferring-state-pension")
          }
        }

        "State Pension page with MQP : Continue Working || no gaps || Full Rate" should {

          def mockSetup = {
            when(mockStatePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(Right(StatePension(
                LocalDate.of(2016, 4, 5),
                amounts = StatePensionAmounts(
                  protectedPayment = false,
                  StatePensionAmountRegular(111.71, 590.10, 7081.15),
                  StatePensionAmountForecast(4, 155.65, 599.44, 7193.34),
                  StatePensionAmountMaximum(4, 1, 149.71, 590.10, 7081.15),
                  StatePensionAmountRegular(0, 0, 0)
                ),
                pensionAge = 67,
                LocalDate.of(2020, 6, 7),
                "2019-20",
                4,
                pensionSharingOrder = false,
                currentFullWeeklyPensionAmount = 155.65,
                false,
                false
              )
              )))

            when(mockNationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(Right(NationalInsuranceRecord(
                qualifyingYears = 4,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 0,
                numberOfGapsPayable = 0,
                Some(LocalDate.of(1954, 3, 6)),
                false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
                ),
                reducedRateElection = false
              )
              )))
          }

          lazy val nonForeignDoc =
            asDocument(contentAsString(controller.show()(FakeRequest())))

          lazy val foreignDoc =
            asDocument(contentAsString(abroadUserController.show()(FakeRequest())))

          "render with correct page title" in {
            mockSetup
            assertElementContainsText(nonForeignDoc, "head>title" ,messages("nisp.main.h1.title") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
          }
          "render page with heading  'Your State Pension' " in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>h1.titleWithPAgeInfo", "nisp.main.h1.title")
          }

          "render page with text  'You can get your State Pension on' " in {
            mockSetup
            assertElemetsOwnMessage(nonForeignDoc, "article.content__body>div>div>div:nth-child(4)>p", "nisp.main.basedOn")
          }
          "render page with text  '7 june 2020' " in {
            mockSetup
            assertEqualsValue(nonForeignDoc, "article.content__body>div>div>div:nth-child(4)>p:nth-child(1)>span:nth-child(1)", Dates.formatDate(LocalDate.of(2020, 6, 7)) + ".")
          }
          "render page with text  'Your forecast is' " in {
            mockSetup
            val sMessage = Messages("nisp.main.caveats") + " " + Messages("nisp.is")
            assertEqualsValue(nonForeignDoc, "article.content__body>div>div>div:nth-child(4)>p:nth-child(1)>span:nth-child(2)", sMessage)
          }

          "render page with text  '£155.65 a week" in {
            mockSetup
            val sWeek = "£155.65 " + Messages("nisp.main.week")
            assertEqualsValue(nonForeignDoc, "article.content__body>div>div>div:nth-child(4)>p:nth-child(2)>em", sWeek)
          }
          "render page with text  ' £599.44 a month, £7,193.34 a year '" in {
            mockSetup
            val sForecastAmount = "£599.44 " + Messages("nisp.main.month") + ", £7,193.34 " + Messages("nisp.main.year")
            assertEqualsValue(nonForeignDoc, "article.content__body>div>div>div:nth-child(4)>p:nth-child(3)", sForecastAmount)
          }
          "render page with text  ' Your forcaste '" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(5)", "nisp.main.caveats")
          }
          "render page with text  ' is not a guarantee and is based on the current law '" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>ul:nth-child(6)>li:nth-child(1)", "nisp.main.notAGuarantee")
          }
          "render page with text  ' is based on your National Insurance record up to 5 April 2016 '" in {
            mockSetup
            assertContainsDynamicMessage(nonForeignDoc, "article.content__body>div>div>ul:nth-child(6)>li:nth-child(2)", "nisp.main.isBased", Dates.formatDate(LocalDate.of(2016, 4, 5)))
          }
          "render page with text  ' assumes that you’ll contribute another 4 years '" in {
            mockSetup
            assertContainsDynamicMessage(nonForeignDoc, "article.content__body>div>div>ul:nth-child(6)>li:nth-child(3)", "nisp.mqp.howManyToContribute", Time.years(4))
          }

          "render page with text  ' does not include any increase due to inflation '" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>ul:nth-child(6)>li:nth-child(4)", "nisp.main.inflation")
          }
          "render page with text  ' You currently have 4 years on your record and you need at least 10 years to get any State Pension. '" in {
            mockSetup
            assertContainsDynamicMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(7)", "nisp.mqp.youCurrentlyHave", Time.years(4).toString(), Constants.minimumQualifyingYearsNSP.toString(), null)
          }

          "render page with Heading  '£155.65 is the most you can get'" in {
            mockSetup
            val sMessage = "£155.65 " + StringEscapeUtils.unescapeHtml4(Messages("nisp.main.mostYouCanGet"))
            assertEqualsValue(nonForeignDoc, "#mostYouCanGet", sMessage)
          }

          "render page with text  'You cannot improve your forecast any further.'" in {
            mockSetup
            assertEqualsMessage(foreignDoc, "article.content__body>div>div>p:nth-child(9)", "nisp.main.context.willReach")
          }
          "render page with text  'If you’re working you may still need to pay National Insurance contributions until 7 June 2020 as they fund other state benefits and the NHS.'" in {
            mockSetup
            assertContainsDynamicMessage(foreignDoc, "article.content__body>div>div>p:nth-child(10)", "nisp.main.context.reachMax.needToPay", Dates.formatDate(LocalDate.of(2020, 6, 7)))
          }
          "render page with link  'View your National Insurence Record'" in {
            mockSetup
            assertEqualsMessage(foreignDoc, "article.content__body>div>div>a:nth-child(11)", "nisp.main.showyourrecord")
          }
          "render page with href link 'View your National Insurence Record'" in {
            mockSetup
            assertLinkHasValue(foreignDoc, "article.content__body>div>div>a:nth-child(11)", "/check-your-state-pension/account/nirecord")
          }

          "render page with href text 'Your forecast may be different if there are any changes to your National Insurance information. There is more about this in the terms and conditions'" in {
            mockSetup
            val sMessage = StringEscapeUtils.unescapeHtml4(Messages("nisp.legal.forecastChanges")) + " ."
            assertElemetsOwnText(foreignDoc, "article.content__body>div>div>p:nth-child(12)", sMessage)
          }

          /*overseas message*/
          "render page with text 'As you are living or working overseas (opens in new tab), you may be entitled to a " +
            "State Pension from the country you are living or working in.'" in {
            mockSetup
            assertEqualsMessage(foreignDoc, "article.content__body>div>div>div.panel-indent:nth-child(13)>p", "nisp.main.overseas")
          }

          "Not render page with heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertPageDoesNotContainMessage(nonForeignDoc, "nisp.spa.under.consideration.title")
          }

          "Not render page with text 'Youll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertPageDoesNotContainDynamicMessage(nonForeignDoc, "nisp.spa.under.consideration.detail", Dates.formatDate(LocalDate.of(2020, 6, 7)))
          }

          "render page with heading 'Putting of claiming'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>h2:nth-child(14)", "nisp.main.puttingOff")
          }

          "render page with text 'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            mockSetup
            assertContainsDynamicMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(15)", "nisp.main.puttingOff.line1", Dates.formatDate(LocalDate.of(2020, 6, 7)))
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>a:nth-child(16)", "nisp.main.puttingOff.linkTitle")
          }
          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertLinkHasValue(nonForeignDoc, "article.content__body>div>div>a:nth-child(16)", "https://www.gov.uk/deferring-state-pension")
          }
        }

        "State Pension page with MQP : Continue Working || no gaps || Full Rate: With State Pension age under consideration message" should {

          def mockSetup = {
            when(mockStatePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(Right(StatePension(
                LocalDate.of(2016, 4, 5),
                amounts = StatePensionAmounts(
                  protectedPayment = false,
                  StatePensionAmountRegular(111.71, 590.10, 7081.15),
                  StatePensionAmountForecast(4, 155.65, 599.44, 7193.34),
                  StatePensionAmountMaximum(4, 1, 149.71, 590.10, 7081.15),
                  StatePensionAmountRegular(0, 0, 0)
                ),
                pensionAge = 67,
                LocalDate.of(2020, 6, 7),
                "2019-20",
                4,
                pensionSharingOrder = false,
                currentFullWeeklyPensionAmount = 155.65,
                reducedRateElection = false,
                statePensionAgeUnderConsideration = true
              )
              )))

            when(mockNationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(Right(NationalInsuranceRecord(
                qualifyingYears = 4,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 0,
                numberOfGapsPayable = 0,
                Some(LocalDate.of(1954, 3, 6)),
                false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
                ),
                reducedRateElection = false
              )
              )))
          }

          lazy val nonForeignDoc =
            asDocument(contentAsString(controller.show()(FakeRequest())))

          lazy val foreignDoc =
            asDocument(contentAsString(abroadUserController.show()(FakeRequest())))

          //overseas message
          "render page with text  'As you are living or working overseas (opens in new tab), you may be entitled to a " +
            "State Pension from the country you are living or working in.'" in {
            mockSetup
            assertEqualsMessage(foreignDoc, "article.content__body>div>div>div.panel-indent:nth-child(13)>p", "nisp.main.overseas")
          }

          "render page with heading  'Proposed change to your State Pension age'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>h2:nth-child(14)", "nisp.spa.under.consideration.title")
          }

          "render page with text  'Youll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertContainsDynamicMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(15)", "nisp.spa.under.consideration.detail", Dates.formatDate(LocalDate.of(2020, 6, 7)))
          }

          "render page with heading  'Putting of claiming'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>h2:nth-child(16)", "nisp.main.puttingOff")
          }

          "render page with text  'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            mockSetup
            assertContainsDynamicMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(17)", "nisp.main.puttingOff.line1", Dates.formatDate(LocalDate.of(2020, 6, 7)))
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>a:nth-child(18)", "nisp.main.puttingOff.linkTitle")
          }
          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertLinkHasValue(nonForeignDoc, "article.content__body>div>div>a:nth-child(18)", "https://www.gov.uk/deferring-state-pension")
          }
        }

        "State Pension page with MQP : Continue Working || 0 Qualify Years || has fillable Gaps ||  Personal Max" should {

          def mockSetup = {
            when(mockStatePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(Right(StatePension(
                LocalDate.of(2016, 4, 5),
                amounts = StatePensionAmounts(
                  protectedPayment = false,
                  StatePensionAmountRegular(111.71, 590.10, 7081.15),
                  StatePensionAmountForecast(4, 155.65, 599.44, 7193.34),
                  StatePensionAmountMaximum(4, 1, 149.71, 590.10, 7081.15),
                  StatePensionAmountRegular(0, 0, 0)
                ),
                pensionAge = 67,
                LocalDate.of(2020, 6, 7),
                "2019-20",
                0,
                pensionSharingOrder = false,
                currentFullWeeklyPensionAmount = 155.65,
                false,
                false
              )
              )))

            when(mockNationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(Right(NationalInsuranceRecord(
                qualifyingYears = 0,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 1,
                numberOfGapsPayable = 1,
                Some(LocalDate.of(1954, 3, 6)),
                false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
                ),
                reducedRateElection = false
              )
              )))
          }

          lazy val nonForeignDoc =
            asDocument(contentAsString(controller.show()(FakeRequest())))

          lazy val foreignDoc =
            asDocument(contentAsString(abroadUserController.show()(FakeRequest())))

          "render with correct page title" in {
            mockSetup
            assertElementContainsText(nonForeignDoc, "head>title" ,messages("nisp.main.h1.title") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
          }
          "render page with heading  'Your State Pension' " in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>h1.titleWithPAgeInfo", "nisp.main.h1.title")
          }

          "render page with text  'You can get your State Pension on' " in {
            mockSetup
            assertElemetsOwnMessage(nonForeignDoc, "article.content__body>div>div>div:nth-child(4)>p", "nisp.main.basedOn")
          }
          "render page with text  '7 june 2020' " in {
            mockSetup
            assertEqualsValue(nonForeignDoc, "article.content__body>div>div>div:nth-child(4)>p:nth-child(1)>span:nth-child(1)", Dates.formatDate(LocalDate.of(2020, 6, 7)) + ".")
          }
          "render page with text  'Your forecast is' " in {
            mockSetup
            val sMessage = Messages("nisp.main.caveats") + " " + Messages("nisp.is")
            assertEqualsValue(nonForeignDoc, "article.content__body>div>div>div:nth-child(4)>p:nth-child(1)>span:nth-child(2)", sMessage)
          }

          "render page with text  '£155.65 a week" in {
            mockSetup
            val sWeek = "£155.65 " + Messages("nisp.main.week")
            assertEqualsValue(nonForeignDoc, "article.content__body>div>div>div:nth-child(4)>p:nth-child(2)>em", sWeek)
          }
          "render page with text  ' £599.44 a month, £7,193.34 a year '" in {
            mockSetup
            val sForecastAmount = "£599.44 " + Messages("nisp.main.month") + ", £7,193.34 " + Messages("nisp.main.year")
            assertEqualsValue(nonForeignDoc, "article.content__body>div>div>div:nth-child(4)>p:nth-child(3)", sForecastAmount)
          }
          "render page with text  ' Your forcaste '" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(5)", "nisp.main.caveats")
          }
          "render page with text  ' is not a guarantee and is based on the current law '" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>ul:nth-child(6)>li:nth-child(1)", "nisp.main.notAGuarantee")
          }
          "render page with text  ' is based on your National Insurance record up to 5 April 2016 '" in {
            mockSetup
            assertContainsDynamicMessage(nonForeignDoc, "article.content__body>div>div>ul:nth-child(6)>li:nth-child(2)", "nisp.main.isBased", Dates.formatDate(LocalDate.of(2016, 4, 5)))
          }
          "render page with text  ' assumes that you’ll contribute another 4 years '" in {
            mockSetup
            assertContainsDynamicMessage(nonForeignDoc, "article.content__body>div>div>ul:nth-child(6)>li:nth-child(3)", "nisp.mqp.howManyToContribute", Time.years(4))
          }

          "render page with text  ' does not include any increase due to inflation '" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>ul:nth-child(6)>li:nth-child(4)", "nisp.main.inflation")
          }
          "render page with text  'You do not have any years on your record and you need at least 10 years to get any State Pension. '" in {
            mockSetup
            assertContainsDynamicMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(7)", "nisp.mqp.youCurrentlyHaveZero", Constants.minimumQualifyingYearsNSP.toString())
          }

          "render page with Heading  '£155.65 is the most you can get'" in {
            mockSetup
            val sMessage = "£155.65 " + Messages("nisp.main.mostYouCanGet")
            assertEqualsValue(nonForeignDoc, "#mostYouCanGet", sMessage)
          }

          "render page with text  'You cannot improve your forecast any further, unless you choose to put off claimimg'" in {
            mockSetup
            assertEqualsMessage(foreignDoc, "article.content__body>div>div>p:nth-child(9)", "nisp.main.context.willReach")
          }
          "render page with text  'If you’re working you may still need to pay National Insurance contributions until 7 June 2020 as they fund other state benefits and the NHS.'" in {
            mockSetup
            assertContainsDynamicMessage(foreignDoc, "article.content__body>div>div>p:nth-child(10)", "nisp.main.context.reachMax.needToPay", Dates.formatDate(LocalDate.of(2020, 6, 7)))
          }
          "render page with link  'View your National Insurence Record'" in {
            mockSetup
            assertEqualsMessage(foreignDoc, "article.content__body>div>div>a:nth-child(11)", "nisp.main.showyourrecord")
          }
          "render page with href link  'View your National Insurence Record'" in {
            mockSetup
            assertLinkHasValue(foreignDoc, "article.content__body>div>div>a:nth-child(11)", "/check-your-state-pension/account/nirecord")
          }

          "render page with href text  'Your forecast may be different if there are any changes to your National Insurance information. There is more about this in the terms and conditions'" in {
            mockSetup
            val sMessage = StringEscapeUtils.unescapeHtml4(Messages("nisp.legal.forecastChanges")) + " ."
            assertElemetsOwnText(foreignDoc, "article.content__body>div>div>p:nth-child(12)", sMessage)
          }

          /*overseas message*/
          "render page with text 'As you are living or working overseas (opens in new tab), you may be entitled to a " +
            "State Pension from the country you are living or working in.'" in {
            mockSetup
            assertEqualsMessage(foreignDoc, "article.content__body>div>div>div.panel-indent:nth-child(13)>p", "nisp.main.overseas")
          }
          /*Ends*/

          /*Start of Non SPA Checks*/
          "Not render page with heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertPageDoesNotContainMessage(nonForeignDoc, "nisp.spa.under.consideration.title")
          }

          "Not render page with text 'Youll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertPageDoesNotContainDynamicMessage(nonForeignDoc, "nisp.spa.under.consideration.detail", Dates.formatDate(LocalDate.of(2020, 6, 7)))
          }
          /*Ends*/

          "render page with heading 'Putting of claiming'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>h2:nth-child(14)", "nisp.main.puttingOff")
          }

          "render page with text 'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            mockSetup
            assertContainsDynamicMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(15)", "nisp.main.puttingOff.line1", Dates.formatDate(LocalDate.of(2020, 6, 7)))
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>a:nth-child(16)", "nisp.main.puttingOff.linkTitle")
          }
          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertLinkHasValue(nonForeignDoc, "article.content__body>div>div>a:nth-child(16)", "https://www.gov.uk/deferring-state-pension")
          }
        }

        "State Pension page with MQP : Continue Working || 0 Qualify Years || has fillable Gaps ||  Personal Max: With State Pension age under consideration message" should {

          def mockSetup = {
            when(mockStatePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(Right(StatePension(
                LocalDate.of(2016, 4, 5),
                amounts = StatePensionAmounts(
                  protectedPayment = false,
                  StatePensionAmountRegular(111.71, 590.10, 7081.15),
                  StatePensionAmountForecast(4, 155.65, 599.44, 7193.34),
                  StatePensionAmountMaximum(4, 1, 149.71, 590.10, 7081.15),
                  StatePensionAmountRegular(0, 0, 0)
                ),
                pensionAge = 67,
                LocalDate.of(2020, 6, 7),
                "2019-20",
                0,
                pensionSharingOrder = false,
                currentFullWeeklyPensionAmount = 155.65,
                reducedRateElection = false,
                statePensionAgeUnderConsideration = true
              )
              )))

            when(mockNationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(Right(NationalInsuranceRecord(
                qualifyingYears = 0,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 1,
                numberOfGapsPayable = 1,
                Some(LocalDate.of(1954, 3, 6)),
                false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
                ),
                reducedRateElection = false
              )
              )))
          }

          lazy val nonForeignDoc =
            asDocument(contentAsString(controller.show()(FakeRequest())))

          lazy val foreignDoc =
            asDocument(contentAsString(abroadUserController.show()(FakeRequest())))

          //overseas message
          "render page with text  'As you are living or working overseas (opens in new tab), you may be entitled to a " +
            "State Pension from the country you are living or working in.'" in {
            mockSetup
            assertEqualsMessage(foreignDoc, "article.content__body>div>div>div.panel-indent:nth-child(13)>p", "nisp.main.overseas")
          }

          "render page with heading  'Proposed change to your State Pension age'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>h2:nth-child(14)", "nisp.spa.under.consideration.title")
          }

          "render page with text  'Youll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertContainsDynamicMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(15)", "nisp.spa.under.consideration.detail", Dates.formatDate(LocalDate.of(2020, 6, 7)))
          }

          "render page with heading  'Putting of claiming'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>h2:nth-child(16)", "nisp.main.puttingOff")
          }

          "render page with text  'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            mockSetup
            assertContainsDynamicMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(17)", "nisp.main.puttingOff.line1", Dates.formatDate(LocalDate.of(2020, 6, 7)))
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>a:nth-child(18)", "nisp.main.puttingOff.linkTitle")
          }
          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertLinkHasValue(nonForeignDoc, "article.content__body>div>div>a:nth-child(18)", "https://www.gov.uk/deferring-state-pension")
          }
        }

        "State Pension page with MQP : Continue Working || 9 Qualify Years || cant fill gaps ||  Personal Max" should {

          def mockSetup = {
            when(mockStatePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(Right(StatePension(
                LocalDate.of(2016, 4, 5),
                amounts = StatePensionAmounts(
                  protectedPayment = false,
                  StatePensionAmountRegular(111.71, 590.10, 7081.15),
                  StatePensionAmountForecast(4, 155.65, 599.44, 7193.34),
                  StatePensionAmountMaximum(4, 1, 149.71, 590.10, 7081.15),
                  StatePensionAmountRegular(0, 0, 0)
                ),
                pensionAge = 67,
                LocalDate.of(2020, 6, 7),
                "2019-20",
                9,
                pensionSharingOrder = false,
                currentFullWeeklyPensionAmount = 155.65,
                false,
                false
              )
              )))

            when(mockNationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(Right(NationalInsuranceRecord(
                qualifyingYears = 9,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 0,
                numberOfGapsPayable = 0,
                Some(LocalDate.of(1954, 3, 6)),
                false,
                LocalDate.of(2017, 4, 5),
                List(
                  NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
                ),
                reducedRateElection = false
              )
              )))
          }

          lazy val nonForeignDoc =
            asDocument(contentAsString(controller.show()(FakeRequest())))

          lazy val foreignDoc =
            asDocument(contentAsString(abroadUserController.show()(FakeRequest())))

          "render with correct page title" in {
            mockSetup
            assertElementContainsText(nonForeignDoc, "head>title" ,messages("nisp.main.h1.title") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
          }
          "render page with heading  'Your State Pension' " in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>h1.titleWithPAgeInfo", "nisp.main.h1.title")
          }

          "render page with text  'You can get your State Pension on' " in {
            mockSetup
            assertElemetsOwnMessage(nonForeignDoc, "article.content__body>div>div>div:nth-child(4)>p", "nisp.main.basedOn")
          }
          "render page with text  '7 june 2020' " in {
            mockSetup
            assertEqualsValue(nonForeignDoc, "article.content__body>div>div>div:nth-child(4)>p:nth-child(1)>span:nth-child(1)", Dates.formatDate(LocalDate.of(2020, 6, 7)) + ".")
          }
          "render page with text  'Your forecast is' " in {
            mockSetup
            val sMessage = Messages("nisp.main.caveats") + " " + Messages("nisp.is")
            assertEqualsValue(nonForeignDoc, "article.content__body>div>div>div:nth-child(4)>p:nth-child(1)>span:nth-child(2)", sMessage)
          }

          "render page with text  '£155.65 a week" in {
            mockSetup
            val sWeek = "£155.65 " + Messages("nisp.main.week")
            assertEqualsValue(nonForeignDoc, "article.content__body>div>div>div:nth-child(4)>p:nth-child(2)>em", sWeek)
          }
          "render page with text  ' £599.44 a month, £7,193.34 a year '" in {
            mockSetup
            val sForecastAmount = "£599.44 " + Messages("nisp.main.month") + ", £7,193.34 " + Messages("nisp.main.year")
            assertEqualsValue(nonForeignDoc, "article.content__body>div>div>div:nth-child(4)>p:nth-child(3)", sForecastAmount)
          }
          "render page with text  ' Your forcaste '" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(5)", "nisp.main.caveats")
          }
          "render page with text  ' is not a guarantee and is based on the current law '" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>ul:nth-child(6)>li:nth-child(1)", "nisp.main.notAGuarantee")
          }
          "render page with text  ' is based on your National Insurance record up to 5 April 2016 '" in {
            mockSetup
            assertContainsDynamicMessage(nonForeignDoc, "article.content__body>div>div>ul:nth-child(6)>li:nth-child(2)", "nisp.main.isBased", Dates.formatDate(LocalDate.of(2016, 4, 5)))
          }
          "render page with text  ' assumes that you’ll contribute another 4 years '" in {
            mockSetup
            assertContainsDynamicMessage(nonForeignDoc, "article.content__body>div>div>ul:nth-child(6)>li:nth-child(3)", "nisp.mqp.howManyToContribute", Time.years(4))
          }

          "render page with text  ' does not include any increase due to inflation '" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>ul:nth-child(6)>li:nth-child(4)", "nisp.main.inflation")
          }

          "render page with text  ' You currently have 9 years on your record and you need at least 10 years to get any State Pension. '" in {
            mockSetup
            assertContainsDynamicMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(7)", "nisp.mqp.youCurrentlyHave", Time.years(9).toString(), Constants.minimumQualifyingYearsNSP.toString(), null)
          }

          "render page with Heading  '£155.65 is the most you can get'" in {
            mockSetup
            val sMessage = "£155.65 " + Messages("nisp.main.mostYouCanGet")
            assertEqualsValue(nonForeignDoc, "#mostYouCanGet", sMessage)
          }

          "render page with text  'You cannot improve your forecast any further, unless you choose to put off claimimg'" in {
            mockSetup
            assertEqualsMessage(foreignDoc, "article.content__body>div>div>p:nth-child(9)", "nisp.main.context.willReach")
          }
          "render page with text  'If you’re working you may still need to pay National Insurance contributions until 7 June 2020 as they fund other state benefits and the NHS.'" in {
            mockSetup
            assertContainsDynamicMessage(foreignDoc, "article.content__body>div>div>p:nth-child(10)", "nisp.main.context.reachMax.needToPay", Dates.formatDate(LocalDate.of(2020, 6, 7)))
          }
          "render page with link  'View your National Insurence Record'" in {
            mockSetup
            assertEqualsMessage(foreignDoc, "article.content__body>div>div>a:nth-child(11)", "nisp.main.showyourrecord")
          }
          "render page with href link 'View your National Insurence Record'" in {
            mockSetup
            assertLinkHasValue(foreignDoc, "article.content__body>div>div>a:nth-child(11)", "/check-your-state-pension/account/nirecord")
          }

          "render page with href text 'Your forecast may be different if there are any changes to your National Insurance information. There is more about this in the terms and conditions''" in {
            mockSetup
            val sMessage = StringEscapeUtils.unescapeHtml4(Messages("nisp.legal.forecastChanges")) + " ."
            assertElemetsOwnText(foreignDoc, "article.content__body>div>div>p:nth-child(12)", sMessage)
          }

          /*overseas message*/
          "render page with text 'As you are living or working overseas (opens in new tab), you may be entitled to a " +
            "State Pension from the country you are living or working in.'" in {
            mockSetup
            assertEqualsMessage(foreignDoc, "article.content__body>div>div>div.panel-indent:nth-child(13)>p", "nisp.main.overseas")
          }
          /*Ends*/

          /*Start of Non SPA Checks*/
          "Not render page with heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertPageDoesNotContainMessage(nonForeignDoc, "nisp.spa.under.consideration.title")
          }

          "Not render page with text 'Youll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertPageDoesNotContainDynamicMessage(nonForeignDoc, "nisp.spa.under.consideration.detail", Dates.formatDate(LocalDate.of(2020, 6, 7)))
          }

          "render page with heading  'Putting of claiming'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>h2:nth-child(14)", "nisp.main.puttingOff")
          }

          "render page with text  'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            mockSetup
            assertContainsDynamicMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(15)", "nisp.main.puttingOff.line1", Dates.formatDate(LocalDate.of(2020, 6, 7)))
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>a:nth-child(16)", "nisp.main.puttingOff.linkTitle")
          }
          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertLinkHasValue(nonForeignDoc, "article.content__body>div>div>a:nth-child(16)", "https://www.gov.uk/deferring-state-pension")
          }
        }

        "State Pension page with MQP : Continue Working || 9 Qualify Years || cant fill gaps ||  Personal Max: With State Pension age under consideration message" should {

          def mockSetup = {
            when(mockStatePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(Right(StatePension(
                LocalDate.of(2016, 4, 5),
                amounts = StatePensionAmounts(
                  protectedPayment = false,
                  StatePensionAmountRegular(111.71, 590.10, 7081.15),
                  StatePensionAmountForecast(4, 155.65, 599.44, 7193.34),
                  StatePensionAmountMaximum(4, 1, 149.71, 590.10, 7081.15),
                  StatePensionAmountRegular(0, 0, 0)
                ),
                pensionAge = 67,
                LocalDate.of(2020, 6, 7),
                "2019-20",
                9,
                pensionSharingOrder = false,
                currentFullWeeklyPensionAmount = 155.65,
                reducedRateElection = false,
                statePensionAgeUnderConsideration = true
              )
              )))

            when(mockNationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(Right(NationalInsuranceRecord(
                qualifyingYears = 9,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 0,
                numberOfGapsPayable = 0,
                Some(LocalDate.of(1954, 3, 6)),
                false,
                LocalDate.of(2017, 4, 5),
                List(
                  NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
                ),
                reducedRateElection = false
              )
              )))
          }

          lazy val nonForeignDoc =
            asDocument(contentAsString(controller.show()(FakeRequest())))

          lazy val foreignDoc =
            asDocument(contentAsString(abroadUserController.show()(FakeRequest())))

          //overseas message
          "render page with text  'As you are living or working overseas (opens in new tab), you may be entitled to a " +
            "State Pension from the country you are living or working in.'" in {
            mockSetup
            assertEqualsMessage(foreignDoc, "article.content__body>div>div>div.panel-indent:nth-child(13)>p", "nisp.main.overseas")
          }

          "render page with heading  'Proposed change to your State Pension age'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>h2:nth-child(14)", "nisp.spa.under.consideration.title")
          }

          "render page with text  'Youll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertContainsDynamicMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(15)", "nisp.spa.under.consideration.detail", Dates.formatDate(LocalDate.of(2020, 6, 7)))
          }

          "render page with heading  'Putting of claiming'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>h2:nth-child(16)", "nisp.main.puttingOff")
          }

          "render page with text  'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            mockSetup
            assertContainsDynamicMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(17)", "nisp.main.puttingOff.line1", Dates.formatDate(LocalDate.of(2020, 6, 7)))
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>a:nth-child(18)", "nisp.main.puttingOff.linkTitle")
          }
          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertLinkHasValue(nonForeignDoc, "article.content__body>div>div>a:nth-child(18)", "https://www.gov.uk/deferring-state-pension")
          }
        }
      }

      "The scenario is No years to contribute" when {

        "State Pension page with MQP :  No Gaps || Cant get pension" should {

          def mockSetup = {
            when(mockStatePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(Right(StatePension(
                LocalDate.of(2016, 4, 5),
                amounts = StatePensionAmounts(
                  protectedPayment = false,
                  StatePensionAmountRegular(0, 0, 0),
                  StatePensionAmountForecast(0, 0, 0, 0),
                  StatePensionAmountMaximum(0, 0, 0, 0, 0),
                  StatePensionAmountRegular(0, 0, 0)
                ),
                pensionAge = 67,
                LocalDate.of(2017, 5, 4),
                "2016-17",
                4,
                pensionSharingOrder = false,
                currentFullWeeklyPensionAmount = 155.65,
                false,
                false
              )
              )))

            when(mockNationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(Right(NationalInsuranceRecord(
                qualifyingYears = 4,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 0,
                numberOfGapsPayable = 0,
                Some(LocalDate.of(1954, 3, 6)),
                false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
                ),
                reducedRateElection = false
              )
              )))
          }

          lazy val nonForeignDoc =
            asDocument(contentAsString(controller.show()(FakeRequest())))

          lazy val foreignDoc =
            asDocument(contentAsString(abroadUserController.show()(FakeRequest())))

          "render with correct page title" in {
            mockSetup
            assertElementContainsText(nonForeignDoc, "head>title" ,messages("nisp.main.h1.title") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
          }
          "render page with heading  'Your State Pension' " in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>h1.heading-large", "nisp.main.h1.title")
          }

          "render page with text  'You’ll reach State Pension age on ' " in {
            mockSetup
            val sMessage = Messages("nisp.main.description.mqp")
            assertElemetsOwnText(nonForeignDoc, "article.content__body>div>div>div:nth-child(2)>p", sMessage)
          }
          "render page with text  'You’ll reach State Pension age on 4 May 2017. ' " in {
            mockSetup
            val sMessage = Dates.formatDate(LocalDate.of(2017, 5, 4)) + "."
            assertEqualsValue(nonForeignDoc, "article.content__body>div>div>div:nth-child(2)>p>span", sMessage)
          }

          "render page with text 'By this time, you will not be able to get the 10 years needed on your National Insurance record to get any State Pension.' " in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>div:nth-child(2)>p:nth-child(2)", "nisp.mqp.notPossible")
          }

          "render page with text 'What you can do next" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>h2:nth-child(3)", "nisp.mqp.doNext")
          }

          /*overseas message*/
          "render page with text 'As you are living or working overseas (opens in new tab), you may be entitled to a " +
            "State Pension from the country you are living or working in.'" in {
            mockSetup
            assertEqualsMessage(foreignDoc, "article.content__body>div>div>p:nth-child(4)", "nisp.main.overseas")
          }
          /*Ends*/

          /*Start of Non SPA Checks*/
          "Not render page with heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertPageDoesNotContainMessage(nonForeignDoc, "nisp.spa.under.consideration.title")
          }

          "Not render page with text 'Youll reach State Pension age on 4 May 2017. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertPageDoesNotContainDynamicMessage(nonForeignDoc, "nisp.spa.under.consideration.detail", Dates.formatDate(LocalDate.of(2017, 5, 4)))
          }
          /*Ends*/

          "render page with text 'You do not have any years on your record that do not count towards your pension.'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(5)", "nisp.mqp.years.dontCount.zero")
          }
          "render page with link  'View your National Insurence Record'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(6)>a", "nisp.main.showyourrecord")
          }
          "render page with href link  'View your National Insurence Record'" in {
            mockSetup
            assertLinkHasValue(nonForeignDoc, "article.content__body>div>div>p:nth-child(6)>a", "/check-your-state-pension/account/nirecord")
          }

          "render page with text  'After State Pension age, 4 May 2017 you no longer pay National Insurance contributions.'" in {
            mockSetup
            assertContainsDynamicMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(7)", "nisp.mqp.afterSpa", Dates.formatDate(LocalDate.of(2017, 5, 4)))
          }

          "render page with text  'These details may be different if there are any changes to your National Insurance information. There is more about this in the terms and conditions'" in {
            mockSetup
            val sMessage = StringEscapeUtils.unescapeHtml4(Messages("nisp.legal.mqp.forecastChanges")) + " ."
            assertElemetsOwnText(nonForeignDoc, "article.content__body>div>div>p:nth-child(8)", sMessage)
          }
          "render page with href text  'These details may be different if there are any changes to your National Insurance information. There is more about this in the terms and conditions'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(8)>a", "nisp.legal.termsAndCondition")
          }
          "render page with href link  'These details may be different if there are any changes to your National Insurance information. There is more about this in the terms and conditions'" in {
            mockSetup
            assertLinkHasValue(nonForeignDoc, "article.content__body>div>div>p:nth-child(8)>a", "/check-your-state-pension/terms-and-conditions?showBackLink=true")
          }

          "render page with link 'What else you can do'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>h2:nth-child(9)", "nisp.mqp.whatElse")
          }
          "render page with link 'You may be eligible for Pension Credit (opens in new tab)  if your retirement income is low.'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(10)", "nisp.mqp.pensionCredit")
          }
          "render page with href link 'You may be eligible for Pension Credit (opens in new tab)  if your retirement income is low'" in {
            mockSetup
            assertLinkHasValue(nonForeignDoc, "article.content__body>div>div>p:nth-child(10)>a", expectedPensionCreditOverviewLink)
          }
          "render page with link 'Contact the Money Advice Service (opens in new tab)  for free impartial advice.'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(11)", "nisp.mqp.moneyAdvice")
          }
          "render page with href link 'Contact the Money Advice Service (opens in new tab)  for free impartial advice.'" in {
            mockSetup
            assertLinkHasValue(nonForeignDoc, "article.content__body>div>div>p:nth-child(11)>a", expectedMoneyServiceLink)
          }

        }

        "State Pension page with MQP :  No Gaps || Cant get pension: With State Pension age under consideration message" should {

          def mockSetup = {
            when(mockStatePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(Right(StatePension(
                LocalDate.of(2016, 4, 5),
                amounts = StatePensionAmounts(
                  protectedPayment = false,
                  StatePensionAmountRegular(0, 0, 0),
                  StatePensionAmountForecast(0, 0, 0, 0),
                  StatePensionAmountMaximum(0, 0, 0, 0, 0),
                  StatePensionAmountRegular(0, 0, 0)
                ),
                pensionAge = 67,
                LocalDate.of(2017, 5, 4),
                "2016-17",
                4,
                pensionSharingOrder = false,
                currentFullWeeklyPensionAmount = 155.65,
                reducedRateElection = false,
                statePensionAgeUnderConsideration = true
              )
              )))

            when(mockNationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(Right(NationalInsuranceRecord(
                qualifyingYears = 4,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 0,
                numberOfGapsPayable = 0,
                Some(LocalDate.of(1954, 3, 6)),
                false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
                ),
                reducedRateElection = false
              )
              )))

          }

          lazy val nonForeignDoc =
            asDocument(contentAsString(controller.show()(FakeRequest())))

          lazy val foreignDoc =
            asDocument(contentAsString(abroadUserController.show()(FakeRequest())))

          "render page with link 'Contact the Money Advice Service (opens in new tab)  for free impartial advice.'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(11)", "nisp.mqp.moneyAdvice")
          }
          "render page with href link 'Contact the Money Advice Service (opens in new tab)  for free impartial advice.'" in {
            mockSetup
            assertLinkHasValue(nonForeignDoc, "article.content__body>div>div>p:nth-child(11)>a", expectedMoneyServiceLink)
          }

          // SPA under consideration message
          "render page with heading  'Proposed change to your State Pension age'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>h2:nth-child(12)", "nisp.spa.under.consideration.title")
          }

          "render page with text  'Youll reach State Pension age on 4 May 2017. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertContainsDynamicMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(13)", "nisp.spa.under.consideration.detail", Dates.formatDate(LocalDate.of(2017, 5, 4)))
          }
        }

        "State Pension page with MQP :  has fillable Gaps || Personal Max" should {

          def mockSetup = {
            when(mockStatePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(Right(StatePension(
                LocalDate.of(2016, 4, 5),
                amounts = StatePensionAmounts(
                  protectedPayment = false,
                  StatePensionAmountRegular(0, 0, 0),
                  StatePensionAmountForecast(0, 0, 0, 0),
                  StatePensionAmountMaximum(2, 2, 12, 0, 0),
                  StatePensionAmountRegular(0, 0, 0)
                ),
                pensionAge = 67,
                LocalDate.of(2018, 5, 4),
                "2017-18",
                4,
                pensionSharingOrder = false,
                currentFullWeeklyPensionAmount = 155.65,
                false,
                false
              )
              )))

            when(mockNationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(Right(NationalInsuranceRecord(
                qualifyingYears = 4,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 2,
                numberOfGapsPayable = 2,
                Some(LocalDate.of(1954, 3, 6)),
                false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
                ),
                reducedRateElection = false
              )
              )))
          }

          lazy val nonForeignDoc =
            asDocument(contentAsString(controller.show()(FakeRequest())))

          lazy val foreignDoc =
            asDocument(contentAsString(abroadUserController.show()(FakeRequest())))

          "render with correct page title" in {
            mockSetup
            assertElementContainsText(nonForeignDoc, "head>title" ,messages("nisp.main.h1.title") + Constants.titleSplitter + messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk"))
          }
          "render page with heading  'Your State Pension' " in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>h1.heading-large", "nisp.main.h1.title")
          }

          "render page with text  'You’ll reach State Pension age on ' " in {
            mockSetup
            val sMessage = Messages("nisp.main.description.mqp")
            assertElemetsOwnText(nonForeignDoc, "article.content__body>div>div>div:nth-child(2)>p", sMessage)
          }
          "render page with text  'You’ll reach State Pension age on 4 May 2018. ' " in {
            mockSetup
            val sMessage = Dates.formatDate(LocalDate.of(2018, 5, 4)) + "."
            assertEqualsValue(nonForeignDoc, "article.content__body>div>div>div:nth-child(2)>p>span", sMessage)
          }

          "render page with text 'By this time, you will not have the 10 years needed on your National Insurance record to get any State Pension.' " in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>div:nth-child(2)>p:nth-child(2)", "nisp.mqp.possible")
          }

          "render page with text 'What you can do next" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>h2:nth-child(3)", "nisp.mqp.doNext")
          }

          /*overseas message*/
          "render page with text 'As you are living or working overseas (opens in new tab), you may be entitled to a " +
            "State Pension from the country you are living or working in.'" in {
            mockSetup
            assertEqualsMessage(foreignDoc, "article.content__body>div>div>p:nth-child(4)", "nisp.main.overseas")
          }
          /*Ends*/

          /*Start of Non SPA Checks*/
          "Not render page with heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertPageDoesNotContainMessage(nonForeignDoc, "nisp.spa.under.consideration.title")
          }

          "Not render page with text 'Youll reach State Pension age on 4 May 2018. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertPageDoesNotContainDynamicMessage(nonForeignDoc, "nisp.spa.under.consideration.detail", Dates.formatDate(LocalDate.of(2018, 5, 4)))
          }
          /*Ends*/

          "render page with text 'You also have 2 years on your record which do not count towards your pension because you did not contribute enough National Insurance." +
            " Filling some of these years may get you some State Pension.  '" in {
            mockSetup
            val sMessage = Messages("nisp.mqp.years.dontCount.plural", Time.years(2).toString()) + " " + Messages("nisp.mqp.filling.may.plural")
            assertEqualsValue(nonForeignDoc, "article.content__body>div>div>p:nth-child(5)", sMessage)
          }
          "render page with link  'Gaps in your record and the cost of filling them'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(6)>a", "nisp.main.context.fillGaps.viewGapsAndCost")
          }
          "render page with href link  'View your National Insurence Record'" in {
            mockSetup
            assertLinkHasValue(nonForeignDoc, "article.content__body>div>div>p:nth-child(6)>a", "/check-your-state-pension/account/nirecord/gaps")
          }

          "render page with text  'After State Pension age, 4 May 2018 you no longer pay National Insurance contributions.'" in {
            mockSetup
            assertContainsDynamicMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(7)", "nisp.mqp.afterSpa", Dates.formatDate(LocalDate.of(2018, 5, 4)))
          }

          "render page with href text  'These details may be different if there are any changes to your National Insurance information. There is more about this in the terms and conditions'" in {
            mockSetup
            val sMessage = StringEscapeUtils.unescapeHtml4(Messages("nisp.legal.mqp.forecastChanges")) + " ."
            assertElemetsOwnText(nonForeignDoc, "article.content__body>div>div>p:nth-child(8)", sMessage)
          }

          "render page with link 'What else you can do'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>h2:nth-child(9)", "nisp.mqp.whatElse")
          }
          "render page with link 'You may be eligible for Pension Credit (opens in new tab)  if your retirement income is low.'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(10)", "nisp.mqp.pensionCredit")
          }
          "render page with href link 'You may be eligible for Pension Credit (opens in new tab)  if your retirement income is low'" in {
            mockSetup
            assertLinkHasValue(nonForeignDoc, "article.content__body>div>div>p:nth-child(10)>a", expectedPensionCreditOverviewLink)
          }
          "render page with link 'Contact the Money Advice Service (opens in new tab)  for free impartial advice.'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(11)", "nisp.mqp.moneyAdvice")
          }
          "render page with href link 'Contact the Money Advice Service (opens in new tab)  for free impartial advice.'" in {
            mockSetup
            assertLinkHasValue(nonForeignDoc, "article.content__body>div>div>p:nth-child(11)>a", expectedMoneyServiceLink)
          }
        }

        "State Pension page with MQP :  has fillable Gaps || Personal Max: With State Pension age under consideration message" should {

          def mockSetup = {
            when(mockStatePensionService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(Right(StatePension(
                LocalDate.of(2016, 4, 5),
                amounts = StatePensionAmounts(
                  protectedPayment = false,
                  StatePensionAmountRegular(0, 0, 0),
                  StatePensionAmountForecast(0, 0, 0, 0),
                  StatePensionAmountMaximum(2, 2, 12, 0, 0),
                  StatePensionAmountRegular(0, 0, 0)
                ),
                pensionAge = 67,
                LocalDate.of(2018, 5, 4),
                "2017-18",
                4,
                pensionSharingOrder = false,
                currentFullWeeklyPensionAmount = 155.65,
                reducedRateElection = false,
                statePensionAgeUnderConsideration = true
              )
              )))

            when(mockNationalInsuranceService.getSummary(ArgumentMatchers.any())(ArgumentMatchers.any()))
              .thenReturn(Future.successful(Right(NationalInsuranceRecord(
                qualifyingYears = 4,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 2,
                numberOfGapsPayable = 2,
                Some(LocalDate.of(1954, 3, 6)),
                false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
                ),
                reducedRateElection = false
              )
              )))
          }

          lazy val nonForeignDoc =
            asDocument(contentAsString(controller.show()(FakeRequest())))

          lazy val foreignDoc =
            asDocument(contentAsString(abroadUserController.show()(FakeRequest())))

          "render page with link 'Contact the Money Advice Service (opens in new tab)  for free impartial advice.'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(11)", "nisp.mqp.moneyAdvice")
          }
          "render page with href link 'Contact the Money Advice Service (opens in new tab)  for free impartial advice.'" in {
            mockSetup
            assertLinkHasValue(nonForeignDoc, "article.content__body>div>div>p:nth-child(11)>a", expectedMoneyServiceLink)
          }

          // SPA under consideration message
          "render page with heading  'Proposed change to your State Pension age'" in {
            mockSetup
            assertEqualsMessage(nonForeignDoc, "article.content__body>div>div>h2:nth-child(12)", "nisp.spa.under.consideration.title")
          }

          "render page with text  'Youll reach State Pension age on 4 May 2018. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertContainsDynamicMessage(nonForeignDoc, "article.content__body>div>div>p:nth-child(13)", "nisp.spa.under.consideration.detail", Dates.formatDate(LocalDate.of(2018, 5, 4)))
          }
        }
      }
    }
  }
}
