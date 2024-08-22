/*
 * Copyright 2024 HM Revenue & Customs
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

import org.apache.commons.text.StringEscapeUtils
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import play.api.i18n.Messages
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{Injector, bind}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers.contentAsString
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.http.{SessionKeys, UpstreamErrorResponse}
import uk.gov.hmrc.nisp.builders.NationalInsuranceTaxYearBuilder
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.StatePensionController
import uk.gov.hmrc.nisp.controllers.auth.{AuthRetrievals, PertaxAuthAction}
import uk.gov.hmrc.nisp.controllers.pertax.PertaxHelper
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.services.{NationalInsuranceService, StatePensionService}
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.nisp.views.formatting.Time
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.language.LanguageUtils

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.Future

class StatePension_MQPViewSpec extends HtmlSpec with Injecting with WireMockSupport {

  val expectedMoneyServiceLink          = "https://www.moneyadviceservice.org.uk/en"
  val expectedPensionCreditOverviewLink = "https://www.gov.uk/pension-credit/overview"

  def generateFakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    SessionKeys.sessionId            -> s"session-${UUID.randomUUID()}",
    SessionKeys.lastRequestTimestamp -> LocalDate.now.toEpochDay.toString
  )

  val mockAuditConnector: AuditConnector                     = mock[AuditConnector]
  val mockNationalInsuranceService: NationalInsuranceService = mock[NationalInsuranceService]
  val mockStatePensionService: StatePensionService           = mock[StatePensionService]
  val mockAppConfig: ApplicationConfig                       = mock[ApplicationConfig]
  val mockPertaxHelper: PertaxHelper                         = mock[PertaxHelper]

  lazy val langUtils: LanguageUtils = inject[LanguageUtils]

  val standardInjector: Injector = GuiceApplicationBuilder()
    .overrides(
      bind[AuthRetrievals].to[FakeAuthAction],
      bind[StatePensionService].toInstance(mockStatePensionService),
      bind[NationalInsuranceService].toInstance(mockNationalInsuranceService),
      bind[AuditConnector].toInstance(mockAuditConnector),
      bind[ApplicationConfig].toInstance(mockAppConfig),
      bind[PertaxHelper].toInstance(mockPertaxHelper),
      bind[PertaxAuthAction].to[FakePertaxAuthAction]
    )
    .build()
    .injector

  val abroadUserInjector: Injector = GuiceApplicationBuilder()
    .overrides(
      bind[StatePensionService].toInstance(mockStatePensionService),
      bind[NationalInsuranceService].toInstance(mockNationalInsuranceService),
      bind[AuditConnector].toInstance(mockAuditConnector),
      bind[ApplicationConfig].toInstance(mockAppConfig),
      bind[PertaxHelper].toInstance(mockPertaxHelper),
      bind[AuthRetrievals].to[FakeAuthActionWithNino],
      bind[NinoContainer].toInstance(AbroadNinoContainer),
      bind[PertaxAuthAction].to[FakePertaxAuthAction]
    )
    .build()
    .injector

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockStatePensionService)
    reset(mockNationalInsuranceService)
    reset(mockAuditConnector)
    reset(mockAppConfig)
    reset(mockPertaxHelper)
    when(mockPertaxHelper.isFromPertax(any())).thenReturn(Future.successful(false))
    when(mockAppConfig.accessibilityStatementUrl(any())).thenReturn("/foo")
    when(mockAppConfig.reportAProblemNonJSUrl).thenReturn("/reportAProblem")
    when(mockAppConfig.contactFormServiceIdentifier).thenReturn("/id")
    wireMockServer.resetAll()
    when(mockAppConfig.pertaxAuthBaseUrl).thenReturn(s"http://localhost:${wireMockServer.port()}")
  }

  lazy val controller: StatePensionController = standardInjector.instanceOf[StatePensionController]
  lazy val abroadUserController: StatePensionController = abroadUserInjector.instanceOf[StatePensionController]

  "The State Pension page" when {

    "the user is a MQP" when {

      "State Pension page with forecast only" should {

        lazy val nonForeignDoc =
          asDocument(contentAsString(controller.show()(generateFakeRequest)))

        lazy val foreignDoc =
          asDocument(contentAsString(abroadUserController.show()(generateFakeRequest)))

        def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
          when(mockStatePensionService.getSummary(any())(any()))
            .thenReturn(Future.successful(Right(Right(StatePension(
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
              statePensionAgeUnderConsideration = false
            )
            ))))

          when(mockNationalInsuranceService.getSummary(any())(any()))
            .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
              qualifyingYears = 11,
              qualifyingYearsPriorTo1975 = 0,
              numberOfGaps = 1,
              numberOfGapsPayable = 1,
              Some(LocalDate.of(1954, 3, 6)),
              homeResponsibilitiesProtection = false,
              LocalDate.of(2017, 4, 5),
              List(

                NationalInsuranceTaxYearBuilder("2015-16", underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false) /*payable = true*/
              ),
              reducedRateElection = false
            )
            ))))
        }

        "render with correct page title" in {
          mockSetup
          assertElementContainsText(
            nonForeignDoc,
            "head > title",
            messages("nisp.main.h1.title")
              + Constants.titleSplitter
              + messages("nisp.title.extension")
              + Constants.titleSplitter
              + messages("nisp.gov-uk")
          )
        }

        "render page with heading 'Your State Pension' " in {
          mockSetup
          assertEqualsMessage(
            nonForeignDoc,
            "[data-spec='state_pension_forecast_only__h1']",
            "nisp.main.h1.title"
          )
        }

        "render page with text 'You can get your State Pension on 7 june 2020' " in {
          mockSetup
          assertEqualsValue(
            nonForeignDoc,
            "[data-spec='state_pension_forecast_only__panel1'] [data-component='nisp_panel__title']",
            Messages("nisp.main.basedOn") + " " +
              langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
          )
        }

        "render page with text 'Your forecast is £150.71 a week, £590.10 a month, £7,081.15 a year' " in {
          mockSetup
          val sMessage =
            Messages("nisp.main.caveats") + " " +
              Messages("nisp.is") + " £150.71 " +
              Messages("nisp.main.week") + ", £590.10 " +
              Messages("nisp.main.month") + ", £7,081.15 " +
              Messages("nisp.main.year")
          assertEqualsValue(
            nonForeignDoc,
            "[data-spec='state_pension__panel1__forecast__caveats']",
            sMessage
          )
        }

        "render page with text 'Your forecast '" in {
          mockSetup
          assertEqualsMessage(
            nonForeignDoc,
            "[data-spec='state_pension_forecast_only__p__caveats']",
            "nisp.main.caveats"
          )
        }

        "render page with text 'is not a guarantee and is based on the current law '" in {
          mockSetup
          assertEqualsMessage(
            nonForeignDoc,
            "[data-spec='state_pension_forecast_only__ul__caveats__1']",
            "nisp.main.notAGuarantee"
          )
        }

        "render page with text ' is based on your National Insurance record up to 5 April 2016 '" in {
          mockSetup
          assertContainsDynamicMessage(
            nonForeignDoc,
            "[data-spec='state_pension_forecast_only__ul__caveats__2']",
            "nisp.main.isBased",
            langUtils.Dates.formatDate(LocalDate.of(2016, 4, 5))
          )
        }

        "render page with text ' does not include any increase due to inflation '" in {
          mockSetup
          assertEqualsMessage(
            nonForeignDoc,
            "[data-spec='state_pension_forecast_only__ul__caveats__3']",
            "nisp.main.inflation"
          )
        }

        "render page with heading ' £155.55 is the most you can get'" in {
          mockSetup
          val sMaxCanGet = "£150.71 " + Messages("nisp.main.mostYouCanGet")
          assertEqualsValue(
            nonForeignDoc,
            "[data-spec='state_pension_forecast_only__h2_1']",
            sMaxCanGet
          )
        }

        "render page with text 'You cannot improve your forecast any further.'" in {
          mockSetup
          assertEqualsMessage(
            nonForeignDoc,
            "[data-spec='state_pension_forecast_only__p3']",
            "nisp.main.cantImprove"
          )
        }

        "render page with text 'If you’re working you may still need to pay National Insurance contributions until 7 June 2020 as they fund other state benefits and the NHS.'" in {
          mockSetup
          assertContainsDynamicMessage(
            foreignDoc,
            "[data-spec='state_pension_forecast_only__p4']",
            "nisp.main.context.reachMax.needToPay",
            langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
          )
        }

        "render page with link 'View your National Insurance Record'" in {
          mockSetup
          assertEqualsMessage(
            nonForeignDoc,
            "[data-spec='state_pension_forecast_only__link1']",
            "nisp.main.showyourrecord"
          )
        }

        "render page with href link 'View your National Insurance Record'" in {
          mockSetup
          assertLinkHasValue(
            foreignDoc,
            "[data-spec='state_pension_forecast_only__link1']",
            "/check-your-state-pension/account/nirecord"
          )
        }

        /*overseas message*/
        "render page with text 'As you are living or working overseas (opens in new tab), " +
          "you may be entitled to a State Pension from the country you are living or working in.'" in {
            mockSetup
            assertEqualsValue(
              foreignDoc,
              "[data-spec='abroad__inset_text__link1']",
              messages("nisp.main.overseas.linktext", messages("nisp.main.overseas.text"))
            )
          }
        /*Ends*/

        /*Start of Non SPA Checks*/
        "NOT render page with heading 'Proposed change to your State Pension age'" in {
          mockSetup
          assertPageDoesNotContainMessage(
            nonForeignDoc,
            "nisp.spa.under.consideration.title"
          )
        }

        "Not render page with text 'You’ll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
          mockSetup
          assertPageDoesNotContainDynamicMessage(
            foreignDoc,
            "nisp.spa.under.consideration.detail",
            "7 June 2020"
          )
        }
        /*Ends*/

        "render page with heading 'Putting off claiming'" in {
          mockSetup
          assertEqualsMessage(
            foreignDoc,
            "[data-spec='deferral__h2_1']",
            "nisp.main.puttingOff"
          )
        }

        "render page with text 'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
          mockSetup
          assertContainsDynamicMessage(
            foreignDoc,
            "[data-spec='deferral__p1']",
            "nisp.main.puttingOff.line1",
            langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
          )
        }

        "render page with link 'More on putting off claiming (opens in new tab)'" in {
          mockSetup
          assertEqualsMessage(
            foreignDoc,
            "[data-spec='deferral__link1']",
            "nisp.main.puttingOff.linkTitle"
          )
        }

        "render page with href link 'More on putting off claiming (opens in new tab)'" in {
          mockSetup
          assertLinkHasValue(
            foreignDoc,
            "[data-spec='deferral__link1']",
            "https://www.gov.uk/deferring-state-pension"
          )
        }

        /*Side bar help*/
        "render page with heading 'Get help'" in {
          mockSetup
          assertEqualsMessage(
            nonForeignDoc,
            "[data-spec='state_pension_forecast_only__sidebar_h2']",
            "nisp.nirecord.helpline.getHelp"
          )
        }

        "render page with text 'Helpline 0800 731 0181'" in {
          mockSetup
          assertEqualsMessage(
            nonForeignDoc,
            "[data-spec='state_pension_forecast_only__sidebar_p1']",
            "nisp.nirecord.helpline.number"
          )
        }

        "render page with text 'Textphone 0800 731 0176'" in {
          mockSetup
          assertEqualsMessage(
            nonForeignDoc,
            "[data-spec='state_pension_forecast_only__sidebar_p2']",
            "nisp.nirecord.helpline.textNumber"
          )
        }

        "render page with text 'Monday to Friday: 8am to 6pm'" in {
          mockSetup
          assertEqualsMessage(
            nonForeignDoc,
            "[data-spec='state_pension_forecast_only__sidebar_p3']",
            "nisp.nirecord.helpline.openTimes"
          )
        }
      }

      "State Pension page with forecast only: With State Pension age under consideration message" should {

        def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
          when(mockStatePensionService.getSummary(any())(any()))
            .thenReturn(Future.successful(Right(Right(StatePension(
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
            ))))

          when(mockNationalInsuranceService.getSummary(any())(any()))
            .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
              qualifyingYears = 11,
              qualifyingYearsPriorTo1975 = 0,
              numberOfGaps = 1,
              numberOfGapsPayable = 1,
              Some(LocalDate.of(1954, 3, 6)),
              homeResponsibilitiesProtection = false,
              LocalDate.of(2017, 4, 5),
              List(

                NationalInsuranceTaxYearBuilder("2015-16", underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false) /*payable = true*/
              ),
              reducedRateElection = false
            )
            ))))
        }

        lazy val foreignDoc =
          asDocument(contentAsString(abroadUserController.show()(FakeRequest())))

        "render page with text 'As you are living or working overseas (opens in new tab), " +
          "you may be entitled to a State Pension from the country you are living or working in.'" in {
            mockSetup
            assertEqualsValue(
              foreignDoc,
              "[data-spec='abroad__inset_text__link1']",
              messages("nisp.main.overseas.linktext", messages("nisp.main.overseas.text"))
            )
          }

        "render page with heading 'Proposed change to your State Pension age'" in {
          mockSetup
          assertEqualsMessage(
            foreignDoc,
            "[data-spec='state_pension_age_under_consideration__h2_1']",
            "nisp.spa.under.consideration.title"
          )
        }

        "render page with text 'You’ll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
          mockSetup
          assertContainsDynamicMessage(
            foreignDoc,
            "[data-spec='state_pension_age_under_consideration__p1']",
            "nisp.spa.under.consideration.detail",
            langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
          )
        }

        // deferral message
        "render page with heading 'Putting off claiming'" in {
          mockSetup
          assertEqualsMessage(
            foreignDoc,
            "[data-spec='deferral__h2_1']",
            "nisp.main.puttingOff"
          )
        }

        "render page with text 'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
          mockSetup
          assertContainsDynamicMessage(
            foreignDoc,
            "[data-spec='deferral__p1']",
            "nisp.main.puttingOff.line1",
            langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
          )
        }

        "render page with link 'More on putting off claiming (opens in new tab)'" in {
          mockSetup
          assertEqualsMessage(
            foreignDoc,
            "[data-spec='deferral__link1']",
            "nisp.main.puttingOff.linkTitle"
          )
        }

        "render page with href link 'More on putting off claiming (opens in new tab)'" in {
          mockSetup
          assertLinkHasValue(
            foreignDoc,
            "[data-spec='deferral__link1']",
            "https://www.gov.uk/deferring-state-pension"
          )
        }
      }

      "The scenario is continue working" when {

        "State Pension page with MQP : Continue Working || Fill Gaps || Full Rate" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(StatePension(
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
                statePensionAgeUnderConsideration = false
              )
              ))))

            when(mockNationalInsuranceService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 4,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 2,
                numberOfGapsPayable = 2,
                Some(LocalDate.of(1954, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false) /*payable = true*/
                ),
                reducedRateElection = false
              )
              ))))
          }

          lazy val nonForeignDoc =
            asDocument(contentAsString(controller.show()(FakeRequest())))

          lazy val foreignDoc =
            asDocument(contentAsString(abroadUserController.show()(FakeRequest())))

          "render with correct page title" in {
            mockSetup
            assertElementContainsText(
              nonForeignDoc,
              "head > title",
              messages("nisp.main.h1.title")
                + Constants.titleSplitter
                + messages("nisp.title.extension")
                + Constants.titleSplitter
                + messages("nisp.gov-uk")
            )
          }

          "render page with heading 'Your State Pension' " in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension__pageheading'] [data-component='nisp_page_heading__h1']",
              "nisp.main.h1.title"
            )
          }

          "render page with text 'You can get your State Pension on 7 june 2020' " in {
            mockSetup
            assertEqualsValue(
              nonForeignDoc,
              "[data-component='nisp_panel'] [data-component='nisp_panel__title']",
              Messages("nisp.main.basedOn") + " " + langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
            )
          }

          "render page with text 'Your forecast is £137.86 a week, £599.44 a month, £7,193.34 a year' " in {
            mockSetup
            val sMessage =
              Messages("nisp.main.caveats") + " " +
                Messages("nisp.is") + " £137.86 " +
                Messages("nisp.main.week") + ", £599.44 " +
                Messages("nisp.main.month") + ", £7,193.34 " +
                Messages("nisp.main.year")
            assertEqualsValue(
              nonForeignDoc,
              "[data-spec='state_pension__panel1__caveats']",
              sMessage
            )
          }

          "render page with text ' Your forecast '" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension__p_caveats__mqp']",
              "nisp.main.caveats"
            )
          }

          "render page with text ' is not a guarantee and is based on the current law '" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension__ul__caveats__mqp_li1']",
              "nisp.main.notAGuarantee"
            )
          }

          "render page with text ' is based on your National Insurance record up to 5 April 2016 '" in {
            mockSetup
            assertContainsDynamicMessage(
              nonForeignDoc,
              "[data-spec='state_pension__ul__caveats__mqp_li2']",
              "nisp.main.isBased",
              langUtils.Dates.formatDate(LocalDate.of(2016, 4, 5))
            )
          }

          "render page with text ' assumes that you’ll contribute another 4 years '" in {
            mockSetup
            assertContainsDynamicMessage(
              nonForeignDoc,
              "[data-spec='state_pension__ul__caveats__mqp_li3']",
              "nisp.mqp.howManyToContribute",
              Time.years(4)
            )
          }

          "render page with text ' does not include any increase due to inflation '" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension__ul__caveats__mqp_li4']",
              "nisp.main.inflation"
            )
          }

          "render page with text ' You currently have 4 years on your record and you need at least 10 years to get any State Pension. '" in {
            mockSetup
            assertContainsDynamicMessage(
              nonForeignDoc,
              "[data-spec='state_pension__p2a']",
              "nisp.mqp.youCurrentlyHave",
              Time.years(4),
              Constants.minimumQualifyingYearsNSP.toString,
              null
            )
          }

          "render page with heading ' You can improve your forecast'" in {
            mockSetup
            assertEqualsMessage(
              foreignDoc,
              "[data-spec='fill_gaps_mqp__h2_1']",
              "nisp.main.context.fillGaps.improve.title"
            )
          }

          "render page with text 'You have years on your record where you did not contribute enough National Insurance and you can make up the shortfall. " +
            "This will make these years count towards your pension and may improve your forecast.'" in {
              mockSetup
              assertEqualsMessage(
                foreignDoc,
                "[data-spec='fill_gaps_mqp__p1']",
                "nisp.main.context.improve.para1.plural"
              )
            }

          "render page with link 'View gaps in your record'" in {
            mockSetup
            assertEqualsMessage(
              foreignDoc,
              "[data-spec='fill_gaps_mqp__link1']",
              "nisp.main.context.fillGaps.viewGaps"
            )
          }

          "render page with href link 'View gaps in your record'" in {
            mockSetup
            assertLinkHasValue(
              foreignDoc,
              "[data-spec='fill_gaps_mqp__link1']",
              "/check-your-state-pension/account/nirecord/gaps"
            )
          }

          "render page with href text 'Your forecast may be different if there are any changes to your National Insurance information. There is more about this in the terms and conditions'" in {
            mockSetup
            assertEqualsValue(
              nonForeignDoc,
              "[data-spec='state_pension__legal__forecast_changes__p']",
              messages("nisp.legal.forecastChanges", messages("nisp.legal.terms.and.conditions")) + "."
            )
            assertLinkHasValue(
              nonForeignDoc,
              "[data-spec='state_pension__legal__forecast_changes__link']",
              "/check-your-state-pension/terms-and-conditions?showBackLink=true"
            )
          }

          /*overseas message*/
          "render page with text 'As you are living or working overseas (opens in new tab) " +
            "you may be entitled to a State Pension from the country you are living or working in.'" in {
              mockSetup
              assertEqualsValue(
                foreignDoc,
                "[data-spec='abroad__inset_text_1']",
                messages("nisp.main.overseas.text", messages("nisp.main.overseas.linktext"))
              )
            }
          /*Ends*/

          /*Start of Non SPA Checks*/
          "Not render page with heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertPageDoesNotContainMessage(
              nonForeignDoc,
              "nisp.spa.under.consideration.title"
            )
          }

          "Not render page with text 'You’ll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertPageDoesNotContainDynamicMessage(
              nonForeignDoc,
              "nisp.spa.under.consideration.detail",
              langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
            )
          }
          /*Ends*/

          "render page with heading 'Putting off claiming'" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='deferral__h2_1']",
              "nisp.main.puttingOff"
            )
          }

          "render page with text 'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            mockSetup
            assertContainsDynamicMessage(
              nonForeignDoc,
              "[data-spec='deferral__p1']",
              "nisp.main.puttingOff.line1",
              langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
            )
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='deferral__link1']",
              "nisp.main.puttingOff.linkTitle"
            )
          }

          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertLinkHasValue(
              nonForeignDoc,
              "[data-spec='deferral__link1']",
              "https://www.gov.uk/deferring-state-pension"
            )
          }

          /*Side bar help*/
          "render page with heading 'Get help'" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension__sidebar_h2']",
              "nisp.nirecord.helpline.getHelp"
            )
          }

          "render page with text 'Helpline 0800 731 0181'" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension__sidebar_p1']",
              "nisp.nirecord.helpline.number"
            )
          }

          "render page with text 'Textphone 0800 731 0176'" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension__sidebar_p2']",
              "nisp.nirecord.helpline.textNumber"
            )
          }

          "render page with text 'Monday to Friday: 8am to 6pm'" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension__sidebar_p3']",
              "nisp.nirecord.helpline.openTimes"
            )
          }
        }

        "State Pension page with MQP : Continue Working || Fill Gaps || Full Rate: With State Pension age under consideration message" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(StatePension(
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
              ))))

            when(mockNationalInsuranceService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 4,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 2,
                numberOfGapsPayable = 2,
                Some(LocalDate.of(1954, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false) /*payable = true*/
                ),
                reducedRateElection = false
              )
              ))))
          }

          lazy val nonForeignDoc =
            asDocument(contentAsString(controller.show()(FakeRequest())))

          lazy val foreignDoc =
            asDocument(contentAsString(abroadUserController.show()(FakeRequest())))

          // overseas message
          "render page with text 'As you are living or working overseas (opens in new tab), " +
            "you may be entitled to a State Pension from the country you are living or working in.'" in {
              mockSetup
              assertEqualsValue(
                foreignDoc,
                "[data-spec='abroad__inset_text__link1']",
                messages("nisp.main.overseas.linktext", messages("nisp.main.overseas.text"))
              )
            }

          // SPA under consideration message
          "render page with heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension_age_under_consideration__h2_1']",
              "nisp.spa.under.consideration.title"
            )
          }

          "render page with text 'You’ll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertContainsDynamicMessage(
              nonForeignDoc,
              "[data-spec='state_pension_age_under_consideration__p1']",
              "nisp.spa.under.consideration.detail",
              langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
            )
          }

          // deferral message
          "render page with heading 'Putting off claiming'" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='deferral__h2_1']",
              "nisp.main.puttingOff"
            )
          }

          "render page with text 'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            mockSetup
            assertContainsDynamicMessage(
              nonForeignDoc,
              "[data-spec='deferral__p1']",
              "nisp.main.puttingOff.line1",
              langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
            )
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='deferral__link1']",
              "nisp.main.puttingOff.linkTitle"
            )
          }

          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertLinkHasValue(
              nonForeignDoc,
              "[data-spec='deferral__link1']",
              "https://www.gov.uk/deferring-state-pension"
            )
          }
        }

        "State Pension page with MQP : Continue Working || no gaps || Full Rate" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(StatePension(
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
                statePensionAgeUnderConsideration = false
              )
              ))))

            when(mockNationalInsuranceService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 4,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 0,
                numberOfGapsPayable = 0,
                Some(LocalDate.of(1954, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false) /*payable = true*/
                ),
                reducedRateElection = false
              )
              ))))
          }

          lazy val nonForeignDoc =
            asDocument(contentAsString(controller.show()(FakeRequest())))

          lazy val foreignDoc =
            asDocument(contentAsString(abroadUserController.show()(FakeRequest())))

          "render with correct page title" in {
            mockSetup
            assertElementContainsText(
              nonForeignDoc,
              "head > title",
              messages("nisp.main.h1.title")
                + Constants.titleSplitter
                + messages("nisp.title.extension")
                + Constants.titleSplitter
                + messages("nisp.gov-uk")
            )
          }

          "render page with heading 'Your State Pension' " in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension__pageheading'] [data-component='nisp_page_heading__h1']",
              "nisp.main.h1.title"
            )
          }

          "render page with text 'You can get your State Pension on 7 june 2020' " in {
            mockSetup
            assertEqualsValue(
              nonForeignDoc,
              "[data-component='nisp_panel'] [data-component='nisp_panel__title']",
              Messages("nisp.main.basedOn") + " " + langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
            )
          }

          "render page with text 'Your forecast is £155.65 a week, £599.44 a month, £7,193.34 a year' " in {
            mockSetup
            val sMessage =
              Messages("nisp.main.caveats") + " " +
                Messages("nisp.is") + " £155.65 " +
                Messages("nisp.main.week") + ", £599.44 " +
                Messages("nisp.main.month") + ", £7,193.34 " +
                Messages("nisp.main.year")
            assertEqualsValue(
              nonForeignDoc,
              "[data-spec='state_pension__panel1__caveats']",
              sMessage
            )
          }

          "render page with text ' Your forecast '" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension__p_caveats__mqp']",
              "nisp.main.caveats"
            )
          }

          "render page with text ' is not a guarantee and is based on the current law '" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension__ul__caveats__mqp_li1']",
              "nisp.main.notAGuarantee"
            )
          }

          "render page with text ' is based on your National Insurance record up to 5 April 2016 '" in {
            mockSetup
            assertContainsDynamicMessage(
              nonForeignDoc,
              "[data-spec='state_pension__ul__caveats__mqp_li2']",
              "nisp.main.isBased",
              langUtils.Dates.formatDate(LocalDate.of(2016, 4, 5))
            )
          }

          "render page with text ' assumes that you’ll contribute another 4 years '" in {
            mockSetup
            assertContainsDynamicMessage(
              nonForeignDoc,
              "[data-spec='state_pension__ul__caveats__mqp_li3']",
              "nisp.mqp.howManyToContribute",
              Time.years(4)
            )
          }

          "render page with text ' does not include any increase due to inflation '" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension__ul__caveats__mqp_li4']",
              "nisp.main.inflation"
            )
          }

          "render page with text ' You currently have 4 years on your record and you need at least 10 years to get any State Pension. '" in {
            mockSetup
            assertContainsDynamicMessage(
              nonForeignDoc,
              "[data-spec='state_pension__p2a']",
              "nisp.mqp.youCurrentlyHave",
              Time.years(4),
              Constants.minimumQualifyingYearsNSP.toString,
              null
            )
          }

          "render page with heading '£155.65 is the most you can get'" in {
            mockSetup
            val sMessage = "£155.65 " +
              StringEscapeUtils.unescapeHtml4(Messages("nisp.main.mostYouCanGet"))
            assertEqualsValue(
              nonForeignDoc,
              "[data-spec='continue_working__mqp_scenario_h2_1']",
              sMessage
            )
          }

          "render page with text 'You cannot improve your forecast any further.'" in {
            mockSetup
            assertEqualsMessage(
              foreignDoc,
              "[data-spec='continue_working__mqp_scenario_p1']",
              "nisp.main.context.willReach"
            )
          }

          "render page with text 'If you’re working you may still need to pay National Insurance contributions until 7 June 2020 as they fund other state benefits and the NHS.'" in {
            mockSetup
            assertContainsDynamicMessage(
              foreignDoc,
              "[data-spec='continue_working__mqp_scenario_p2']",
              "nisp.main.context.reachMax.needToPay",
              langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
            )
          }

          "render page with link 'View your National Insurance Record'" in {
            mockSetup
            assertEqualsMessage(
              foreignDoc,
              "[data-spec='continue_working__link1']",
              "nisp.main.showyourrecord"
            )
          }

          "render page with href link 'View your National Insurance Record'" in {
            mockSetup
            assertLinkHasValue(
              foreignDoc,
              "[data-spec='continue_working__link1']",
              "/check-your-state-pension/account/nirecord"
            )
          }

          "render page with href text 'Your forecast may be different if there are any changes to your National Insurance information. There is more about this in the terms and conditions'" in {
            mockSetup
            assertEqualsValue(
              nonForeignDoc,
              "[data-spec='state_pension__legal__forecast_changes__p']",
              messages("nisp.legal.forecastChanges", messages("nisp.legal.terms.and.conditions")) + "."
            )
            assertLinkHasValue(
              nonForeignDoc,
              "[data-spec='state_pension__legal__forecast_changes__link']",
              "/check-your-state-pension/terms-and-conditions?showBackLink=true"
            )
          }

          /*overseas message*/
          "render page with text 'As you are living or working overseas (opens in new tab), " +
            "you may be entitled to a State Pension from the country you are living or working in.'" in {
              mockSetup
              assertEqualsValue(
                foreignDoc,
                "[data-spec='abroad__inset_text__link1']",
                messages("nisp.main.overseas.linktext", messages("nisp.main.overseas.text"))
              )
            }

          "Not render page with heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertPageDoesNotContainMessage(
              nonForeignDoc,
              "nisp.spa.under.consideration.title"
            )
          }

          "Not render page with text 'You’ll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertPageDoesNotContainDynamicMessage(
              nonForeignDoc,
              "nisp.spa.under.consideration.detail",
              langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
            )
          }

          "render page with heading 'Putting off claiming'" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='deferral__h2_1']",
              "nisp.main.puttingOff"
            )
          }

          "render page with text 'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            mockSetup
            assertContainsDynamicMessage(
              nonForeignDoc,
              "[data-spec='deferral__p1']",
              "nisp.main.puttingOff.line1",
              langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
            )
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='deferral__link1']",
              "nisp.main.puttingOff.linkTitle"
            )
          }

          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertLinkHasValue(
              nonForeignDoc,
              "[data-spec='deferral__link1']",
              "https://www.gov.uk/deferring-state-pension"
            )
          }
        }

        "State Pension page with MQP : Continue Working || no gaps || Full Rate: With State Pension age under consideration message" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(StatePension(
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
              ))))

            when(mockNationalInsuranceService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 4,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 0,
                numberOfGapsPayable = 0,
                Some(LocalDate.of(1954, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false) /*payable = true*/
                ),
                reducedRateElection = false
              )
              ))))
          }

          lazy val nonForeignDoc =
            asDocument(contentAsString(controller.show()(FakeRequest())))

          lazy val foreignDoc =
            asDocument(contentAsString(abroadUserController.show()(FakeRequest())))

          // overseas message
          "render page with text 'As you are living or working overseas (opens in new tab), " +
            "you may be entitled to a State Pension from the country you are living or working in.'" in {
              mockSetup
              assertEqualsValue(
                foreignDoc,
                "[data-spec='abroad__inset_text__link1']",
                messages("nisp.main.overseas.linktext", messages("nisp.main.overseas.text"))
              )
            }

          "render page with heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension_age_under_consideration__h2_1']",
              "nisp.spa.under.consideration.title"
            )
          }

          "render page with text 'You’ll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertContainsDynamicMessage(
              nonForeignDoc,
              "[data-spec='state_pension_age_under_consideration__p1']",
              "nisp.spa.under.consideration.detail",
              langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
            )
          }

          "render page with heading 'Putting off claiming'" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='deferral__h2_1']",
              "nisp.main.puttingOff"
            )
          }

          "render page with text 'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            mockSetup
            assertContainsDynamicMessage(
              nonForeignDoc,
              "[data-spec='deferral__p1']",
              "nisp.main.puttingOff.line1",
              langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
            )
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='deferral__link1']",
              "nisp.main.puttingOff.linkTitle"
            )
          }

          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertLinkHasValue(
              nonForeignDoc,
              "[data-spec='deferral__link1']",
              "https://www.gov.uk/deferring-state-pension"
            )
          }
        }

        "State Pension page with MQP : Continue Working || 0 Qualify Years || has fillable Gaps ||  Personal Max" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(StatePension(
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
                statePensionAgeUnderConsideration = false
              )
              ))))

            when(mockNationalInsuranceService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 0,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 1,
                numberOfGapsPayable = 1,
                Some(LocalDate.of(1954, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false) /*payable = true*/
                ),
                reducedRateElection = false
              )
              ))))
          }

          lazy val nonForeignDoc =
            asDocument(contentAsString(controller.show()(FakeRequest())))

          lazy val foreignDoc =
            asDocument(contentAsString(abroadUserController.show()(FakeRequest())))

          "render with correct page title" in {
            mockSetup
            assertElementContainsText(
              nonForeignDoc,
              "head > title",
              messages("nisp.main.h1.title")
                + Constants.titleSplitter
                + messages("nisp.title.extension")
                + Constants.titleSplitter
                + messages("nisp.gov-uk")
            )
          }

          "render page with heading 'Your State Pension' " in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension__pageheading'] [data-component='nisp_page_heading__h1']",
              "nisp.main.h1.title"
            )
          }

          "render page with text 'You can get your State Pension on 7 june 2020' " in {
            mockSetup
            assertEqualsValue(
              nonForeignDoc,
              "[data-spec='state_pension__panel1'] [data-component='nisp_panel__title']",
              Messages("nisp.main.basedOn") + " " + langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
            )
          }

          "render page with text 'Your forecast is £155.65 a week, £599.44 a month, £7,193.34 a year' " in {
            mockSetup
            val sMessage =
              Messages("nisp.main.caveats") + " " +
                Messages("nisp.is") + " £155.65 " +
                Messages("nisp.main.week") + ", £599.44 " +
                Messages("nisp.main.month") + ", £7,193.34 " +
                Messages("nisp.main.year")
            assertEqualsValue(
              nonForeignDoc,
              "[data-spec='state_pension__panel1__caveats']",
              sMessage
            )
          }

          "render page with text ' Your forecast '" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension__p_caveats__mqp']",
              "nisp.main.caveats"
            )
          }

          "render page with text ' is not a guarantee and is based on the current law '" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension__ul__caveats__mqp_li1']",
              "nisp.main.notAGuarantee"
            )
          }

          "render page with text ' is based on your National Insurance record up to 5 April 2016 '" in {
            mockSetup
            assertContainsDynamicMessage(
              nonForeignDoc,
              "[data-spec='state_pension__ul__caveats__mqp_li2']",
              "nisp.main.isBased",
              langUtils.Dates.formatDate(LocalDate.of(2016, 4, 5))
            )
          }

          "render page with text ' assumes that you’ll contribute another 4 years '" in {
            mockSetup
            assertContainsDynamicMessage(
              nonForeignDoc,
              "[data-spec='state_pension__ul__caveats__mqp_li3']",
              "nisp.mqp.howManyToContribute",
              Time.years(4)
            )
          }

          "render page with text ' does not include any increase due to inflation '" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension__ul__caveats__mqp_li4']",
              "nisp.main.inflation"
            )
          }

          "render page with text 'You do not have any years on your record and you need at least 10 years to get any State Pension. '" in {
            mockSetup
            assertContainsDynamicMessage(
              nonForeignDoc,
              "[data-spec='state_pension__p2b']",
              "nisp.mqp.youCurrentlyHaveZero",
              Constants.minimumQualifyingYearsNSP.toString
            )
          }

          "render page with heading '£155.65 is the most you can get'" in {
            mockSetup
            val sMessage = "£155.65 " + Messages("nisp.main.mostYouCanGet")
            assertEqualsValue(
              nonForeignDoc,
              "[data-spec='continue_working__mqp_scenario_h2_1']",
              sMessage
            )
          }

          "render page with text 'You cannot improve your forecast any further, unless you choose to put off claiming'" in {
            mockSetup
            assertEqualsMessage(
              foreignDoc,
              "[data-spec='continue_working__mqp_scenario_p1']",
              "nisp.main.context.willReach"
            )
          }

          "render page with text 'If you’re working you may still need to pay National Insurance contributions until 7 June 2020 as they fund other state benefits and the NHS.'" in {
            mockSetup
            assertContainsDynamicMessage(
              foreignDoc,
              "[data-spec='continue_working__mqp_scenario_p2']",
              "nisp.main.context.reachMax.needToPay",
              langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
            )
          }

          "render page with link 'View your National Insurance Record'" in {
            mockSetup
            assertEqualsMessage(
              foreignDoc,
              "[data-spec='continue_working__link1']",
              "nisp.main.showyourrecord"
            )
          }

          "render page with href link 'View your National Insurance Record'" in {
            mockSetup
            assertLinkHasValue(
              foreignDoc,
              "[data-spec='continue_working__link1']",
              "/check-your-state-pension/account/nirecord"
            )
          }

          "render page with href text 'Your forecast may be different if there are any changes to your National Insurance information. There is more about this in the terms and conditions'" in {
            mockSetup
            assertEqualsValue(
              nonForeignDoc,
              "[data-spec='state_pension__legal__forecast_changes__p']",
              messages("nisp.legal.forecastChanges", messages("nisp.legal.terms.and.conditions")) + "."
            )
            assertLinkHasValue(
              nonForeignDoc,
              "[data-spec='state_pension__legal__forecast_changes__link']",
              "/check-your-state-pension/terms-and-conditions?showBackLink=true"
            )
          }

          /*overseas message*/
          "render page with text 'As you are living or working overseas (opens in new tab), " +
            "you may be entitled to a State Pension from the country you are living or working in.'" in {
              mockSetup
              assertEqualsValue(
                foreignDoc,
                "[data-spec='abroad__inset_text__link1']",
                messages("nisp.main.overseas.linktext", messages("nisp.main.overseas.text"))
              )
            }
          /*Ends*/

          /*Start of Non SPA Checks*/
          "Not render page with heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertPageDoesNotContainMessage(
              nonForeignDoc,
              "nisp.spa.under.consideration.title"
            )
          }

          "Not render page with text 'You’ll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertPageDoesNotContainDynamicMessage(
              nonForeignDoc,
              "nisp.spa.under.consideration.detail",
              langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
            )
          }
          /*Ends*/

          "render page with heading 'Putting off claiming'" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='deferral__h2_1']",
              "nisp.main.puttingOff"
            )
          }

          "render page with text 'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            mockSetup
            assertContainsDynamicMessage(
              nonForeignDoc,
              "[data-spec='deferral__p1']",
              "nisp.main.puttingOff.line1",
              langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
            )
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='deferral__link1']",
              "nisp.main.puttingOff.linkTitle"
            )
          }

          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertLinkHasValue(
              nonForeignDoc,
              "[data-spec='deferral__link1']",
              "https://www.gov.uk/deferring-state-pension"
            )
          }
        }

        "State Pension page with MQP : Continue Working || 0 Qualify Years || has fillable Gaps ||  Personal Max: With State Pension age under consideration message" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(StatePension(
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
              ))))

            when(mockNationalInsuranceService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 0,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 1,
                numberOfGapsPayable = 1,
                Some(LocalDate.of(1954, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false) /*payable = true*/
                ),
                reducedRateElection = false
              )
              ))))
          }

          lazy val nonForeignDoc =
            asDocument(contentAsString(controller.show()(FakeRequest())))

          lazy val foreignDoc =
            asDocument(contentAsString(abroadUserController.show()(FakeRequest())))

          // overseas message
          "render page with text 'As you are living or working overseas (opens in new tab), " +
            "you may be entitled to a State Pension from the country you are living or working in.'" in {
              mockSetup
              assertEqualsValue(
                foreignDoc,
                "[data-spec='abroad__inset_text__link1']",
                messages("nisp.main.overseas.linktext", messages("nisp.main.overseas.text"))
              )
            }

          "render page with heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension_age_under_consideration__h2_1']",
              "nisp.spa.under.consideration.title"
            )
          }

          "render page with text 'You’ll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertContainsDynamicMessage(
              nonForeignDoc,
              "[data-spec='state_pension_age_under_consideration__p1']",
              "nisp.spa.under.consideration.detail",
              langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
            )
          }

          "render page with heading 'Putting off claiming'" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='deferral__h2_1']",
              "nisp.main.puttingOff"
            )
          }

          "render page with text 'You can put off claiming your State Pension from 7 June 2020. " +
            "Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
              mockSetup
              assertContainsDynamicMessage(
                nonForeignDoc,
                "[data-spec='deferral__p1']",
                "nisp.main.puttingOff.line1",
                langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
              )
            }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='deferral__link1']",
              "nisp.main.puttingOff.linkTitle"
            )
          }

          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertLinkHasValue(
              nonForeignDoc,
              "[data-spec='deferral__link1']",
              "https://www.gov.uk/deferring-state-pension"
            )
          }
        }

        "State Pension page with MQP : Continue Working || 9 Qualify Years || cant fill gaps ||  Personal Max" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(StatePension(
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
                statePensionAgeUnderConsideration = false
              )
              ))))

            when(mockNationalInsuranceService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 9,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 0,
                numberOfGapsPayable = 0,
                Some(LocalDate.of(1954, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(
                  NationalInsuranceTaxYearBuilder("2015-16", underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false) /*payable = true*/
                ),
                reducedRateElection = false
              )
              ))))
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
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension__pageheading'] [data-component='nisp_page_heading__h1']",
              "nisp.main.h1.title"
            )
          }

          "render page with text 'You can get your State Pension on 7 june 2020' " in {
            mockSetup
            assertEqualsValue(
              nonForeignDoc,
              "[data-spec='state_pension__panel1'] [data-component='nisp_panel__title']",
              Messages("nisp.main.basedOn") + " " +
                langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
            )
          }

          "render page with text 'Your forecast is £155.65 a week, £599.44 a month, £7,193.34 a year' " in {
            mockSetup
            val sMessage =
              Messages("nisp.main.caveats") + " " +
                Messages("nisp.is") + " £155.65 " +
                Messages("nisp.main.week") + ", £599.44 " +
                Messages("nisp.main.month") + ", £7,193.34 " +
                Messages("nisp.main.year")
            assertEqualsValue(
              nonForeignDoc,
              "[data-spec='state_pension__panel1__caveats']",
              sMessage
            )
          }

          "render page with text ' Your forecast '" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension__p_caveats__mqp']",
              "nisp.main.caveats"
            )
          }

          "render page with text ' is not a guarantee and is based on the current law '" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension__ul__caveats__mqp_li1']",
              "nisp.main.notAGuarantee"
            )
          }

          "render page with text ' is based on your National Insurance record up to 5 April 2016 '" in {
            mockSetup
            assertContainsDynamicMessage(
              nonForeignDoc,
              "[data-spec='state_pension__ul__caveats__mqp_li2']",
              "nisp.main.isBased",
              langUtils.Dates.formatDate(LocalDate.of(2016, 4, 5))
            )
          }

          "render page with text ' assumes that you’ll contribute another 4 years '" in {
            mockSetup
            assertContainsDynamicMessage(
              nonForeignDoc,
              "[data-spec='state_pension__ul__caveats__mqp_li3']",
              "nisp.mqp.howManyToContribute",
              Time.years(4)
            )
          }

          "render page with text ' does not include any increase due to inflation '" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension__ul__caveats__mqp_li4']",
              "nisp.main.inflation"
            )
          }

          "render page with text ' You currently have 9 years on your record and you need at least 10 years to get any State Pension. '" in {
            mockSetup
            assertContainsDynamicMessage(
              nonForeignDoc,
              "[data-spec='state_pension__p2a']",
              "nisp.mqp.youCurrentlyHave",
              Time.years(9),
              Constants.minimumQualifyingYearsNSP.toString,
              null
            )
          }

          "render page with heading '£155.65 is the most you can get'" in {
            mockSetup
            val sMessage = "£155.65 " + Messages("nisp.main.mostYouCanGet")
            assertEqualsValue(
              nonForeignDoc,
              "[data-spec='continue_working__mqp_scenario_h2_1']",
              sMessage
            )
          }

          "render page with text 'You cannot improve your forecast any further, unless you choose to put off claiming'" in {
            mockSetup
            assertEqualsMessage(
              foreignDoc,
              "[data-spec='continue_working__mqp_scenario_p1']",
              "nisp.main.context.willReach"
            )
          }

          "render page with text 'If you’re working you may still need to pay National Insurance contributions until 7 June 2020 as they fund other state benefits and the NHS.'" in {
            mockSetup
            assertContainsDynamicMessage(
              foreignDoc,
              "[data-spec='continue_working__mqp_scenario_p2']",
              "nisp.main.context.reachMax.needToPay",
              langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
            )
          }

          "render page with link 'View your National Insurance Record'" in {
            mockSetup
            assertEqualsMessage(
              foreignDoc,
              "[data-spec='continue_working__link1']",
              "nisp.main.showyourrecord"
            )
          }

          "render page with href link 'View your National Insurance Record'" in {
            mockSetup
            assertLinkHasValue(
              foreignDoc,
              "[data-spec='continue_working__link1']",
              "/check-your-state-pension/account/nirecord"
            )
          }

          "render page with href text 'Your forecast may be different if there are any changes to your National Insurance information. There is more about this in the terms and conditions''" in {
            mockSetup
            assertEqualsValue(
              nonForeignDoc,
              "[data-spec='state_pension__legal__forecast_changes__p']",
              messages("nisp.legal.forecastChanges", messages("nisp.legal.terms.and.conditions")) + "."
            )
            assertLinkHasValue(
              nonForeignDoc,
              "[data-spec='state_pension__legal__forecast_changes__link']",
              "/check-your-state-pension/terms-and-conditions?showBackLink=true"
            )
          }

          /*overseas message*/
          "render page with text 'As you are living or working overseas (opens in new tab), " +
            "you may be entitled to a State Pension from the country you are living or working in.'" in {
              mockSetup
              assertEqualsValue(
                foreignDoc,
                "[data-spec='abroad__inset_text__link1']",
                messages("nisp.main.overseas.linktext", messages("nisp.main.overseas.text"))
              )
            }
          /*Ends*/

          /*Start of Non SPA Checks*/
          "Not render page with heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertPageDoesNotContainMessage(
              nonForeignDoc,
              "nisp.spa.under.consideration.title"
            )
          }

          "Not render page with text 'You’ll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertPageDoesNotContainDynamicMessage(
              nonForeignDoc,
              "nisp.spa.under.consideration.detail",
              langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
            )
          }

          "render page with heading 'Putting off claiming'" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='deferral__h2_1']",
              "nisp.main.puttingOff"
            )
          }

          "render page with text 'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            mockSetup
            assertContainsDynamicMessage(
              nonForeignDoc,
              "[data-spec='deferral__p1']",
              "nisp.main.puttingOff.line1",
              langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
            )
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='deferral__link1']",
              "nisp.main.puttingOff.linkTitle"
            )
          }

          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertLinkHasValue(
              nonForeignDoc,
              "[data-spec='deferral__link1']",
              "https://www.gov.uk/deferring-state-pension"
            )
          }
        }

        "State Pension page with MQP : Continue Working || 9 Qualify Years || cant fill gaps ||  Personal Max: With State Pension age under consideration message" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(StatePension(
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
              ))))

            when(mockNationalInsuranceService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 9,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 0,
                numberOfGapsPayable = 0,
                Some(LocalDate.of(1954, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(
                  NationalInsuranceTaxYearBuilder("2015-16", underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false) /*payable = true*/
                ),
                reducedRateElection = false
              )
              ))))
          }

          lazy val nonForeignDoc =
            asDocument(contentAsString(controller.show()(FakeRequest())))

          lazy val foreignDoc =
            asDocument(contentAsString(abroadUserController.show()(FakeRequest())))

          // overseas message
          "render page with text 'As you are living or working overseas (opens in new tab), " +
            "you may be entitled to a State Pension from the country you are living or working in.'" in {
              mockSetup
              assertEqualsValue(
                foreignDoc,
                "[data-spec='abroad__inset_text__link1']",
                messages("nisp.main.overseas.linktext", messages("nisp.main.overseas.text"))
              )
            }

          "render page with heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension_age_under_consideration__h2_1']",
              "nisp.spa.under.consideration.title"
            )
          }

          "render page with text 'You’ll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertContainsDynamicMessage(
              nonForeignDoc,
              "[data-spec='state_pension_age_under_consideration__p1']",
              "nisp.spa.under.consideration.detail",
              langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
            )
          }

          "render page with heading 'Putting off claiming'" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='deferral__h2_1']",
              "nisp.main.puttingOff"
            )
          }

          "render page with text 'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            mockSetup
            assertContainsDynamicMessage(
              nonForeignDoc,
              "[data-spec='deferral__p1']",
              "nisp.main.puttingOff.line1",
              langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
            )
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='deferral__link1']",
              "nisp.main.puttingOff.linkTitle"
            )
          }

          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertLinkHasValue(
              nonForeignDoc,
              "[data-spec='deferral__link1']",
              "https://www.gov.uk/deferring-state-pension"
            )
          }
        }
      }

      "The scenario is No years to contribute" when {

        "State Pension page with MQP :  No Gaps || Cant get pension" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(StatePension(
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
                statePensionAgeUnderConsideration = false
              )
              ))))

            when(mockNationalInsuranceService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 4,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 0,
                numberOfGapsPayable = 0,
                Some(LocalDate.of(1954, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false) /*payable = true*/
                ),
                reducedRateElection = false
              )
              ))))
          }

          lazy val nonForeignDoc =
            asDocument(contentAsString(controller.show()(FakeRequest())))

          lazy val foreignDoc =
            asDocument(contentAsString(abroadUserController.show()(FakeRequest())))

          "render with correct page title" in {
            mockSetup
            assertElementContainsText(
              nonForeignDoc,
              "head > title",
              messages("nisp.main.h1.title")
                + Constants.titleSplitter
                + messages("nisp.title.extension")
                + Constants.titleSplitter
                + messages("nisp.gov-uk")
            )
          }

          "render page with heading 'Your State Pension' " in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension_mqp__h1']",
              "nisp.main.h1.title"
            )
          }

          "render page with text 'You’ll reach State Pension age on ' " in {
            mockSetup
            val sMessage = Messages("nisp.main.description.mqp")
            assertElementsOwnText(
              nonForeignDoc,
              "[data-spec='state_pension_mqp__inset_p1']",
              sMessage
            )
          }

          "render page with text 'You’ll reach State Pension age on 4 May 2017. ' " in {
            mockSetup
            val sMessage = langUtils.Dates.formatDate(LocalDate.of(2017, 5, 4)) + "."
            assertEqualsValue(
              nonForeignDoc,
              "[data-spec='state_pension_mqp__inset_p1'] > span",
              sMessage
            )
          }

          "render page with text 'By this time, you will not be able to get the 10 years needed on your National Insurance record to get any State Pension.' " in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension_mqp__inset_p2b']",
              "nisp.mqp.notPossible"
            )
          }

          "render page with text 'What you can do next" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension_mqp__h2_1']",
              "nisp.mqp.doNext"
            )
          }

          /*overseas message*/
          "render page with text 'As you are living or working overseas (opens in new tab), " +
            "you may be entitled to a State Pension from the country you are living or working in.'" in {
              mockSetup
              assertEqualsValue(
                foreignDoc,
                "[data-spec='state_pension_mqp__is_abroad_p1']",
                messages("nisp.main.overseas.text", messages("nisp.main.overseas.linktext"))
              )
            }
          /*Ends*/

          /*Start of Non SPA Checks*/
          "Not render page with heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertPageDoesNotContainMessage(
              nonForeignDoc,
              "nisp.spa.under.consideration.title"
            )
          }

          "Not render page with text 'You’ll reach State Pension age on 4 May 2017. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertPageDoesNotContainDynamicMessage(
              nonForeignDoc,
              "nisp.spa.under.consideration.detail",
              langUtils.Dates.formatDate(LocalDate.of(2017, 5, 4))
            )
          }
          /*Ends*/

          "render page with text 'You do not have any years on your record that do not count towards your pension.'" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension__mqp__years_dont_count__zero']",
              "nisp.mqp.years.dontCount.zero"
            )
          }

          "render page with link 'View your National Insurance Record'" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension__mqp__cant_get']",
              "nisp.main.showyourrecord"
            )
          }

          "render page with href link 'View your National Insurance Record'" in {
            mockSetup
            assertLinkHasValue(
              nonForeignDoc,
              "[data-spec='state_pension__mqp__cant_get']",
              "/check-your-state-pension/account/nirecord"
            )
          }

          "render page with text 'After State Pension age, 4 May 2017 you no longer pay National Insurance contributions.'" in {
            mockSetup
            assertContainsDynamicMessage(
              nonForeignDoc,
              "[data-spec='state_pension__mqp__after_spa']",
              "nisp.mqp.afterSpa",
              langUtils.Dates.formatDate(LocalDate.of(2017, 5, 4))
            )
          }

          "render page with text 'These details may be different if there are any changes to your National Insurance information. There is more about this in the terms and conditions'" in {
            mockSetup
            assertEqualsValue(
              nonForeignDoc,
              "[data-spec='state_pension__mqp__forecast_changes__p1']",
              messages("nisp.legal.mqp.forecastChanges", messages("nisp.legal.terms.and.conditions")) + "."
            )
          }

          "render page with href text 'terms and conditions'" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension__mqp__forecast_changes__link1']",
              "nisp.legal.terms.and.conditions"
            )
            assertLinkHasValue(
              nonForeignDoc,
              "[data-spec='state_pension__mqp__forecast_changes__link1']",
              "/check-your-state-pension/terms-and-conditions?showBackLink=true"
            )
          }

          "render page with href link 'These details may be different if there are any changes to your National Insurance information. There is more about this in the terms and conditions'" in {
            mockSetup
            assertLinkHasValue(
              nonForeignDoc,
              "[data-spec='state_pension__mqp__forecast_changes__link1']",
              "/check-your-state-pension/terms-and-conditions?showBackLink=true"
            )
          }

          "render page with link 'What else you can do'" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension_mqp__h2_2']",
              "nisp.mqp.whatElse"
            )
          }

          "render page with link 'You may be eligible for Pension Credit (opens in new tab)  if your retirement income is low.'" in {
            assertEqualsValue(
              nonForeignDoc,
              "[data-spec='state_pension_mqp__pension_credit_p1']",
              messages("nisp.mqp.pensionCredit", messages("nisp.mqp.pensionCredit.linktext"))
            )
            assertLinkHasValue(
              nonForeignDoc,
              "[data-spec='state_pension_mqp__pension_credit_link1']",
              "https://www.gov.uk/pension-credit/overview"
            )
          }

          "render page with href link 'You may be eligible for Pension Credit (opens in new tab)  if your retirement income is low'" in {
            mockSetup
            assertLinkHasValue(
              nonForeignDoc,
              "[data-spec='state_pension_mqp__pension_credit_link1']",
              expectedPensionCreditOverviewLink
            )
          }

          "render page with link 'Contact the Money Advice Service (opens in new tab) for free impartial advice.'" in {
            assertEqualsValue(
              nonForeignDoc,
              "[data-spec='state_pension_mqp__money_advice__en_p1']",
              messages("nisp.mqp.moneyAdvice", messages("nisp.mqp.moneyAdvice.linktext"))
            )
            assertLinkHasValue(
              nonForeignDoc,
              "[data-spec='state_pension_mqp__money_advice__en_link1']",
              "https://www.moneyadviceservice.org.uk/en"
            )
          }

          "render page with href link 'Contact the Money Advice Service (opens in new tab) for free impartial advice.'" in {
            mockSetup
            assertLinkHasValue(
              nonForeignDoc,
              "[data-spec='state_pension_mqp__money_advice__en_link1']",
              expectedMoneyServiceLink
            )
          }

        }

        "State Pension page with MQP :  No Gaps || Cant get pension: With State Pension age under consideration message" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(StatePension(
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
              ))))

            when(mockNationalInsuranceService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 4,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 0,
                numberOfGapsPayable = 0,
                Some(LocalDate.of(1954, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false) /*payable = true*/
                ),
                reducedRateElection = false
              )
              ))))

          }

          lazy val nonForeignDoc =
            asDocument(contentAsString(controller.show()(FakeRequest())))

          "render page with link 'Contact the Money Advice Service (opens in new tab) for free impartial advice.'" in {
            mockSetup
            assertEqualsValue(
              nonForeignDoc,
              "[data-spec='state_pension_mqp__money_advice__en_p1']",
              messages("nisp.mqp.moneyAdvice", messages("nisp.mqp.moneyAdvice.linktext"))
            )
            assertLinkHasValue(
              nonForeignDoc,
              "[data-spec='state_pension_mqp__money_advice__en_link1']",
              "https://www.moneyadviceservice.org.uk/en"
            )
          }

          "render page with href link 'Contact the Money Advice Service (opens in new tab) for free impartial advice.'" in {
            mockSetup
            assertLinkHasValue(
              nonForeignDoc,
              "[data-spec='state_pension_mqp__money_advice__en_link1']",
              expectedMoneyServiceLink
            )
          }

          // SPA under consideration message
          "render page with heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension_age_under_consideration__h2_1']",
              "nisp.spa.under.consideration.title"
            )
          }

          "render page with text 'You’ll reach State Pension age on 4 May 2017. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertContainsDynamicMessage(
              nonForeignDoc,
              "[data-spec='state_pension_age_under_consideration__p1']",
              "nisp.spa.under.consideration.detail",
              langUtils.Dates.formatDate(LocalDate.of(2017, 5, 4))
            )
          }
        }

        "State Pension page with MQP :  has fillable Gaps || Personal Max" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(StatePension(
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
                statePensionAgeUnderConsideration = false
              )
              ))))

            when(mockNationalInsuranceService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 4,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 2,
                numberOfGapsPayable = 2,
                Some(LocalDate.of(1954, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false) /*payable = true*/
                ),
                reducedRateElection = false
              )
              ))))
          }

          lazy val nonForeignDoc =
            asDocument(contentAsString(controller.show()(FakeRequest())))

          lazy val foreignDoc =
            asDocument(contentAsString(abroadUserController.show()(FakeRequest())))

          "render with correct page title" in {
            mockSetup
            assertElementContainsText(
              nonForeignDoc,
              "head > title",
              messages("nisp.main.h1.title")
                + Constants.titleSplitter
                + messages("nisp.title.extension")
                + Constants.titleSplitter
                + messages("nisp.gov-uk")
            )
          }

          "render page with heading 'Your State Pension' " in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension_mqp__h1']",
              "nisp.main.h1.title"
            )
          }

          "render page with text 'You’ll reach State Pension age on ' " in {
            mockSetup
            val sMessage = Messages("nisp.main.description.mqp")
            assertElementsOwnText(
              nonForeignDoc,
              "[data-spec='state_pension_mqp__inset_p1']",
              sMessage
            )
          }

          "render page with text 'You’ll reach State Pension age on 4 May 2018. ' " in {
            mockSetup
            val sMessage = langUtils.Dates.formatDate(LocalDate.of(2018, 5, 4)) + "."
            assertEqualsValue(
              nonForeignDoc,
              "[data-spec='state_pension_mqp__inset_p1'] > span",
              sMessage
            )
          }

          "render page with text 'By this time, you will not have the 10 years needed on your National Insurance record to get any State Pension.' " in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension_mqp__inset_p2a']",
              "nisp.mqp.possible"
            )
          }

          "render page with text 'What you can do next" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension_mqp__h2_1']",
              "nisp.mqp.doNext"
            )
          }

          /*overseas message*/
          "render page with text 'As you are living or working overseas (opens in new tab), " +
            "you may be entitled to a State Pension from the country you are living or working in.'" in {
              mockSetup
              assertEqualsValue(
                foreignDoc,
                "[data-spec='state_pension_mqp__is_abroad_p1']",
                messages("nisp.main.overseas.text", messages("nisp.main.overseas.linktext"))
              )
            }
          /*Ends*/

          /*Start of Non SPA Checks*/
          "Not render page with heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertPageDoesNotContainMessage(
              nonForeignDoc,
              "nisp.spa.under.consideration.title"
            )
          }

          "Not render page with text 'You’ll reach State Pension age on 4 May 2018. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertPageDoesNotContainDynamicMessage(
              nonForeignDoc,
              "nisp.spa.under.consideration.detail",
              langUtils.Dates.formatDate(LocalDate.of(2018, 5, 4))
            )
          }
          /*Ends*/

          "render page with text 'You also have 2 years on your record which do not count towards your pension because you did not contribute enough National Insurance." +
            " Filling some of these years may get you some State Pension.'" in {
              mockSetup
              val sMessage = Messages(
                "nisp.mqp.years.dontCount.plural",
                Time.years(2)
              )
              assertEqualsValue(
                nonForeignDoc,
                "[data-spec='state_pension__mqp__years_dont_count__plural']",
                sMessage
              )
              assertEqualsValue(
                nonForeignDoc,
                "[data-spec='state_pension__mqp__filling_may__plural']",
                messages("nisp.mqp.filling.may.plural")
              )
            }

          "render page with link 'Gaps in your record and the cost of filling them'" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension__mqp__cant_get_with_gaps']",
              "nisp.main.context.fillGaps.viewGapsAndCost"
            )
          }

          "render page with href link 'View your National Insurance Record'" in {
            mockSetup
            assertLinkHasValue(
              nonForeignDoc,
              "[data-spec='state_pension__mqp__cant_get_with_gaps']",
              "/check-your-state-pension/account/nirecord/gaps"
            )
          }

          "render page with text 'After State Pension age, 4 May 2018 you no longer pay National Insurance contributions.'" in {
            mockSetup
            assertContainsDynamicMessage(
              nonForeignDoc,
              "[data-spec='state_pension__mqp__after_spa']",
              "nisp.mqp.afterSpa",
              langUtils.Dates.formatDate(LocalDate.of(2018, 5, 4))
            )
          }

          "render page with href text 'These details may be different if there are any changes to your National Insurance information. " +
            "There is more about this in the terms and conditions.'" in {
              mockSetup
              assertEqualsValue(
                nonForeignDoc,
                "[data-spec='state_pension__mqp__forecast_changes__p1']",
                messages("nisp.legal.mqp.forecastChanges", messages("nisp.legal.terms.and.conditions")) + "."
              )
              assertLinkHasValue(
                nonForeignDoc,
                "[data-spec='state_pension__mqp__forecast_changes__link1']",
                "/check-your-state-pension/terms-and-conditions?showBackLink=true"
              )
            }

          "render page with link 'What else you can do'" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension_mqp__h2_2']",
              "nisp.mqp.whatElse"
            )
          }

          "render page with link 'You may be eligible for Pension Credit (opens in new tab) if your retirement income is low.'" in {
            mockSetup
            assertEqualsValue(
              nonForeignDoc,
              "[data-spec='state_pension_mqp__pension_credit_p1']",
              messages("nisp.mqp.pensionCredit", messages("nisp.mqp.pensionCredit.linktext"))
            )
            assertLinkHasValue(
              nonForeignDoc,
              "[data-spec='state_pension_mqp__pension_credit_link1']",
              "https://www.gov.uk/pension-credit/overview"
            )
          }

          "render page with href link 'You may be eligible for Pension Credit (opens in new tab)  if your retirement income is low'" in {
            mockSetup
            assertLinkHasValue(
              nonForeignDoc,
              "[data-spec='state_pension_mqp__pension_credit_link1']",
              expectedPensionCreditOverviewLink
            )
          }

          "render page with link 'Contact the Money Advice Service (opens in new tab) for free impartial advice.'" in {
            mockSetup
            assertEqualsValue(
              nonForeignDoc,
              "[data-spec='state_pension_mqp__money_advice__en_p1']",
              messages("nisp.mqp.moneyAdvice", messages("nisp.mqp.moneyAdvice.linktext"))
            )
            assertLinkHasValue(
              nonForeignDoc,
              "[data-spec='state_pension_mqp__money_advice__en_link1']",
              "https://www.moneyadviceservice.org.uk/en"
            )
          }

          "render page with href link 'Contact the Money Advice Service (opens in new tab) for free impartial advice.'" in {
            mockSetup
            assertLinkHasValue(
              nonForeignDoc,
              "[data-spec='state_pension_mqp__money_advice__en_link1']",
              expectedMoneyServiceLink
            )
          }
        }

        "State Pension page with MQP :  has fillable Gaps || Personal Max: With State Pension age under consideration message" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(StatePension(
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
              ))))

            when(mockNationalInsuranceService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 4,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 2,
                numberOfGapsPayable = 2,
                Some(LocalDate.of(1954, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false) /*payable = true*/
                ),
                reducedRateElection = false
              )
              ))))
          }

          lazy val nonForeignDoc =
            asDocument(contentAsString(controller.show()(FakeRequest())))

          "render page with link 'Contact the Money Advice Service (opens in new tab) for free impartial advice.'" in {
            mockSetup
            assertEqualsValue(
              nonForeignDoc,
              "[data-spec='state_pension_mqp__money_advice__en_p1']",
              messages("nisp.mqp.moneyAdvice", messages("nisp.mqp.moneyAdvice.linktext"))
            )
            assertLinkHasValue(
              nonForeignDoc,
              "[data-spec='state_pension_mqp__money_advice__en_link1']",
              "https://www.moneyadviceservice.org.uk/en"
            )
          }

          "render page with href link 'Contact the Money Advice Service (opens in new tab) for free impartial advice.'" in {
            mockSetup
            assertLinkHasValue(
              nonForeignDoc,
              "[data-spec='state_pension_mqp__money_advice__en_link1']",
              expectedMoneyServiceLink
            )
          }

          // SPA under consideration message
          "render page with heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertEqualsMessage(
              nonForeignDoc,
              "[data-spec='state_pension_age_under_consideration__h2_1']",
              "nisp.spa.under.consideration.title"
            )
          }

          "render page with text 'You’ll reach State Pension age on 4 May 2018. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertContainsDynamicMessage(
              nonForeignDoc,
              "[data-spec='state_pension_age_under_consideration__p1']",
              "nisp.spa.under.consideration.detail",
              langUtils.Dates.formatDate(LocalDate.of(2018, 5, 4))
            )
          }
        }
      }
    }
  }
}
