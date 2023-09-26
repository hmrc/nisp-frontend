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

package uk.gov.hmrc.nisp.controllers

import org.mockito.ArgumentMatchers.{any => mockAny, eq => mockEQ}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{Injector, bind}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.auth.{AuthAction, PertaxAuthAction}
import uk.gov.hmrc.nisp.controllers.pertax.PertaxHelper
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.services.{NationalInsuranceService, StatePensionService}
import uk.gov.hmrc.nisp.utils.{UnitSpec, WireMockHelper}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.Future

class StatePensionControllerSpec extends UnitSpec with BeforeAndAfterEach with GuiceOneAppPerSuite with Injecting
  with WireMockHelper {

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val mockAuditConnector: AuditConnector                     = mock[AuditConnector]
  val mockNationalInsuranceService: NationalInsuranceService = mock[NationalInsuranceService]
  val mockStatePensionService: StatePensionService           = mock[StatePensionService]
  val mockAppConfig: ApplicationConfig                       = mock[ApplicationConfig]
  val mockPertaxHelper: PertaxHelper                         = mock[PertaxHelper]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuditConnector)
    reset(mockNationalInsuranceService)
    reset(mockStatePensionService)
    reset(mockAppConfig)
    reset(mockPertaxHelper)
    server.resetAll()
    when(mockAppConfig.pertaxAuthBaseUrl).thenReturn(s"http://localhost:${server.port()}")
    featureFlagSCAWrapperMock()
  }

  def generateFakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    SessionKeys.sessionId            -> s"session-${UUID.randomUUID()}",
    SessionKeys.lastRequestTimestamp -> LocalDate.now.toEpochDay.toString
  )

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

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[StatePensionService].toInstance(mockStatePensionService),
      bind[NationalInsuranceService].toInstance(mockNationalInsuranceService),
      bind[AuditConnector].toInstance(mockAuditConnector),
      bind[ApplicationConfig].toInstance(mockAppConfig),
      bind[PertaxHelper].toInstance(mockPertaxHelper),
      bind[AuthAction].to[FakeAuthAction],
      bind[PertaxAuthAction].to[FakePertaxAuthAction],
      featureFlagServiceBinding
    )
    .build()

  val standardNino: Nino = TestAccountBuilder.regularNino
  val foreignNino: Nino = TestAccountBuilder.abroadNino

  val statePensionController: StatePensionController = inject[StatePensionController]

  val statePensionCopeResponse: StatePension = StatePension(
    LocalDate.of(2014, 4, 5),
    StatePensionAmounts(
      protectedPayment = false,
      StatePensionAmountRegular(46.38, 201.67, 2420.04),
      StatePensionAmountForecast(3, 155.55, 622.35, 76022.24),
      StatePensionAmountMaximum(3, 0, 155.55, 622.35, 76022.24),
      StatePensionAmountRegular(50, 217.41, 2608.93)
    ),
    64,
    LocalDate.of(2021, 7, 18),
    "2017-18",
    30,
    pensionSharingOrder = false,
    155.65,
    reducedRateElection = false,
    statePensionAgeUnderConsideration = false
  )

  val statePensionResponse: StatePension = StatePension(
    LocalDate.of(2015, 4, 5),
    StatePensionAmounts(
      protectedPayment = false,
      StatePensionAmountRegular(133.41, 580.1, 6961.14),
      StatePensionAmountForecast(3, 146.76, 638.14, 7657.73),
      StatePensionAmountMaximum(3, 2, 155.65, 676.8, 8121.59),
      StatePensionAmountRegular(0, 0, 0)
    ),
    64,
    LocalDate.of(2018, 7, 6),
    "2017-18",
    30,
    pensionSharingOrder = false,
    155.65,
    reducedRateElection = false,
    statePensionAgeUnderConsideration = false
  )

  val statePensionVariation2: StatePension = StatePension(
    LocalDate.of(2015, 4, 5),
    StatePensionAmounts(
      protectedPayment = false,
      StatePensionAmountRegular(0, 0, 0),
      StatePensionAmountForecast(1, 0, 0, 0),
      StatePensionAmountMaximum(1, 6, 155.65, 676.8, 8121.59),
      StatePensionAmountRegular(0, 0, 0)
    ),
    64,
    LocalDate.of(2016, 7, 6),
    "2034-35",
    30,
    pensionSharingOrder = false,
    0,
    reducedRateElection = true,
    statePensionAgeUnderConsideration = false
  )

  val statePensionResponseVariation3: StatePension = StatePension(
    LocalDate.of(2014, 4, 5),
    StatePensionAmounts(
      protectedPayment = false,
      StatePensionAmountRegular(133.41, 580.1, 6961.14),
      StatePensionAmountForecast(0, 146.76, 638.14, 7657.73),
      StatePensionAmountMaximum(50, 7, 155.65, 676.8, 8121.59),
      StatePensionAmountRegular(0, 0, 0)
    ),
    64,
    LocalDate.of(2050, 7, 6),
    "2050-51",
    25,
    pensionSharingOrder = false,
    155.65,
    reducedRateElection = false,
    statePensionAgeUnderConsideration = false
  )

  val nationalInsuranceRecord: NationalInsuranceRecord = NationalInsuranceRecord(
    28,
    -3,
    6,
    4,
    Some(LocalDate.of(1975, 8, 1)),
    homeResponsibilitiesProtection = true,
    LocalDate.of(2016, 4, 5),
    List(
      NationalInsuranceTaxYear("2015-16", qualifying = true, 2430.24, 0, 0, 0, 0, None, None, payable = false, underInvestigation = false),
      NationalInsuranceTaxYear("2014-15", qualifying = false, 2430.24, 0, 0, 0, 0, None, None, payable = false, underInvestigation = false)
    ),
    reducedRateElection = false
  )

  val nationalInsuranceRecordVariant2: NationalInsuranceRecord = NationalInsuranceRecord(
    28,
    28,
    6,
    4,
    Some(LocalDate.of(1975, 8, 1)),
    homeResponsibilitiesProtection = true,
    LocalDate.of(2014, 4, 5),
    List(
      NationalInsuranceTaxYear(
        "2015-16",
        qualifying = true,
        2430.24,
        0,
        0,
        0,
        722.8,
        Some(LocalDate.of(2019, 4, 5)),
        Some(LocalDate.of(2024, 4, 5)),
        payable = true,
        underInvestigation = false
      ),
      NationalInsuranceTaxYear(
        "2014-15",
        qualifying = false,
        2430.24,
        0,
        0,
        0,
        722.8,
        Some(LocalDate.of(2018, 4, 5)),
        Some(LocalDate.of(2023, 4, 5)),
        payable = true,
        underInvestigation = false
      )
    ),
    reducedRateElection = false
  )

  "State Pension controller" should {

    "GET /statepension" should {

      "return the forecast only page for a user with a forecast lower than current amount" in {
        when(mockPertaxHelper.isFromPertax(mockAny())).thenReturn(false)

        when(mockNationalInsuranceService.getSummary(mockEQ(standardNino), mockAny())(mockAny())).thenReturn(
          Future.successful(Right(Right(nationalInsuranceRecord)))
        )

        when(mockStatePensionService.getSummary(mockEQ(standardNino), mockAny())(mockAny())).thenReturn(
          Future.successful(Right(Right(statePensionResponse)))
        )

        when(mockAppConfig.urBannerUrl).thenReturn("/foo")
        when(mockAppConfig.reportAProblemNonJSUrl).thenReturn("/reportAProblem")
        when(mockAppConfig.contactFormServiceIdentifier).thenReturn("/id")

        val result = statePensionController.show()(generateFakeRequest)
        contentAsString(result) should not include "£80.38"
      }

      "return 200, with exclusion message for excluded user" in {
        when(mockPertaxHelper.isFromPertax(mockAny())).thenReturn(false)

        when(mockNationalInsuranceService.getSummary(mockEQ(standardNino), mockAny())(mockAny())).thenReturn(
          Future.successful(Right(Left(StatePensionExclusionFiltered(Exclusion.Dead))))
        )

        when(mockStatePensionService.getSummary(mockEQ(standardNino), mockAny())(mockAny())).thenReturn(
          Future.successful(Right(Left(StatePensionExclusionFiltered(Exclusion.Dead))))
        )

        val result = statePensionController
          .show()(
            fakeRequest.withSession(
              SessionKeys.sessionId            -> s"session-${UUID.randomUUID()}",
              SessionKeys.lastRequestTimestamp -> LocalDate.now.toEpochDay.toString
            )
          )
        redirectLocation(result) shouldBe Some("/check-your-state-pension/exclusion")
      }

      "return content about COPE for contracted out (B) user" in {
        when(mockPertaxHelper.isFromPertax(mockAny())).thenReturn(false)

        val expectedNationalInsuranceRecord = NationalInsuranceRecord(
          28,
          -8,
          0,
          0,
          Some(LocalDate.of(1975, 8, 1)),
          homeResponsibilitiesProtection = true,
          LocalDate.of(2016, 4, 5),
          List(
            NationalInsuranceTaxYear("2015-16", qualifying = true, 2430.24, 0, 0, 0, 0, None, None, payable = false, underInvestigation = false),
            NationalInsuranceTaxYear("2014-15", qualifying = false, 2430.24, 0, 0, 0, 0, None, None, payable = false, underInvestigation = false)
          ),
          reducedRateElection = false
        )

        when(mockNationalInsuranceService.getSummary(mockEQ(standardNino), mockAny())(mockAny())).thenReturn(
          Future.successful(Right(Right(expectedNationalInsuranceRecord)))
        )

        when(mockStatePensionService.getSummary(mockEQ(standardNino), mockAny())(mockAny())).thenReturn(
          Future.successful(Right(Right(statePensionCopeResponse)))
        )

        when(mockAppConfig.urBannerUrl).thenReturn("/foo")
        when(mockAppConfig.reportAProblemNonJSUrl).thenReturn("/reportAProblem")
        when(mockAppConfig.contactFormServiceIdentifier).thenReturn("/id")

        val result = statePensionController.show()(generateFakeRequest)
        contentAsString(result) should include("You’ve been in a contracted-out pension scheme")
      }

      "return COPE page for contracted out (B) user" in {
        when(mockPertaxHelper.isFromPertax(mockAny())).thenReturn(false)

        when(mockStatePensionService.getSummary(mockEQ(standardNino), mockAny())(mockAny())).thenReturn(
          Future.successful(Right(Right(statePensionCopeResponse)))
        )

        when(mockAppConfig.urBannerUrl).thenReturn("/foo")
        when(mockAppConfig.reportAProblemNonJSUrl).thenReturn("/reportAProblem")
        when(mockAppConfig.contactFormServiceIdentifier).thenReturn("/id")

        val result = statePensionController.showCope()(generateFakeRequest)
        contentAsString(result) should include("You were contracted out")
      }

      "return abroad message for abroad user" in {
        when(mockPertaxHelper.isFromPertax(mockAny())).thenReturn(false)

        when(mockNationalInsuranceService.getSummary(mockEQ(foreignNino), mockAny())(mockAny())).thenReturn(
          Future.successful(Right(Right(nationalInsuranceRecord)))
        )

        when(mockStatePensionService.getSummary(mockEQ(foreignNino), mockAny())(mockAny())).thenReturn(
          Future.successful(Right(Right(statePensionResponse)))
        )

        when(mockAppConfig.urBannerUrl).thenReturn("/foo")
        when(mockAppConfig.reportAProblemNonJSUrl).thenReturn("/reportAProblem")
        when(mockAppConfig.contactFormServiceIdentifier).thenReturn("/id")

        val statePensionController = abroadUserInjector.instanceOf[StatePensionController]

        val result = statePensionController.show()(generateFakeRequest)
        contentAsString(result) should include("As you are living or working overseas")
      }

      "return /exclusion for MWRRE user" in {
        when(mockPertaxHelper.isFromPertax(mockAny())).thenReturn(false)

        when(mockNationalInsuranceService.getSummary(mockEQ(standardNino), mockAny())(mockAny())).thenReturn(
          Future.successful(Right(Left(StatePensionExclusionFiltered(Exclusion.MarriedWomenReducedRateElection))))
        )

        when(mockStatePensionService.getSummary(mockEQ(standardNino), mockAny())(mockAny())).thenReturn(
          Future.successful(Right(Left(StatePensionExclusionFiltered(Exclusion.MarriedWomenReducedRateElection))))
        )

        when(mockAppConfig.urBannerUrl).thenReturn("/foo")
        when(mockAppConfig.reportAProblemNonJSUrl).thenReturn("/reportAProblem")
        when(mockAppConfig.contactFormServiceIdentifier).thenReturn("/id")

        val result = statePensionController.show()(generateFakeRequest)
        status(result)           shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/check-your-state-pension/exclusion")
      }

      "return abroad message for forecast only user" in {
        when(mockPertaxHelper.isFromPertax(mockAny())).thenReturn(false)

        when(mockNationalInsuranceService.getSummary(mockEQ(foreignNino), mockAny())(mockAny())).thenReturn(
          Future.successful(Right(Right(nationalInsuranceRecord)))
        )

        when(mockStatePensionService.getSummary(mockEQ(foreignNino), mockAny())(mockAny())).thenReturn(
          Future.successful(Right(Right(statePensionResponse)))
        )

        when(mockAppConfig.urBannerUrl).thenReturn("/foo")
        when(mockAppConfig.reportAProblemNonJSUrl).thenReturn("/reportAProblem")
        when(mockAppConfig.contactFormServiceIdentifier).thenReturn("/id")

        val statePensionController = abroadUserInjector.instanceOf[StatePensionController]

        val result = statePensionController.show()(generateFakeRequest)
        contentAsString(result) should include("As you are living or working overseas")
        contentAsString(result) should not include "£80.38"
      }

      "return abroad message for an mqp user instead of standard mqp overseas message" in {
        when(mockPertaxHelper.isFromPertax(mockAny())).thenReturn(false)

        when(mockNationalInsuranceService.getSummary(mockEQ(foreignNino), mockAny())(mockAny())).thenReturn(
          Future.successful(Right(Right(nationalInsuranceRecord)))
        )

        when(mockStatePensionService.getSummary(mockEQ(foreignNino), mockAny())(mockAny())).thenReturn(
          Future.successful(Right(Right(statePensionResponse)))
        )

        when(mockAppConfig.urBannerUrl).thenReturn("/foo")
        when(mockAppConfig.reportAProblemNonJSUrl).thenReturn("/reportAProblem")
        when(mockAppConfig.contactFormServiceIdentifier).thenReturn("/id")

        val statePensionController = abroadUserInjector.instanceOf[StatePensionController]

        val result = statePensionController.show()(generateFakeRequest)
        contentAsString(result) should include("As you are living or working overseas")
        contentAsString(result) should not include "If you have lived or worked overseas"
      }

      "redirect to statepension page for non contracted out user" in {
        when(mockPertaxHelper.isFromPertax(mockAny())).thenReturn(false)

        when(mockStatePensionService.getSummary(mockEQ(standardNino), mockAny())(mockAny())).thenReturn(
          Future.successful(Right(Right(statePensionVariation2)))
        )

        val result = statePensionController.showCope()(generateFakeRequest)
        redirectLocation(result) shouldBe Some("/check-your-state-pension/account")
      }

      "return page with MQP messaging for MQP user" in {
        when(mockPertaxHelper.isFromPertax(mockAny())).thenReturn(false)

        val expectedNationalInsuranceRecord = NationalInsuranceRecord(
          3,
          3,
          19,
          7,
          Some(LocalDate.of(1975, 8, 1)),
          homeResponsibilitiesProtection = false,
          LocalDate.of(2014, 4, 5),
          List.empty[NationalInsuranceTaxYear],
          reducedRateElection = false
        )

        when(mockNationalInsuranceService.getSummary(mockEQ(standardNino), mockAny())(mockAny())).thenReturn(
          Future.successful(Right(Right(expectedNationalInsuranceRecord)))
        )

        when(mockStatePensionService.getSummary(mockEQ(standardNino), mockAny())(mockAny())).thenReturn(
          Future.successful(Right(Right(statePensionVariation2)))
        )

        when(mockAppConfig.urBannerUrl).thenReturn("/foo")
        when(mockAppConfig.reportAProblemNonJSUrl).thenReturn("/reportAProblem")
        when(mockAppConfig.contactFormServiceIdentifier).thenReturn("/id")

        val result = statePensionController.show()(generateFakeRequest)
        contentAsString(result) should include(
          "10 years needed on your National Insurance record to get any State Pension"
        )
      }
    }

    "when there is a Fill Gaps Scenario" when {
      "the future config is set to off" should {
        "show year information when there is multiple years" in {
          when(mockPertaxHelper.isFromPertax(mockAny())).thenReturn(false)

          when(mockNationalInsuranceService.getSummary(mockEQ(standardNino), mockAny())(mockAny())).thenReturn(
            Future.successful(Right(Right(nationalInsuranceRecordVariant2)))
          )

          when(mockStatePensionService.getSummary(mockEQ(standardNino), mockAny())(mockAny())).thenReturn(
            Future.successful(Right(Right(statePensionResponseVariation3)))
          )

          when(mockAppConfig.urBannerUrl).thenReturn("/foo")
          when(mockAppConfig.reportAProblemNonJSUrl).thenReturn("/reportAProblem")
          when(mockAppConfig.contactFormServiceIdentifier).thenReturn("/id")

          val result = statePensionController.show()(generateFakeRequest)
          contentAsString(result) should include(
            "You have years on your National Insurance record where you did not contribute enough."
          )
          contentAsString(result) should include("filling years can improve your forecast")
          contentAsString(result) should include("you only need to fill 7 years to get the most you can")
          contentAsString(result) should include("The most you can get by filling any 7 years in your record is")
        }
        "show specific text when is only one payable gap" in {
          when(mockPertaxHelper.isFromPertax(mockAny())).thenReturn(false)

          val expectedNationalInsuranceRecord = NationalInsuranceRecord(
            28,
            28,
            1,
            1,
            Some(LocalDate.of(1975, 8, 1)),
            homeResponsibilitiesProtection = true,
            LocalDate.of(2014, 4, 5),
            List(
              NationalInsuranceTaxYear(
                "2013-14",
                qualifying = false,
                2430.24,
                0,
                0,
                0,
                722.8,
                Some(LocalDate.of(2019, 4, 5)),
                Some(LocalDate.of(2024, 4, 5)),
                payable = true,
                underInvestigation = false
              ),
              NationalInsuranceTaxYear(
                "2012-13",
                qualifying = false,
                2430.24,
                0,
                0,
                0,
                722.8,
                Some(LocalDate.of(2018, 4, 5)),
                Some(LocalDate.of(2023, 4, 5)),
                payable = true,
                underInvestigation = false
              )
            ),
            reducedRateElection = false
          )

          when(mockNationalInsuranceService.getSummary(mockEQ(standardNino), mockAny())(mockAny())).thenReturn(
            Future.successful(Right(Right(expectedNationalInsuranceRecord)))
          )

          when(mockStatePensionService.getSummary(mockEQ(standardNino), mockAny())(mockAny())).thenReturn(
            Future.successful(Right(Right(statePensionResponseVariation3)))
          )

          when(mockAppConfig.urBannerUrl).thenReturn("/foo")
          when(mockAppConfig.reportAProblemNonJSUrl).thenReturn("/reportAProblem")
          when(mockAppConfig.contactFormServiceIdentifier).thenReturn("/id")

          val result = statePensionController.show()(generateFakeRequest)
          contentAsString(result) should include(
            "You have a year on your National Insurance record where you did not contribute enough. You only need to fill this year to get the most you can."
          )
          contentAsString(result) should include("The most you can get by filling this year in your record is")
        }
      }

      "the future proof config is set to true" should {
        "show new personal max text when there are multiple/single year" in {
          when(mockPertaxHelper.isFromPertax(mockAny())).thenReturn(false)
          when(mockAppConfig.futureProofPersonalMax).thenReturn(true)

          when(mockNationalInsuranceService.getSummary(mockEQ(standardNino), mockAny())(mockAny())).thenReturn(
            Future.successful(Right(Right(nationalInsuranceRecordVariant2)))
          )

          when(mockStatePensionService.getSummary(mockEQ(standardNino), mockAny())(mockAny())).thenReturn(
            Future.successful(Right(Right(statePensionResponseVariation3)))
          )

          when(mockAppConfig.urBannerUrl).thenReturn("/foo")
          when(mockAppConfig.reportAProblemNonJSUrl).thenReturn("/reportAProblem")
          when(mockAppConfig.contactFormServiceIdentifier).thenReturn("/id")

          val result = statePensionController.show()(generateFakeRequest)
          contentAsString(result) should include(
            "You have shortfalls in your National Insurance record that you can fill and make count towards your State Pension."
          )
          contentAsString(result) should include("The most you can increase your forecast to is")
        }
      }
    }

    "GET /signout" should {
      "redirect to the questionnaire page when govuk done page is disabled" in {
        when(mockAppConfig.feedbackFrontendUrl).thenReturn("/foo")

        val result = statePensionController.signOut(fakeRequest)
        redirectLocation(result).get shouldBe "/foo"
      }

      "redirect to the feedback questionnaire page when govuk done page is enabled" in {
        when(mockAppConfig.feedbackFrontendUrl).thenReturn("/foo")
        when(mockAppConfig.reportAProblemNonJSUrl).thenReturn("/reportAProblem")

        val result = statePensionController.signOut(fakeRequest)
        redirectLocation(result).get shouldBe "/foo"
      }
    }

  }
}
