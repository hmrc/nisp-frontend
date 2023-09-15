/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{Injector, bind}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers.contentAsString
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.http.{SessionKeys, UpstreamErrorResponse}
import uk.gov.hmrc.nisp.builders.NationalInsuranceTaxYearBuilder
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.StatePensionController
import uk.gov.hmrc.nisp.controllers.auth.{AuthAction, PertaxAuthAction}
import uk.gov.hmrc.nisp.controllers.pertax.PertaxHelper
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.services.{NationalInsuranceService, StatePensionService}
import uk.gov.hmrc.nisp.utils.{Constants, WireMockHelper}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.language.LanguageUtils

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.Future

class StatePensionViewSpec extends HtmlSpec with Injecting with WireMockHelper {

  val urResearchURL =
    "https://signup.take-part-in-research.service.gov.uk/?utm_campaign=checkyourstatepensionPTA&utm_source=Other&utm_medium=other&t=HMRC&id=183"

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val mockAuditConnector: AuditConnector                     = mock[AuditConnector]
  val mockNationalInsuranceService: NationalInsuranceService = mock[NationalInsuranceService]
  val mockStatePensionService: StatePensionService           = mock[StatePensionService]
  val mockAppConfig: ApplicationConfig                       = mock[ApplicationConfig]
  val mockPertaxHelper: PertaxHelper                         = mock[PertaxHelper]

  lazy val langUtils: LanguageUtils = inject[LanguageUtils]

  val standardInjector: Injector = GuiceApplicationBuilder()
    .overrides(
      bind[AuthAction].to[FakeAuthAction],
      bind[StatePensionService].toInstance(mockStatePensionService),
      bind[NationalInsuranceService].toInstance(mockNationalInsuranceService),
      bind[AuditConnector].toInstance(mockAuditConnector),
      bind[ApplicationConfig].toInstance(mockAppConfig),
      bind[PertaxAuthAction].to[FakePertaxAuthAction],
      featureFlagServiceBinding
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
      bind[AuthAction].to[FakeAuthActionWithNino],
      bind[NinoContainer].toInstance(AbroadNinoContainer),
      bind[PertaxAuthAction].to[FakePertaxAuthAction],
      featureFlagServiceBinding
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
    server.resetAll()
    when(mockPertaxHelper.isFromPertax(any())).thenReturn(Future.successful(false))
    when(mockAppConfig.urBannerUrl).thenReturn(urResearchURL)
    when(mockAppConfig.reportAProblemNonJSUrl).thenReturn("/reportAProblem")
    when(mockAppConfig.contactFormServiceIdentifier).thenReturn("/id")
    when(mockAppConfig.pertaxAuthBaseUrl).thenReturn(s"http://localhost:${server.port()}")
    featureFlagSCAWrapperMock()
  }

  lazy val statePensionController: StatePensionController = standardInjector.instanceOf[StatePensionController]
  lazy val abroadUserController: StatePensionController = abroadUserInjector.instanceOf[StatePensionController]

  def generateFakeRequest: FakeRequest[AnyContentAsEmpty.type] = fakeRequest.withSession(
    SessionKeys.sessionId            -> s"session-${UUID.randomUUID()}",
    SessionKeys.lastRequestTimestamp -> LocalDate.now.toEpochDay.toString
  )

  "The State Pension page" when {

    "the user is a NON-MQP" when {

      "The scenario is continue working  || Fill Gaps" when {

        "State Pension view with NON-MQP :  Personal Max" should {
          def mockSetup: OngoingStubbing[String] = {
            when(mockStatePensionService.getSummary(any(), any())(any()))
              .thenReturn(Future.successful(Right(Right(StatePension(
                LocalDate.of(2016, 4, 5),
                amounts = StatePensionAmounts(
                  protectedPayment = false,
                  StatePensionAmountRegular(149.71, 590.10, 7081.15),
                  StatePensionAmountForecast(4, 148.71, 590.10, 7081.15),
                  StatePensionAmountMaximum(4, 2, 149.71, 590.10, 7081.15),
                  StatePensionAmountRegular(0, 0, 0)
                ),
                pensionAge = 67,
                LocalDate.of(2020, 6, 7),
                "2019-20",
                11,
                pensionSharingOrder = false,
                currentFullWeeklyPensionAmount = 149.65,
                reducedRateElection = false,
                statePensionAgeUnderConsideration = false
              )
              ))))

            when(mockNationalInsuranceService.getSummary(any(), any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 11,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 2,
                numberOfGapsPayable = 2,
                Some(LocalDate.of(1954, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false)
                ),
                reducedRateElection = false
              )
              ))))

            when(mockAppConfig.showUrBanner).thenReturn(true)
            when(mockAppConfig.urRecruitmentLinkURL).thenReturn(urResearchURL)
          }

          lazy val doc =
            asDocument(contentAsString(statePensionController.show()(generateFakeRequest)))

          lazy val abroadUserDoc =
            asDocument(contentAsString(abroadUserController.show()(generateFakeRequest)))

          "render with correct page title" in {
            mockSetup
            assertElementContainsText(
              doc,
              "head > title",
              messages("nisp.main.h1.title") + Constants.titleSplitter + messages(
                "nisp.title.extension"
              ) + Constants.titleSplitter + messages("nisp.gov-uk")
            )
          }

          "render page with Heading 'Your State Pension' " in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-component='nisp_page_heading__h1']",
              "nisp.main.h1.title"
            )
          }

          "render page with text 'You can get your State Pension on 7 june 2020' " in {
            mockSetup
            assertEqualsValue(
              doc,
              "[data-component='nisp_panel'] [data-component='nisp_panel__title']",
              messages("nisp.main.basedOn") + " " +
                langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
            )
          }

          "render page with text 'Your forecast is £148.71 a week, £590.10 a month, £7,081.15 a year' " in {
            mockSetup
            val sMessage =
              messages("nisp.main.caveats") + " " +
                messages("nisp.is") + " £148.71 " +
                messages("nisp.main.week") + ", £590.10 " +
                messages("nisp.main.month") + ", £7,081.15 " +
                messages("nisp.main.year")
            assertEqualsValue(
              doc,
              "[data-spec='state_pension__panel1__caveats']",
              sMessage
            )
          }

          "render page with text 'Your forecast '" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension__p_caveats__forecast']",
              "nisp.main.caveats"
            )
          }

          "render page with text 'is not a guarantee and is based on the current law '" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension__ul__caveats__forecast_scenario__1']",
              "nisp.main.notAGuarantee"
            )
          }

          "render page with text 'does not include any increase due to inflation '" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension__ul__caveats__forecast_scenario__2']",
              "nisp.main.inflation"
            )
          }

          "render page with Heading 'You need to continue to contribute National Insurance to reach your forecast'" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension__h2_1']",
              "nisp.main.continueContribute"
            )
          }

          "render page with text 'Estimate based on your National Insurance record up to '" in {
            mockSetup
            assertContainsDynamicMessage(
              doc,
              "[data-spec='state_pension__chart1'] [data-component='nisp_chart__title']",
              "nisp.main.chart.lastprocessed.title",
              "2016"
            )
          }

          "render page with text '£149.71 a week '" in {
            mockSetup
            val sMessage = "£149.71 " + messages("nisp.main.chart.week")
            assertEqualsValue(
              doc,
              "[data-spec='state_pension__chart1'] [data-component='nisp_chart__inner_text']",
              sMessage
            )
          }

          "render page with text 'Forecast if you contribute until '" in {
            mockSetup
            assertContainsDynamicMessage(
              doc,
              "[data-spec='state_pension__chart3'] [data-component='nisp_chart__title']",
              "nisp.main.chart.spa.title",
              "2020"
            )
          }

          "render page with text '£148.71 a week '" in {
            mockSetup
            val sMessage = "£148.71 " + messages("nisp.main.chart.week")
            assertEqualsValue(
              doc,
              "[data-spec='state_pension__chart3'] [data-component='nisp_chart__inner_text']",
              sMessage
            )
          }

          "render page with text 'You can improve your forecast'" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='fill_gaps__h2_1']",
              "nisp.main.context.fillGaps.improve.title"
            )
          }

          "render page with text 'You have years on your National Insurance record where you did not contribute enough.'" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='fill_gaps__p1b']",
              "nisp.main.context.fillgaps.para1.plural"
            )
          }

          "render page with text 'filling years can improve your forecast.'" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='fill_gaps__ul__improve__li1']",
              "nisp.main.context.fillgaps.bullet1"
            )
          }

          "render page with text 'you only need to fill 2 years to get the most you can'" in {
            mockSetup
            assertContainsDynamicMessage(
              doc,
              "[data-spec='fill_gaps__ul__improve__li3']",
              "nisp.main.context.fillgaps.bullet2.plural",
              "2"
            )
          }

          "render page with text 'The most you can get by filling any 2 years in your record is'" in {
            mockSetup
            assertContainsDynamicMessage(
              doc,
              "[data-spec='fill_gaps__chart3'] [data-component='nisp_chart__title']",
              "nisp.main.context.fillgaps.chart.plural",
              "2"
            )
          }

          "render page with text '£149.71 a week'" in {
            mockSetup
            val sMessage = "£149.71 " + messages("nisp.main.chart.week")
            assertEqualsValue(
              doc,
              "[data-spec='fill_gaps__chart3'] [data-component='nisp_chart__inner_text']",
              sMessage
            )
          }

          "render page with link 'Gaps in your record and the cost of filling them'" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='fill_gaps__link_1']",
              "nisp.main.context.fillGaps.viewGapsAndCost"
            )
          }

          "render page with href link 'Gaps in your record and the cost of filling them'" in {
            mockSetup
            assertLinkHasValue(
              doc,
              "[data-spec='fill_gaps__link_1']",
              "/check-your-state-pension/account/nirecord/gaps"
            )
          }

          "render page with text 'Your forecast may be different if there are any changes to your National Insurance information. There is more about this in the terms and conditions'" in {
            mockSetup
            assertEqualsValue(
              doc,
              "[data-spec='state_pension__legal__forecast_changes__p']",
              messages("nisp.legal.forecastChanges", messages("nisp.legal.terms.and.conditions")) + "."
            )
            assertLinkHasValue(
              doc,
              "[data-spec='state_pension__legal__forecast_changes__link']",
              "/check-your-state-pension/terms-and-conditions?showBackLink=true"
            )
          }

          "render page with href text 'Your forecast may be different if there are any changes to your National Insurance information. There is more about this in the terms and conditions -terms and condition'" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension__legal__forecast_changes__link']",
              "nisp.legal.terms.and.conditions"
            )
          }

          "render page with href link 'Your forecast may be different if there are any changes to your National Insurance information. There is more about this in the terms and conditions -terms and condition'" in {
            mockSetup
            assertLinkHasValue(
              doc,
              "[data-spec='state_pension__legal__forecast_changes__link']",
              "/check-your-state-pension/terms-and-conditions?showBackLink=true"
            )
          }

          "render page with text 'As you are living or working overseas (opens in new tab), you may be entitled to a " +
            "State Pension from the country you are living or working in.'" in {
              mockSetup
              assertEqualsMessage(
                abroadUserDoc,
                "[data-spec='abroad__inset_text__link1']",
                "nisp.main.overseas.linktext"
              )
            }

          "Not render page with heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertPageDoesNotContainMessage(
              doc,
              "nisp.spa.under.consideration.title"
            )
          }

          "Not render page with text 'You’ll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertPageDoesNotContainDynamicMessage(
              doc,
              "nisp.spa.under.consideration.detail",
              langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
            )
          }

          "render page with Heading 'Putting off claiming'" in {
            mockSetup
            assertEqualsMessage(
              abroadUserDoc,
              "[data-spec='deferral__h2_1']",
              "nisp.main.puttingOff"
            )
          }

          "render page with text 'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            mockSetup
            assertContainsDynamicMessage(
              abroadUserDoc,
              "[data-spec='deferral__p1']",
              "nisp.main.puttingOff.line1",
              langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
            )
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertEqualsMessage(
              abroadUserDoc,
              "[data-spec='deferral__link1']",
              "nisp.main.puttingOff.linkTitle"
            )
          }

          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertLinkHasValue(
              abroadUserDoc,
              "[data-spec='deferral__link1']",
              "https://www.gov.uk/deferring-state-pension"
            )
          }

          "render page with Heading 'Get help'" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension__sidebar_h2']",
              "nisp.nirecord.helpline.getHelp"
            )
          }

          "render page with text 'Helpline 0800 731 0181'" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension__sidebar_p1']",
              "nisp.nirecord.helpline.number"
            )
          }

          "render page with text 'Textphone 0800 731 0176'" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension__sidebar_p2']",
              "nisp.nirecord.helpline.textNumber"
            )
          }

          "render page with text 'Monday to Friday: 8am to 6pm'" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension__sidebar_p3']",
              "nisp.nirecord.helpline.openTimes"
            )
          }

          "render page with print link" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension__printlink']",
              "nisp.print.your.state.pension.summary"
            )
          }

        }

        "State Pension view with NON-MQP : Personal Max: With State Pension age under consideration message" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any(), any())(any()))
              .thenReturn(Future.successful(Right(Right(StatePension(
                LocalDate.of(2016, 4, 5),
                amounts = StatePensionAmounts(
                  protectedPayment = false,
                  StatePensionAmountRegular(149.71, 590.10, 7081.15),
                  StatePensionAmountForecast(4, 148.71, 590.10, 7081.15),
                  StatePensionAmountMaximum(4, 2, 149.71, 590.10, 7081.15),
                  StatePensionAmountRegular(0, 0, 0)
                ),
                pensionAge = 67,
                LocalDate.of(2020, 6, 7),
                "2019-20",
                11,
                pensionSharingOrder = false,
                currentFullWeeklyPensionAmount = 149.65,
                reducedRateElection = false,
                statePensionAgeUnderConsideration = true
              )
              ))))

            when(mockNationalInsuranceService.getSummary(any(), any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 11,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 2,
                numberOfGapsPayable = 2,
                Some(LocalDate.of(1954, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
                ),
                reducedRateElection = false
              )
              ))))
          }

          lazy val doc =
            asDocument(contentAsString(statePensionController.show()(generateFakeRequest)))

          lazy val abroadUserDoc =
            asDocument(contentAsString(abroadUserController.show()(generateFakeRequest)))

          // overseas message
          "render page with text 'As you are living or working overseas (opens in new tab), you may be entitled to a " +
            "State Pension from the country you are living or working in.'" in {
              mockSetup
              assertEqualsMessage(
                abroadUserDoc,
                "[data-spec='abroad__inset_text__link1']",
                "nisp.main.overseas.linktext"
              )
            }

          // SPA under consideration message
          "render page with Heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertEqualsMessage(
              abroadUserDoc,
              "[data-spec='state_pension_age_under_consideration__h2_1']",
              "nisp.spa.under.consideration.title"
            )
          }

          "render page with text 'You’ll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertContainsDynamicMessage(
              abroadUserDoc,
              "[data-spec='state_pension_age_under_consideration__p1']",
              "nisp.spa.under.consideration.detail",
              langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
            )
          }

          "render page with Heading 'Putting off claiming'" in {
            mockSetup
            assertEqualsMessage(
              abroadUserDoc,
              "[data-spec='deferral__h2_1']",
              "nisp.main.puttingOff"
            )
          }

          "render page with text 'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            mockSetup
            assertContainsDynamicMessage(
              abroadUserDoc,
              "[data-spec='deferral__p1']",
              "nisp.main.puttingOff.line1",
              langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
            )
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertEqualsMessage(
              abroadUserDoc,
              "[data-spec='deferral__link1']",
              "nisp.main.puttingOff.linkTitle"
            )
          }

          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertLinkHasValue(
              abroadUserDoc,
              "[data-spec='deferral__link1']",
              "https://www.gov.uk/deferring-state-pension"
            )
          }

          "render page with print link" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension__printlink']",
              "nisp.print.your.state.pension.summary"
            )
          }
        }

        "State Pension view with NON-MQP : Full Rate current more than 155.65" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any(), any())(any()))
              .thenReturn(Future.successful(Right(Right(StatePension(
                LocalDate.of(2016, 4, 5),
                amounts = StatePensionAmounts(
                  protectedPayment = true,
                  StatePensionAmountRegular(162.34, 590.10, 7081.15),
                  StatePensionAmountForecast(4, 168.08, 590.10, 7081.15),
                  StatePensionAmountMaximum(4, 2, 172.71, 590.10, 7081.15),
                  StatePensionAmountRegular(0, 0, 0)
                ),
                pensionAge = 67,
                LocalDate.of(2020, 6, 7),
                "2019-20",
                11,
                pensionSharingOrder = false,
                currentFullWeeklyPensionAmount = 149.65,
                reducedRateElection = false,
                statePensionAgeUnderConsideration = false
              )
              ))))

            when(mockNationalInsuranceService.getSummary(any(), any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 11,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 2,
                numberOfGapsPayable = 2,
                Some(LocalDate.of(1954, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false)
                ),
                reducedRateElection = false
              )
              ))))
          }

          lazy val doc =
            asDocument(contentAsString(statePensionController.show()(generateFakeRequest)))

          lazy val abroadUserDoc =
            asDocument(contentAsString(abroadUserController.show()(generateFakeRequest)))

          "render with correct page title" in {
            mockSetup
            assertElementContainsText(
              doc,
              "head > title",
              messages("nisp.main.h1.title")
                + Constants.titleSplitter
                + messages("nisp.title.extension")
                + Constants.titleSplitter
                + messages("nisp.gov-uk")
            )
          }

          "render page with Heading 'Your State Pension' " in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-component='nisp_page_heading__h1']",
              "nisp.main.h1.title"
            )
          }

          "render page with text 'You can get your State Pension on 7 june 2020' " in {
            mockSetup
            assertEqualsValue(
              doc,
              "[data-component='nisp_panel'] [data-component='nisp_panel__title']",
              messages("nisp.main.basedOn") + " " +
                langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
            )
          }

          "render page with text 'Your forecast is £168.08 a week, £590.10 a month, £7,081.15 a year' " in {
            mockSetup
            val sMessage =
              messages("nisp.main.caveats") + " " +
                messages("nisp.is") + " £168.08 " +
                messages("nisp.main.week") + ", £590.10 " +
                messages("nisp.main.month") + ", £7,081.15 " +
                messages("nisp.main.year")
            assertEqualsValue(
              doc,
              "[data-spec='state_pension__panel1__caveats']",
              sMessage
            )
          }

          "render page with text 'Your forecast '" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension__p_caveats__forecast']",
              "nisp.main.caveats"
            )
          }

          "render page with text 'is not a guarantee and is based on the current law '" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension__ul__caveats__forecast_scenario__1']",
              "nisp.main.notAGuarantee"
            )
          }

          "render page with text 'does not include any increase due to inflation '" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension__ul__caveats__forecast_scenario__2']",
              "nisp.main.inflation"
            )
          }

          "render page with Heading 'You need to continue to contribute National Insurance to reach your forecast'" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension__h2_1']",
              "nisp.main.continueContribute"
            )
          }

          "render page with text 'Estimate based on your National Insurance record up to '" in {
            mockSetup
            assertContainsDynamicMessage(
              doc,
              "[data-spec='state_pension__chart1'] [data-component='nisp_chart__title']",
              "nisp.main.chart.lastprocessed.title",
              "2016"
            )
          }

          "render page with text '£162.34 a week '" in {
            mockSetup
            val sMessage = "£162.34 " + messages("nisp.main.chart.week")
            assertEqualsValue(
              doc,
              "[data-spec='state_pension__chart1'] [data-component='nisp_chart__inner_text']",
              sMessage
            )
          }

          "render page with text 'Forecast if you contribute enough in year up to 5 April 2016'" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension__chart2'] [data-component='nisp_chart__title']",
              "nisp.main.chart.estimateIfYouContinue2016"
            )
          }

          "render page with text '£168.08 a week '" in {
            mockSetup
            val sMessage = "£168.08 " + messages("nisp.main.chart.week")
            assertEqualsValue(
              doc,
              "[data-spec='state_pension__chart2'] [data-component='nisp_chart__inner_text']",
              sMessage
            )
          }

          "render page with text 'You can improve your forecast'" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='fill_gaps__h2_1']",
              "nisp.main.context.fillGaps.improve.title"
            )
          }

          "render page with text 'You have years on your National Insurance record where you did not contribute enough.'" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='fill_gaps__p1b']",
              "nisp.main.context.fillgaps.para1.plural"
            )
          }

          "render page with text 'filling years can improve your forecast.'" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='fill_gaps__ul__improve__li1']",
              "nisp.main.context.fillgaps.bullet1"
            )
          }

          "render page with text 'you only need to fill 2 years to get the most you can'" in {
            mockSetup
            assertContainsDynamicMessage(
              doc,
              "[data-spec='fill_gaps__ul__improve__li3']",
              "nisp.main.context.fillgaps.bullet2.plural",
              "2"
            )
          }

          "render page with text 'The most you can get by filling any 2 years in your record is'" in {
            mockSetup
            assertContainsDynamicMessage(
              doc,
              "[data-spec='fill_gaps__chart3'] [data-component='nisp_chart__title']",
              "nisp.main.context.fillgaps.chart.plural",
              "2"
            )
          }

          "render page with text '£172.71 a week'" in {
            mockSetup
            val sMessage = "£172.71 " + messages("nisp.main.chart.week")
            assertEqualsValue(
              doc,
              "[data-spec='fill_gaps__chart3'] [data-component='nisp_chart__inner_text']",
              sMessage
            )
          }

          "render page with link 'Gaps in your record and the cost of filling them'" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='fill_gaps__link_1']",
              "nisp.main.context.fillGaps.viewGapsAndCost"
            )
          }

          "render page with href link 'Gaps in your record and the cost of filling them'" in {
            mockSetup
            assertLinkHasValue(
              doc,
              "[data-spec='fill_gaps__link_1']",
              "/check-your-state-pension/account/nirecord/gaps"
            )
          }

          "render page with href text 'Your forecast may be different if there are any changes to your National Insurance information. There is more about this in the terms and conditions'" in {
            mockSetup
            assertEqualsValue(
              doc,
              "[data-spec='state_pension__legal__forecast_changes__p']",
              messages("nisp.legal.forecastChanges", messages("nisp.legal.terms.and.conditions")) + "."
            )
            assertLinkHasValue(
              doc,
              "[data-spec='state_pension__legal__forecast_changes__link']",
              "/check-your-state-pension/terms-and-conditions?showBackLink=true"
            )
          }

          "render page with text 'As you are living or working overseas (opens in new tab), you may be entitled to a " +
            "State Pension from the country you are living or working in.'" in {
              mockSetup
              assertEqualsMessage(
                abroadUserDoc,
                "[data-spec='abroad__inset_text__link1']",
                "nisp.main.overseas.linktext"
              )
            }

          "Not render page with heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertPageDoesNotContainMessage(
              doc,
              "nisp.spa.under.consideration.title"
            )
          }

          "Not render page with text 'You’ll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertPageDoesNotContainDynamicMessage(
              doc,
              "nisp.spa.under.consideration.detail",
              langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
            )
          }
          /*Ends*/

          "render page with heading 'Putting off claiming'" in {
            mockSetup
            assertEqualsMessage(
              abroadUserDoc,
              "[data-spec='deferral__h2_1']",
              "nisp.main.puttingOff"
            )
          }

          "render page with text 'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            mockSetup
            assertContainsDynamicMessage(
              abroadUserDoc,
              "[data-spec='deferral__p1']",
              "nisp.main.puttingOff.line1",
              langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
            )
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertEqualsMessage(
              abroadUserDoc,
              "[data-spec='deferral__link1']",
              "nisp.main.puttingOff.linkTitle"
            )
          }

          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertLinkHasValue(
              abroadUserDoc,
              "[data-spec='deferral__link1']",
              "https://www.gov.uk/deferring-state-pension"
            )
          }

          /*Side bar help*/
          "render page with heading 'Get help'" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension__sidebar_h2']",
              "nisp.nirecord.helpline.getHelp"
            )
          }

          "render page with text 'Helpline 0800 731 0181'" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension__sidebar_p1']",
              "nisp.nirecord.helpline.number"
            )
          }

          "render page with text 'Textphone 0800 731 0176'" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension__sidebar_p2']",
              "nisp.nirecord.helpline.textNumber"
            )
          }

          "render page with text 'Monday to Friday: 8am to 6pm'" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension__sidebar_p3']",
              "nisp.nirecord.helpline.openTimes"
            )
          }

          "render page with print link" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension__printlink']",
              "nisp.print.your.state.pension.summary"
            )
          }
        }

        "State Pension view with NON-MQP : Full Rate current more than 155.65: With State Pension age under consideration message" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any(), any())(any()))
              .thenReturn(Future.successful(Right(Right(StatePension(
                LocalDate.of(2016, 4, 5),
                amounts = StatePensionAmounts(
                  protectedPayment = true,
                  StatePensionAmountRegular(162.34, 590.10, 7081.15),
                  StatePensionAmountForecast(4, 168.08, 590.10, 7081.15),
                  StatePensionAmountMaximum(4, 2, 172.71, 590.10, 7081.15),
                  StatePensionAmountRegular(0, 0, 0)
                ),
                pensionAge = 67,
                LocalDate.of(2020, 6, 7),
                "2019-20",
                11,
                pensionSharingOrder = false,
                currentFullWeeklyPensionAmount = 149.65,
                reducedRateElection = false,
                statePensionAgeUnderConsideration = true
              )
              ))))

            when(mockNationalInsuranceService.getSummary(any(), any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 11,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 2,
                numberOfGapsPayable = 2,
                Some(LocalDate.of(1954, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
                ),
                reducedRateElection = false
              )
              ))))
          }

          lazy val doc =
            asDocument(contentAsString(statePensionController.show()(generateFakeRequest)))

          lazy val abroadUserDoc =
            asDocument(contentAsString(abroadUserController.show()(generateFakeRequest)))

          // overseas message
          "render page with text 'As you are living or working overseas (opens in new tab), you may be entitled to a " +
            "State Pension from the country you are living or working in.'" in {
              mockSetup
              assertEqualsMessage(
                abroadUserDoc,
                "[data-spec='abroad__inset_text__link1']",
                "nisp.main.overseas.linktext"
              )
            }

          // SPA under consideration message
          "render page with Heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertEqualsMessage(
              abroadUserDoc,
              "[data-spec='state_pension_age_under_consideration__h2_1']",
              "nisp.spa.under.consideration.title"
            )
          }

          "render page with text 'You’ll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertContainsDynamicMessage(
              abroadUserDoc,
              "[data-spec='state_pension_age_under_consideration__p1']",
              "nisp.spa.under.consideration.detail",
              langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
            )
          }
          /*Ends*/

          // deferral message
          "render page with Heading 'Putting off claiming'" in {
            mockSetup
            assertEqualsMessage(
              abroadUserDoc,
              "[data-spec='deferral__h2_1']",
              "nisp.main.puttingOff"
            )
          }

          "render page with text 'You can put off claiming your State Pension from 7 June 2020. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            mockSetup
            assertContainsDynamicMessage(
              abroadUserDoc,
              "[data-spec='deferral__p1']",
              "nisp.main.puttingOff.line1",
              langUtils.Dates.formatDate(LocalDate.of(2020, 6, 7))
            )
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertEqualsMessage(
              abroadUserDoc,
              "[data-spec='deferral__link1']",
              "nisp.main.puttingOff.linkTitle"
            )
          }

          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertLinkHasValue(
              abroadUserDoc,
              "[data-spec='deferral__link1']",
              "https://www.gov.uk/deferring-state-pension"
            )
          }

          "render page with print link" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension__printlink']",
              "nisp.print.your.state.pension.summary"
            )
          }
        }

        "State Pension view with NON-MQP :  Full Rate will reach full rate by filling gaps" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any(), any())(any()))
              .thenReturn(Future.successful(Right(Right(StatePension(
                LocalDate.of(2016, 4, 5),
                amounts = StatePensionAmounts(
                  protectedPayment = false,
                  StatePensionAmountRegular(133.71, 590.10, 7081.15),
                  StatePensionAmountForecast(4, 148.71, 590.10, 7081.15),
                  StatePensionAmountMaximum(4, 2, 149.71, 590.10, 7081.15),
                  StatePensionAmountRegular(0, 0, 0)
                ),
                pensionAge = 67,
                LocalDate.of(2017, 6, 7),
                "2019-20",
                11,
                pensionSharingOrder = false,
                currentFullWeeklyPensionAmount = 149.65,
                reducedRateElection = false,
                statePensionAgeUnderConsideration = false
              )
              ))))

            when(mockNationalInsuranceService.getSummary(any(), any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 11,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 2,
                numberOfGapsPayable = 2,
                Some(LocalDate.of(1989, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
                ),
                reducedRateElection = false
              )
              ))))
          }

          lazy val doc =
            asDocument(contentAsString(statePensionController.show()(generateFakeRequest)))

          lazy val abroadUserDoc =
            asDocument(contentAsString(abroadUserController.show()(generateFakeRequest)))

          "render with correct page title" in {
            mockSetup
            assertElementContainsText(
              doc,
              "head > title",
              messages("nisp.main.h1.title")
                + Constants.titleSplitter
                + messages("nisp.title.extension")
                + Constants.titleSplitter
                + messages("nisp.gov-uk")
            )
          }

          "render page with Heading 'Your State Pension' " in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-component='nisp_page_heading__h1']",
              "nisp.main.h1.title"
            )
          }

          "render page with text 'You can get your State Pension on 7 june 2017' " in {
            mockSetup
            assertEqualsValue(
              doc,
              "[data-component='nisp_panel'] [data-component='nisp_panel__title']",
              messages("nisp.main.basedOn") + " " + langUtils.Dates.formatDate(LocalDate.of(2017, 6, 7))
            )
          }

          "render page with text 'Your forecast is £148.71 a week, £590.10 a month, £7,081.15 a year' " in {
            mockSetup
            val sMessage =
              messages("nisp.main.caveats") + " " +
                messages("nisp.is") + " £148.71 " +
                messages("nisp.main.week") + ", £590.10 " +
                messages("nisp.main.month") + ", £7,081.15 " +
                messages("nisp.main.year")
            assertEqualsValue(
              doc,
              "[data-spec='state_pension__panel1__caveats']",
              sMessage
            )
          }

          "render page with text 'Your forecast '" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension__p_caveats__forecast']",
              "nisp.main.caveats"
            )
          }

          "render page with text 'is not a guarantee and is based on the current law '" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension__ul__caveats__forecast_scenario__1']",
              "nisp.main.notAGuarantee"
            )
          }

          "render page with text 'does not include any increase due to inflation '" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension__ul__caveats__forecast_scenario__2']",
              "nisp.main.inflation"
            )
          }

          "render page with Heading 'You need to continue to contribute National Insurance to reach your forecast'" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension__h2_1']",
              "nisp.main.continueContribute"
            )
          }

          "render page with text 'Estimate based on your National Insurance record up to '" in {
            mockSetup
            assertContainsDynamicMessage(
              doc,
              "[data-spec='state_pension__chart1'] [data-component='nisp_chart__title']",
              "nisp.main.chart.lastprocessed.title",
              "2016"
            )
          }

          "render page with text '£133.71 a week '" in {
            mockSetup
            val sMessage = "£133.71 " + messages("nisp.main.chart.week")
            assertEqualsValue(
              doc,
              "[data-spec='state_pension__chart1'] [data-component='nisp_chart__inner_text']",
              sMessage
            )
          }

          "render page with text 'Forecast if you contribute until '" in {
            mockSetup
            assertContainsDynamicMessage(
              doc,
              "[data-spec='state_pension__chart3'] [data-component='nisp_chart__title']",
              "nisp.main.chart.spa.title",
              "2020"
            )
          }

          "render page with text '£148.71 a week '" in {
            mockSetup
            val sMessage = "£148.71 " + messages("nisp.main.chart.week")
            assertEqualsValue(
              doc,
              "[data-spec='state_pension__chart3'] [data-component='nisp_chart__inner_text']",
              sMessage
            )
          }

          "render page with text 'You can improve your forecast'" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='fill_gaps__h2_1']",
              "nisp.main.context.fillGaps.improve.title"
            )
          }

          "render page with text 'You have years on your National Insurance record where you did not contribute enough.'" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='fill_gaps__p1b']",
              "nisp.main.context.fillgaps.para1.plural"
            )
          }

          "render page with text 'filling years can improve your forecast.'" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='fill_gaps__ul__improve__li1']",
              "nisp.main.context.fillgaps.bullet1"
            )
          }

          "render page with text 'you only need to fill 2 years to get the most you can'" in {
            mockSetup
            assertContainsDynamicMessage(
              doc,
              "[data-spec='fill_gaps__ul__improve__li3']",
              "nisp.main.context.fillgaps.bullet2.plural",
              "2"
            )
          }

          "render page with text 'The most you can get by filling any 2 years in your record is'" in {
            mockSetup
            assertContainsDynamicMessage(
              doc,
              "[data-spec='fill_gaps__chart3'] [data-component='nisp_chart__title']",
              "nisp.main.context.fillgaps.chart.plural",
              "2"
            )
          }

          "render page with text '£149.71 a week'" in {
            mockSetup
            val sMessage = "£149.71 " + messages("nisp.main.chart.week")
            assertEqualsValue(
              doc,
              "[data-spec='fill_gaps__chart3'] [data-component='nisp_chart__inner_text']",
              sMessage
            )
          }

          "render page with link 'Gaps in your record and the cost of filling them'" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='fill_gaps__link_1']",
              "nisp.main.context.fillGaps.viewGapsAndCost"
            )
          }

          "render page with href link 'Gaps in your record and the cost of filling them'" in {
            mockSetup
            assertLinkHasValue(
              doc,
              "[data-spec='fill_gaps__link_1']",
              "/check-your-state-pension/account/nirecord/gaps"
            )
          }

          "render page with href text 'Your forecast may be different if there are any changes to your National Insurance information. There is more about this in the terms and conditions'" in {
            mockSetup
            assertEqualsValue(
              doc,
              "[data-spec='state_pension__legal__forecast_changes__p']",
              messages("nisp.legal.forecastChanges", messages("nisp.legal.terms.and.conditions")) + "."
            )
            assertLinkHasValue(
              doc,
              "[data-spec='state_pension__legal__forecast_changes__link']",
              "/check-your-state-pension/terms-and-conditions?showBackLink=true"
            )
          }

          /*overseas message*/
          "render page with text 'As you are living or working overseas (opens in new tab), you may be entitled to a " +
            "State Pension from the country you are living or working in.'" in {
              mockSetup
              assertEqualsMessage(
                abroadUserDoc,
                "[data-spec='abroad__inset_text__link1']",
                "nisp.main.overseas.linktext"
              )
            }
          /*Ends*/

          /*Start of Non SPA Checks*/
          "Not render page with Heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertPageDoesNotContainMessage(
              doc,
              "nisp.spa.under.consideration.title"
            )
          }

          "Not render page with text 'You’ll reach State Pension age on 7 June 2017. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertPageDoesNotContainDynamicMessage(
              doc,
              "nisp.spa.under.consideration.detail",
              langUtils.Dates.formatDate(LocalDate.of(2017, 6, 7))
            )
          }
          /*Ends*/

          "render page with Heading 'Putting off claiming'" in {
            mockSetup
            assertEqualsMessage(
              abroadUserDoc,
              "[data-spec='deferral__h2_1']",
              "nisp.main.puttingOff"
            )
          }

          "render page with text 'You can put off claiming your State Pension from 7 June 2017. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            mockSetup
            assertContainsDynamicMessage(
              abroadUserDoc,
              "[data-spec='deferral__p1']",
              "nisp.main.puttingOff.line1",
              langUtils.Dates.formatDate(LocalDate.of(2017, 6, 7))
            )
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertEqualsMessage(
              abroadUserDoc,
              "[data-spec='deferral__link1']",
              "nisp.main.puttingOff.linkTitle"
            )
          }

          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertLinkHasValue(
              abroadUserDoc,
              "[data-spec='deferral__link1']",
              "https://www.gov.uk/deferring-state-pension"
            )
          }

          "render page with print link" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension__printlink']",
              "nisp.print.your.state.pension.summary"
            )
          }
        }

        "State Pension view with NON-MQP :  Full Rate will reach full rate by filling gaps: With State Pension age under consideration message" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any(), any())(any()))
              .thenReturn(Future.successful(Right(Right(StatePension(
                LocalDate.of(2016, 4, 5),
                amounts = StatePensionAmounts(
                  protectedPayment = false,
                  StatePensionAmountRegular(133.71, 590.10, 7081.15),
                  StatePensionAmountForecast(4, 148.71, 590.10, 7081.15),
                  StatePensionAmountMaximum(4, 2, 149.71, 590.10, 7081.15),
                  StatePensionAmountRegular(0, 0, 0)
                ),
                pensionAge = 67,
                LocalDate.of(2017, 6, 7),
                "2019-20",
                11,
                pensionSharingOrder = false,
                currentFullWeeklyPensionAmount = 149.65,
                reducedRateElection = false,
                statePensionAgeUnderConsideration = true
              )
              ))))

            when(mockNationalInsuranceService.getSummary(any(), any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 11,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 2,
                numberOfGapsPayable = 2,
                Some(LocalDate.of(1989, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
                ),
                reducedRateElection = false
              )
              ))))
          }

          lazy val doc =
            asDocument(contentAsString(statePensionController.show()(generateFakeRequest)))

          lazy val abroadUserDoc =
            asDocument(contentAsString(abroadUserController.show()(generateFakeRequest)))

          // overseas message
          "render page with text 'As you are living or working overseas (opens in new tab), you may be entitled to a " +
            "State Pension from the country you are living or working in.'" in {
              mockSetup
              assertEqualsMessage(
                abroadUserDoc,
                "[data-spec='abroad__inset_text__link1']",
                "nisp.main.overseas.linktext"
              )
            }

          // SPA under consideration message
          "render page with Heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertEqualsMessage(
              abroadUserDoc,
              "[data-spec='state_pension_age_under_consideration__h2_1']",
              "nisp.spa.under.consideration.title"
            )
          }

          "render page with text 'You’ll reach State Pension age on 7 June 2017. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertContainsDynamicMessage(
              abroadUserDoc,
              "[data-spec='state_pension_age_under_consideration__p1']",
              "nisp.spa.under.consideration.detail",
              langUtils.Dates.formatDate(LocalDate.of(2017, 6, 7))
            )
          }
          /*Ends*/

          // deferral message
          "render page with Heading 'Putting off claiming'" in {
            mockSetup
            assertEqualsMessage(
              abroadUserDoc,
              "[data-spec='deferral__h2_1']",
              "nisp.main.puttingOff"
            )
          }

          "render page with text 'You can put off claiming your State Pension from 7 June 2017. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            mockSetup
            assertContainsDynamicMessage(
              abroadUserDoc,
              "[data-spec='deferral__p1']",
              "nisp.main.puttingOff.line1",
              langUtils.Dates.formatDate(LocalDate.of(2017, 6, 7))
            )
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertEqualsMessage(
              abroadUserDoc,
              "[data-spec='deferral__link1']",
              "nisp.main.puttingOff.linkTitle"
            )
          }

          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertLinkHasValue(
              abroadUserDoc,
              "[data-spec='deferral__link1']",
              "https://www.gov.uk/deferring-state-pension"
            )
          }

          "render page with print link" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension__printlink']",
              "nisp.print.your.state.pension.summary"
            )
          }
        }
      }

      "The scenario is continue working || No Gaps/No need to fill gaps" when {

        "State Pension view with NON-MQP : No Gaps || Full Rate & Personal Max" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any(), any())(any()))
              .thenReturn(Future.successful(Right(Right(StatePension(
                LocalDate.of(2016, 4, 5),
                amounts = StatePensionAmounts(
                  protectedPayment = false,
                  StatePensionAmountRegular(118.65, 590.10, 7081.15),
                  StatePensionAmountForecast(0, 150.65, 676.80, 8121.59),
                  StatePensionAmountMaximum(0, 0, 150.65, 590.10, 7081.15),
                  StatePensionAmountRegular(0, 0, 0)
                ),
                pensionAge = 67,
                LocalDate.of(2022, 6, 7),
                "2021-22",
                11,
                pensionSharingOrder = false,
                currentFullWeeklyPensionAmount = 151.65,
                reducedRateElection = false,
                statePensionAgeUnderConsideration = false
              )
              ))))

            when(mockNationalInsuranceService.getSummary(any(), any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 11,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 0,
                numberOfGapsPayable = 0,
                Some(LocalDate.of(1989, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
                ),
                reducedRateElection = false
              )
              ))))

          }

          lazy val doc =
            asDocument(contentAsString(statePensionController.show()(generateFakeRequest)))

          lazy val abroadUserDoc =
            asDocument(contentAsString(abroadUserController.show()(generateFakeRequest)))

          "render with correct page title" in {
            mockSetup
            assertElementContainsText(
              doc,
              "head > title",
              messages("nisp.main.title") +
                Constants.titleSplitter +
                messages("nisp.title.extension") +
                Constants.titleSplitter +
                messages("nisp.gov-uk")
            )
          }

          "render page with Heading 'Your State Pension' " in {
            mockSetup
            assertEqualsValue(
              doc,
              "[data-component='nisp_page_heading__h1']",
              messages("nisp.main.h1.title")
            )
          }

          "render page with text 'You can get your State Pension on 7 june 2022' " in {
            mockSetup
            assertEqualsValue(
              doc,
              "[data-component='nisp_panel'] [data-component='nisp_panel__title']",
              messages("nisp.main.basedOn") + " " +
                langUtils.Dates.formatDate(LocalDate.of(2022, 6, 7))
            )
          }

          "render page with text 'Your forecast is £150.65  a week, £676.80 a month, £8,121.59 a year' " in {
            mockSetup
            val sMessage =
              messages("nisp.main.caveats") + " " +
                messages("nisp.is") + " £150.65 " +
                messages("nisp.main.week") + ", £676.80 " +
                messages("nisp.main.month") + ", £8,121.59 " +
                messages("nisp.main.year")
            assertEqualsValue(
              doc,
              "[data-spec='state_pension__panel1__caveats']",
              sMessage
            )
          }

          "render page with text 'Your forecast '" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='continue_working__p__caveats']",
              "nisp.main.caveats"
            )
          }

          "render page with text 'is not a guarantee and is based on the current law '" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='continue_working__ul__caveats__1']",
              "nisp.main.notAGuarantee"
            )
          }

          "render page with text 'does not include any increase due to inflation '" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='continue_working__ul__caveats__2']",
              "nisp.main.inflation"
            )
          }

          "render page with Heading 'You need to continue to contribute National Insurance to reach your forecast'" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='continue_working__h2_1']",
              "nisp.main.continueContribute"
            )
          }

          "render page with text 'Estimate based on your National Insurance record up to '" in {
            mockSetup
            assertContainsDynamicMessage(
              doc,
              "[data-spec='continue_working__chart1'] [data-component='nisp_chart__title']",
              "nisp.main.chart.lastprocessed.title",
              "2016"
            )
          }

          "render page with text '£118.65 a week '" in {
            mockSetup
            val sMessage = "£118.65 " + messages("nisp.main.chart.week")
            assertEqualsValue(
              doc,
              "[data-spec='continue_working__chart1'] [data-component='nisp_chart__inner_text']",
              sMessage
            )
          }

          "render page with text 'Forecast if you contribute until 5 April 2022'" in {
            mockSetup
            assertContainsDynamicMessage(
              doc,
              "[data-spec='continue_working__chart5'] [data-component='nisp_chart__title']",
              "nisp.main.chart.spa.title",
              "2022"
            )
          }

          "render page with text '£150.65 a week '" in {
            mockSetup
            val sMessage = "£150.65 " + messages("nisp.main.chart.week")
            assertEqualsValue(
              doc,
              "[data-spec='continue_working__chart5'] [data-component='nisp_chart__inner_text']",
              sMessage
            )
          }

          "render page with text '£150.65 is the most you can get'" in {
            mockSetup
            val sMessage = "£150.65 " + StringEscapeUtils.unescapeHtml4(messages("nisp.main.mostYouCanGet"))
            assertEqualsValue(
              doc,
              "[data-spec='continue_working__h2_2']",
              sMessage
            )
          }

          "render page with text 'After State Pension age, 7 June 2022 you no longer pay National Insurance contributions.'" in {
            mockSetup
            assertContainsExpectedValue(
              doc,
              "[data-spec='continue_working__p4']",
              "nisp.main.after",
              langUtils.Dates.formatDate(LocalDate.of(2022, 6, 7))
            )
          }

          "render page with link 'View your National Insurance Record'" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='continue_working__link1']",
              "nisp.main.showyourrecord"
            )
          }

          "render page with href link 'View your National Insurance Record'" in {
            mockSetup
            assertLinkHasValue(
              doc,
              "[data-spec='continue_working__link1']",
              "/check-your-state-pension/account/nirecord"
            )
          }

          "render page with href text 'Your forecast may be different if there are any changes to your National Insurance information. There is more about this in the terms and conditions'" in {
            mockSetup
            assertEqualsValue(
              doc,
              "[data-spec='state_pension__legal__forecast_changes__p']",
              messages("nisp.legal.forecastChanges", messages("nisp.legal.terms.and.conditions")) + "."
            )
            assertLinkHasValue(
              doc,
              "[data-spec='state_pension__legal__forecast_changes__link']",
              "/check-your-state-pension/terms-and-conditions?showBackLink=true"
            )
          }

          /*overseas message*/
          "render page with text 'As you are living or working overseas (opens in new tab), you may be entitled to a " +
            "State Pension from the country you are living or working in.'" in {
              mockSetup
              assertEqualsMessage(
                abroadUserDoc,
                "[data-spec='abroad__inset_text__link1']",
                "nisp.main.overseas.linktext"
              )
            }
          /*Ends*/

          // Non SPA under consideration message
          "render page with Heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertPageDoesNotContainMessage(
              doc,
              "nisp.spa.under.consideration.title"
            )
          }

          "render page with text 'You’ll reach State Pension age on 7 June 2022. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertPageDoesNotContainDynamicMessage(
              abroadUserDoc,
              "nisp.spa.under.consideration.detail",
              langUtils.Dates.formatDate(LocalDate.of(2022, 6, 7))
            )
          }
          /*Ends*/

          "render page with Heading 'Putting off claiming'" in {
            mockSetup
            assertEqualsMessage(
              abroadUserDoc,
              "[data-spec='deferral__h2_1']",
              "nisp.main.puttingOff"
            )
          }

          "render page with text 'You can put off claiming your State Pension from 7 June 2022. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            mockSetup
            assertContainsDynamicMessage(
              abroadUserDoc,
              "[data-spec='deferral__p1']",
              "nisp.main.puttingOff.line1",
              langUtils.Dates.formatDate(LocalDate.of(2022, 6, 7))
            )
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertEqualsMessage(
              abroadUserDoc,
              "[data-spec='deferral__link1']",
              "nisp.main.puttingOff.linkTitle"
            )
          }

          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertLinkHasValue(
              abroadUserDoc,
              "[data-spec='deferral__link1']",
              "https://www.gov.uk/deferring-state-pension"
            )
          }

          "render page with print link" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension__printlink']",
              "nisp.print.your.state.pension.summary"
            )
          }
        }

        "State Pension view with NON-MQP : No Gaps || Full Rate & Personal Max: With State Pension age under consideration message" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any(), any())(any()))
              .thenReturn(Future.successful(Right(Right(StatePension(
                LocalDate.of(2016, 4, 5),
                amounts = StatePensionAmounts(
                  protectedPayment = false,
                  StatePensionAmountRegular(118.65, 590.10, 7081.15),
                  StatePensionAmountForecast(0, 150.65, 676.80, 8121.59),
                  StatePensionAmountMaximum(0, 0, 150.65, 590.10, 7081.15),
                  StatePensionAmountRegular(0, 0, 0)
                ),
                pensionAge = 67,
                LocalDate.of(2022, 6, 7),
                "2021-22",
                11,
                pensionSharingOrder = false,
                currentFullWeeklyPensionAmount = 151.65,
                reducedRateElection = false,
                statePensionAgeUnderConsideration = true
              )
              ))))

            when(mockNationalInsuranceService.getSummary(any(), any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 11,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 0,
                numberOfGapsPayable = 0,
                Some(LocalDate.of(1989, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
                ),
                reducedRateElection = false
              )
              ))))
          }

          lazy val doc =
            asDocument(contentAsString(statePensionController.show()(generateFakeRequest)))

          lazy val abroadUserDoc =
            asDocument(contentAsString(abroadUserController.show()(generateFakeRequest)))

          // overseas message
          "render page with text 'As you are living or working overseas (opens in new tab), you may be entitled to a " +
            "State Pension from the country you are living or working in.'" in {
              mockSetup
              assertEqualsMessage(
                abroadUserDoc,
                "[data-spec='abroad__inset_text__link1']",
                "nisp.main.overseas.linktext"
              )
            }

          // SPA under consideration message
          "render page with Heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertEqualsMessage(
              abroadUserDoc,
              "[data-spec='state_pension_age_under_consideration__h2_1']",
              "nisp.spa.under.consideration.title"
            )
          }

          "render page with text 'You’ll reach State Pension age on 7 June 2022. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertContainsDynamicMessage(
              abroadUserDoc,
              "[data-spec='state_pension_age_under_consideration__p1']",
              "nisp.spa.under.consideration.detail",
              langUtils.Dates.formatDate(LocalDate.of(2022, 6, 7))
            )
          }
          /*Ends*/

          // deferral message
          "render page with Heading 'Putting off claiming'" in {
            mockSetup
            assertEqualsMessage(
              abroadUserDoc,
              "[data-spec='deferral__h2_1']",
              "nisp.main.puttingOff"
            )
          }

          "render page with text 'You can put off claiming your State Pension from 7 June 2022. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            mockSetup
            assertContainsDynamicMessage(
              abroadUserDoc,
              "[data-spec='deferral__p1']",
              "nisp.main.puttingOff.line1",
              langUtils.Dates.formatDate(LocalDate.of(2022, 6, 7))
            )
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertEqualsMessage(
              abroadUserDoc,
              "[data-spec='deferral__link1']",
              "nisp.main.puttingOff.linkTitle"
            )
          }

          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertLinkHasValue(
              abroadUserDoc,
              "[data-spec='deferral__link1']",
              "https://www.gov.uk/deferring-state-pension"
            )
          }

          "render page with print link" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension__printlink']",
              "nisp.print.your.state.pension.summary"
            )
          }
        }

        "State Pension view with NON-MQP : No need to fill gaps || Full Rate and Personal Max: when some one has more years left" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any(), any())(any()))
              .thenReturn(Future.successful(Right(Right(StatePension(
                LocalDate.of(2016, 4, 5),
                amounts = StatePensionAmounts(
                  protectedPayment = false,
                  StatePensionAmountRegular(149.65, 590.10, 7081.15),
                  StatePensionAmountForecast(4, 155.65, 676.80, 8121.59),
                  StatePensionAmountMaximum(4, 2, 155.65, 590.10, 7081.15),
                  StatePensionAmountRegular(0, 0, 0)
                ),
                pensionAge = 67,
                LocalDate.of(2017, 6, 7),
                "2019-20",
                11,
                pensionSharingOrder = false,
                currentFullWeeklyPensionAmount = 149.65,
                reducedRateElection = false,
                statePensionAgeUnderConsideration = false
              )
              ))))

            when(mockNationalInsuranceService.getSummary(any(), any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 11,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 2,
                numberOfGapsPayable = 2,
                Some(LocalDate.of(1989, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
                ),
                reducedRateElection = false
              )
              ))))
          }

          lazy val doc =
            asDocument(contentAsString(statePensionController.show()(generateFakeRequest)))

          lazy val abroadUserDoc =
            asDocument(contentAsString(abroadUserController.show()(generateFakeRequest)))

          "render with correct page title" in {
            mockSetup
            assertElementContainsText(
              doc,
              "head > title",
              messages("nisp.main.title")
                + Constants.titleSplitter
                + messages("nisp.title.extension")
                + Constants.titleSplitter
                + messages("nisp.gov-uk")
            )
          }

          "render page with Heading 'Your State Pension' " in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-component='nisp_page_heading__h1']",
              "nisp.main.h1.title"
            )
          }

          "render page with text 'You can get your State Pension on 7 june 2017' " in {
            mockSetup
            assertEqualsValue(
              doc,
              "[data-component='nisp_panel'] [data-component='nisp_panel__title']",
              messages("nisp.main.basedOn") + " " + langUtils.Dates.formatDate(LocalDate.of(2017, 6, 7))
            )
          }

          "render page with text 'Your forecast is £155.65 a week, £676.80 a month, £8,121.59 a year' " in {
            mockSetup
            val sMessage =
              messages("nisp.main.caveats") + " " +
                messages("nisp.is") + " £155.65 " +
                messages("nisp.main.week") + ", £676.80 " +
                messages("nisp.main.month") + ", £8,121.59 " +
                messages("nisp.main.year")
            assertEqualsValue(
              doc,
              "[data-spec='state_pension__panel1__caveats']",
              sMessage
            )
          }

          "render page with text 'Your forecast '" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='continue_working__p__caveats']",
              "nisp.main.caveats"
            )
          }

          "render page with text 'is not a guarantee and is based on the current law '" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='continue_working__ul__caveats__1']",
              "nisp.main.notAGuarantee"
            )
          }

          "render page with text 'does not include any increase due to inflation '" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='continue_working__ul__caveats__2']",
              "nisp.main.inflation"
            )
          }

          "render page with Heading 'You need to continue to contribute National Insurance to reach your forecast'" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='continue_working__h2_1']",
              "nisp.main.continueContribute"
            )
          }

          "render page with text 'Estimate based on your National Insurance record up to '" in {
            mockSetup
            assertContainsDynamicMessage(
              doc,
              "[data-spec='continue_working__chart1'] [data-component='nisp_chart__title']",
              "nisp.main.chart.lastprocessed.title",
              "2016"
            )
          }

          "render page with text '£149.65 a week '" in {
            mockSetup
            val sMessage = "£149.65 " + messages("nisp.main.chart.week")
            assertEqualsValue(
              doc,
              "[data-spec='continue_working__chart1'] [data-component='nisp_chart__inner_text']",
              sMessage
            )
          }

          "render page with text 'Forecast if you contribute another 4 years before 5 April 2020'" in {
            mockSetup
            assertContainsDynamicMessage(
              doc,
              "[data-spec='continue_working__chart4'] [data-component='nisp_chart__title']",
              "nisp.main.chart.estimateIfYouContinue.plural",
              "4",
              "2020"
            )
          }

          "render page with text '£155.65 a week '" in {
            mockSetup
            val sMessage = "£155.65 " + messages("nisp.main.chart.week")
            assertEqualsValue(
              doc,
              "[data-spec='continue_working__chart4'] [data-component='nisp_chart__inner_text']",
              sMessage
            )
          }

          "render page with text '£155.65 is the most you can get'" in {
            mockSetup
            val sMessage = "£155.65 " + StringEscapeUtils.unescapeHtml4(messages("nisp.main.mostYouCanGet"))
            assertEqualsValue(
              doc,
              "[data-spec='continue_working__h2_2']",
              sMessage
            )
          }

          "render page with text 'You cannot improve your forecast any further, unless you choose to put off claiming'" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='continue_working__p2']",
              "nisp.main.context.willReach"
            )
          }

          "render page with text 'If you’re working you may still need to pay National Insurance contributions until 7 June 2020 as they fund other state benefits and the NHS.'" in {
            mockSetup
            assertContainsDynamicMessage(
              doc,
              "[data-spec='continue_working__p3']",
              "nisp.main.context.reachMax.needToPay",
              langUtils.Dates.formatDate(LocalDate.of(2017, 6, 7))
            )
          }

          "render page with link 'View your National Insurance Record'" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='continue_working__link1']",
              "nisp.main.showyourrecord"
            )
          }

          "render page with href link 'View your National Insurance Record'" in {
            mockSetup
            assertLinkHasValue(
              doc,
              "[data-spec='continue_working__link1']",
              "/check-your-state-pension/account/nirecord"
            )
          }

          // overseas message
          "render page with text 'As you are living or working overseas (opens in new tab), you may be entitled to a " +
            "State Pension from the country you are living or working in.'" in {
              mockSetup
              assertEqualsMessage(
                abroadUserDoc,
                "[data-spec='abroad__inset_text__link1']",
                "nisp.main.overseas.linktext"
              )
            }
          /*Ends*/

          // SPA under consideration message
          "Not render page with Heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertPageDoesNotContainMessage(
              doc,
              "nisp.spa.under.consideration.title"
            )
          }

          "Not render page with text 'You’ll reach State Pension age on 7 June 2017. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertPageDoesNotContainDynamicMessage(
              abroadUserDoc,
              "nisp.spa.under.consideration.detail",
              langUtils.Dates.formatDate(LocalDate.of(2017, 6, 7))
            )
          }
          /*Ends*/

          "render page with Heading 'Putting off claiming'" in {
            mockSetup
            assertEqualsMessage(
              abroadUserDoc,
              "[data-spec='deferral__h2_1']",
              "nisp.main.puttingOff"
            )
          }

          "render page with text 'You can put off claiming your State Pension from 7 June 2017. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            mockSetup
            assertContainsDynamicMessage(
              abroadUserDoc,
              "[data-spec='deferral__p1']",
              "nisp.main.puttingOff.line1",
              langUtils.Dates.formatDate(LocalDate.of(2017, 6, 7))
            )
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertEqualsMessage(
              abroadUserDoc,
              "[data-spec='deferral__link1']",
              "nisp.main.puttingOff.linkTitle"
            )
          }

          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertLinkHasValue(
              abroadUserDoc,
              "[data-spec='deferral__link1']",
              "https://www.gov.uk/deferring-state-pension"
            )
          }

          "render page with print link" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension__printlink']",
              "nisp.print.your.state.pension.summary"
            )
          }

        }

        "State Pension view with NON-MQP : No need to fill gaps || Full Rate and Personal Max: when some one has more years left: With State Pension age under consideration message" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any(), any())(any()))
              .thenReturn(Future.successful(Right(Right(StatePension(
                LocalDate.of(2016, 4, 5),
                amounts = StatePensionAmounts(
                  protectedPayment = false,
                  StatePensionAmountRegular(149.65, 590.10, 7081.15),
                  StatePensionAmountForecast(4, 155.65, 676.80, 8121.59),
                  StatePensionAmountMaximum(4, 2, 155.65, 590.10, 7081.15),
                  StatePensionAmountRegular(0, 0, 0)
                ),
                pensionAge = 67,
                LocalDate.of(2017, 6, 7),
                "2019-20",
                11,
                pensionSharingOrder = false,
                currentFullWeeklyPensionAmount = 149.65,
                reducedRateElection = false,
                statePensionAgeUnderConsideration = true
              )
              ))))

            when(mockNationalInsuranceService.getSummary(any(), any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 11,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 2,
                numberOfGapsPayable = 2,
                Some(LocalDate.of(1989, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
                ),
                reducedRateElection = false
              )
              ))))

          }

          lazy val doc =
            asDocument(contentAsString(statePensionController.show()(generateFakeRequest)))

          lazy val abroadUserDoc =
            asDocument(contentAsString(abroadUserController.show()(generateFakeRequest)))

          // overseas message
          "render page with text 'As you are living or working overseas (opens in new tab), you may be entitled to a " +
            "State Pension from the country you are living or working in.'" in {
              mockSetup
              assertEqualsMessage(
                abroadUserDoc,
                "[data-spec='abroad__inset_text__link1']",
                "nisp.main.overseas.linktext"
              )
            }

          // SPA under consideration message
          "render page with Heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertEqualsMessage(
              abroadUserDoc,
              "[data-spec='state_pension_age_under_consideration__h2_1']",
              "nisp.spa.under.consideration.title"
            )
          }

          "render page with text 'You’ll reach State Pension age on 7 June 2017. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertContainsDynamicMessage(
              abroadUserDoc,
              "[data-spec='state_pension_age_under_consideration__p1']",
              "nisp.spa.under.consideration.detail",
              langUtils.Dates.formatDate(LocalDate.of(2017, 6, 7))
            )
          }
          /*Ends*/

          // deferral message
          "render page with Heading 'Putting off claiming'" in {
            mockSetup
            assertEqualsMessage(
              abroadUserDoc,
              "[data-spec='deferral__h2_1']",
              "nisp.main.puttingOff"
            )
          }

          "render page with text 'You can put off claiming your State Pension from 7 June 2017. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
            mockSetup
            assertContainsDynamicMessage(
              abroadUserDoc,
              "[data-spec='deferral__p1']",
              "nisp.main.puttingOff.line1",
              langUtils.Dates.formatDate(LocalDate.of(2017, 6, 7))
            )
          }

          "render page with link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertEqualsMessage(
              abroadUserDoc,
              "[data-spec='deferral__link1']",
              "nisp.main.puttingOff.linkTitle"
            )
          }

          "render page with href link 'More on putting off claiming (opens in new tab)'" in {
            mockSetup
            assertLinkHasValue(
              abroadUserDoc,
              "[data-spec='deferral__link1']",
              "https://www.gov.uk/deferring-state-pension"
            )
          }

          "render page with print link" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension__printlink']",
              "nisp.print.your.state.pension.summary"
            )
          }
        }
      }

      "State Pension view with NON-MQP : Reached || No Gaps || Full Rate and Personal Max" should {

        def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
          when(mockStatePensionService.getSummary(any(), any())(any()))
            .thenReturn(Future.successful(Right(Right(StatePension(
              LocalDate.of(2016, 4, 5),
              amounts = StatePensionAmounts(
                protectedPayment = false,
                StatePensionAmountRegular(155.65, 590.10, 7081.15),
                StatePensionAmountForecast(4, 155.65, 676.80, 8121.59),
                StatePensionAmountMaximum(4, 2, 155.65, 590.10, 7081.15),
                StatePensionAmountRegular(0, 0, 0)
              ),
              pensionAge = 67,
              LocalDate.of(2017, 6, 7),
              "2019-20",
              11,
              pensionSharingOrder = false,
              currentFullWeeklyPensionAmount = 149.65,
              reducedRateElection = false,
              statePensionAgeUnderConsideration = false
            )
            ))))

          when(mockNationalInsuranceService.getSummary(any(), any())(any()))
            .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
              qualifyingYears = 11,
              qualifyingYearsPriorTo1975 = 0,
              numberOfGaps = 0,
              numberOfGapsPayable = 0,
              Some(LocalDate.of(1989, 3, 6)),
              homeResponsibilitiesProtection = false,
              LocalDate.of(2017, 4, 5),
              List(

                NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
              ),
              reducedRateElection = false
            )
            ))))
        }

        lazy val doc =
          asDocument(contentAsString(statePensionController.show()(generateFakeRequest)))

        lazy val abroadUserDoc =
          asDocument(contentAsString(abroadUserController.show()(generateFakeRequest)))

        "render with correct page title" in {
          mockSetup
          assertElementContainsText(
            doc,
            "head > title",
            messages("nisp.main.title")
              + Constants.titleSplitter
              + messages("nisp.title.extension")
              + Constants.titleSplitter
              + messages("nisp.gov-uk")
          )
        }

        "render page with Heading 'Your State Pension' " in {
          mockSetup
          assertEqualsMessage(
            doc,
            "[data-component='nisp_page_heading__h1']",
            "nisp.main.h1.title"
          )
        }

        "render page with text 'You can get your State Pension on 7 june 2017' " in {
          mockSetup
          assertEqualsValue(
            doc,
            "[data-component='nisp_panel'] [data-component='nisp_panel__title']",
            messages("nisp.main.basedOn") + " " + langUtils.Dates.formatDate(LocalDate.of(2017, 6, 7))
          )
        }

        "render page with text 'Your forecast is £155.65 a week, £676.80 a month, £8,121.59 a year ' " in {
          mockSetup
          val sMessage =
            messages("nisp.main.caveats") + " " +
              messages("nisp.is") + " £155.65 " +
              messages("nisp.main.week") + ", £676.80 " +
              messages("nisp.main.month") + ", £8,121.59 " +
              messages("nisp.main.year")
          assertEqualsValue(
            doc,
            "[data-spec='state_pension__panel1__caveats']",
            sMessage
          )
        }

        "render page with text 'Your forecast '" in {
          mockSetup
          assertEqualsMessage(
            doc,
            "[data-spec='reached__p__caveats']",
            "nisp.main.caveats"
          )
        }

        "render page with text 'is not a guarantee and is based on the current law '" in {
          mockSetup
          assertEqualsMessage(
            doc,
            "[data-spec='reached__ul__caveats__li1']",
            "nisp.main.notAGuarantee"
          )
        }

        "render page with text 'is based on your National Insurance record up to 5 April 2016 '" in {
          mockSetup
          assertContainsDynamicMessage(
            doc,
            "[data-spec='reached__ul__caveats__li2']",
            "nisp.main.isBased",
            langUtils.Dates.formatDate(LocalDate.of(2016, 4, 5))
          )
        }

        "render page with text 'does not include any increase due to inflation '" in {
          mockSetup
          assertEqualsMessage(
            doc,
            "[data-spec='reached__ul__caveats__li3']",
            "nisp.main.inflation"
          )
        }

        "render page with text '£155.65 is the most you can get'" in {
          mockSetup
          val sMessage = "£155.65 " + StringEscapeUtils.unescapeHtml4(messages("nisp.main.mostYouCanGet"))
          assertEqualsValue(
            doc,
            "[data-spec='reached_h2_1']",
            sMessage
          )
        }

        "render page with text 'You cannot improve your forecast any more'" in {
          mockSetup
          assertEqualsMessage(
            doc,
            "[data-spec='reached__p2']",
            "nisp.main.cantImprove"
          )
        }

        "render page with text 'If you’re working you may still need to pay National Insurance contributions until 7 June 2020 as they fund other state benefits and the NHS.'" in {
          mockSetup
          assertContainsDynamicMessage(
            doc,
            "[data-spec='reached__p3']",
            "nisp.main.context.reachMax.needToPay",
            langUtils.Dates.formatDate(LocalDate.of(2017, 6, 7))
          )
        }

        "render page with link 'View your National Insurance Record'" in {
          mockSetup
          assertEqualsMessage(
            doc,
            "[data-spec='reached__link1']",
            "nisp.main.showyourrecord"
          )
        }

        "render page with href link 'View your National Insurance Record'" in {
          mockSetup
          assertLinkHasValue(
            doc,
            "[data-spec='reached__link1']",
            "/check-your-state-pension/account/nirecord"
          )
        }

        /*overseas message*/
        "render page with text 'As you are living or working overseas (opens in new tab), you may be entitled to a " +
          "State Pension from the country you are living or working in.'" in {
            mockSetup
            assertEqualsMessage(
              abroadUserDoc,
              "[data-spec='abroad__inset_text__link1']",
              "nisp.main.overseas.linktext"
            )
          }
        /*Ends*/

        // SPA under consideration message
        "Not render page with heading 'Proposed change to your State Pension age'" in {
          mockSetup
          assertPageDoesNotContainMessage(
            abroadUserDoc,
            "nisp.spa.under.consideration.title"
          )
        }

        "Not render page with text 'You’ll reach State Pension age on 7 June 2017. Under government proposals this may increase by up to a year.'" in {
          mockSetup
          assertPageDoesNotContainDynamicMessage(
            abroadUserDoc,
            "nisp.spa.under.consideration.detail",
            langUtils.Dates.formatDate(LocalDate.of(2017, 6, 7))
          )
        }
        /*Ends*/

        "render page with Heading 'Putting off claiming'" in {
          mockSetup
          assertEqualsMessage(
            abroadUserDoc,
            "[data-spec='deferral__h2_1']",
            "nisp.main.puttingOff"
          )
        }

        "render page with text 'You can put off claiming your State Pension from 7 June 2017. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
          mockSetup
          assertContainsDynamicMessage(
            abroadUserDoc,
            "[data-spec='deferral__p1']",
            "nisp.main.puttingOff.line1",
            langUtils.Dates.formatDate(LocalDate.of(2017, 6, 7))
          )
        }

        "render page with link 'More on putting off claiming (opens in new tab)'" in {
          mockSetup
          assertEqualsMessage(
            abroadUserDoc,
            "[data-spec='deferral__link1']",
            "nisp.main.puttingOff.linkTitle"
          )
        }

        "render page with href link 'More on putting off claiming (opens in new tab)'" in {
          mockSetup
          assertLinkHasValue(
            abroadUserDoc,
            "[data-spec='deferral__link1']",
            "https://www.gov.uk/deferring-state-pension"
          )
        }

        "render page with print link" in {
          mockSetup
          assertEqualsMessage(
            doc,
            "[data-spec='state_pension__printlink']",
            "nisp.print.your.state.pension.summary"
          )
        }

      }

      "State Pension view with NON-MQP : Reached || No Gaps || Full Rate and Personal Max: With State Pension age under consideration message" should {

        def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
          when(mockStatePensionService.getSummary(any(), any())(any()))
            .thenReturn(Future.successful(Right(Right(StatePension(
              LocalDate.of(2016, 4, 5),
              amounts = StatePensionAmounts(
                protectedPayment = false,
                StatePensionAmountRegular(155.65, 590.10, 7081.15),
                StatePensionAmountForecast(4, 155.65, 676.80, 8121.59),
                StatePensionAmountMaximum(4, 2, 155.65, 590.10, 7081.15),
                StatePensionAmountRegular(0, 0, 0)
              ),
              pensionAge = 67,
              LocalDate.of(2017, 6, 7),
              "2019-20",
              11,
              pensionSharingOrder = false,
              currentFullWeeklyPensionAmount = 149.65,
              reducedRateElection = false,
              statePensionAgeUnderConsideration = true
            )
            ))))

          when(mockNationalInsuranceService.getSummary(any(), any())(any()))
            .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
              qualifyingYears = 11,
              qualifyingYearsPriorTo1975 = 0,
              numberOfGaps = 0,
              numberOfGapsPayable = 0,
              Some(LocalDate.of(1989, 3, 6)),
              homeResponsibilitiesProtection = false,
              LocalDate.of(2017, 4, 5),
              List(

                NationalInsuranceTaxYearBuilder("2015-16", qualifying = true, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2013-14", qualifying = true, underInvestigation = false) /*payable = true*/
              ),
              reducedRateElection = false
            )
            ))))
        }

        lazy val abroadUserDoc =
          asDocument(contentAsString(abroadUserController.show()(generateFakeRequest)))

        // overseas message
        "render page with text 'As you are living or working overseas (opens in new tab), you may be entitled to a " +
          "State Pension from the country you are living or working in.'" in {
            mockSetup
            assertEqualsMessage(
              abroadUserDoc,
              "[data-spec='abroad__inset_text__link1']",
              "nisp.main.overseas.linktext"
            )
          }

        // SPA under consideration message
        "render page with Heading 'Proposed change to your State Pension age'" in {
          mockSetup
          assertEqualsMessage(
            abroadUserDoc,
            "[data-spec='state_pension_age_under_consideration__h2_1']",
            "nisp.spa.under.consideration.title"
          )
        }

        "render page with text 'You’ll reach State Pension age on 7 June 2017. Under government proposals this may increase by up to a year.'" in {
          mockSetup
          assertContainsDynamicMessage(
            abroadUserDoc,
            "[data-spec='state_pension_age_under_consideration__p1']",
            "nisp.spa.under.consideration.detail",
            langUtils.Dates.formatDate(LocalDate.of(2017, 6, 7))
          )
        }
        /*Ends*/

        // deferral message
        "render page with Heading 'Putting off claiming'" in {
          mockSetup
          assertEqualsMessage(
            abroadUserDoc,
            "[data-spec='deferral__h2_1']",
            "nisp.main.puttingOff"
          )
        }

        "render page with text 'You can put off claiming your State Pension from 7 June 2017. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
          mockSetup
          assertContainsDynamicMessage(
            abroadUserDoc,
            "[data-spec='deferral__p1']",
            "nisp.main.puttingOff.line1",
            langUtils.Dates.formatDate(LocalDate.of(2017, 6, 7))
          )
        }

        "render page with link 'More on putting off claiming (opens in new tab)'" in {
          mockSetup
          assertEqualsMessage(
            abroadUserDoc,
            "[data-spec='deferral__link1']",
            "nisp.main.puttingOff.linkTitle"
          )
        }

        "render page with href link 'More on putting off claiming (opens in new tab)'" in {
          mockSetup
          assertLinkHasValue(
            abroadUserDoc,
            "[data-spec='deferral__link1']",
            "https://www.gov.uk/deferring-state-pension"
          )
        }
        "render page with print link" in {
          mockSetup
          assertEqualsMessage(
            abroadUserDoc,
            "[data-spec='state_pension__printlink']",
            "nisp.print.your.state.pension.summary"
          )
        }
      }
    }
  }
}
