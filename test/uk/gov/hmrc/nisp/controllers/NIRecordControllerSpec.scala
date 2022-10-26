/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.http.{SessionKeys, UpstreamErrorResponse}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.auth.AuthAction
import uk.gov.hmrc.nisp.controllers.pertax.PertaxHelper
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.models.Exclusion.CopeProcessing
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.services.{NationalInsuranceService, StatePensionService}
import uk.gov.hmrc.nisp.utils.{DateProvider, UnitSpec}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.Future

class NIRecordControllerSpec extends UnitSpec with GuiceOneAppPerSuite with Injecting with BeforeAndAfterEach {
  val mockAuditConnector: AuditConnector                     = mock[AuditConnector]
  val mockNationalInsuranceService: NationalInsuranceService = mock[NationalInsuranceService]
  val mockStatePensionService: StatePensionService           = mock[StatePensionService]
  val mockAppConfig: ApplicationConfig                       = mock[ApplicationConfig]
  val mockPertaxHelper: PertaxHelper                         = mock[PertaxHelper]
  val mockDateProvider: DateProvider                         = mock[DateProvider]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(
      mockNationalInsuranceService,
      mockStatePensionService,
      mockAppConfig,
      mockPertaxHelper,
      mockAuditConnector,
      mockDateProvider
    )
    when(mockAppConfig.urBannerUrl).thenReturn("/foo")
    when(mockAppConfig.reportAProblemNonJSUrl).thenReturn("/reportAProblem")
    when(mockAppConfig.contactFormServiceIdentifier).thenReturn("/id")
  }

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[AuditConnector].toInstance(mockAuditConnector),
      bind[AuthAction].to[FakeAuthAction],
      bind[NationalInsuranceService].toInstance(mockNationalInsuranceService),
      bind[StatePensionService].toInstance(mockStatePensionService),
      bind[ApplicationConfig].toInstance(mockAppConfig),
      bind[PertaxHelper].toInstance(mockPertaxHelper),
      bind[DateProvider].toInstance(mockDateProvider)
    )
    .build()

  val fakeRequest        = FakeRequest()
  val niRecordController = inject[NIRecordController]

  def generateFakeRequest = FakeRequest().withSession(
    SessionKeys.sessionId            -> s"session-${UUID.randomUUID()}",
    SessionKeys.lastRequestTimestamp -> LocalDate.now.toEpochDay.toString
  )

  "GET /account/nirecord/gaps (gaps)" should {

    "return gaps page for user with gaps" in {

      val statePensionResponse = StatePension(
        LocalDate.of(2015, 4, 5),
        StatePensionAmounts(
          false,
          StatePensionAmountRegular(133.41, 580.1, 6961.14),
          StatePensionAmountForecast(3, 146.76, 638.14, 7657.73),
          StatePensionAmountMaximum(3, 2, 155.65, 676.8, 8121.59),
          StatePensionAmountRegular(0, 0, 0)
        ),
        64,
        LocalDate.of(2018, 7, 6),
        "2017",
        30,
        false,
        155.65,
        false,
        false
      )

      val expectedNationalInsuranceRecord = NationalInsuranceRecord(
        40,
        39,
        2,
        1,
        Some(LocalDate.of(1973, 7, 7)),
        false,
        LocalDate.of(2016, 4, 5),
        List(
          NationalInsuranceTaxYear("2015", true, 12345.45, 0, 0, 0, 0, None, None, false, false),
          NationalInsuranceTaxYear(
            "2014",
            false,
            123,
            1,
            1,
            1,
            456.58,
            Some(LocalDate.of(2019, 4, 5)),
            Some(LocalDate.of(2023, 4, 5)),
            false,
            false
          ),
          NationalInsuranceTaxYear(
            "1999",
            false,
            2,
            5,
            0,
            1,
            111.11,
            Some(LocalDate.of(2019, 4, 5)),
            Some(LocalDate.of(2023, 4, 5)),
            false,
            false
          )
        ),
        false
      )

      when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
        Future.successful(Right(Right(expectedNationalInsuranceRecord)))
      )

      when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
        Future.successful(Right(Right(statePensionResponse)))
      )

      when(mockDateProvider.currentDate).thenReturn(LocalDate.of(2016, 9, 9))

      val result = niRecordController.showGaps(generateFakeRequest)
      contentAsString(result) should include("View all years of contributions")
    }

    "return full page for user without gaps" in {
      val expectedNationalInsuranceRecord = NationalInsuranceRecord(
        28,
        -8,
        0,
        0,
        Some(LocalDate.of(1975, 8, 1)),
        false,
        LocalDate.of(2016, 4, 5),
        List(
          NationalInsuranceTaxYear("2015-16", true, 2430.24, 0, 0, 0, 0, None, None, false, false),
          NationalInsuranceTaxYear("2014-15", true, 2430.24, 0, 0, 0, 0, None, None, false, false)
        ),
        false
      )

      val expectedStatePensionResponse = StatePension(
        LocalDate.of(2016, 4, 5),
        StatePensionAmounts(
          false,
          StatePensionAmountRegular(133.41, 580.1, 6961.14),
          StatePensionAmountForecast(3, 146.76, 638.14, 7657.73),
          StatePensionAmountMaximum(3, 0, 155.65, 676.8, 8121.59),
          StatePensionAmountRegular(0, 0, 0)
        ),
        64,
        LocalDate.of(2018, 7, 6),
        "2017-18",
        30,
        false,
        155.65,
        false,
        false
      )

      when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
        Future.successful(Right(Right(expectedNationalInsuranceRecord)))
      )

      when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
        Future.successful(Right(Right(expectedStatePensionResponse)))
      )

      val result = niRecordController.showGaps(generateFakeRequest)
      redirectLocation(result) shouldBe Some("/check-your-state-pension/account/nirecord")
    }

    "redirect to exclusion for excluded user" in {
      // FIXME: add COPE
      when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
        Future.successful(Right(Left(StatePensionExclusionFiltered(Exclusion.Dead))))
      )

      when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
        Future.successful(Right(Left(StatePensionExclusionFiltered(Exclusion.Dead))))
      )

      val result = niRecordController
        .showGaps(
          fakeRequest.withSession(
            SessionKeys.sessionId            -> s"session-${UUID.randomUUID()}",
            SessionKeys.lastRequestTimestamp -> LocalDate.now.toEpochDay.toString
          )
        )

      redirectLocation(result) shouldBe Some("/check-your-state-pension/exclusionni")
    }
  }

  "GET /account/nirecord (full)" should {

    "return gaps page for user with gaps" in {

      val expectedNationalInsuranceRecord = NationalInsuranceRecord(
        28,
        -3,
        10,
        4,
        Some(LocalDate.of(1975, 8, 1)),
        false,
        LocalDate.of(2014, 4, 5),
        List(
          NationalInsuranceTaxYear(
            "2013-14",
            false,
            0,
            0,
            0,
            0,
            704.60,
            Some(LocalDate.of(2019, 4, 5)),
            Some(LocalDate.of(2023, 4, 5)),
            true,
            false
          )
        ),
        false
      )

      val expectedStatePensionResponse = StatePension(
        LocalDate.of(2015, 4, 5),
        StatePensionAmounts(
          false,
          StatePensionAmountRegular(133.41, 580.1, 6961.14),
          StatePensionAmountForecast(3, 146.76, 638.14, 7657.73),
          StatePensionAmountMaximum(3, 2, 155.65, 676.8, 8121.59),
          StatePensionAmountRegular(0, 0, 0)
        ),
        64,
        LocalDate.of(2018, 7, 6),
        "2017-18",
        30,
        false,
        155.65,
        false,
        false
      )

      when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
        Future.successful(Right(Right(expectedNationalInsuranceRecord)))
      )

      when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
        Future.successful(Right(Right(expectedStatePensionResponse)))
      )

      when(mockDateProvider.currentDate).thenReturn(LocalDate.of(2016, 9, 9))

      val result = niRecordController.showFull(generateFakeRequest)
      contentAsString(result) should include("View years only showing gaps in your contributions")
    }

    "return full page for user without gaps" in {

      val expectedNationalInsuranceRecord = NationalInsuranceRecord(
        28,
        -8,
        0,
        0,
        Some(LocalDate.of(1975, 8, 1)),
        false,
        LocalDate.of(2016, 4, 5),
        List(NationalInsuranceTaxYear("2015-16", true, 2430.24, 0, 0, 0, 0, None, None, false, false)),
        false
      )

      val expectedStatePension = StatePension(
        LocalDate.of(2016, 4, 5),
        StatePensionAmounts(
          false,
          StatePensionAmountRegular(133.41, 580.1, 6961.14),
          StatePensionAmountForecast(3, 146.76, 638.14, 7657.73),
          StatePensionAmountMaximum(3, 0, 155.65, 676.8, 8121.59),
          StatePensionAmountRegular(0, 0, 0)
        ),
        64,
        LocalDate.of(2018, 7, 6),
        "2017-18",
        30,
        false,
        155.65,
        false,
        false
      )

      when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
        Future.successful(Right(Right(expectedNationalInsuranceRecord)))
      )

      when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
        Future.successful(Right(Right(expectedStatePension)))
      )

      val result = niRecordController.showFull(generateFakeRequest)
      contentAsString(result) should include("You do not have any gaps in your record.")
    }

    "redirect to exclusion for excluded user" in {

      when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
        Future.successful(Right(Left(StatePensionExclusionFiltered(Exclusion.Dead))))
      )

      when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
        Future.successful(Right(Left(StatePensionExclusionFiltered(Exclusion.Dead))))
      )

      val result = niRecordController
        .showFull(
          fakeRequest.withSession(
            SessionKeys.sessionId            -> s"session-${UUID.randomUUID()}",
            SessionKeys.lastRequestTimestamp -> LocalDate.now.toEpochDay.toString
          )
        )

      redirectLocation(result) shouldBe Some("/check-your-state-pension/exclusionni")
    }

    "Redirect to exclusion for an unexpected error" in {

      when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
        Future.successful(Left(UpstreamErrorResponse("Failed", 502)))
      )

      when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
        Future.successful(Right(Left(StatePensionExclusionFiltered(Exclusion.Dead))))
      )

      val caught = intercept[RuntimeException] {
        await(niRecordController
          .showFull(
            fakeRequest.withSession(
              SessionKeys.sessionId            -> s"session-${UUID.randomUUID()}",
              SessionKeys.lastRequestTimestamp -> LocalDate.now.toEpochDay.toString
            )
          )
        )
      }

      caught.getMessage shouldBe "NIRecordController: an unexpected error has occurred"
    }

    "Redirect to exclusion for an unexpected error in successful response" in {

      when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
        Future.successful(Right(Left(StatePensionExclusionFiltered(Exclusion.Dead))))
      )

      when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
        Future.successful(Right(Left(StatePensionExclusionFiltered(Exclusion.Dead))))
      )

      val result = niRecordController
        .showFull(
          fakeRequest.withSession(
            SessionKeys.sessionId            -> s"session-${UUID.randomUUID()}",
            SessionKeys.lastRequestTimestamp -> LocalDate.now.toEpochDay.toString
          )
        )

      redirectLocation(result) shouldBe Some("/check-your-state-pension/exclusionni")
    }

    "throw a runtime exception when receiving an unexpected error" in {
      val expectedNationalInsuranceRecord = NationalInsuranceRecord(
        28,
        -8,
        0,
        0,
        Some(LocalDate.of(1975, 8, 1)),
        false,
        LocalDate.of(2016, 4, 5),
        List(NationalInsuranceTaxYear("2015-16", true, 2430.24, 0, 0, 0, 0, None, None, false, false)),
        false
      )

      when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
        Future.successful(Right(Right(expectedNationalInsuranceRecord)))
      )

      when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
        Future.successful(Left(UpstreamErrorResponse("failed", 500)))
      )

      val caught = intercept[RuntimeException] {
        await(niRecordController
          .showFull(
            fakeRequest.withSession(
              SessionKeys.sessionId            -> s"session-${UUID.randomUUID()}",
              SessionKeys.lastRequestTimestamp -> LocalDate.now.toEpochDay.toString
            )
          )
        )
      }

      caught.getMessage shouldBe "NIRecordController: an unexpected error has occurred"
    }
  }

  "GET /account/nirecord/gapsandhowtocheck" should {

    "return how to check page for authenticated user" in {

      val expectedNationalInsuranceRecord = NationalInsuranceRecord(
        28,
        -3,
        10,
        4,
        Some(LocalDate.of(1975, 8, 1)),
        false,
        LocalDate.of(2014, 4, 5),
        List(
          NationalInsuranceTaxYear(
            "2013-14",
            false,
            0,
            0,
            0,
            0,
            704.60,
            Some(LocalDate.of(2019, 4, 5)),
            Some(LocalDate.of(2023, 4, 5)),
            true,
            false
          )
        ),
        false
      )

      when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
        Future.successful(Right(Right(expectedNationalInsuranceRecord)))
      )

      when(mockAppConfig.urBannerUrl).thenReturn("/foo")
      when(mockAppConfig.reportAProblemNonJSUrl).thenReturn("/reportAProblem")
      when(mockAppConfig.contactFormServiceIdentifier).thenReturn("/id")

      val result = niRecordController
        .showGapsAndHowToCheckThem(generateFakeRequest)
      contentAsString(result) should include("Gaps in your record and how to check them")
    }

    "return hrp message for hrp user" in {

      val expectedNationalInsuranceRecord = NationalInsuranceRecord(
        28,
        -3,
        6,
        4,
        Some(LocalDate.of(1975, 8, 1)),
        true,
        LocalDate.of(2016, 4, 5),
        List(
          NationalInsuranceTaxYear("2015-16", true, 2430.24, 0, 0, 0, 0, None, None, false, false)
        ),
        false
      )

      when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
        Future.successful(Right(Right(expectedNationalInsuranceRecord)))
      )

      when(mockAppConfig.urBannerUrl).thenReturn("/foo")
      when(mockAppConfig.reportAProblemNonJSUrl).thenReturn("/reportAProblem")
      when(mockAppConfig.contactFormServiceIdentifier).thenReturn("/id")

      val result = niRecordController.showGapsAndHowToCheckThem(generateFakeRequest)
      contentAsString(result) should include(
        "Home Responsibilities Protection (HRP) is only available for <strong>full</strong> tax years, from 6 April to 5 April, between 1978 and 2010."
      )
    }

    "do not return hrp message for non hrp user" in {

      val expectedNationalInsuranceRecord = NationalInsuranceRecord(
        28,
        -3,
        10,
        4,
        Some(LocalDate.of(1975, 8, 1)),
        false,
        LocalDate.of(2014, 4, 5),
        List(
          NationalInsuranceTaxYear(
            "2013-14",
            false,
            0,
            0,
            0,
            0,
            704.60,
            Some(LocalDate.of(2019, 4, 5)),
            Some(LocalDate.of(2023, 4, 5)),
            true,
            false
          )
        ),
        false
      )

      when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
        Future.successful(Right(Right(expectedNationalInsuranceRecord)))
      )

      when(mockAppConfig.urBannerUrl).thenReturn("/foo")
      when(mockAppConfig.reportAProblemNonJSUrl).thenReturn("/reportAProblem")
      when(mockAppConfig.contactFormServiceIdentifier).thenReturn("/id")

      val result = niRecordController.showGapsAndHowToCheckThem(generateFakeRequest)
      contentAsString(result) should not include
        "Home Responsibilities Protection (HRP) is only available for <strong>full</strong> tax years, from 6 April to 5 April, between 1978 and 2010."
    }

    "redirect to exclusion page when there is an unexpected error" in {

      when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
        Future.successful(Left(UpstreamErrorResponse("test", 500)))
      )

      when(mockAppConfig.urBannerUrl).thenReturn("/foo")
      when(mockAppConfig.reportAProblemNonJSUrl).thenReturn("/reportAProblem")
      when(mockAppConfig.contactFormServiceIdentifier).thenReturn("/id")

      val result = niRecordController.showGapsAndHowToCheckThem(generateFakeRequest)
      redirectLocation(result) shouldBe Some("/check-your-state-pension/exclusionni")
    }

    "redirect to exclusion page when there is an unexpected error in successful response" in {

      when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
        Future.successful(Right(Left(StatePensionExclusionFiltered(Exclusion.Dead))))
      )

      when(mockAppConfig.urBannerUrl).thenReturn("/foo")
      when(mockAppConfig.reportAProblemNonJSUrl).thenReturn("/reportAProblem")
      when(mockAppConfig.contactFormServiceIdentifier).thenReturn("/id")

      val result = niRecordController.showGapsAndHowToCheckThem(generateFakeRequest)
      redirectLocation(result) shouldBe Some("/check-your-state-pension/exclusionni")
    }
  }

  "GET /account/nirecord/voluntarycontribs" should {

    "return how to check page for authenticated user" in {
      when(mockAppConfig.urBannerUrl).thenReturn("/foo")
      when(mockAppConfig.reportAProblemNonJSUrl).thenReturn("/reportAProblem")
      when(mockAppConfig.contactFormServiceIdentifier).thenReturn("/id")

      val result = niRecordController.showVoluntaryContributions(generateFakeRequest)
      contentAsString(result) should include("Voluntary contributions")
    }
  }

  "GET /account/nirecord (full)" should {

    "return NI record page with details for full years - when showFullNI is true" in {
      val expectedNationalInsuranceResponse = NationalInsuranceRecord(
        28,
        -8,
        0,
        0,
        Some(LocalDate.of(1975, 8, 1)),
        false,
        LocalDate.of(2016, 4, 5),
        List(
          NationalInsuranceTaxYear("2015", false, 2430.24, 0, 0, 52, 0, None, None, false, false)
        ),
        false
      )

      val expectedStatePension = StatePension(
        LocalDate.of(2016, 4, 5),
        StatePensionAmounts(
          false,
          StatePensionAmountRegular(133.41, 580.1, 6961.14),
          StatePensionAmountForecast(3, 146.76, 638.14, 7657.73),
          StatePensionAmountMaximum(3, 0, 155.65, 676.8, 8121.59),
          StatePensionAmountRegular(0, 0, 0)
        ),
        64,
        LocalDate.of(2018, 7, 6),
        "2017",
        30,
        false,
        155.65,
        false,
        false
      )

      when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
        Future.successful(Right(Right(expectedNationalInsuranceResponse)))
      )

      when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
        Future.successful(Right(Right(expectedStatePension)))
      )

      when(mockDateProvider.currentDate).thenReturn(LocalDate.of(2016, 9, 9))
      when(mockAppConfig.showFullNI).thenReturn(true)

      val result = niRecordController.showFull(generateFakeRequest)
      contentAsString(result) should include("52 weeks")
    }

    "return NI record page with no details for full years - when showFullNI is false" in {

      val expectedNationalInsuranceResponse = NationalInsuranceRecord(
        28,
        -8,
        0,
        0,
        Some(LocalDate.of(1975, 8, 1)),
        false,
        LocalDate.of(2016, 4, 5),
        List(
          NationalInsuranceTaxYear("2015-16", true, 2430.24, 0, 0, 0, 0, None, None, false, false)
        ),
        false
      )

      val expectedStatePension = StatePension(
        LocalDate.of(2016, 4, 5),
        StatePensionAmounts(
          false,
          StatePensionAmountRegular(133.41, 580.1, 6961.14),
          StatePensionAmountForecast(3, 146.76, 638.14, 7657.73),
          StatePensionAmountMaximum(3, 0, 155.65, 676.8, 8121.59),
          StatePensionAmountRegular(0, 0, 0)
        ),
        64,
        LocalDate.of(2018, 7, 6),
        "2017-18",
        30,
        false,
        155.65,
        false,
        false
      )

      when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
        Future.successful(Right(Right(expectedNationalInsuranceResponse)))
      )

      when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
        Future.successful(Right(Right(expectedStatePension)))
      )

      when(mockDateProvider.currentDate).thenReturn(LocalDate.of(2016, 9, 9))
      when(mockAppConfig.showFullNI).thenReturn(false)

      val result = niRecordController.showFull(generateFakeRequest)
      contentAsString(result) shouldNot include("52 weeks")
    }

    "return NI record page when number of qualifying years is 0" in {

      val expectedNationalInsuranceResponse = NationalInsuranceRecord(
        0,
        0,
        0,
        0,
        Some(LocalDate.of(1975, 8, 1)),
        false,
        LocalDate.of(2016, 4, 5),
        List.empty[NationalInsuranceTaxYear],
        false
      )

      val expectedStatePension = StatePension(
        LocalDate.of(2016, 4, 5),
        StatePensionAmounts(
          false,
          StatePensionAmountRegular(133.41, 580.1, 6961.14),
          StatePensionAmountForecast(3, 146.76, 638.14, 7657.73),
          StatePensionAmountMaximum(3, 0, 155.65, 676.8, 8121.59),
          StatePensionAmountRegular(0, 0, 0)
        ),
        64,
        LocalDate.of(2018, 7, 6),
        "2017-18",
        30,
        false,
        155.65,
        false,
        false
      )

      when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
        Future.successful(Right(Right(expectedNationalInsuranceResponse)))
      )

      when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
        Future.successful(Right(Right(expectedStatePension)))
      )

      when(mockDateProvider.currentDate).thenReturn(LocalDate.of(2016, 9, 9))
      when(mockAppConfig.showFullNI).thenReturn(true)

      val result = niRecordController.showFull(generateFakeRequest)
      result.header.status shouldBe 200
    }
  }

  "GET /account/nirecord (Gaps)" should {

    "return NI record page - gap details should not show shortfall may increase messages - if current date is after 5 April 2019" in {

      val expectedNationalInsuranceResponse = NationalInsuranceRecord(
        28,
        28,
        6,
        4,
        Some(LocalDate.of(1975, 8, 1)),
        true,
        LocalDate.of(2014, 4, 5),
        List(
          NationalInsuranceTaxYear(
            "2013",
            false,
            2430.24,
            0,
            0,
            0,
            0,
            Some(LocalDate.of(2019, 4, 5)),
            Some(LocalDate.of(2024, 4, 5)),
            true,
            false
          ),
          NationalInsuranceTaxYear(
            "2012",
            false,
            2430.24,
            0,
            0,
            0,
            722.8,
            Some(LocalDate.of(2018, 4, 5)),
            Some(LocalDate.of(2023, 4, 5)),
            true,
            false
          )
        ),
        false
      )

      val expectedStatePension = StatePension(
        LocalDate.of(2014, 4, 5),
        StatePensionAmounts(
          false,
          StatePensionAmountRegular(133.41, 580.1, 6961.14),
          StatePensionAmountForecast(0, 146.76, 638.14, 7657.73),
          StatePensionAmountMaximum(50, 7, 155.65, 676.8, 8121.59),
          StatePensionAmountRegular(0, 0, 0)
        ),
        64,
        LocalDate.of(2050, 7, 6),
        "2050",
        25,
        false,
        155.65,
        false,
        false
      )

      when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
        Future.successful(Right(Right(expectedNationalInsuranceResponse)))
      )

      when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
        Future.successful(Right(Right(expectedStatePension)))
      )

      when(mockDateProvider.currentDate).thenReturn(LocalDate.of(2019, 4, 6))
      when(mockAppConfig.showFullNI).thenReturn(false)

      val result = niRecordController.showGaps(generateFakeRequest)
      contentAsString(result) should not include "shortfall may increase"
    }

    "return NI record page - gap details should show shortfall may increase messages - if current date is before 5 April 2019" in {

      val expectedNationalInsuranceResponse = NationalInsuranceRecord(
        28,
        28,
        6,
        4,
        Some(LocalDate.of(1975, 8, 1)),
        true,
        LocalDate.of(2014, 4, 5),
        List(
          NationalInsuranceTaxYear(
            "2013",
            false,
            2430.24,
            0,
            0,
            0,
            722.8,
            Some(LocalDate.of(2019, 4, 5)),
            Some(LocalDate.of(2024, 4, 5)),
            true,
            false
          ),
          NationalInsuranceTaxYear(
            "2012",
            false,
            2430.24,
            0,
            0,
            0,
            722.8,
            Some(LocalDate.of(2018, 4, 5)),
            Some(LocalDate.of(2023, 4, 5)),
            true,
            false
          )
        ),
        false
      )

      val expectedStatePension = StatePension(
        LocalDate.of(2014, 4, 5),
        StatePensionAmounts(
          false,
          StatePensionAmountRegular(133.41, 580.1, 6961.14),
          StatePensionAmountForecast(0, 146.76, 638.14, 7657.73),
          StatePensionAmountMaximum(50, 7, 155.65, 676.8, 8121.59),
          StatePensionAmountRegular(0, 0, 0)
        ),
        64,
        LocalDate.of(2050, 7, 6),
        "2050",
        25,
        false,
        155.65,
        false,
        false
      )

      when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
        Future.successful(Right(Right(expectedNationalInsuranceResponse)))
      )

      when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
        Future.successful(Right(Right(expectedStatePension)))
      )

      when(mockDateProvider.currentDate).thenReturn(LocalDate.of(2019, 4, 4))
      when(mockAppConfig.showFullNI).thenReturn(false)

      val result = niRecordController.showGaps(generateFakeRequest)

      contentAsString(result) should include("shortfall may increase")
    }

    "return NI record page - gap details should show shortfall may increase messages - if current date is same 5 April 2019" in {

      val expectedNationalInsuranceResponse = NationalInsuranceRecord(
        28,
        28,
        6,
        4,
        Some(LocalDate.of(1975, 8, 1)),
        true,
        LocalDate.of(2014, 4, 5),
        List(
          NationalInsuranceTaxYear(
            "2013",
            false,
            2430.24,
            0,
            0,
            0,
            722.8,
            Some(LocalDate.of(2019, 4, 5)),
            Some(LocalDate.of(2024, 4, 5)),
            true,
            false
          ),
          NationalInsuranceTaxYear(
            "2012",
            false,
            2430.24,
            0,
            0,
            0,
            722.8,
            Some(LocalDate.of(2018, 4, 5)),
            Some(LocalDate.of(2023, 4, 5)),
            true,
            false
          )
        ),
        false
      )

      val expectedStatePension = StatePension(
        LocalDate.of(2014, 4, 5),
        StatePensionAmounts(
          false,
          StatePensionAmountRegular(133.41, 580.1, 6961.14),
          StatePensionAmountForecast(0, 146.76, 638.14, 7657.73),
          StatePensionAmountMaximum(50, 7, 155.65, 676.8, 8121.59),
          StatePensionAmountRegular(0, 0, 0)
        ),
        64,
        LocalDate.of(2050, 7, 6),
        "2050",
        25,
        false,
        155.65,
        false,
        false
      )

      when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
        Future.successful(Right(Right(expectedNationalInsuranceResponse)))
      )

      when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
        Future.successful(Right(Right(expectedStatePension)))
      )

      when(mockDateProvider.currentDate).thenReturn(LocalDate.of(2019, 4, 5))
      when(mockAppConfig.showFullNI).thenReturn(false)

      val result = niRecordController.showGaps(generateFakeRequest)
      contentAsString(result) should include("shortfall may increase")
    }
  }

  "showPre75Years" when {

    "the date of entry is the sixteenth birthday" should {

      "return true for 5th April 1975" in {
        val date = LocalDate.of(1975, 4, 5)
        niRecordController.showPre1975Years(Some(date), date.minusYears(16), 0) shouldBe true
      }

      "return false for 6th April 1975" in {
        val date = LocalDate.of(1975, 4, 6)
        niRecordController.showPre1975Years(Some(date), date.minusYears(16), 0) shouldBe false
      }
    }

    "the date of entry and sixteenth are different" should {

      "return true for 16th: 5th April 1970, Date of entry: 5th April 1975" in {
        val dob   = LocalDate.of(1970, 4, 5).minusYears(16)
        val entry = LocalDate.of(1975, 4, 5)
        niRecordController.showPre1975Years(Some(entry), dob, 0) shouldBe true
      }

      "return true for 16th: 5th April 1970, Date of entry: 6th April 1975" in {
        val dob   = LocalDate.of(1970, 4, 5).minusYears(16)
        val entry = LocalDate.of(1975, 4, 6)
        niRecordController.showPre1975Years(Some(entry), dob, 0) shouldBe false
      }

      "return true for 16th: 5th April 1975, Date of entry: 5th April 1970" in {
        val dob   = LocalDate.of(1975, 4, 5).minusYears(16)
        val entry = LocalDate.of(1970, 4, 5)
        niRecordController.showPre1975Years(Some(entry), dob, 0) shouldBe true
      }

      "return true for 16th: 6th April 1975, Date of entry: 5th April 1970" in {
        val dob   = LocalDate.of(1975, 4, 6).minusYears(16)
        val entry = LocalDate.of(1970, 4, 5)
        niRecordController.showPre1975Years(Some(entry), dob, 0) shouldBe false
      }

      "return false for 16th: 10th July 1983, Date of Entry: 16th October 1977" in {
        val dob   = LocalDate.of(1983, 7, 10).minusYears(16)
        val entry = LocalDate.of(1977, 10, 16)
        niRecordController.showPre1975Years(Some(entry), dob, 0) shouldBe false
      }

    }

    "there is no date of entry" should {

      "return false for 16th birthday: 6th April 1975" in {
        val dob = LocalDate.of(1975, 4, 6).minusYears(16)
        niRecordController.showPre1975Years(None, dob, 0) shouldBe false
      }

      "return true for 16th birthday: 5th April 1975" in {
        val dob = LocalDate.of(1975, 4, 5).minusYears(16)
        niRecordController.showPre1975Years(None, dob, 0) shouldBe true
      }
    }

  }

  "generateTableList" when {

    "start and end are the same" should {

      "return a list with one string of that year" in {
        val start = "2015"
        val end   = "2015"
        niRecordController.generateTableList(start, end) shouldBe List("2015")
      }
    }

    "the start is less then the end" should {

      "throw an illegal argument exception" in {
        val start  = "2014 to 2015"
        val end    = "2015 to 2016"
        val caught = intercept[IllegalArgumentException] {
          niRecordController.generateTableList(start, end)
        }
        caught shouldBe a[IllegalArgumentException]
      }
    }

    "the inputs are not dates" should {

      "throw exception for start" in {
        val start  = "hello"
        val end    = "2014 to 2015"
        val caught = intercept[IllegalArgumentException] {
          niRecordController.generateTableList(start, end)
        }
        caught shouldBe a[IllegalArgumentException]
      }

      "throw exception for end" in {
        val start  = "2015-16"
        val end    = "hello"
        val caught = intercept[IllegalArgumentException] {
          niRecordController.generateTableList(start, end)
        }
        caught shouldBe a[IllegalArgumentException]
      }

    }

    "the end is greater than the start" should {

      "return a list of two adjacent dates" in {
        val start = "2016 to 2017"
        val end   = "2015 to 2016"
        niRecordController.generateTableList(start, end) shouldBe Seq("2016", "2015")
      }

      "return a list of three dates" in {
        val start = "2016-17"
        val end   = "2014-15"
        niRecordController.generateTableList(start, end) shouldBe Seq("2016", "2015", "2014")
      }

      "return a full NI Record" in {
        val start = "2016 to 2017"
        val end   = "1975 to 1976"
        niRecordController.generateTableList(start, end) shouldBe Seq(
          "2016",
          "2015",
          "2014",
          "2013",
          "2012",
          "2011",
          "2010",
          "2009",
          "2008",
          "2007",
          "2006",
          "2005",
          "2004",
          "2003",
          "2002",
          "2001",
          "2000",
          "1999",
          "1998",
          "1997",
          "1996",
          "1995",
          "1994",
          "1993",
          "1992",
          "1991",
          "1990",
          "1989",
          "1988",
          "1987",
          "1986",
          "1985",
          "1984",
          "1983",
          "1982",
          "1981",
          "1980",
          "1979",
          "1978",
          "1977",
          "1976",
          "1975"
        )
      }
    }
  }

  "redirect to State Pension page" in {

    val expectedNationalInsuranceRecord = NationalInsuranceRecord(
      40,
      39,
      2,
      1,
      Some(LocalDate.of(1973, 7, 7)),
      false,
      LocalDate.of(2016, 4, 5),
      List(
        NationalInsuranceTaxYear("2015-16", true, 12345.45, 0, 0, 0, 0, None, None, false, false),
        NationalInsuranceTaxYear(
          "2014-15",
          false,
          123,
          1,
          1,
          1,
          456.58,
          Some(LocalDate.of(2019, 4, 5)),
          Some(LocalDate.of(2023, 4, 5)),
          false,
          false
        ),
        NationalInsuranceTaxYear(
          "1999-00",
          false,
          2,
          5,
          0,
          1,
          111.11,
          Some(LocalDate.of(2019, 4, 5)),
          Some(LocalDate.of(2023, 4, 5)),
          false,
          false
        )
      ),
      false
    )

    val copeExclusionResponse =
      StatePensionExclusionFilteredWithCopeDate(CopeProcessing, LocalDate.of(2023, 4, 5), None)

    when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
      Future.successful(Right(Right(expectedNationalInsuranceRecord)))
    )

    when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
      Future.successful(Right(Left(copeExclusionResponse)))
    )

    val result: Future[Result] = niRecordController.showGaps(generateFakeRequest)

    status(result) shouldBe SEE_OTHER
  }

  "throw an exception when the response is an exclusion without finalRelevantStartYear" in {

    val expectedNationalInsuranceRecord = NationalInsuranceRecord(
      40,
      39,
      2,
      1,
      Some(LocalDate.of(1973, 7, 7)),
      false,
      LocalDate.of(2016, 4, 5),
      List(
        NationalInsuranceTaxYear("2015-16", true, 12345.45, 0, 0, 0, 0, None, None, false, false),
        NationalInsuranceTaxYear(
          "2014-15",
          false,
          123,
          1,
          1,
          1,
          456.58,
          Some(LocalDate.of(2019, 4, 5)),
          Some(LocalDate.of(2023, 4, 5)),
          false,
          false
        ),
        NationalInsuranceTaxYear(
          "1999-00",
          false,
          2,
          5,
          0,
          1,
          111.11,
          Some(LocalDate.of(2019, 4, 5)),
          Some(LocalDate.of(2023, 4, 5)),
          false,
          false
        )
      ),
      false
    )

    when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
      Future.successful(Right(Right(expectedNationalInsuranceRecord)))
    )

    when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
      Future.successful(Right(Left(StatePensionExclusionFiltered(Exclusion.Dead))))
    )

    val caught = intercept[RuntimeException] {
      await(niRecordController.showGaps(generateFakeRequest))
    }

    caught.getMessage shouldBe "NIRecordController: Can't get pensionDate from StatePensionExclusion StatePensionExclusionFiltered(Dead,None,None,None)"
  }

}
