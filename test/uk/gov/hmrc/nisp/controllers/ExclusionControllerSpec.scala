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

import java.time.LocalDate
import java.util.UUID

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
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.nisp.controllers.auth.ExcludedAuthAction
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.models.{Exclusion, _}
import uk.gov.hmrc.nisp.services.{NationalInsuranceService, StatePensionService}
import uk.gov.hmrc.nisp.utils.UnitSpec

import scala.concurrent.Future

class ExclusionControllerSpec extends UnitSpec with GuiceOneAppPerSuite with Injecting with BeforeAndAfterEach {

  val fakeRequest                  = FakeRequest()
  val mockStatePensionService      = mock[StatePensionService]
  val mockNationalInsuranceService = mock[NationalInsuranceService]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[StatePensionService].toInstance(mockStatePensionService),
      bind[NationalInsuranceService].toInstance(mockNationalInsuranceService),
      bind[ExcludedAuthAction].to[FakeExcludedAuthAction]
    )
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockStatePensionService, mockNationalInsuranceService)
  }

  def authId(username: String): String = s"/auth/oid/$username"

  val testExclusionController = inject[ExclusionController]

  val deadMessaging = "Please contact HMRC National Insurance helpline on 0300 200 3500."
  val mciMessaging  = "We need to talk to you about an MCI error before you sign in."

  val postSPAMessagingText1     = "If you have not already started"
  val postSPAMessagingHref1     = "href=\"https://www.gov.uk/claim-state-pension-online\""
  val postSPAMessagingLinkText1 = "claiming your State Pension"
  val postSPAMessagingHref2     = "href=\"https://www.gov.uk/deferring-state-pension\""
  val postSPAMessagingLinkText2 = "put off claiming your State Pension (opens in new tab)"
  val postSPAMessagingText2     = "and this may mean you get extra State Pension when you do want to claim it."

  val dissonanceMessaging  =
    "We’re unable to calculate your State Pension forecast at the moment and we’re working on fixing this."
  val isleOfManMessagingSP =
    "We’re unable to calculate your State Pension, as the Isle of Man Government is currently undertaking a review of its Retirement Pension scheme."
  val isleOfManMessagingNI =
    "We’re unable to show your National Insurance record as you have contributions from the Isle of Man."

  val mwrreMessagingSP         = "We’re unable to calculate your State Pension forecast as you have"
  val mwrreMessagingSPHref     = "href=\"https://www.gov.uk/reduced-national-insurance-married-women\""
  val mwrreMessagingSPLinkText = "paid a reduced rate of National Insurance as a married woman (opens in new tab)"

  val mwrreMessagingNI         = "We’re currently unable to show your National Insurance Record as you have"
  val mwrreMessagingNIHref     = "href=\"https://www.gov.uk/reduced-national-insurance-married-women\""
  val mwrreMessagingNILinkText = "paid a reduced rate of National Insurance as a married woman (opens in new tab)"

  val abroadMessaging                = "We’re unable to calculate your UK State Pension forecast as you’ve lived or worked abroad."
  val spaUnderConsiderationMessaging = "Proposed change to your State Pension age"
  val copeProcessingHeader           = "Sorry, we are unable to calculate your forecast at the moment"
  val copeProcessingExtendedHeader   = "Sorry, we are still working on updates to your forecast"
  val copeFailedHeader               = "Sorry, we cannot show your forecast online"

  "GET /exclusion" should {

    "return redirect to account page for non-excluded user" in {

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
          ),
          NationalInsuranceTaxYear(
            "2012-13",
            true,
            0,
            0,
            0,
            52,
            689,
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

      val result = testExclusionController.showSP()(
        fakeRequest.withSession(
          SessionKeys.sessionId            -> s"session-${UUID.randomUUID()}",
          SessionKeys.lastRequestTimestamp -> LocalDate.now.toEpochDay.toString
        )
      )

      redirectLocation(result) shouldBe Some("/check-your-state-pension/account")
    }

    "Exclusion Controller" when {

      def generateSPRequest: Future[Result] =
        testExclusionController.showSP()(
          fakeRequest.withSession(
            SessionKeys.sessionId            -> s"session-${UUID.randomUUID()}",
            SessionKeys.lastRequestTimestamp -> LocalDate.now.toEpochDay.toString
          )
        )

      def generateNIRequest: Future[Result] =
        testExclusionController.showNI()(
          fakeRequest.withSession(
            SessionKeys.sessionId            -> s"session-${UUID.randomUUID()}",
            SessionKeys.lastRequestTimestamp -> LocalDate.now.toEpochDay.toString
          )
        )

      "The User has every exclusion" should {
        "return only the Dead Exclusion on /exclusion" in {

          when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
            Future.successful(Right(Left(StatePensionExclusionFiltered(Exclusion.Dead))))
          )

          when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
            Future.successful(Right(Left(StatePensionExclusionFiltered(Exclusion.Dead))))
          )

          val result = generateSPRequest
          redirectLocation(result) shouldBe None
          contentAsString(result)    should include(deadMessaging)
          contentAsString(result)    should not include mciMessaging
          contentAsString(result)    should not include postSPAMessagingText1
          contentAsString(result)    should not include dissonanceMessaging
          contentAsString(result)    should not include isleOfManMessagingSP
          contentAsString(result)    should not include mwrreMessagingSP
          contentAsString(result)    should not include abroadMessaging
        }

        "return only the Dead Exclusion on /exclusionni" in {
          when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
            Future.successful(Right(Left(StatePensionExclusionFiltered(Exclusion.Dead))))
          )

          val result = generateNIRequest
          redirectLocation(result) shouldBe None
          contentAsString(result)    should include(deadMessaging)
          contentAsString(result)    should not include mciMessaging
          contentAsString(result)    should not include isleOfManMessagingNI
          contentAsString(result)    should not include mwrreMessagingNI
        }
      }

      "The User has every exclusion except Dead" should {
        "return only the MCI Exclusion on /exclusion" in {
          when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
            Future.successful(Right(Left(StatePensionExclusionFiltered(Exclusion.ManualCorrespondenceIndicator))))
          )

          when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
            Future.successful(Right(Left(StatePensionExclusionFiltered(Exclusion.ManualCorrespondenceIndicator))))
          )

          val result = generateSPRequest
          redirectLocation(result) shouldBe None
          contentAsString(result)    should not include deadMessaging
          contentAsString(result)    should include(mciMessaging)
          contentAsString(result)    should not include postSPAMessagingText1
          contentAsString(result)    should not include dissonanceMessaging
          contentAsString(result)    should not include isleOfManMessagingSP
          contentAsString(result)    should not include mwrreMessagingSP
          contentAsString(result)    should not include abroadMessaging
        }

        "return only the MCI Exclusion on /exclusionni" in {
          when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
            Future.successful(Right(Left(StatePensionExclusionFiltered(Exclusion.ManualCorrespondenceIndicator))))
          )

          val result = generateNIRequest
          redirectLocation(result) shouldBe None
          contentAsString(result)    should not include deadMessaging
          contentAsString(result)    should include(mciMessaging)
          contentAsString(result)    should not include isleOfManMessagingNI
          contentAsString(result)    should not include mwrreMessagingNI
        }
      }

      "The User has every exclusion except Dead and MCI" should {
        "return only the Post SPA Exclusion on /exclusion" in {
          when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
            Future.successful(Right(Left(StatePensionExclusionFiltered(Exclusion.IsleOfMan))))
          )

          when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
            Future.successful(Right(Left(StatePensionExclusionFiltered(Exclusion.PostStatePensionAge, Some(65),
              Some(LocalDate.of(2017, 7, 18)), Some(false))))
          ))

          val result = generateSPRequest
          redirectLocation(result) shouldBe None
          contentAsString(result)    should not include deadMessaging
          contentAsString(result)    should not include mciMessaging
          contentAsString(result)    should include(postSPAMessagingText1)
          contentAsString(result)    should include(postSPAMessagingHref1)
          contentAsString(result)    should include(postSPAMessagingLinkText1)
          contentAsString(result)    should include(postSPAMessagingHref2)
          contentAsString(result)    should include(postSPAMessagingLinkText2)
          contentAsString(result)    should include(postSPAMessagingText2)
          contentAsString(result)    should not include dissonanceMessaging
          contentAsString(result)    should not include isleOfManMessagingSP
          contentAsString(result)    should not include mwrreMessagingSP
          contentAsString(result)    should not include abroadMessaging
        }

        "return only the Isle of Man Exclusion on /exclusionni" in {
          when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
            Future.successful(Right(Left(StatePensionExclusionFiltered(Exclusion.IsleOfMan))))
          )

          val result = generateNIRequest
          redirectLocation(result) shouldBe None
          contentAsString(result)    should not include deadMessaging
          contentAsString(result)    should not include mciMessaging
          contentAsString(result)    should include(isleOfManMessagingNI)
          contentAsString(result)    should not include mwrreMessagingNI
        }
      }

      "The User has every exclusion except Dead, MCI and Post SPA" should {
        "return only the Amount Dissonance Exclusion on /exclusion" in {
          when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
            Future.successful(Right(Left(StatePensionExclusionFiltered(Exclusion.IsleOfMan))))
          )

          when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
            Future.successful(Right(Left(StatePensionExclusionFiltered(Exclusion.AmountDissonance, Some(65),
              Some(LocalDate.of(2017, 7, 18)), Some(true))))
          ))

          val result = generateSPRequest
          redirectLocation(result) shouldBe None
          contentAsString(result)    should not include deadMessaging
          contentAsString(result)    should not include mciMessaging
          contentAsString(result)    should not include postSPAMessagingText1
          contentAsString(result)    should include(dissonanceMessaging)
          contentAsString(result)    should not include isleOfManMessagingSP
          contentAsString(result)    should not include mwrreMessagingSP
          contentAsString(result)    should not include abroadMessaging
        }

        "return only the Isle of Man Exclusion on /exclusionni" in {
          when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
            Future.successful(Right(Left(StatePensionExclusionFiltered(Exclusion.IsleOfMan))))
          )

          val result = generateNIRequest
          redirectLocation(result) shouldBe None
          contentAsString(result)    should not include deadMessaging
          contentAsString(result)    should not include mciMessaging
          contentAsString(result)    should include(isleOfManMessagingNI)
          contentAsString(result)    should not include mwrreMessagingNI
        }
      }

      "The User has the Isle of Man, MWRRE and Abroad exclusions" should {
        "return only the Isle of Man Exclusion on /exclusion" in {
          when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
            Future.successful(Right(Left(StatePensionExclusionFiltered(Exclusion.IsleOfMan))))
          )

          when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
            Future.successful(Right(Left(StatePensionExclusionFiltered(Exclusion.IsleOfMan, Some(65), Some(LocalDate.of(2017, 7, 18))
              , Some(true))))
          ))

          val result = generateSPRequest
          redirectLocation(result) shouldBe None
          contentAsString(result)    should not include deadMessaging
          contentAsString(result)    should not include mciMessaging
          contentAsString(result)    should not include postSPAMessagingText1
          contentAsString(result)    should not include dissonanceMessaging
          contentAsString(result)    should include(isleOfManMessagingSP)
          contentAsString(result)    should not include mwrreMessagingSP
          contentAsString(result)    should not include abroadMessaging
        }

        "return only the Isle of Man Exclusion on /exclusionni" in {
          when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
            Future.successful(Right(Left(StatePensionExclusionFiltered(Exclusion.IsleOfMan))))
          )

          val result = generateNIRequest
          redirectLocation(result) shouldBe None
          contentAsString(result)    should not include deadMessaging
          contentAsString(result)    should not include mciMessaging
          contentAsString(result)    should include(isleOfManMessagingNI)
          contentAsString(result)    should not include mwrreMessagingNI
        }
      }

      "The User has MWRRE and Abroad exclusions" should {
        "return only the MWRRE Exclusion on /exclusion" in {
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
            "2050-51",
            25,
            false,
            155.65,
            true,
            false
          )

          when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
            Future.successful(Right(Left(StatePensionExclusionFiltered(Exclusion.MarriedWomenReducedRateElection))))
          )

          when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
            Future.successful(Right(Right(expectedStatePension)))
          )

          val result = generateSPRequest
          redirectLocation(result) shouldBe None
          contentAsString(result)    should not include deadMessaging
          contentAsString(result)    should not include mciMessaging
          contentAsString(result)    should not include postSPAMessagingText1
          contentAsString(result)    should not include dissonanceMessaging
          contentAsString(result)    should not include isleOfManMessagingSP
          contentAsString(result)    should include(mwrreMessagingSP)
          contentAsString(result)    should include(mwrreMessagingSPHref)
          contentAsString(result)    should include(mwrreMessagingSPLinkText)
          contentAsString(result)    should not include abroadMessaging
        }

        "return only the MWRRE Exclusion on /exclusionni" in {
          when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
            Future.successful(Right(Left(StatePensionExclusionFiltered(Exclusion.MarriedWomenReducedRateElection))))
          )

          val result = generateNIRequest
          redirectLocation(result) shouldBe None
          contentAsString(result)    should not include deadMessaging
          contentAsString(result)    should not include mciMessaging
          contentAsString(result)    should not include isleOfManMessagingNI
          contentAsString(result)    should include(mwrreMessagingNI)
          contentAsString(result)    should include(mwrreMessagingNIHref)
          contentAsString(result)    should include(mwrreMessagingNILinkText)
        }
      }

      "The User has MWRRE exclusion" should {

        "return only the MWRRE Exclusion on /exclusion" in {
          val expectedStatePension = StatePension(
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
            true,
            false
          )

          when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
            Future.successful(Right(Right(expectedStatePension)))
          )

          when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
            Future.successful(Right(Left(StatePensionExclusionFiltered(Exclusion.MarriedWomenReducedRateElection))))
          )

          val result = testExclusionController.showSP()(
            fakeRequest.withSession(
              SessionKeys.sessionId            -> s"session-${UUID.randomUUID()}",
              SessionKeys.lastRequestTimestamp -> LocalDate.now.toEpochDay.toString
            )
          )

          redirectLocation(result) shouldBe None
          contentAsString(result)    should not include deadMessaging
          contentAsString(result)    should not include mciMessaging
          contentAsString(result)    should not include isleOfManMessagingSP
          contentAsString(result)    should include(mwrreMessagingSP)
          contentAsString(result)    should include(mwrreMessagingSPHref)
          contentAsString(result)    should include(mwrreMessagingSPLinkText)
        }

        "return only the MWRRE Exclusion on /exclusionni" in {
          when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
            Future.successful(Right(Left(StatePensionExclusionFiltered(Exclusion.MarriedWomenReducedRateElection))))
          )

          val result = generateNIRequest
          redirectLocation(result) shouldBe None
          contentAsString(result)    should not include deadMessaging
          contentAsString(result)    should not include mciMessaging
          contentAsString(result)    should not include isleOfManMessagingNI
          contentAsString(result)    should include(mwrreMessagingNI)
          contentAsString(result)    should include(mwrreMessagingNIHref)
          contentAsString(result)    should include(mwrreMessagingNILinkText)
        }
      }

      "The User has SPA under consideration flag and Amount Dis exclusion" should {
        "return with SPA under consideration message" in {
          val expectedStatePensionResponse = StatePensionExclusionFiltered(
            Exclusion.AmountDissonance,
            Some(65),
            Some(LocalDate.of(2017, 7, 18)),
            Some(true)
          )

          val expectedNationalInsuranceResponse = NationalInsuranceRecord(
            28,
            28,
            10,
            4,
            Some(LocalDate.of(1975, 8, 1)),
            false,
            LocalDate.of(2014, 4, 5),
            List.empty[NationalInsuranceTaxYear],
            false
          )

          when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
            Future.successful(Right(Left(expectedStatePensionResponse)))
          )

          when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
            Future.successful(Right(Right(expectedNationalInsuranceResponse)))
          )

          val result = generateSPRequest
          redirectLocation(result) shouldBe None
          contentAsString(result)    should include(spaUnderConsiderationMessaging)
        }
      }

      "The User has SPA under consideration flag and IoM exclusion" should {
        "return with SPA under consideration message" in {

          val statePensionResponse =
            StatePensionExclusionFiltered(Exclusion.IsleOfMan, Some(65), Some(LocalDate.of(2017, 7, 18)), Some(true))

          val expectedNationalInsuranceResponse = NationalInsuranceRecord(
            28,
            28,
            10,
            4,
            Some(LocalDate.of(1975, 8, 1)),
            false,
            LocalDate.of(2014, 4, 5),
            List.empty[NationalInsuranceTaxYear],
            false
          )

          when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
            Future.successful(Right(Left(statePensionResponse)))
          )

          when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
            Future.successful(Right(Right(expectedNationalInsuranceResponse)))
          )

          val result = generateSPRequest
          redirectLocation(result) shouldBe None
          contentAsString(result)    should include(spaUnderConsiderationMessaging)
        }
      }

      "The User has SPA under consideration flag and Mwrre exclusion" should {
        "return with no SPA under consideration message" in {

          val statePensionResponse = StatePensionExclusionFiltered(
            Exclusion.MarriedWomenReducedRateElection,
            Some(65),
            Some(LocalDate.of(2017, 7, 18)),
            Some(true)
          )

          val expectedNationalInsuranceRecord = NationalInsuranceRecord(
            28,
            28,
            10,
            4,
            Some(LocalDate.of(1975, 8, 1)),
            false,
            LocalDate.of(2014, 4, 5),
            List.empty[NationalInsuranceTaxYear],
            false
          )

          when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
            Future.successful(Right(Left(statePensionResponse)))
          )

          when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
            Future.successful(Right(Right(expectedNationalInsuranceRecord)))
          )

          val result = generateSPRequest
          redirectLocation(result) shouldBe None
          contentAsString(result)    should not include spaUnderConsiderationMessaging
        }
      }

      "The User has SPA under consideration flag and Over Spa exclusion" should {
        "return with no SPA under consideration message" in {

          val statePensionResponse = StatePensionExclusionFiltered(
            Exclusion.PostStatePensionAge,
            Some(65),
            Some(LocalDate.of(2017, 7, 18)),
            Some(true)
          )

          val nationalInsuranceRecord = NationalInsuranceRecord(
            28,
            28,
            10,
            4,
            Some(LocalDate.of(1975, 8, 1)),
            false,
            LocalDate.of(2014, 4, 5),
            List.empty[NationalInsuranceTaxYear],
            false
          )

          when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
            Future.successful(Right(Left(statePensionResponse)))
          )

          when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
            Future.successful(Right(Right(nationalInsuranceRecord)))
          )

          val result = generateSPRequest
          redirectLocation(result) shouldBe None
          contentAsString(result)    should not include spaUnderConsiderationMessaging
        }
      }

      "The User has SPA under consideration flag and Multiple exclusions with Over SPA first" should {
        "return with no SPA under consideration message" in {
          val statePensionResponse = StatePensionExclusionFiltered(
            Exclusion.PostStatePensionAge,
            Some(65),
            Some(LocalDate.of(2017, 7, 18)),
            Some(true)
          )

          val nationalInsuranceRecord = NationalInsuranceRecord(
            28,
            28,
            10,
            4,
            Some(LocalDate.of(1975, 8, 1)),
            false,
            LocalDate.of(2014, 4, 5),
            List.empty[NationalInsuranceTaxYear],
            false
          )

          when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
            Future.successful(Right(Left(statePensionResponse)))
          )

          when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
            Future.successful(Right(Right(nationalInsuranceRecord)))
          )

          val result = generateSPRequest
          redirectLocation(result) shouldBe None
          contentAsString(result)    should not include spaUnderConsiderationMessaging
        }
      }

      "The User has no SPA under consideration flag and exclusion" should {
        "return with no SPA under consideration message" in {
          val statePensionResponse =
            StatePensionExclusionFiltered(Exclusion.IsleOfMan, Some(65), Some(LocalDate.of(2017, 7, 18)), None)

          val nationalInsuranceRecord = NationalInsuranceRecord(
            28,
            28,
            10,
            4,
            Some(LocalDate.of(1975, 8, 1)),
            false,
            LocalDate.of(2014, 4, 5),
            List.empty[NationalInsuranceTaxYear],
            false
          )

          when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
            Future.successful(Right(Left(statePensionResponse)))
          )

          when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
            Future.successful(Right(Right(nationalInsuranceRecord)))
          )

          val result = generateSPRequest
          redirectLocation(result) shouldBe None
          contentAsString(result)    should not include spaUnderConsiderationMessaging
        }
      }

      "The user has COPE Processing exclusion" should {

        "return the COPE Processing Exclusion on /exclusion" in {
          val statePensionCopeProcessingResponse = StatePensionExclusionFilteredWithCopeDate(
            exclusion = Exclusion.CopeProcessing,
            copeDataAvailableDate = LocalDate.of(2017, 7, 18),
            previousAvailableDate = None
          )

          val nationalInsuranceRecord = NationalInsuranceRecord(
            28,
            28,
            10,
            4,
            Some(LocalDate.of(1975, 8, 1)),
            false,
            LocalDate.of(2014, 4, 5),
            List.empty[NationalInsuranceTaxYear],
            false
          )

          when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
            Future.successful(Right(Left(statePensionCopeProcessingResponse)))
          )

          when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
            Future.successful(Right(Right(nationalInsuranceRecord))))

          val result = testExclusionController.showSP()(FakeRequest())

          status(result)        shouldBe OK
          contentAsString(result) should include(copeProcessingHeader)
        }
      }

      "return the COPE Processing Extended Exclusion on /exclusion" in {
        val statePensionCopeProcessingExtendedResponse = StatePensionExclusionFilteredWithCopeDate(
          exclusion = Exclusion.CopeProcessing,
          copeDataAvailableDate = LocalDate.of(2017, 7, 28),
          previousAvailableDate = Some(LocalDate.of(2017, 7, 18))
        )

        val nationalInsuranceRecord = NationalInsuranceRecord(
          28,
          28,
          10,
          4,
          Some(LocalDate.of(1975, 8, 1)),
          false,
          LocalDate.of(2014, 4, 5),
          List.empty[NationalInsuranceTaxYear],
          false
        )

        when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
          Future.successful(Right(Left(statePensionCopeProcessingExtendedResponse)))
        )

        when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
          Future.successful(Right(Right(nationalInsuranceRecord))))

        val result = testExclusionController.showSP()(FakeRequest())

        status(result)        shouldBe OK
        contentAsString(result) should include(copeProcessingExtendedHeader)
      }
    }

    "The user has COPE Failed exclusion" should {

      "return the COPE Failed Exclusion on /exclusion" in {
        val statePensionCopeFailedResponse = StatePensionExclusionFiltered(
          exclusion = Exclusion.CopeProcessingFailed
        )

        val nationalInsuranceRecord = NationalInsuranceRecord(
          28,
          28,
          10,
          4,
          Some(LocalDate.of(1975, 8, 1)),
          false,
          LocalDate.of(2014, 4, 5),
          List.empty[NationalInsuranceTaxYear],
          false
        )

        when(mockStatePensionService.getSummary(mockEQ(TestAccountBuilder.regularNino), mockAny())(mockAny())).thenReturn(
          Future.successful(Right(Left(statePensionCopeFailedResponse)))
        )

        when(mockNationalInsuranceService.getSummary(mockEQ(TestAccountBuilder.regularNino))(mockAny())).thenReturn(
          Future.successful(Right(Right(nationalInsuranceRecord))))

        val result = testExclusionController.showSP()(FakeRequest())

        status(result)        shouldBe OK
        contentAsString(result) should include(copeFailedHeader)
      }

    }
  }
}
