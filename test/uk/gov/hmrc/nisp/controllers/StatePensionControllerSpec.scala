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

package uk.gov.hmrc.nisp.controllers

import java.util.UUID

import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers.{any => mockAny, eq => mockEQ}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.auth.AuthAction
import uk.gov.hmrc.nisp.controllers.pertax.PertaxHelper
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.models.enums.Exclusion
import uk.gov.hmrc.nisp.services.{NationalInsuranceService, StatePensionService}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.time.DateTimeUtils.now
import scala.concurrent.Future

class StatePensionControllerSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with GuiceOneAppPerSuite {

  val fakeRequest = FakeRequest()

  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val mockNationalInsuranceService: NationalInsuranceService = mock[NationalInsuranceService]
  val mockStatePensionService: StatePensionService = mock[StatePensionService]
  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  val mockPertaxHelper: PertaxHelper = mock[PertaxHelper]

  implicit val cachedRetriever: CachedStaticHtmlPartialRetriever = FakeCachedStaticHtmlPartialRetriever
  implicit val formPartialRetriever: FormPartialRetriever = FakePartialRetriever
  implicit val templateRenderer: TemplateRenderer = FakeTemplateRenderer

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuditConnector, mockNationalInsuranceService, mockStatePensionService, mockAppConfig, mockPertaxHelper)
  }

  def generateFakeRequest = FakeRequest().withSession(
    SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
    SessionKeys.lastRequestTimestamp -> now.getMillis.toString
  )

  val injector = GuiceApplicationBuilder()
  .overrides(
    bind[StatePensionService].toInstance(mockStatePensionService),
    bind[NationalInsuranceService].toInstance(mockNationalInsuranceService),
    bind[AuditConnector].toInstance(mockAuditConnector),
    bind[ApplicationConfig].toInstance(mockAppConfig),
    bind[PertaxHelper].toInstance(mockPertaxHelper),
    bind[CachedStaticHtmlPartialRetriever].toInstance(cachedRetriever),
    bind[FormPartialRetriever].toInstance(formPartialRetriever),
    bind[TemplateRenderer].toInstance(templateRenderer),
    bind[AuthAction].to[FakeAuthAction])
    .build()
    .injector

  val abroadUserInjector = GuiceApplicationBuilder()
    .overrides(
      bind[StatePensionService].toInstance(mockStatePensionService),
      bind[NationalInsuranceService].toInstance(mockNationalInsuranceService),
      bind[AuditConnector].toInstance(mockAuditConnector),
      bind[ApplicationConfig].toInstance(mockAppConfig),
      bind[PertaxHelper].toInstance(mockPertaxHelper),
      bind[CachedStaticHtmlPartialRetriever].toInstance(cachedRetriever),
      bind[FormPartialRetriever].toInstance(formPartialRetriever),
      bind[TemplateRenderer].toInstance(templateRenderer),
      bind[AuthAction].to[FakeAuthActionWithNino],
      bind[NinoContainer].toInstance(AbroadNinoContainer)
    )
    .build()
    .injector

  val standardNino = TestAccountBuilder.regularNino
  val foreignNino = TestAccountBuilder.abroadNino

  val statePensionController = injector.instanceOf[StatePensionController]

  val statePensionCopeResponse = StatePension(
    new LocalDate(2014, 4, 5),
    StatePensionAmounts(
      false,
      StatePensionAmountRegular(46.38, 201.67, 2420.04),
      StatePensionAmountForecast(3, 155.55, 622.35, 76022.24),
      StatePensionAmountMaximum(3, 0, 155.55, 622.35, 76022.24),
      StatePensionAmountRegular(50, 217.41, 2608.93))
    ,64, new LocalDate(2021, 7, 18), "2017-18", 30, false, 155.65, false, false)

  val statePensionResponse = StatePension(
    new LocalDate(2015, 4, 5),
    StatePensionAmounts(
      false,
      StatePensionAmountRegular(133.41, 580.1, 6961.14),
      StatePensionAmountForecast(3, 146.76, 638.14, 7657.73),
      StatePensionAmountMaximum(3, 2, 155.65, 676.8, 8121.59),
      StatePensionAmountRegular(0, 0, 0))
    ,64, new LocalDate(2018, 7, 6), "2017-18", 30, false, 155.65, false, false)

  val statePensionVariation2 = StatePension(
    new LocalDate(2015, 4, 5),
    StatePensionAmounts(
      false,
      StatePensionAmountRegular(0, 0, 0),
      StatePensionAmountForecast(1, 0, 0, 0),
      StatePensionAmountMaximum(1, 6, 155.65, 676.8, 8121.59),
      StatePensionAmountRegular(0, 0, 0))
    ,64, new LocalDate(2016, 7, 6),
    "2034-35", 30, false, 0, true, false)

  val statePensionResponseVariation3 = StatePension(
    new LocalDate(2014, 4, 5),
    StatePensionAmounts(
      false,
      StatePensionAmountRegular(133.41, 580.1, 6961.14),
      StatePensionAmountForecast(0, 146.76, 638.14, 7657.73),
      StatePensionAmountMaximum(50, 7, 155.65, 676.8, 8121.59),
      StatePensionAmountRegular(0, 0, 0))
    ,64, new LocalDate(2050, 7, 6), "2050-51", 25, false, 155.65, false, false)


  val nationalInsuranceRecord = NationalInsuranceRecord(28, -3, 6, 4, Some(new LocalDate(1975, 8, 1)),
    true, new LocalDate(2016, 4, 5),
    List(
      NationalInsuranceTaxYear("2015-16", true, 2430.24, 0, 0, 0, 0, None, None, false, false),
      NationalInsuranceTaxYear("2014-15", false, 2430.24, 0, 0, 0, 0, None, None, false, false)
    ),
    false
  )

  val nationaInsuranceRecordVariant2 = NationalInsuranceRecord(28, 28, 6, 4, Some(new LocalDate(1975, 8, 1)),
    true, new LocalDate(2014, 4, 5),
    List(
      NationalInsuranceTaxYear("2015-16", true, 2430.24, 0, 0, 0, 722.8, Some(new LocalDate(2019, 4, 5)),
        Some(new LocalDate(2024, 4, 5)), true, false),
      NationalInsuranceTaxYear("2014-15", false, 2430.24, 0, 0, 0, 722.8, Some(new LocalDate(2018, 4, 5)),
        Some(new LocalDate(2023, 4, 5)), true, false)
    ),
    false
  )

  "State Pension controller" should {

    "GET /statepension" should {

      "return the forecast only page for a user with a forecast lower than current amount" in {
        when(mockPertaxHelper.isFromPertax(mockAny())).thenReturn(false)

        when(mockNationalInsuranceService.getSummary(mockEQ(standardNino))(mockAny())).thenReturn(
          Future.successful(Right(nationalInsuranceRecord))
        )

        when(mockStatePensionService.getSummary(mockEQ(standardNino))(mockAny())).thenReturn(
          Future.successful(Right(statePensionResponse))
        )

        val result = statePensionController.show()(generateFakeRequest)
        contentAsString(result) should not include ("£80.38")
      }

      "return 200, with exclusion message for excluded user" in {
        when(mockPertaxHelper.isFromPertax(mockAny())).thenReturn(false)

        when(mockNationalInsuranceService.getSummary(mockEQ(standardNino))(mockAny())).thenReturn(
          Future.successful(Left(Exclusion.Dead))
        )

        when(mockStatePensionService.getSummary(mockEQ(standardNino))(mockAny())).thenReturn(
          Future.successful(Left(StatePensionExclusionFiltered(Exclusion.Dead)))
        )

        val result = statePensionController
          .show()(fakeRequest.withSession(
          SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
          SessionKeys.lastRequestTimestamp -> now.getMillis.toString
        ))
        redirectLocation(result) shouldBe Some("/check-your-state-pension/exclusion")
      }

      "return content about COPE for contracted out (B) user" in {
        when(mockPertaxHelper.isFromPertax(mockAny())).thenReturn(false)

        val expectedNationalInsuranceRecord = NationalInsuranceRecord(28, -8, 0, 0, Some(new LocalDate(1975, 8, 1)),
          true, new LocalDate(2016, 4, 5),
          List(
            NationalInsuranceTaxYear("2015-16", true, 2430.24, 0, 0, 0, 0, None, None, false, false),
            NationalInsuranceTaxYear("2014-15", false, 2430.24, 0, 0, 0, 0, None, None, false, false)
          ),
          false
        )

        when(mockNationalInsuranceService.getSummary(mockEQ(standardNino))(mockAny())).thenReturn(
          Future.successful(Right(expectedNationalInsuranceRecord))
        )

        when(mockStatePensionService.getSummary(mockEQ(standardNino))(mockAny())).thenReturn(
          Future.successful(Right(statePensionCopeResponse))
        )

        val result = statePensionController.show()(generateFakeRequest)
        contentAsString(result) should include("You’ve been in a contracted-out pension scheme")
      }

      "return COPE page for contracted out (B) user" in {
        when(mockPertaxHelper.isFromPertax(mockAny())).thenReturn(false)

        when(mockStatePensionService.getSummary(mockEQ(standardNino))(mockAny())).thenReturn(
          Future.successful(Right(statePensionCopeResponse))
        )

        val result = statePensionController.showCope()(generateFakeRequest)
        contentAsString(result) should include("You were contracted out")
      }

      "return abroad message for abroad user" in {
        when(mockPertaxHelper.isFromPertax(mockAny())).thenReturn(false)

        when(mockNationalInsuranceService.getSummary(mockEQ(foreignNino))(mockAny())).thenReturn(
          Future.successful(Right(nationalInsuranceRecord))
        )

        when(mockStatePensionService.getSummary(mockEQ(foreignNino))(mockAny())).thenReturn(
          Future.successful(Right(statePensionResponse))
        )

        val statePensionController = abroadUserInjector.instanceOf[StatePensionController]

        val result = statePensionController.show()(generateFakeRequest)
        contentAsString(result) should include("As you are living or working overseas")
      }

      "return /exclusion for MWRRE user" in {
        when(mockPertaxHelper.isFromPertax(mockAny())).thenReturn(false)

        when(mockNationalInsuranceService.getSummary(mockEQ(standardNino))(mockAny())).thenReturn(
          Future.successful(Left(Exclusion.MarriedWomenReducedRateElection))
        )

        when(mockStatePensionService.getSummary(mockEQ(standardNino))(mockAny())).thenReturn(
          Future.successful(Left(StatePensionExclusionFiltered(Exclusion.MarriedWomenReducedRateElection)))
        )

        val result = statePensionController.show()(generateFakeRequest)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/check-your-state-pension/exclusion")
      }

      "return abroad message for forecast only user" in {
        when(mockPertaxHelper.isFromPertax(mockAny())).thenReturn(false)

        when(mockNationalInsuranceService.getSummary(mockEQ(foreignNino))(mockAny())).thenReturn(
          Future.successful(Right(nationalInsuranceRecord))
        )

        when(mockStatePensionService.getSummary(mockEQ(foreignNino))(mockAny())).thenReturn(
          Future.successful(Right(statePensionResponse))
        )

        val statePensionController = abroadUserInjector.instanceOf[StatePensionController]

        val result = statePensionController.show()(generateFakeRequest)
        contentAsString(result) should include("As you are living or working overseas")
        contentAsString(result) should not include "£80.38"
      }

      "return abroad message for an mqp user instead of standard mqp overseas message" in {
        when(mockPertaxHelper.isFromPertax(mockAny())).thenReturn(false)

        when(mockNationalInsuranceService.getSummary(mockEQ(foreignNino))(mockAny())).thenReturn(
          Future.successful(Right(nationalInsuranceRecord))
        )

        when(mockStatePensionService.getSummary(mockEQ(foreignNino))(mockAny())).thenReturn(
          Future.successful(Right(statePensionResponse))
        )

        val statePensionController = abroadUserInjector.instanceOf[StatePensionController]

        val result = statePensionController.show()(generateFakeRequest)
        contentAsString(result) should include("As you are living or working overseas")
        contentAsString(result) should not include "If you have lived or worked overseas"
      }

      "redirect to statepension page for non contracted out user" in {
        when(mockPertaxHelper.isFromPertax(mockAny())).thenReturn(false)

        when(mockStatePensionService.getSummary(mockEQ(standardNino))(mockAny())).thenReturn(
          Future.successful(Right(statePensionVariation2))
        )

        val result = statePensionController.showCope()(generateFakeRequest)
        redirectLocation(result) shouldBe Some("/check-your-state-pension/account")
      }

      "return page with MQP messaging for MQP user" in {
        when(mockPertaxHelper.isFromPertax(mockAny())).thenReturn(false)

        val expectedNationalInsuranceRecord = NationalInsuranceRecord(3, 3, 19, 7, Some(new LocalDate(1975, 8, 1)),
          false, new LocalDate(2014, 4, 5),
          List.empty[NationalInsuranceTaxYear],
          false
        )

        when(mockNationalInsuranceService.getSummary(mockEQ(standardNino))(mockAny())).thenReturn(
          Future.successful(Right(expectedNationalInsuranceRecord))
        )

        when(mockStatePensionService.getSummary(mockEQ(standardNino))(mockAny())).thenReturn(
          Future.successful(Right(statePensionVariation2))
        )

        val result = statePensionController.show()(generateFakeRequest)
        contentAsString(result) should include("10 years needed on your National Insurance record to get any State Pension")
      }
    }

    "when there is a Fill Gaps Scenario" when {
      "the future config is set to off" should {
        "show year information when there is multiple years" in {
          when(mockPertaxHelper.isFromPertax(mockAny())).thenReturn(false)

          when(mockNationalInsuranceService.getSummary(mockEQ(standardNino))(mockAny())).thenReturn(
            Future.successful(Right(nationaInsuranceRecordVariant2))
          )

          when(mockStatePensionService.getSummary(mockEQ(standardNino))(mockAny())).thenReturn(
            Future.successful(Right(statePensionResponseVariation3))
          )

          val result = statePensionController.show()(generateFakeRequest)
          contentAsString(result) should include("You have years on your National Insurance record where you did not contribute enough.")
          contentAsString(result) should include("filling years can improve your forecast")
          contentAsString(result) should include("you only need to fill 7 years to get the most you can")
          contentAsString(result) should include("The most you can get by filling any 7 years in your record is")
        }
        "show specific text when is only one payable gap" in {
          when(mockPertaxHelper.isFromPertax(mockAny())).thenReturn(false)

          val expectedNationalInsuranceRecord = NationalInsuranceRecord(28, 28, 1, 1, Some(new LocalDate(1975, 8, 1)),
            true, new LocalDate(2014, 4, 5),
            List(
              NationalInsuranceTaxYear("2013-14", false, 2430.24, 0, 0, 0, 722.8, Some(new LocalDate(2019, 4, 5)),
                Some(new LocalDate(2024, 4, 5)), true, false),
              NationalInsuranceTaxYear("2012-13", false, 2430.24, 0, 0, 0, 722.8, Some(new LocalDate(2018, 4, 5)),
                Some(new LocalDate(2023, 4, 5)), true, false)
            ),
            false
          )

          when(mockNationalInsuranceService.getSummary(mockEQ(standardNino))(mockAny())).thenReturn(
            Future.successful(Right(expectedNationalInsuranceRecord))
          )

          when(mockStatePensionService.getSummary(mockEQ(standardNino))(mockAny())).thenReturn(
            Future.successful(Right(statePensionResponseVariation3))
          )

          val result = statePensionController.show()(generateFakeRequest)
          contentAsString(result) should include("You have a year on your National Insurance record where you did not contribute enough. You only need to fill this year to get the most you can.")
          contentAsString(result) should include("The most you can get by filling this year in your record is")
        }
      }

      "the future proof config is set to true" should {
        "show new personal max text when there are multiple/single year" in {
          when(mockPertaxHelper.isFromPertax(mockAny())).thenReturn(false)
          when(mockAppConfig.futureProofPersonalMax).thenReturn(true)

          when(mockNationalInsuranceService.getSummary(mockEQ(standardNino))(mockAny())).thenReturn(
            Future.successful(Right(nationaInsuranceRecordVariant2))
          )

          when(mockStatePensionService.getSummary(mockEQ(standardNino))(mockAny())).thenReturn(
            Future.successful(Right(statePensionResponseVariation3))
          )

          val result = statePensionController.show()(generateFakeRequest)
          contentAsString(result) should include("You have shortfalls in your National Insurance record that you can fill and make count towards your State Pension.")
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

        val result = statePensionController.signOut(fakeRequest)
        redirectLocation(result).get shouldBe "/foo"
      }
    }

  }
}
