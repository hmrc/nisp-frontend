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

package uk.gov.hmrc.nisp.views.statePension

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{Injector, bind}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers.contentAsString
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.{LoginTimes, Name}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.http.{SessionKeys, UpstreamErrorResponse}
import uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlag
import uk.gov.hmrc.nisp.builders.NationalInsuranceTaxYearBuilder
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.StatePensionController
import uk.gov.hmrc.nisp.controllers.auth.{AuthDetails, AuthRetrievals, AuthenticatedRequest, NispAuthedUser, PertaxAuthAction}
import uk.gov.hmrc.nisp.controllers.pertax.PertaxHelper
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.models.admin.NewStatePensionUIToggle
import uk.gov.hmrc.nisp.services.{NationalInsuranceService, StatePensionService}
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.nisp.views.HtmlSpec
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.language.LanguageUtils

import java.time.{Instant, LocalDate}
import java.util.UUID
import scala.concurrent.Future

class StatePensionViewSpec
  extends HtmlSpec
    with Injecting
    with WireMockSupport {

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val mockNationalInsuranceService: NationalInsuranceService = mock[NationalInsuranceService]
  val mockStatePensionService: StatePensionService = mock[StatePensionService]
  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  val mockPertaxHelper: PertaxHelper = mock[PertaxHelper]

  lazy val langUtils: LanguageUtils = inject[LanguageUtils]

  val standardInjector: Injector = GuiceApplicationBuilder()
    .overrides(
      bind[AuthRetrievals].to[FakeAuthAction],
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
      bind[AuthRetrievals].to[FakeAuthActionWithNino],
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
    reset(mockFeatureFlagService)

    wireMockServer.resetAll()
    when(mockPertaxHelper.isFromPertax(any()))
      .thenReturn(Future.successful(false))
    when(mockAppConfig.reportAProblemNonJSUrl)
      .thenReturn("/reportAProblem")
    when(mockAppConfig.contactFormServiceIdentifier)
      .thenReturn("/id")
    when(mockAppConfig.pertaxAuthBaseUrl)
      .thenReturn(s"http://localhost:${wireMockServer.port()}")
    when(mockFeatureFlagService.get(NewStatePensionUIToggle))
      .thenReturn(Future.successful(FeatureFlag(NewStatePensionUIToggle, isEnabled = true)))
  }

  lazy val statePensionController: StatePensionController = standardInjector.instanceOf[StatePensionController]
  lazy val abroadUserController: StatePensionController = abroadUserInjector.instanceOf[StatePensionController]

  def generateFakeRequest: FakeRequest[AnyContentAsEmpty.type] = fakeRequest.withSession(
    SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
    SessionKeys.lastRequestTimestamp -> LocalDate.now.toEpochDay.toString
  )

  "The State Pension page" when {

    "the user is a MQP" when {
      "The scenario is continue working || No Gaps" when {

        "State Pension view with MQP : No Gaps || Full Rate & Personal Max" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(StatePension(
                LocalDate.of(2016, 4, 5),
                amounts = StatePensionAmounts(
                  protectedPayment = false,
                  StatePensionAmountRegular(118.65, 590.10, 7081.15),
                  StatePensionAmountForecast(10, 150.65, 676.80, 8121.59),
                  StatePensionAmountMaximum(0, 0, 150.65, 590.10, 7081.15),
                  StatePensionAmountRegular(0, 0, 0)
                ),
                pensionAge = 67,
                LocalDate.of(2022, 6, 7),
                "2021-22",
                4,
                pensionSharingOrder = false,
                currentFullWeeklyPensionAmount = 151.65,
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
                Some(LocalDate.of(1989, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false)
                ),
                reducedRateElection = false
              )
              ))))

          }

          lazy val doc =
            asDocument(contentAsString(statePensionController.show()(generateFakeRequest)))

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

          "render page with Page Heading 'Your State Pension summary'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__pageheading']",
              "Your State Pension summary"
            )
          }

          "render page with heading 'When will I reach State Pension age?'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__h2_1']",
              "When will I reach State Pension age?"
            )
          }

          "render page with text 'You will reach State Pension age on 7 June 2022.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__p1']",
              "You will reach State Pension age on 7 June 2022."
            )
          }

          "render page with text 'Your State Pension age and forecast are not a guarantee and are based on the current law." +
            " Your State Pension age may change in the future.' " in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__p2']",
              "Your State Pension age and forecast are not a guarantee and are based on the current law. Your State Pension age may change in the future."
            )
          }

          "render page with text 'Your forecast:'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__p_caveats']",
              "Your forecast:"
            )
          }

          "render page with bullet text 'is based on your National Insurance record up to 5th April 2016'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__ul__caveats__li1']",
              "is based on your National Insurance record up to 5 April 2016"
            )
          }

          "render page with bullet text 'assumes that you’ll contribute another 10 years'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__ul__caveats__li2__plural']",
              "assumes that you’ll contribute another 10 years"
            )
          }

          "render page with bullet text 'does not include any increase due to inflation'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__ul__caveats_inflation__li3']",
              "does not include any increase due to inflation"
            )
          }

          "render page with warning text" in {
            mockSetup
            assertEqualsText(
              doc,
              "#state_pension__warning_text",
              "! Warning Your State Pension forecast is for your information only. This service does not offer financial advice. " +
                "When planning for your retirement, you should seek guidance or financial advice."
            )

            assertLinkHasValue(
              doc,
              "[data-spec='state_pension__warning__link']",
              "/seek-financial-advice"
            )
          }

          "render page with heading 'How much will I get?'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__h2_2']",
              "How much will I get?"
            )
          }

          "render page with text 'The full new State Pension is £151.65 a week.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='new_state_pension__p1']",
              "The full new State Pension is £151.65 a week."
            )
          }

          "render page with text and link 'Your forecast may change if there are any updates to your National Insurance information." +
            " You can find out more in the terms and conditions.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__legal__forecast_changes__p']",
              "Your forecast may change if there are any updates to your National Insurance information." +
                " You can find out more in the terms and conditions."
            )
            mockSetup
            assertLinkHasValue(
              doc,
              "[data-spec='state_pension__legal__forecast_changes__link']",
              "/check-your-state-pension/terms-and-conditions?showBackLink=true"
            )
          }

          "render page with heading 'How can I increase my State Pension?'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__h2_3']",
              "How can I increase my State Pension?"
            )
          }

          "render page with button 'View your National Insurance Record'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__showyourrecord']",
              "View your National Insurance record"
            )

            assertLinkHasValue(
              doc,
              "[data-spec='state_pension__showyourrecord']",
              "/check-your-state-pension/account/nirecord"
            )
          }

          "render page with link 'Find out about National Insurance.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__mqp__find_out_ni']",
              "Find out about National Insurance."
            )

            assertLinkHasValue(
              doc,
              "[data-spec='state_pension__mqp__find_out_ni']",
              "https://www.gov.uk/national-insurance"
            )
          }

          "render page with link 'Find out how much National Insurance you pay.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__mqp__find_out_ni_you_pay']",
              "Find out how much National Insurance you pay."
            )

            assertLinkHasValue(
              doc,
              "[data-spec='state_pension__mqp__find_out_ni_you_pay']",
              "https://www.gov.uk/national-insurance/how-much-you-pay"
            )
          }

          "render page with link 'Find out about getting National Insurance credits.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__mqp__find_out_ni_credits']",
              "Find out about getting National Insurance credits."
            )

            assertLinkHasValue(
              doc,
              "[data-spec='state_pension__mqp__find_out_ni_credits']",
              "https://www.gov.uk/national-insurance-credits"
            )
          }

          "render page with section 'Other ways to increase my income'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__h3_1']",
              "Other ways to increase my income"
            )
          }

          "render page with 'Home Responsibilities Protection' paragraph with link" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension_other_ways__p1']",
              "If you claimed Child Benefit between 6 April 1978 and 5 April 2010, you may be able to claim " +
                "Home Responsibilities Protection to fill gaps in your National Insurance record. Check if you are eligible to claim Home Responsibilities Protection."
            )

            assertLinkHasValue(
              doc,
              "[data-spec='state_pension__home_responsibilities_protection__link']",
              "https://www.gov.uk/home-responsibilities-protection-hrp"
            )
          }

          "render page with 'Pension Credit' paragraph with link" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension_other_ways__p2']",
              "You may be able to claim Pension Credit if you’re on a low income. " +
                "Find out about claiming Pension Credit."
            )

            assertLinkHasValue(
              doc,
              "[data-spec='state_pension__pension_credit__link']",
              "https://www.gov.uk/pension-credit/overview"
            )
          }

          "render page with 'Delaying your State Pension' paragraph with link" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__deferral__p1']",
              "You can delay claiming your State Pension. " +
                "This means you may get extra State Pension when you do claim it. " +
                "Find out about delaying your State Pension."

            )

            assertLinkHasValue(
              doc,
              "[data-spec='state_pension__deferral__link']",
              "https://www.gov.uk/deferring-state-pension"
            )
          }

          "render page with State Pension footer" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__footer_h2']",
              "Get help"
            )

            assertEqualsText(
              doc,
              "[data-spec='state_pension__footer_p1']",
              "For more information on gaps in your National Insurance record contact the Future Pension Centre."
            )

            assertLinkHasValue(
              doc,
              "[data-spec='state_pension__footer_link']",
              "https://www.gov.uk/future-pension-centre"
            )
          }

          "render page with Print Link" in {
            mockSetup
            doc.getElementById("printLink") shouldNot be(null)
            assertEqualsText(
              doc,
              "[data-spec='state_pension__printlink']",
              "Print your State Pension summary"
            )
            assertLinkHasValue(
              doc,
              "[data-spec='state_pension__printlink']",
              "#"
            )
          }

          // MQP message
          "render page with text 'You have 4 qualifying years on your National Insurance record." +
            " You usually need at least 10 qualifying years to get any State Pension.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__current_qualifying_years__plural']",
              "You have 4 qualifying years on your National Insurance record. You usually need at least 10 qualifying years to get any State Pension."
            )
          }

          // overseas message
          "render page with text and link 'If you’ve lived or worked outside the UK...'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension_outside_uk__p1']",
              "If you’ve lived or worked outside the UK, you may be able to use your time outside the UK " +
                "to make up the 10 qualifying years you need to get any UK State Pension. " +
                "Find out more about living or working outside the UK."
            )

            assertLinkHasValue(
              doc,
              "[data-spec='state_pension_outside_uk__link']",
              "https://www.gov.uk/new-state-pension/living-and-working-overseas"
            )
          }

          // MQP bar charts
          "render page with forecast chart title 'Forecast if you contribute until'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='continue_working__mqp_forecast__chart1-key']",
              "Forecast if you contribute National Insurance until 5 April 2022"
            )
          }

          "render page with forecast chart text '£150.65 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='continue_working__mqp_forecast__chart1-value']",
              "£150.65 a week"
            )
          }

          "render page with personal max chart title 'Forecast if you contribute until'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='continue_working__mqp_personal_max__chart2-key']",
              "The most you can increase your forecast to is"
            )
          }

          "render page with personal max chart text '£150.65 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='continue_working__mqp_personal_max__chart2-value']",
              "£150.65 a week"
            )
          }

          // no gaps
          "render page with text 'You cannot increase your State Pension forecast." +
            " £150.65 a week is the most you can get.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__cant_pay_gaps__p1']",
              "You cannot increase your State Pension forecast. " +
                "£150.65 a week is the most you can get."
            )
          }

          "render page with text 'This means you are unable to pay for gaps in your National Insurance record online.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__cant_pay_gaps__p2']",
              "This means you are unable to pay for gaps in your National Insurance record online."
            )
          }

          // SPA not under consideration
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

        }

        "State Pension view with MQP : No Gaps || Full Rate & Personal Max: With State Pension age under consideration message" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(StatePension(
                LocalDate.of(2016, 4, 5),
                amounts = StatePensionAmounts(
                  protectedPayment = false,
                  StatePensionAmountRegular(118.65, 590.10, 7081.15),
                  StatePensionAmountForecast(20, 150.65, 676.80, 8121.59),
                  StatePensionAmountMaximum(0, 0, 150.65, 590.10, 7081.15),
                  StatePensionAmountRegular(0, 0, 0)
                ),
                pensionAge = 67,
                LocalDate.of(2022, 6, 7),
                "2021-22",
                1,
                pensionSharingOrder = false,
                currentFullWeeklyPensionAmount = 151.65,
                reducedRateElection = false,
                statePensionAgeUnderConsideration = true
              )
              ))))

            when(mockNationalInsuranceService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 1,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 0,
                numberOfGapsPayable = 0,
                Some(LocalDate.of(1989, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false)
                ),
                reducedRateElection = false
              )
              ))))
          }

          lazy val doc =
            asDocument(contentAsString(statePensionController.show()(generateFakeRequest)))

          lazy val abroadUserDoc =
            asDocument(contentAsString(abroadUserController.show()(generateFakeRequest)))

          // MQP message
          "render page with text 'You have 1 qualifying year on your National Insurance record." +
            " You usually need at least 10 qualifying years to get any State Pension.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__current_qualifying_years__single']",
              "You have 1 qualifying year on your National Insurance record. " +
                "You usually need at least 10 qualifying years to get any State Pension."
            )
          }

          // Overseas message
          "render page with text and link 'If you’ve lived or worked outside the UK...'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension_outside_uk__p1']",
              "If you’ve lived or worked outside the UK, you may be able to use your time outside the UK " +
                "to make up the 10 qualifying years you need to get any UK State Pension. " +
                "Find out more about living or working outside the UK."
            )

            assertLinkHasValue(
              doc,
              "[data-spec='state_pension_outside_uk__link']",
              "https://www.gov.uk/new-state-pension/living-and-working-overseas"
            )
          }

          // MQP bar charts
          "render page with forecast chart title 'Forecast if you contribute until'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='continue_working__mqp_forecast__chart1-key']",
              "Forecast if you contribute National Insurance until 5 April 2022"
            )
          }

          "render page with forecast chart text '£150.65 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='continue_working__mqp_forecast__chart1-value']",
              "£150.65 a week"
            )
          }

          "render page with personal max chart title 'Forecast if you contribute until'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='continue_working__mqp_personal_max__chart2-key']",
              "The most you can increase your forecast to is"
            )
          }

          "render page with personal max chart text '£150.65 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='continue_working__mqp_personal_max__chart2-value']",
              "£150.65 a week"
            )
          }

          "Not render page with current chart 'Current estimate based on your National Insurance record up to'" in {
            mockSetup
            assertPageDoesNotContainMessage(
              doc,
              "nisp.main.chart.current"
            )

          }

          // No gaps
          "render page with text 'You cannot increase your State Pension forecast. £150.65 a week is the most you can get.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__cant_pay_gaps__p1']",
              "You cannot increase your State Pension forecast." +
                " £150.65 a week is the most you can get."
            )
          }

          "render page with text 'This means you are unable to pay for gaps in your National Insurance record online.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__cant_pay_gaps__p2']",
              "This means you are unable to pay for gaps in your National Insurance record online."
            )
          }

          // SPA under consideration message
          "render page with Heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertEqualsText(
              abroadUserDoc,
              "[data-spec='state_pension_age_under_consideration__h2_1']",
              "Proposed change to your State Pension age"
            )
          }

          "render page with text 'You’ll reach State Pension age on 7 June 2022. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertEqualsText(
              abroadUserDoc,
              "[data-spec='state_pension_age_under_consideration__p1']",
              "You’ll reach State Pension age on 7 June 2022. Under government proposals this may increase by up to a year."
            )
          }
        }

      }

      "The scenario is continue working || Fill Gaps" when {

        "State Pension view with MQP :  Personal Max" should {
          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any())(any()))
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
                0,
                pensionSharingOrder = false,
                currentFullWeeklyPensionAmount = 149.65,
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
                  NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false)
                ),
                reducedRateElection = false
              )
              ))))

          }

          lazy val doc =
            asDocument(contentAsString(statePensionController.show()(generateFakeRequest)))

          "render page with text 'You do not qualify for State Pension'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__do_not_qualify']",
              "You do not qualify for State Pension."
            )
          }

          // MQP message
          "render page with text 'You have no qualifying years on your National Insurance record." +
            " You usually need at least 10 qualifying years to get any State Pension.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__no_qualifying_years']",
              "You have no qualifying years on your National Insurance record." +
                " You usually need at least 10 qualifying years to get any State Pension."
            )
          }

          // Overseas message
          "render page with text and link 'If you’ve lived or worked outside the UK...'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension_outside_uk__p1']",
              "If you’ve lived or worked outside the UK, you may be able to use your time outside the UK " +
                "to make up the 10 qualifying years you need to get any UK State Pension. " +
                "Find out more about living or working outside the UK."
            )

            assertLinkHasValue(
              doc,
              "[data-spec='state_pension_outside_uk__link']",
              "https://www.gov.uk/new-state-pension/living-and-working-overseas"
            )
          }

          // MQP bar charts
          "render page with forecast chart title 'Forecast if you contribute until '" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='fill_gaps_MQP__chart1-key']",
              "Forecast if you contribute National Insurance until 5 April 2020"
            )
          }

          "render page with forecast chart text '£148.71 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='fill_gaps_MQP__chart1-value']",
              "£148.71 a week"
            )
          }

          "render page with personal max chart title 'Forecast if you contribute until'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='fill_gaps_MQP__chart2-key']",
              "The most you can increase your forecast to is"
            )
          }

          "render page with personal max chart text '£149.71 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='fill_gaps_MQP__chart2-value']",
              "£149.71 a week"
            )
          }

          "Not render page with current chart 'Current estimate based on your National Insurance record up to'" in {
            mockSetup
            assertPageDoesNotContainMessage(
              doc,
              "nisp.main.chart.current"
            )

          }

          // Fill gaps
          "render page with text 'You have 1 gap in your National Insurance record that you may be able to fill to increase your State Pension'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__gaps_to_fill_MQP__p1_single']",
              "You have 1 gap in your National Insurance record that you may be able to fill to increase your State Pension."
            )
          }

          "render page with text 'You can view your National Insurance record to check for gaps that you may be able to fill to increase your State Pension.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__view_gaps__p1']",
              "You can view your National Insurance record to check for gaps that you may be able to fill to increase your State Pension."
            )
          }

          // SPA not under consideration
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

        }

        "State Pension view with MQP : Personal Max: With State Pension age under consideration message" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any())(any()))
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
                1,
                pensionSharingOrder = false,
                currentFullWeeklyPensionAmount = 149.65,
                reducedRateElection = false,
                statePensionAgeUnderConsideration = true
              )
              ))))

            when(mockNationalInsuranceService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 1,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 2,
                numberOfGapsPayable = 2,
                Some(LocalDate.of(1954, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false)
                ),
                reducedRateElection = false
              )
              ))))
          }

          lazy val doc =
            asDocument(contentAsString(statePensionController.show()(generateFakeRequest)))
          lazy val abroadUserDoc =
            asDocument(contentAsString(abroadUserController.show()(generateFakeRequest)))

          // MQP message
          "render page with text 'You have 1 qualifying year on your National Insurance record." +
            " You usually need at least 10 qualifying years to get any State Pension.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__current_qualifying_years__single']",
              "You have 1 qualifying year on your National Insurance record. " +
                "You usually need at least 10 qualifying years to get any State Pension."
            )
          }

          // Overseas message
          "render page with text and link 'If you’ve lived or worked outside the UK...'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension_outside_uk__p1']",
              "If you’ve lived or worked outside the UK, you may be able to use your time outside the UK " +
                "to make up the 10 qualifying years you need to get any UK State Pension. " +
                "Find out more about living or working outside the UK."
            )

            assertLinkHasValue(
              doc,
              "[data-spec='state_pension_outside_uk__link']",
              "https://www.gov.uk/new-state-pension/living-and-working-overseas"
            )
          }

          // MQP bar charts
          "render page with forecast chart title 'Forecast if you contribute until'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='fill_gaps_MQP__chart1-key']",
              "Forecast if you contribute National Insurance until 5 April 2020"
            )
          }

          "render page with forecast chart text '£148.71 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='fill_gaps_MQP__chart1-value']",
              "£148.71 a week"
            )
          }

          "render page with personal max chart title 'Forecast if you contribute until'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='fill_gaps_MQP__chart2-key']",
              "The most you can increase your forecast to is"
            )
          }

          "render page with personal max chart text '£149.71 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='fill_gaps_MQP__chart2-value']",
              "£149.71 a week"
            )
          }

          "Not render page with current chart 'Current estimate based on your National Insurance record up to'" in {
            mockSetup
            assertPageDoesNotContainMessage(
              doc,
              "nisp.main.chart.current"
            )

          }

          // Fill gaps
          "render page with text 'You have 2 gaps in your National Insurance record that you may be able to fill to increase your State Pension'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__gaps_to_fill_MQP__p1_plural']",
              "You have 2 gaps in your National Insurance record that you may be able to fill to increase your State Pension."
            )
          }

          // SPA under consideration message
          "render page with Heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertEqualsText(
              abroadUserDoc,
              "[data-spec='state_pension_age_under_consideration__h2_1']",
              "Proposed change to your State Pension age"
            )
          }

          "render page with text 'You’ll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertEqualsText(
              abroadUserDoc,
              "[data-spec='state_pension_age_under_consideration__p1']",
              "You’ll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year."
            )
          }


        }

      }
    }

    "the user is a NON-MQP" when {

      "The scenario is continue working  || Fill Gaps" when {

        "State Pension view with NON-MQP :  Personal Max" should {
          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any())(any()))
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

            when(mockNationalInsuranceService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 11,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 2,
                numberOfGapsPayable = 2,
                Some(LocalDate.of(1954, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false)
                ),
                reducedRateElection = false
              )
              ))))

          }

          lazy val doc =
            asDocument(contentAsString(statePensionController.show()(generateFakeRequest)))

          // static content
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

          "render page with Page Heading 'Your State Pension summary'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__pageheading']",
              "Your State Pension summary"
            )
          }

          "render page with heading 'When will I reach State Pension age?'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__h2_1']",
              "When will I reach State Pension age?"
            )
          }

          "render page with text 'You will reach State Pension age on 7 June 2020.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__p1']",
              "You will reach State Pension age on 7 June 2020."
            )
          }

          "render page with text 'Your State Pension age and forecast are not a guarantee and are based on the current law." +
            " Your State Pension age may change in the future.' " in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__p2']",
              "Your State Pension age and forecast are not a guarantee and are based on the current law. Your State Pension age may change in the future."
            )
          }

          "render page with text 'Your forecast:'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__p_caveats']",
              "Your forecast:"
            )
          }

          "render page with bullet text 'is based on your National Insurance record up to 5th April 2016'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__ul__caveats__li1']",
              "is based on your National Insurance record up to 5 April 2016"
            )
          }

          "render page with bullet text 'assumes that you’ll contribute another 4 years'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__ul__caveats__li2__plural']",
              "assumes that you’ll contribute another 4 years"
            )
          }

          "render page with bullet text 'does not include any increase due to inflation'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__ul__caveats_inflation__li3']",
              "does not include any increase due to inflation"
            )
          }

          "render page with warning text" in {
            mockSetup
            assertEqualsText(
              doc,
              "#state_pension__warning_text",
              "! Warning Your State Pension forecast is for your information only. This service does not offer financial advice. " +
                "When planning for your retirement, you should seek guidance or financial advice."
            )

            assertLinkHasValue(
              doc,
              "[data-spec='state_pension__warning__link']",
              "/seek-financial-advice"
            )
          }

          "render page with heading 'How much will I get?'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__h2_2']",
              "How much will I get?"
            )
          }

          "render page with text 'The full new State Pension is £149.65 a week.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='new_state_pension__p1']",
              "The full new State Pension is £149.65 a week."
            )
          }

          "render page with href text 'Your forecast may change if there are any updates to your National Insurance information." +
            " You can find out more in the terms and conditions.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__legal__forecast_changes__p']",
              "Your forecast may change if there are any updates to your National Insurance information. You can find out more in the terms and conditions."
            )
          }

          "render page with href link 'Your forecast may be different if there are any changes to your National Insurance information. " +
            "There is more about this in the terms and conditions -terms and condition'" in {
            mockSetup
            assertLinkHasValue(
              doc,
              "[data-spec='state_pension__legal__forecast_changes__link']",
              "/check-your-state-pension/terms-and-conditions?showBackLink=true"
            )
          }

          "render page with heading 'How can I increase my State Pension?'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__h2_3']",
              "How can I increase my State Pension?"
            )
          }

          "render page with button 'View your National Insurance Record'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__showyourrecord']",
              "View your National Insurance record"
            )

            assertLinkHasValue(
              doc,
              "[data-spec='state_pension__showyourrecord']",
              "/check-your-state-pension/account/nirecord"
            )
          }

          "render page with link 'Find out about National Insurance.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__mqp__find_out_ni']",
              "Find out about National Insurance."
            )

            assertLinkHasValue(
              doc,
              "[data-spec='state_pension__mqp__find_out_ni']",
              "https://www.gov.uk/national-insurance"
            )
          }

          "render page with link 'Find out how much National Insurance you pay.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__mqp__find_out_ni_you_pay']",
              "Find out how much National Insurance you pay."
            )

            assertLinkHasValue(
              doc,
              "[data-spec='state_pension__mqp__find_out_ni_you_pay']",
              "https://www.gov.uk/national-insurance/how-much-you-pay"
            )
          }

          "render page with link 'Find out about getting National Insurance credits.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__mqp__find_out_ni_credits']",
              "Find out about getting National Insurance credits."
            )

            assertLinkHasValue(
              doc,
              "[data-spec='state_pension__mqp__find_out_ni_credits']",
              "https://www.gov.uk/national-insurance-credits"
            )
          }

          "render page with section 'Other ways to increase my income'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__h3_1']",
              "Other ways to increase my income"
            )
          }

          "render page with 'Home Responsibilities Protection' paragraph with link" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension_other_ways__p1']",
              "If you claimed Child Benefit between 6 April 1978 and 5 April 2010, you may be able to claim " +
                "Home Responsibilities Protection to fill gaps in your National Insurance record. Check if you are eligible to claim Home Responsibilities Protection."
            )

            assertLinkHasValue(
              doc,
              "[data-spec='state_pension__home_responsibilities_protection__link']",
              "https://www.gov.uk/home-responsibilities-protection-hrp"
            )
          }

          "render page with 'Pension Credit' paragraph with link" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension_other_ways__p2']",
              "You may be able to claim Pension Credit if you’re on a low income. " +
                "Find out about claiming Pension Credit."
            )

            assertLinkHasValue(
              doc,
              "[data-spec='state_pension__pension_credit__link']",
              "https://www.gov.uk/pension-credit/overview"
            )
          }

          "render page with 'Delaying your State Pension' paragraph with link" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__deferral__p1']",
              "You can delay claiming your State Pension. " +
                "This means you may get extra State Pension when you do claim it. " +
                "Find out about delaying your State Pension."

            )

            assertLinkHasValue(
              doc,
              "[data-spec='state_pension__deferral__link']",
              "https://www.gov.uk/deferring-state-pension"
            )
          }

          "render page with State Pension footer" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__footer_h2']",
              "Get help"
            )

            assertEqualsText(
              doc,
              "[data-spec='state_pension__footer_p1']",
              "For more information on gaps in your National Insurance record contact the Future Pension Centre."
            )

            assertLinkHasValue(
              doc,
              "[data-spec='state_pension__footer_link']",
              "https://www.gov.uk/future-pension-centre"
            )
          }

          "render page with Print Link" in {
            mockSetup
            doc.getElementById("printLink") shouldNot be(null)
            assertEqualsText(
              doc,
              "[data-spec='state_pension__printlink']",
              "Print your State Pension summary"
            )
            assertLinkHasValue(
              doc,
              "[data-spec='state_pension__printlink']",
              "#"
            )
          }

          // Bar charts - Fill gaps
          "render page with current chart title 'Current estimate based on your National Insurance record up to '" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='fill_gaps_current__chart1-key']",
              "Estimate based on your National Insurance record up to 5 April 2016"
            )
          }

          "render page with current chart text '£149.71 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='fill_gaps_current__chart1-value']",
              "£149.71 a week"
            )
          }

          "render page with forecast chart title 'Forecast if you contribute until '" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='fill_gaps_forecast__chart2-key']",
              "Forecast if you contribute National Insurance until 5 April 2020"
            )
          }

          "render page with forecast chart text '£148.71 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='fill_gaps_forecast__chart2-value']",
              "£148.71 a week"
            )
          }

          "render page with text 'You have 2 gaps in your National Insurance record that you may be able to fill to increase your State Pension'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__gaps_to_fill__p1_plural']",
              "You have 2 gaps in your National Insurance record that you may be able to fill to increase your State Pension."
            )
          }

          "render page with personal max chart title 'Forecast if you contribute until'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='fill_gaps_personal_max__chart3-key']",
              "The most you can increase your forecast to is"
            )
          }

          "render page with personal max chart text '£149.71 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='fill_gaps_personal_max__chart3-value']",
              "£149.71 a week"
            )
          }

          // Fill gaps
          "render page with text 'You can view your National Insurance record to check for gaps that you may be able to fill to increase your State Pension.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__view_gaps__p1']",
              "You can view your National Insurance record to check for gaps that you may be able to fill to increase your State Pension."
            )
          }

          // SPA not under consideration
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
        }

        "State Pension view with NON-MQP : Personal Max: With State Pension age under consideration message" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any())(any()))
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

            when(mockNationalInsuranceService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 11,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 2,
                numberOfGapsPayable = 2,
                Some(LocalDate.of(1954, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false)
                ),
                reducedRateElection = false
              )
              ))))
          }

          lazy val doc =
            asDocument(contentAsString(statePensionController.show()(generateFakeRequest)))
          lazy val abroadUserDoc =
            asDocument(contentAsString(abroadUserController.show()(generateFakeRequest)))

          // Bar charts - Fill gaps
          "render page with current chart title 'Current estimate based on your National Insurance record up to '" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='fill_gaps_current__chart1-key']",
              "Estimate based on your National Insurance record up to 5 April 2016"
            )
          }

          "render page with current chart text '£149.71 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='fill_gaps_current__chart1-value']",
              "£149.71 a week"
            )
          }

          "render page with forecast chart title 'Forecast if you contribute until '" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='fill_gaps_forecast__chart2-key']",
              "Forecast if you contribute National Insurance until 5 April 2020"
            )
          }

          "render page with forecast chart text '£148.71 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='fill_gaps_forecast__chart2-value']",
              "£148.71 a week"
            )
          }

          "render page with text 'You have 2 gaps in your National Insurance record that you may be able to fill to increase your State Pension'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__gaps_to_fill__p1_plural']",
              "You have 2 gaps in your National Insurance record that you may be able to fill to increase your State Pension."
            )
          }

          "render page with personal max chart title 'Forecast if you contribute until'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='fill_gaps_personal_max__chart3-key']",
              "The most you can increase your forecast to is"
            )
          }

          "render page with personal max chart text '£149.71 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='fill_gaps_personal_max__chart3-value']",
              "£149.71 a week"
            )
          }

          // Fill gaps
          "render page with text 'You can view your National Insurance record to check for gaps that you may be able to fill to increase your State Pension.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__view_gaps__p1']",
              "You can view your National Insurance record to check for gaps that you may be able to fill to increase your State Pension."
            )
          }

          // SPA under consideration message
          "render page with Heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertEqualsText(
              abroadUserDoc,
              "[data-spec='state_pension_age_under_consideration__h2_1']",
              "Proposed change to your State Pension age"
            )
          }

          "render page with text 'You’ll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertEqualsText(
              abroadUserDoc,
              "[data-spec='state_pension_age_under_consideration__p1']",
              "You’ll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year."
            )
          }

        }

        "State Pension view with NON-MQP : Full Rate current more than 155.65" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any())(any()))
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

            when(mockNationalInsuranceService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 11,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 2,
                numberOfGapsPayable = 2,
                Some(LocalDate.of(1954, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false)
                ),
                reducedRateElection = false
              )
              ))))
          }

          lazy val doc =
            asDocument(contentAsString(statePensionController.show()(generateFakeRequest)))

          // Bar charts - Fill gaps
          "render page with current chart title 'Current estimate based on your National Insurance record up to '" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='fill_gaps_current__chart1-key']",
              "Estimate based on your National Insurance record up to 5 April 2016"
            )
          }

          "render page with current chart text '£162.34 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='fill_gaps_current__chart1-value']",
              "£162.34 a week"
            )
          }

          "render page with forecast chart title 'Forecast if you contribute until '" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='fill_gaps_forecast__chart2-key']",
              "Forecast if you contribute National Insurance until 5 April 2020"
            )
          }

          "render page with forecast chart text '£168.08 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='fill_gaps_forecast__chart2-value']",
              "£168.08 a week"
            )
          }

          "render page with text 'You have 2 gaps in your National Insurance record that you may be able to fill to increase your State Pension'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__gaps_to_fill__p1_plural']",
              "You have 2 gaps in your National Insurance record that you may be able to fill to increase your State Pension."
            )
          }

          "render page with personal max chart title 'Forecast if you contribute until'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='fill_gaps_personal_max__chart3-key']",
              "The most you can increase your forecast to is"
            )
          }

          "render page with personal max chart text '£172.71 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='fill_gaps_personal_max__chart3-value']",
              "£172.71 a week"
            )
          }

          // Fill gaps
          "render page with text 'You can view your National Insurance record to check for gaps that you may be able to fill to increase your State Pension.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__view_gaps__p1']",
              "You can view your National Insurance record to check for gaps that you may be able to fill to increase your State Pension."
            )
          }

          // SPA not under consideration
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

        }

        "State Pension view with NON-MQP : Full Rate current more than 155.65: With State Pension age under consideration message" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any())(any()))
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

            when(mockNationalInsuranceService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 11,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 2,
                numberOfGapsPayable = 2,
                Some(LocalDate.of(1954, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false)
                ),
                reducedRateElection = false
              )
              ))))
          }

          lazy val doc =
            asDocument(contentAsString(statePensionController.show()(generateFakeRequest)))

          lazy val abroadUserDoc =
            asDocument(contentAsString(abroadUserController.show()(generateFakeRequest)))

          // Bar charts - Fill gaps
          "render page with current chart title 'Current estimate based on your National Insurance record up to '" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='fill_gaps_current__chart1-key']",
              "Estimate based on your National Insurance record up to 5 April 2016"
            )
          }

          "render page with current chart text '£162.34 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='fill_gaps_current__chart1-value']",
              "£162.34 a week"
            )
          }

          "render page with forecast chart title 'Forecast if you contribute until '" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='fill_gaps_forecast__chart2-key']",
              "Forecast if you contribute National Insurance until 5 April 2020"
            )
          }

          "render page with forecast chart text '£168.08 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='fill_gaps_forecast__chart2-value']",
              "£168.08 a week"
            )
          }

          "render page with text 'You have 2 gaps in your National Insurance record that you may be able to fill to increase your State Pension'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__gaps_to_fill__p1_plural']",
              "You have 2 gaps in your National Insurance record that you may be able to fill to increase your State Pension."
            )
          }

          "render page with personal max chart title 'Forecast if you contribute until'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='fill_gaps_personal_max__chart3-key']",
              "The most you can increase your forecast to is"
            )
          }

          "render page with personal max chart text '£172.71 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='fill_gaps_personal_max__chart3-value']",
              "£172.71 a week"
            )
          }

          // Fill gaps
          "render page with text 'You can view your National Insurance record to check for gaps that you may be able to fill to increase your State Pension.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__view_gaps__p1']",
              "You can view your National Insurance record to check for gaps that you may be able to fill to increase your State Pension."
            )
          }

          // SPA under consideration message
          "render page with Heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertEqualsText(
              abroadUserDoc,
              "[data-spec='state_pension_age_under_consideration__h2_1']",
              "Proposed change to your State Pension age"
            )
          }

          "render page with text 'You’ll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertEqualsText(
              abroadUserDoc,
              "[data-spec='state_pension_age_under_consideration__p1']",
              "You’ll reach State Pension age on 7 June 2020. Under government proposals this may increase by up to a year."
            )
          }

        }

        "State Pension view with NON-MQP :  Full Rate will reach full rate by filling gaps" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any())(any()))
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

            when(mockNationalInsuranceService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 11,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 2,
                numberOfGapsPayable = 2,
                Some(LocalDate.of(1989, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false)
                ),
                reducedRateElection = false
              )
              ))))
          }

          lazy val doc =
            asDocument(contentAsString(statePensionController.show()(generateFakeRequest)))

          // Bar charts - Fill gaps
          "render page with current chart title 'Current estimate based on your National Insurance record up to '" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='fill_gaps_current__chart1-key']",
              "Estimate based on your National Insurance record up to 5 April 2016"
            )
          }

          "render page with current chart text '£133.71 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='fill_gaps_current__chart1-value']",
              "£133.71 a week"
            )
          }

          "render page with forecast chart title 'Forecast if you contribute until '" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='fill_gaps_forecast__chart2-key']",
              "Forecast if you contribute National Insurance until 5 April 2020"
            )
          }

          "render page with forecast chart text '£148.71 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='fill_gaps_forecast__chart2-value']",
              "£148.71 a week"
            )
          }

          "render page with text 'You have 2 gaps in your National Insurance record that you may be able to fill to increase your State Pension'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__gaps_to_fill__p1_plural']",
              "You have 2 gaps in your National Insurance record that you may be able to fill to increase your State Pension."
            )
          }

          "render page with personal max chart title 'Forecast if you contribute until'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='fill_gaps_personal_max__chart3-key']",
              "The most you can increase your forecast to is"
            )
          }

          "render page with personal max chart text '£149.71 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='fill_gaps_personal_max__chart3-value']",
              "£149.71 a week"
            )
          }

          // Fill gaps
          "render page with text 'You can view your National Insurance record to check for gaps that you may be able to fill to increase your State Pension.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__view_gaps__p1']",
              "You can view your National Insurance record to check for gaps that you may be able to fill to increase your State Pension."
            )
          }

          // SPA not under consideration
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

        }

        "State Pension view with NON-MQP :  Full Rate will reach full rate by filling gaps: With State Pension age under consideration message" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any())(any()))
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

            when(mockNationalInsuranceService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 11,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 2,
                numberOfGapsPayable = 2,
                Some(LocalDate.of(1989, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false)
                ),
                reducedRateElection = false
              )
              ))))
          }

          lazy val doc =
            asDocument(contentAsString(statePensionController.show()(generateFakeRequest)))

          lazy val abroadUserDoc =
            asDocument(contentAsString(abroadUserController.show()(generateFakeRequest)))

          // Bar charts - Fill gaps
          "render page with current chart title 'Current estimate based on your National Insurance record up to '" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='fill_gaps_current__chart1-key']",
              "Estimate based on your National Insurance record up to 5 April 2016"
            )
          }

          "render page with current chart text '£133.71 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='fill_gaps_current__chart1-value']",
              "£133.71 a week"
            )
          }

          "render page with forecast chart title 'Forecast if you contribute until '" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='fill_gaps_forecast__chart2-key']",
              "Forecast if you contribute National Insurance until 5 April 2020"
            )
          }

          "render page with forecast chart text '£148.71 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='fill_gaps_forecast__chart2-value']",
              "£148.71 a week"
            )
          }

          "render page with text 'You have 2 gaps in your National Insurance record that you may be able to fill to increase your State Pension'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__gaps_to_fill__p1_plural']",
              "You have 2 gaps in your National Insurance record that you may be able to fill to increase your State Pension."
            )
          }

          "render page with personal max chart title 'Forecast if you contribute until'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='fill_gaps_personal_max__chart3-key']",
              "The most you can increase your forecast to is"
            )
          }

          "render page with personal max chart text '£149.71 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='fill_gaps_personal_max__chart3-value']",
              "£149.71 a week"
            )
          }

          // Fill gaps
          "render page with text 'You can view your National Insurance record to check for gaps that you may be able to fill to increase your State Pension.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__view_gaps__p1']",
              "You can view your National Insurance record to check for gaps that you may be able to fill to increase your State Pension."
            )
          }

          // SPA under consideration message
          "render page with Heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertEqualsText(
              abroadUserDoc,
              "[data-spec='state_pension_age_under_consideration__h2_1']",
              "Proposed change to your State Pension age"
            )
          }

          "render page with text 'You’ll reach State Pension age on 7 June 2017. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertEqualsText(
              abroadUserDoc,
              "[data-spec='state_pension_age_under_consideration__p1']",
              "You’ll reach State Pension age on 7 June 2017. Under government proposals this may increase by up to a year."
            )
          }

        }
      }

      "The scenario is continue working || No Gaps/No need to fill gaps" when {

        "State Pension view with NON-MQP : No Gaps || Full Rate & Personal Max" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any())(any()))
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

            when(mockNationalInsuranceService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 11,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 0,
                numberOfGapsPayable = 0,
                Some(LocalDate.of(1989, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false)
                ),
                reducedRateElection = false
              )
              ))))

          }

          lazy val doc =
            asDocument(contentAsString(statePensionController.show()(generateFakeRequest)))


          // Bar charts - no gaps
          "render page with current chart title 'Current estimate based on your National Insurance record up to '" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='continue_working_current__chart1-key']",
              "Estimate based on your National Insurance record up to 5 April 2016"
            )
          }

          "render page with current chart text '£150.65 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='continue_working_forecast__chart2-value']",
              "£150.65 a week"
            )
          }

          "render page with forecast chart title 'Forecast if you contribute until '" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='continue_working_forecast__chart2-key']",
              "Forecast if you contribute National Insurance until 5 April 2022"
            )
          }

          "render page with forecast chart text '£150.65 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='continue_working_forecast__chart2-value']",
              "£150.65 a week"
            )
          }

          "render page with personal max chart title 'Forecast if you contribute until'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='continue_working_personal_max__chart3-key']",
              "The most you can increase your forecast to is"
            )
          }

          "render page with personal max chart text '£150.65 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='continue_working_personal_max__chart3-value']",
              "£150.65 a week"
            )
          }

          // No gaps
          "render page with text 'You cannot increase your State Pension forecast. £150.65 a week is the most you can get.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__cant_pay_gaps__p1']",
              "You cannot increase your State Pension forecast." +
                " £150.65 a week is the most you can get."
            )
          }

          "render page with text 'This means you are unable to pay for gaps in your National Insurance record online.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__cant_pay_gaps__p2']",
              "This means you are unable to pay for gaps in your National Insurance record online."
            )
          }

          "Not render page with text 'You can view your National Insurance record to check for gaps that you may be able to fill to increase your State Pension.'" in {
            mockSetup
            assertPageDoesNotContainMessage(
              doc,
              "nisp.main.context.fillGaps.viewGaps"
            )
          }

          // No SPA under consideration
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

        }

        "State Pension view with NON-MQP : No Gaps || Full Rate & Personal Max: With State Pension age under consideration message" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any())(any()))
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

            when(mockNationalInsuranceService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 11,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 0,
                numberOfGapsPayable = 0,
                Some(LocalDate.of(1989, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false)
                ),
                reducedRateElection = false
              )
              ))))
          }

          lazy val doc =
            asDocument(contentAsString(statePensionController.show()(generateFakeRequest)))

          // Bar charts - no gaps
          "render page with current chart title 'Current estimate based on your National Insurance record up to '" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='continue_working_current__chart1-key']",
              "Estimate based on your National Insurance record up to 5 April 2016"
            )
          }

          "render page with current chart text '£150.65 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='continue_working_forecast__chart2-value']",
              "£150.65 a week"
            )
          }

          "render page with forecast chart title 'Forecast if you contribute until '" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='continue_working_forecast__chart2-key']",
              "Forecast if you contribute National Insurance until 5 April 2022"
            )
          }

          "render page with forecast chart text '£150.65 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='continue_working_forecast__chart2-value']",
              "£150.65 a week"
            )
          }

          "render page with personal max chart title 'Forecast if you contribute until'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='continue_working_personal_max__chart3-key']",
              "The most you can increase your forecast to is"
            )
          }

          "render page with personal max chart text '£150.65 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='continue_working_personal_max__chart3-value']",
              "£150.65 a week"
            )
          }

          // No gaps
          "render page with text 'You cannot increase your State Pension forecast. £150.65 a week is the most you can get.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__cant_pay_gaps__p1']",
              "You cannot increase your State Pension forecast." +
                " £150.65 a week is the most you can get."
            )
          }

          "render page with text 'This means you are unable to pay for gaps in your National Insurance record online.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__cant_pay_gaps__p2']",
              "This means you are unable to pay for gaps in your National Insurance record online."
            )
          }

          "Not render page with text 'You can view your National Insurance record to check for gaps that you may be able to fill to increase your State Pension.'" in {
            mockSetup
            assertPageDoesNotContainMessage(
              doc,
              "nisp.main.context.fillGaps.viewGaps"
            )
          }

          // SPA under consideration
          "render page with Heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension_age_under_consideration__h2_1']",
              "Proposed change to your State Pension age"
            )
          }

          "render page with text 'You’ll reach State Pension age on 7 June 2022. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension_age_under_consideration__p1']",
              "You’ll reach State Pension age on 7 June 2022. Under government proposals this may increase by up to a year."
            )
          }

        }

        "State Pension view with NON-MQP : No need to fill gaps || Full Rate and Personal Max: when some one has more years left" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any())(any()))
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

            when(mockNationalInsuranceService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 11,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 2,
                numberOfGapsPayable = 2,
                Some(LocalDate.of(1989, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false)
                ),
                reducedRateElection = false
              )
              ))))
          }

          lazy val doc =
            asDocument(contentAsString(statePensionController.show()(generateFakeRequest)))

          // Bar charts - no gaps
          "render page with current chart title 'Current estimate based on your National Insurance record up to '" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='continue_working_current__chart1-key']",
              "Estimate based on your National Insurance record up to 5 April 2016"
            )
          }

          "render page with current chart text '£155.65 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='continue_working_forecast__chart2-value']",
              "£155.65 a week"
            )
          }

          "render page with forecast chart title 'Forecast if you contribute until '" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='continue_working_forecast__chart2-key']",
              "Forecast if you contribute National Insurance until 5 April 2020"
            )
          }

          "render page with forecast chart text '£155.65 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='continue_working_forecast__chart2-value']",
              "£155.65 a week"
            )
          }

          "render page with personal max chart title 'Forecast if you contribute until'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='continue_working_personal_max__chart3-key']",
              "The most you can increase your forecast to is"
            )
          }

          "render page with personal max chart text '£155.65 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='continue_working_personal_max__chart3-value']",
              "£155.65 a week"
            )
          }

          // No gaps
          "render page with text 'You cannot increase your State Pension forecast. £155.65 a week is the most you can get.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__cant_pay_gaps__p1']",
              "You cannot increase your State Pension forecast." +
                " £155.65 a week is the most you can get."
            )
          }

          "render page with text 'This means you are unable to pay for gaps in your National Insurance record online.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__cant_pay_gaps__p2']",
              "This means you are unable to pay for gaps in your National Insurance record online."
            )
          }

          "Not render page with text 'You can view your National Insurance record to check for gaps that you may be able to fill to increase your State Pension.'" in {
            mockSetup
            assertPageDoesNotContainMessage(
              doc,
              "nisp.main.context.fillGaps.viewGaps"
            )
          }

          // No SPA under consideration
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

        }

        "State Pension view with NON-MQP : No need to fill gaps || Full Rate and Personal Max: when some one has more years left: With State Pension age under consideration message" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any())(any()))
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

            when(mockNationalInsuranceService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
                qualifyingYears = 11,
                qualifyingYearsPriorTo1975 = 0,
                numberOfGaps = 2,
                numberOfGapsPayable = 2,
                Some(LocalDate.of(1989, 3, 6)),
                homeResponsibilitiesProtection = false,
                LocalDate.of(2017, 4, 5),
                List(

                  NationalInsuranceTaxYearBuilder("2015-16", underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                  NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false)
                ),
                reducedRateElection = false
              )
              ))))

          }

          lazy val doc =
            asDocument(contentAsString(statePensionController.show()(generateFakeRequest)))

          // Bar charts - no gaps
          "render page with current chart title 'Current estimate based on your National Insurance record up to '" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='continue_working_current__chart1-key']",
              "Estimate based on your National Insurance record up to 5 April 2016"
            )
          }

          "render page with current chart text '£155.65 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='continue_working_forecast__chart2-value']",
              "£155.65 a week"
            )
          }

          "render page with forecast chart title 'Forecast if you contribute until '" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='continue_working_forecast__chart2-key']",
              "Forecast if you contribute National Insurance until 5 April 2020"
            )
          }

          "render page with forecast chart text '£155.65 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='continue_working_forecast__chart2-value']",
              "£155.65 a week"
            )
          }

          "render page with personal max chart title 'Forecast if you contribute until'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dt[data-spec='continue_working_personal_max__chart3-key']",
              "The most you can increase your forecast to is"
            )
          }

          "render page with personal max chart text '£155.65 a week'" in {
            mockSetup
            assertEqualsText(
              doc,
              "dd[data-spec='continue_working_personal_max__chart3-value']",
              "£155.65 a week"
            )
          }

          // No gaps
          "render page with text 'You cannot increase your State Pension forecast. £155.65 a week is the most you can get.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__cant_pay_gaps__p1']",
              "You cannot increase your State Pension forecast." +
                " £155.65 a week is the most you can get."
            )
          }

          "render page with text 'This means you are unable to pay for gaps in your National Insurance record online.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__cant_pay_gaps__p2']",
              "This means you are unable to pay for gaps in your National Insurance record online."
            )
          }

          "Not render page with text 'You can view your National Insurance record to check for gaps that you may be able to fill to increase your State Pension.'" in {
            mockSetup
            assertPageDoesNotContainMessage(
              doc,
              "nisp.main.context.fillGaps.viewGaps"
            )
          }

          //  SPA under consideration
          "render page with Heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension_age_under_consideration__h2_1']",
              "Proposed change to your State Pension age"
            )
          }

          "render page with text 'You’ll reach State Pension age on 7 June 2017. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension_age_under_consideration__p1']",
              "You’ll reach State Pension age on 7 June 2017. Under government proposals this may increase by up to a year."
            )
          }

        }
      }

      "State Pension view with NON-MQP : Reached || No Gaps || Full Rate and Personal Max" should {

        def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
          when(mockStatePensionService.getSummary(any())(any()))
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

          when(mockNationalInsuranceService.getSummary(any())(any()))
            .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
              qualifyingYears = 11,
              qualifyingYearsPriorTo1975 = 0,
              numberOfGaps = 0,
              numberOfGapsPayable = 0,
              Some(LocalDate.of(1989, 3, 6)),
              homeResponsibilitiesProtection = false,
              LocalDate.of(2017, 4, 5),
              List(

                NationalInsuranceTaxYearBuilder("2015-16", underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false)
              ),
              reducedRateElection = false
            )
            ))))
        }

        lazy val doc =
          asDocument(contentAsString(statePensionController.show()(generateFakeRequest)))

        // Bar charts - reached
        "render page with forecast chart title 'Forecast if you contribute until '" in {
          mockSetup
          assertEqualsText(
            doc,
            "dt[data-spec='reached__chart1-key']",
            "Forecast if you contribute National Insurance until 5 April 2020"
          )
        }

        "render page with forecast chart text '£155.65 a week'" in {
          mockSetup
          assertEqualsText(
            doc,
            "dd[data-spec='reached__chart1-value']",
            "£155.65 a week"
          )
        }

        "render page with personal max chart title 'Forecast if you contribute until'" in {
          mockSetup
          assertEqualsText(
            doc,
            "dt[data-spec='reached__chart2-key']",
            "The most you can increase your forecast to is"
          )
        }

        "render page with personal max chart text '£155.65 a week'" in {
          mockSetup
          assertEqualsText(
            doc,
            "dd[data-spec='reached__chart2-value']",
            "£155.65 a week"
          )
        }

        "Not render page with current chart 'Current estimate based on your National Insurance record up to'" in {
          mockSetup
          assertPageDoesNotContainMessage(
            doc,
            "nisp.main.chart.current"
          )

        }

        // No gaps
        "render page with text 'You cannot increase your State Pension forecast. £155.65 a week is the most you can get.'" in {
          mockSetup
          assertEqualsText(
            doc,
            "[data-spec='state_pension__cant_pay_gaps__p1']",
            "You cannot increase your State Pension forecast." +
              " £155.65 a week is the most you can get."
          )
        }

        "render page with text 'This means you are unable to pay for gaps in your National Insurance record online.'" in {
          mockSetup
          assertEqualsText(
            doc,
            "[data-spec='state_pension__cant_pay_gaps__p2']",
            "This means you are unable to pay for gaps in your National Insurance record online."
          )
        }

        "Not render page with text 'You can view your National Insurance record to check for gaps that you may be able to fill to increase your State Pension.'" in {
          mockSetup
          assertPageDoesNotContainMessage(
            doc,
            "nisp.main.context.fillGaps.viewGaps"
          )
        }

        // Additional State Pension
        "render page with 'Additional State Pension' paragraph with link" in {
          mockSetup
          assertEqualsText(
            doc,
            "[data-spec='state_pension__additional_state_pension__p1']",
            "You may get more than this if you have some Additional State Pension. Find out about Additional State Pension and protected payment.")

          assertLinkHasValue(
            doc,
            "[data-spec='state_pension__additional_state_pension__link']",
            "https://www.gov.uk/additional-state-pension"
          )
        }

        // No SPA under consideration
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

      }

      "State Pension view with NON-MQP : Reached || No Gaps || Full Rate and Personal Max: With State Pension age under consideration message" should {

        def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
          when(mockStatePensionService.getSummary(any())(any()))
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

          when(mockNationalInsuranceService.getSummary(any())(any()))
            .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
              qualifyingYears = 11,
              qualifyingYearsPriorTo1975 = 0,
              numberOfGaps = 0,
              numberOfGapsPayable = 0,
              Some(LocalDate.of(1989, 3, 6)),
              homeResponsibilitiesProtection = false,
              LocalDate.of(2017, 4, 5),
              List(

                NationalInsuranceTaxYearBuilder("2015-16", underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false)
              ),
              reducedRateElection = false
            )
            ))))
        }

        lazy val abroadUserDoc =
          asDocument(contentAsString(abroadUserController.show()(generateFakeRequest)))


        // Bar charts - reached
        "render page with forecast chart title 'Forecast if you contribute until '" in {
          mockSetup
          assertEqualsText(
            abroadUserDoc,
            "dt[data-spec='reached__chart1-key']",
            "Forecast if you contribute National Insurance until 5 April 2020"
          )
        }

        "render page with forecast chart text '£155.65 a week'" in {
          mockSetup
          assertEqualsText(
            abroadUserDoc,
            "dd[data-spec='reached__chart1-value']",
            "£155.65 a week"
          )
        }

        "render page with personal max chart title 'Forecast if you contribute until'" in {
          mockSetup
          assertEqualsText(
            abroadUserDoc,
            "dt[data-spec='reached__chart2-key']",
            "The most you can increase your forecast to is"
          )
        }

        "render page with personal max chart text '£155.65 a week'" in {
          mockSetup
          assertEqualsText(
            abroadUserDoc,
            "dd[data-spec='reached__chart2-value']",
            "£155.65 a week"
          )
        }

        "Not render page with current chart 'Current estimate based on your National Insurance record up to'" in {
          mockSetup
          assertPageDoesNotContainMessage(
            abroadUserDoc,
            "nisp.main.chart.current"
          )

        }

        // No gaps
        "render page with text 'You cannot increase your State Pension forecast. £155.65 a week is the most you can get.'" in {
          mockSetup
          assertEqualsText(
            abroadUserDoc,
            "[data-spec='state_pension__cant_pay_gaps__p1']",
            "You cannot increase your State Pension forecast." +
              " £155.65 a week is the most you can get."
          )
        }

        "render page with text 'This means you are unable to pay for gaps in your National Insurance record online.'" in {
          mockSetup
          assertEqualsText(
            abroadUserDoc,
            "[data-spec='state_pension__cant_pay_gaps__p2']",
            "This means you are unable to pay for gaps in your National Insurance record online."
          )
        }

        "Not render page with text 'You can view your National Insurance record to check for gaps that you may be able to fill to increase your State Pension.'" in {
          mockSetup
          assertPageDoesNotContainMessage(
            abroadUserDoc,
            "nisp.main.context.fillGaps.viewGaps"
          )
        }

        // Additional State Pension
        "render page with 'Additional State Pension' paragraph with link" in {
          mockSetup
          assertEqualsText(
            abroadUserDoc,
            "[data-spec='state_pension__additional_state_pension__p1']",
            "You may get more than this if you have some Additional State Pension. Find out about Additional State Pension and protected payment.")

          assertLinkHasValue(
            abroadUserDoc,
            "[data-spec='state_pension__additional_state_pension__link']",
            "https://www.gov.uk/additional-state-pension"
          )
        }

        // SPA under consideration
        "render page with Heading 'Proposed change to your State Pension age'" in {
          mockSetup
          assertEqualsText(
            abroadUserDoc,
            "[data-spec='state_pension_age_under_consideration__h2_1']",
            "Proposed change to your State Pension age"
          )
        }

        "render page with text 'You’ll reach State Pension age on 7 June 2017. Under government proposals this may increase by up to a year.'" in {
          mockSetup
          assertEqualsText(
            abroadUserDoc,
            "[data-spec='state_pension_age_under_consideration__p1']",
            "You’ll reach State Pension age on 7 June 2017. Under government proposals this may increase by up to a year."
          )
        }

      }

      "State Pension view with Contracted out User" should {
        val mockUserNino: Nino = TestAccountBuilder.regularNino
        implicit val user: NispAuthedUser =
          NispAuthedUser(mockUserNino, LocalDate.now(), UserName(Name(None, None)), None, None, isSa = false)
        val authDetails: AuthDetails = AuthDetails(ConfidenceLevel.L200, LoginTimes(Instant.now(), None))

        def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {

          when(mockStatePensionService.getSummary(any())(any()))
            .thenReturn(Future.successful(Right(Right(StatePension(
              LocalDate.of(2014, 4, 5),
              StatePensionAmounts(
                protectedPayment = false,
                StatePensionAmountRegular(46.38, 201.67, 2420.04),
                StatePensionAmountForecast(3, 155.55, 622.35, 76022.24),
                StatePensionAmountMaximum(3, 0, 155.55, 622.35, 76022.24),
                StatePensionAmountRegular(50, 217.41, 2608.93))
              , 64, LocalDate.of(2021, 7, 18), "2017-18", 30, pensionSharingOrder = false, 155.65, reducedRateElection = false, statePensionAgeUnderConsideration = false)
            ))))

          when(mockNationalInsuranceService.getSummary(any())(any()))
            .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
              qualifyingYears = 11,
              qualifyingYearsPriorTo1975 = 0,
              numberOfGaps = 2,
              numberOfGapsPayable = 2,
              Some(LocalDate.of(1954, 3, 6)),
              homeResponsibilitiesProtection = false,
              LocalDate.of(2017, 4, 5),
              List(

                NationalInsuranceTaxYearBuilder("2015-16", underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false)
              ),
              reducedRateElection = false
            )
            ))))

        }

        lazy val result = statePensionController.show()(AuthenticatedRequest(FakeRequest(), user, authDetails))
        lazy val doc = asDocument(contentAsString(result))

        "render with correct page title" in {
          mockSetup
          println(s"\n\n\n${doc.getElementById("which-view").text()}\n\n\n")

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

        // Contracted Out
        "render page with heading 'You’ve been in a contracted-out pension scheme'" in {
          mockSetup
          assertEqualsText(
            doc,
            "[data-spec='state_pension__contracted_out__h2']",
            "You’ve been in a contracted-out pension scheme"
          )
        }

        "render page with text 'Like most people, you were contracted out of the additional State Pension...'" in {
          mockSetup
          assertEqualsText(
            doc,
            "[data-spec='state_pension__contracted_out__p1']",
            "Like most people, you were contracted out of the additional State Pension. This means you paid less National Insurance into your State Pension. Your State Pension forecast takes this into account."
          )
        }

        "render page with link 'Find out more about being contracted out of the State Pension'" in {
          mockSetup
          assertEqualsText(
            doc,
            "[data-spec='state_pension__contracted_out__link_1']",
            "Find out more about being contracted out of the State Pension"
          )

          assertLinkHasValue(
            doc,
            "[data-spec='state_pension__contracted_out__link_1']",
            "https://www.gov.uk/contracted-out"
          )
        }

        "render page with link 'Find out more about the new State Pension'" in {
          mockSetup
          assertEqualsText(
            doc,
            "[data-spec='state_pension__contracted_out__link_2']",
            "Find out more about the new State Pension"
          )

        }

        // Additional State Pension
        "render page with 'Additional State Pension' paragraph with link" in {
          mockSetup
          assertEqualsText(
            doc,
            "[data-spec='state_pension__additional_state_pension__p1']",
            "You may get more than this if you have some Additional State Pension. Find out about Additional State Pension and protected payment.")

          assertLinkHasValue(
            doc,
            "[data-spec='state_pension__additional_state_pension__link']",
            "https://www.gov.uk/additional-state-pension"
          )
        }

      }

      "State Pension view with Pension Sharing Order" should {
        def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
          when(mockStatePensionService.getSummary(any())(any()))
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
              pensionSharingOrder = true,
              currentFullWeeklyPensionAmount = 149.65,
              reducedRateElection = false,
              statePensionAgeUnderConsideration = false
            )
            ))))

          when(mockNationalInsuranceService.getSummary(any())(any()))
            .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
              qualifyingYears = 11,
              qualifyingYearsPriorTo1975 = 0,
              numberOfGaps = 0,
              numberOfGapsPayable = 0,
              Some(LocalDate.of(1989, 3, 6)),
              homeResponsibilitiesProtection = false,
              LocalDate.of(2017, 4, 5),
              List(

                NationalInsuranceTaxYearBuilder("2015-16", underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
                NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false)
              ),
              reducedRateElection = false
            )
            ))))
        }

        lazy val doc =
          asDocument(contentAsString(statePensionController.show()(generateFakeRequest)))

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

        "render page with text 'Your forecast:'" in {
          mockSetup
          assertEqualsText(
            doc,
            "[data-spec='state_pension__p_caveats']",
            "Your forecast:"
          )
        }

        "render page with bullet text 'is based on your National Insurance record up to 5th April 2016'" in {
          mockSetup
          assertEqualsText(
            doc,
            "[data-spec='state_pension__ul__caveats__li1']",
            "is based on your National Insurance record up to 5 April 2016"
          )
        }

        "render page with bullet text 'assumes that you’ll contribute another 4 years'" in {
          mockSetup
          assertEqualsText(
            doc,
            "[data-spec='state_pension__ul__caveats__li2__plural']",
            "assumes that you’ll contribute another 4 years"
          )
        }

        "render page with bullet text 'does not include any increase due to inflation'" in {
          mockSetup
          assertEqualsText(
            doc,
            "[data-spec='state_pension__ul__caveats_inflation__li3']",
            "does not include any increase due to inflation"
          )
        }

        "render page with bullet text 'does not include the pension sharing order you have in effect'" in {
          mockSetup
          assertEqualsText(
            doc,
            "[data-spec='state_pension__ul__caveats__li4']",
            "does not include the pension sharing order you have in effect"
          )
        }
      }
    }
  }
}
