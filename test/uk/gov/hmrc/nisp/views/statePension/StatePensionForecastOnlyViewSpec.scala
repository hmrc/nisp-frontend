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
import play.api.i18n.Messages
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{Injector, bind}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers.contentAsString
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.http.{SessionKeys, UpstreamErrorResponse}
import uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlag
import uk.gov.hmrc.nisp.builders.NationalInsuranceTaxYearBuilder
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.StatePensionController
import uk.gov.hmrc.nisp.controllers.auth.{AuthRetrievals, PertaxAuthAction}
import uk.gov.hmrc.nisp.controllers.pertax.PertaxHelper
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.models.admin.NewStatePensionUIToggle
import uk.gov.hmrc.nisp.services.{NationalInsuranceService, StatePensionService}
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.nisp.views.HtmlSpec
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.language.LanguageUtils

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.Future

class StatePensionForecastOnlyViewSpec
  extends HtmlSpec
    with Injecting
    with WireMockSupport {

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

    when(mockPertaxHelper.isFromPertax(any()))
      .thenReturn(Future.successful(false))
    when(mockAppConfig.accessibilityStatementUrl(any()))
      .thenReturn("/foo")
    when(mockAppConfig.reportAProblemNonJSUrl)
      .thenReturn("/reportAProblem")
    when(mockAppConfig.contactFormServiceIdentifier)
      .thenReturn("/id")
    wireMockServer.resetAll()
    when(mockAppConfig.pertaxAuthBaseUrl)
      .thenReturn(s"http://localhost:${wireMockServer.port()}")
    when(mockFeatureFlagService.get(NewStatePensionUIToggle))
      .thenReturn(Future.successful(FeatureFlag(NewStatePensionUIToggle, isEnabled = true)))
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
    }
  }
}
