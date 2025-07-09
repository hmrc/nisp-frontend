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
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.http.{SessionKeys, UpstreamErrorResponse}
import uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlag
import uk.gov.hmrc.nisp.builders.NationalInsuranceTaxYearBuilder
import uk.gov.hmrc.nisp.controllers.StatePensionController
import uk.gov.hmrc.nisp.controllers.auth.{AuthRetrievals, PertaxAuthAction}
import uk.gov.hmrc.nisp.controllers.pertax.PertaxHelper
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.models.admin.NewStatePensionUIToggle
import uk.gov.hmrc.nisp.services.{NationalInsuranceService, StatePensionService}
import uk.gov.hmrc.nisp.views.HtmlSpec
import uk.gov.hmrc.play.language.LanguageUtils

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.Future

class StatePensionMQPViewSpec
  extends HtmlSpec
    with Injecting
    with WireMockSupport {

  val expectedMoneyServiceLink          = "https://www.moneyadviceservice.org.uk/en"
  val expectedPensionCreditOverviewLink = "https://www.gov.uk/pension-credit/overview"

  def generateFakeRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withSession(
      SessionKeys.sessionId            -> s"session-${UUID.randomUUID()}",
      SessionKeys.lastRequestTimestamp -> LocalDate.now.toEpochDay.toString
    )

  val mockNationalInsuranceService: NationalInsuranceService = mock[NationalInsuranceService]
  val mockStatePensionService: StatePensionService           = mock[StatePensionService]
  val mockPertaxHelper: PertaxHelper                         = mock[PertaxHelper]

  lazy val langUtils: LanguageUtils = inject[LanguageUtils]

  val standardInjector: Injector = GuiceApplicationBuilder()
    .overrides(
      bind[AuthRetrievals].to[FakeAuthAction],
      bind[StatePensionService].toInstance(mockStatePensionService),
      bind[NationalInsuranceService].toInstance(mockNationalInsuranceService),
      bind[PertaxHelper].toInstance(mockPertaxHelper),
      bind[PertaxAuthAction].to[FakePertaxAuthAction],
      featureFlagServiceBinding
    )
    .build()
    .injector

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockStatePensionService)
    reset(mockNationalInsuranceService)
    reset(mockPertaxHelper)
    reset(mockFeatureFlagService)

    when(mockPertaxHelper.isFromPertax(any()))
      .thenReturn(Future.successful(false))
    when(mockFeatureFlagService.get(NewStatePensionUIToggle))
      .thenReturn(Future.successful(FeatureFlag(NewStatePensionUIToggle, isEnabled = true)))
  }

  val taxYears: List[NationalInsuranceTaxYear] =
    List(
      NationalInsuranceTaxYearBuilder(
        taxYear            = "2015-16",
        underInvestigation = false
      ),
      NationalInsuranceTaxYearBuilder(
        taxYear            = "2014-15",
        qualifying         = false,
        underInvestigation = false
      ),
      NationalInsuranceTaxYearBuilder(
        taxYear            = "2013-14",
        underInvestigation = false
      )
    )

  def statePensionAmounts(
    maximum: StatePensionAmountMaximum = StatePensionAmountMaximum(
      yearsToWork   = 0,
      gapsToFill    = 0,
      weeklyAmount  = 0,
      monthlyAmount = 0,
      annualAmount  = 0
    )
  ): StatePensionAmounts = StatePensionAmounts(
    protectedPayment = false,
    current          = StatePensionAmountRegular(0, 0, 0),
    forecast         = StatePensionAmountForecast(0, 0, 0, 0),
    maximum          = maximum,
    cope             = StatePensionAmountRegular(0, 0, 0)
  )

  def statePension(
    pensionDate          : LocalDate           = LocalDate.of(2017, 5, 4),
    finalRelevantYear    : String              = "2016-17",
    ageUnderConsideration: Boolean             = false,
    amounts              : StatePensionAmounts = statePensionAmounts()
  ): StatePension = StatePension(
    earningsIncludedUpTo              = LocalDate.of(2016, 4, 5),
    pensionAge                        = 67,
    pensionDate                       = pensionDate,
    finalRelevantYear                 = finalRelevantYear,
    numberOfQualifyingYears           = 4,
    pensionSharingOrder               = false,
    currentFullWeeklyPensionAmount    = 155.65,
    reducedRateElection               = false,
    statePensionAgeUnderConsideration = ageUnderConsideration,
    amounts                           = amounts
  )

  def nationalInsuranceRecord(
    numberOfGaps       : Int = 0,
    numberOfGapsPayable: Int = 0
  ): NationalInsuranceRecord = NationalInsuranceRecord(
    qualifyingYears                = 4,
    qualifyingYearsPriorTo1975     = 0,
    numberOfGaps                   = numberOfGaps,
    numberOfGapsPayable            = numberOfGapsPayable,
    dateOfEntry                    = Some(LocalDate.of(1954, 3, 6)),
    homeResponsibilitiesProtection = false,
    earningsIncludedUpTo           = LocalDate.of(2017, 4, 5),
    reducedRateElection            = false,
    taxYears                       = taxYears
  )

  lazy val controller: StatePensionController =
    standardInjector.instanceOf[StatePensionController]

  "The State Pension page" when {

    "the user is a MQP" when {

      "The scenario is No years to contribute" when {

        "State Pension page with MQP: No Gaps || Cant get pension" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(statePension()))))

            when(mockNationalInsuranceService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(nationalInsuranceRecord()))))
          }

          lazy val doc =
            asDocument(contentAsString(controller.showNew()(FakeRequest())))

          "render with correct page title" in {
            mockSetup
            assertElementContainsText(
              doc,
              "head > title",
              "Your State Pension summary - Check your State Pension - GOV.UK"
            )
          }

          "render page with heading 'Your State Pension summary'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension_mqp__h1']",
              "Your State Pension summary"
            )
          }

          "render page with heading 'When will I reach State Pension age?'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension_mqp__h2_1']",
              "When will I reach State Pension age?"
            )
          }

          "render page with text 'You will reach State Pension age on 4 May 2017.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension_mqp__p1']",
              "You will reach State Pension age on 4 May 2017."
            )
          }

          "render page with text 'When you reach State Pension age, you no longer pay National Insurance contributions." +
            " State Pension age may change in the future.' " in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension_mqp__p2']",
              "When you reach State Pension age, you no longer pay National Insurance contributions. State Pension age may change in the future."
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
              "[data-spec='state_pension_mqp__warning__link']",
              "/check-your-state-pension/seek-financial-advice"
            )
          }

          "render page with text 'Your forecast may change if there are any updates to your National Insurance information.' " in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__mqp__p3']",
              "Your forecast may change if there are any updates to your National Insurance information. " +
                "You can find out more in the terms and conditions."
            )
          }

          "render page with 'terms and conditions' link" in {
            mockSetup
            assertLinkHasValue(
              doc,
              "[data-spec='state_pension__mqp__forecast_changes__link1']",
              "/check-your-state-pension/terms-and-conditions?showBackLink=true"
            )
          }

          "render page with text 'How much will I get?'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension_mqp__h2_2']",
              "How much will I get?"
            )
          }

          "render page with text 'You do not qualify for State Pension.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension_mqp__p4']",
              "You do not qualify for State Pension."
            )
          }

          "render page 'You usually need at least 10 qualifying years on your National Insurance record to " +
            "get any State Pension. You will not get these by 4 May 2017.' " in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension_mqp__p5']",
              "You usually need at least 10 qualifying years on your National Insurance record to " +
                "get any State Pension. You will not get these by 4 May 2017."
            )
          }

          "render page with text 'If you’ve lived or worked outside the UK...'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension_mqp__p6']",
              "If you’ve lived or worked outside the UK, you may be able to use your time outside the UK " +
                "to make up the 10 qualifying years you need to get any UK State Pension. " +
                "Find out more about living or working outside the UK."
            )
          }

          "render page with link 'How can I increase my State Pension?'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension_mqp__h2_3']",
              "How can I increase my State Pension?"
            )
          }

          "render page with 'You cannot increase your State Pension forecast.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__mqp__cant_fill_gaps_p1']",
              "You cannot increase your State Pension forecast."
            )
          }

          "render page with 'This means you are unable to pay for gaps in your National Insurance record online.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__mqp__cant_fill_gaps_p2']",
              "This means you are unable to pay for gaps in your National Insurance record online."
            )
          }

          "render page with link 'View your National Insurance Record'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__mqp__cant_get']",
              "View your National Insurance record"
            )

            assertLinkHasValue(
              doc,
              "[data-spec='state_pension__mqp__cant_get']",
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

          "render page with Other ways to increase my income h3" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension_mqp__h3_1']",
              "Other ways to increase my income"
            )
          }

          "render page with 'Home Responsibilities Protection' paragraph with link" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension_mqp__p7']",
              "If you claimed Child Benefit between 6 April 1978 and 5 April 2010, you may be able to claim " +
                "Home Responsibilities Protection to fill gaps in your National Insurance record. Check if you are eligible to claim Home Responsibilities Protection."
            )

            assertLinkHasValue(
              doc,
              "[data-spec='state_pension_mqp__home_responsibilities_protection_link']",
              "https://www.gov.uk/home-responsibilities-protection-hrp"
            )
          }

          "render page with 'Pension Credit' paragraph with link" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension_mqp__p8']",
              "You may be able to claim Pension Credit if you’re on a low income. " +
                "Find out about claiming Pension Credit."
            )

            assertLinkHasValue(
              doc,
              "[data-spec='state_pension_mqp__pension_credit_link']",
              "https://www.gov.uk/pension-credit/overview"
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
        }

        "State Pension page with MQP: No Gaps || Cant get pension: With State Pension age under consideration message" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(statePension(ageUnderConsideration = true)))))

            when(mockNationalInsuranceService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(nationalInsuranceRecord()))))
          }

          lazy val doc =
            asDocument(contentAsString(controller.showNew()(FakeRequest())))

          // SPA under consideration message
          "render page with heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension_age_under_consideration__h2_1']",
              "nisp.spa.under.consideration.title"
            )
          }

          "render page with text 'You’ll reach State Pension age on 4 May 2017. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertContainsDynamicMessage(
              doc,
              "[data-spec='state_pension_age_under_consideration__p1']",
              "nisp.spa.under.consideration.detail",
              langUtils.Dates.formatDate(LocalDate.of(2017, 5, 4))
            )
          }
        }

        "State Pension page with MQP: has fillable Gaps: Cant get pension" should {
          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(statePension(
                pensionDate       = LocalDate.of(2018, 5, 4),
                finalRelevantYear = "2017-18",
                amounts           = statePensionAmounts(StatePensionAmountMaximum(
                  yearsToWork   = 2,
                  gapsToFill    = 2,
                  weeklyAmount  = 0,
                  monthlyAmount = 0,
                  annualAmount  = 0
                ))
              )))))

            when(mockNationalInsuranceService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(nationalInsuranceRecord(
                numberOfGaps        = 2,
                numberOfGapsPayable = 2
              )))))
          }

          lazy val doc =
            asDocument(contentAsString(controller.showNew()(FakeRequest())))

          "render page with 'Filling the gaps in your record is not enough to get State Pension.'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__mqp__cant_fill_gaps_p3']",
              "Filling the gaps in your record is not enough to get State Pension."
            )
          }
        }

        "State Pension page with MQP: has fillable Gaps || Personal Max" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(statePension(
                pensionDate       = LocalDate.of(2018, 5, 4),
                finalRelevantYear = "2017-18",
                amounts           = statePensionAmounts(StatePensionAmountMaximum(
                  yearsToWork   = 2,
                  gapsToFill    = 2,
                  weeklyAmount  = 12,
                  monthlyAmount = 0,
                  annualAmount  = 0
                ))
              )))))

            when(mockNationalInsuranceService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(nationalInsuranceRecord(
                numberOfGaps        = 2,
                numberOfGapsPayable = 2
              )))))
          }

          lazy val doc =
            asDocument(contentAsString(controller.showNew()(FakeRequest())))

          "render page with link 'Gaps in your record and the cost of filling them'" in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__mqp__cant_get_with_gaps']",
              "View your National Insurance record"
            )
          }

          "render page with 'You can view your National Insurance record to check for gaps that you may be " +
            "able to fill to increase your State Pension.' " in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__mqp__filling_gaps']",
              "You can view your National Insurance record to check for gaps that you may be " +
                "able to fill to increase your State Pension."
            )
          }

          "render page with href link 'View your National Insurance Record'" in {
            mockSetup
            assertLinkHasValue(
              doc,
              "[data-spec='state_pension__mqp__cant_get_with_gaps']",
              "/check-your-state-pension/account/nirecord/gaps"
            )
          }
        }

        "State Pension page with MQP: has fillable Gaps || Personal Max: With State Pension age under consideration message" should {

          def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {
            when(mockStatePensionService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(statePension(
                pensionDate           = LocalDate.of(2018, 5, 4),
                finalRelevantYear     = "2017-18",
                ageUnderConsideration = true,
                amounts               = statePensionAmounts(StatePensionAmountMaximum(
                  yearsToWork   = 2,
                  gapsToFill    = 2,
                  weeklyAmount  = 12,
                  monthlyAmount = 0,
                  annualAmount  = 0
                ))
              )))))

            when(mockNationalInsuranceService.getSummary(any())(any()))
              .thenReturn(Future.successful(Right(Right(nationalInsuranceRecord(
                numberOfGaps        = 2,
                numberOfGapsPayable = 2
              )))))
          }

          lazy val doc =
            asDocument(contentAsString(controller.showNew()(FakeRequest())))

          // SPA under consideration message
          "render page with heading 'Proposed change to your State Pension age'" in {
            mockSetup
            assertEqualsMessage(
              doc,
              "[data-spec='state_pension_age_under_consideration__h2_1']",
              "nisp.spa.under.consideration.title"
            )
          }

          "render page with text 'You’ll reach State Pension age on 4 May 2018. Under government proposals this may increase by up to a year.'" in {
            mockSetup
            assertContainsDynamicMessage(
              doc,
              "[data-spec='state_pension_age_under_consideration__p1']",
              "nisp.spa.under.consideration.detail",
              langUtils.Dates.formatDate(LocalDate.of(2018, 5, 4))
            )
          }

          "render page with 'You can view your National Insurance record to check for gaps that you may be " +
            "able to fill to increase your State Pension.' " in {
            mockSetup
            assertEqualsText(
              doc,
              "[data-spec='state_pension__mqp__filling_gaps']",
              "You can view your National Insurance record to check for gaps that you may be " +
                "able to fill to increase your State Pension."
            )
          }
        }
      }
    }
  }
}
