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

package uk.gov.hmrc.nisp.services

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Injecting
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.nisp.connectors.NationalInsuranceConnector
import uk.gov.hmrc.nisp.models.StatePensionExclusion.{CopeStatePensionExclusion, ForbiddenStatePensionExclusion}
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.utils.UnitSpec

import java.time.LocalDate
import scala.concurrent.Future

class NationalInsuranceServiceSpec
  extends UnitSpec
    with ScalaFutures
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach
    with Injecting {

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
  val mockNationalInsuranceConnector: NationalInsuranceConnector = mock[NationalInsuranceConnector]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[NationalInsuranceConnector].toInstance(mockNationalInsuranceConnector)
    ).build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockNationalInsuranceConnector)
  }

  val nationalInsuranceService: NationalInsuranceService = inject[NationalInsuranceService]

  "NationalInsuranceConnection" when {

    "There is a successful response" should {

      val mockNationalInsuranceRecord = NationalInsuranceRecord(
        qualifyingYears = 40,
        qualifyingYearsPriorTo1975 = 39,
        numberOfGaps = 2,
        numberOfGapsPayable = 1,
        Some(LocalDate.of(1973, 7, 7)),
        homeResponsibilitiesProtection = false,
        earningsIncludedUpTo = LocalDate.of(2016, 4, 5),
        taxYears = List(
          NationalInsuranceTaxYear(
            taxYear = "2015",
            qualifying = true,
            classOneContributions = 12345.45,
            classTwoCredits = 0,
            classThreeCredits = 0,
            otherCredits = 0,
            classThreePayable = 0,
            classThreePayableBy = None,
            classThreePayableByPenalty = None,
            payable = false,
            underInvestigation = false
          ),
          NationalInsuranceTaxYear(
            taxYear = "2014",
            qualifying = false,
            classOneContributions = 123,
            classTwoCredits = 1,
            classThreeCredits = 1,
            otherCredits = 1,
            classThreePayable = 456.58,
            classThreePayableBy = Some(LocalDate.of(2019, 4, 5)),
            classThreePayableByPenalty = Some(LocalDate.of(2023, 4, 5)),
            payable = false,
            underInvestigation = false
          ),
          NationalInsuranceTaxYear(
            taxYear = "1999",
            qualifying = false,
            classOneContributions = 2,
            classTwoCredits = 5,
            classThreeCredits = 0,
            otherCredits = 1,
            classThreePayable = 111.11,
            classThreePayableBy = Some(LocalDate.of(2019, 4, 5)),
            classThreePayableByPenalty = Some(LocalDate.of(2023, 4, 5)),
            payable = false,
            underInvestigation = false
          )
        ),
        reducedRateElection = false
      )

      "return a Right(NationalInsuranceRecord)" in {
        when(mockNationalInsuranceConnector.getNationalInsurance(any(), any())(any()))
          .thenReturn(Future.successful(Right(Right(mockNationalInsuranceRecord))))

        nationalInsuranceService.getSummary(generateNino).isRight shouldBe true
        nationalInsuranceService.getSummary(generateNino).toOption.get.map {
          nir =>
            nir shouldBe a[NationalInsuranceRecord]
        }
      }

      "return unmodified data" in {

        when(mockNationalInsuranceConnector.getNationalInsurance(any(), any())(any()))
          .thenReturn(Future.successful(Right(Right(mockNationalInsuranceRecord))))

        whenReady(nationalInsuranceService.getSummary(generateNino)) { record =>
          record shouldBe Right(Right(mockNationalInsuranceRecord))
        }
      }

      "return the tax years in descending order" in {

        val jumbledRecord = mockNationalInsuranceRecord.copy(taxYears =
          List(
            NationalInsuranceTaxYear(
              taxYear = "2014-15",
              qualifying = false,
              classOneContributions = 123,
              classTwoCredits = 1,
              classThreeCredits = 1,
              otherCredits = 1,
              classThreePayable = 456.58,
              classThreePayableBy = Some(LocalDate.of(2019, 4, 5)),
              classThreePayableByPenalty = Some(LocalDate.of(2023, 4, 5)),
              payable = false,
              underInvestigation = false
            ),
            NationalInsuranceTaxYear(
              taxYear = "1999-00",
              qualifying = false,
              classOneContributions = 2,
              classTwoCredits = 5,
              classThreeCredits = 0,
              otherCredits = 1,
              classThreePayable = 111.11,
              classThreePayableBy = Some(LocalDate.of(2019, 4, 5)),
              classThreePayableByPenalty = Some(LocalDate.of(2023, 4, 5)),
              payable = false,
              underInvestigation = false
            ),
            NationalInsuranceTaxYear(
              taxYear = "2015-16",
              qualifying = true,
              classOneContributions = 12345.45,
              classTwoCredits = 0,
              classThreeCredits = 0,
              otherCredits = 0,
              classThreePayable = 0,
              classThreePayableBy = None,
              classThreePayableByPenalty = None,
              payable = false,
              underInvestigation = false
            )
          )
        )

        when(mockNationalInsuranceConnector.getNationalInsurance(any(), any())(any()))
          .thenReturn(Future.successful(Right(Right(jumbledRecord))))

        whenReady(nationalInsuranceService.getSummary(generateNino)) {
          result: Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]] =>
            result.map(_.map(_.taxYears shouldBe mockNationalInsuranceRecord.taxYears))
        }

      }

    }

    "There is failed future from a Dead exclusion" should {
      "return a Dead Exclusion" in {

        when(mockNationalInsuranceConnector.getNationalInsurance(any(), any())(any()))
          .thenReturn(Future.successful(
            Right(Left(ForbiddenStatePensionExclusion(Exclusion.Dead, Some("The customer needs to contact the National Insurance helpline")))))
          )

        val result = await(nationalInsuranceService.getSummary(generateNino))

        result.map {
          excl =>
            val exclusion = excl.swap.getOrElse(StatePensionExclusionFiltered)

            exclusion shouldBe StatePensionExclusionFiltered(Exclusion.Dead)
        }
      }
    }

    "There is failed future from a MCI exclusion" should {
      "return a Left MCI Exclusion" in {

        when(mockNationalInsuranceConnector.getNationalInsurance(any(), any())(any()))
          .thenReturn(Future.successful(
            Right(Left(ForbiddenStatePensionExclusion(Exclusion.ManualCorrespondenceIndicator, Some("The customer cannot access the service, they should contact HMRC")))))
          )

        val result = await(nationalInsuranceService.getSummary(generateNino))

        result.map {
          excl =>
            val exclusion = excl.swap.getOrElse(StatePensionExclusionFiltered)

            exclusion shouldBe StatePensionExclusionFiltered(Exclusion.ManualCorrespondenceIndicator)
        }
      }
    }

    "There is failed future from a Isle of Man exclusion" should {
      "return a Left Isle of Man Exclusion" in {

        when(mockNationalInsuranceConnector.getNationalInsurance(any(), any())(any()))
          .thenReturn(Future.successful(
            Right(Left(ForbiddenStatePensionExclusion(Exclusion.IsleOfMan, Some("The customer cannot access the service, they should contact HMRC")))))
          )

        val result = await(nationalInsuranceService.getSummary(generateNino))

        result.map {
          excl =>
            val exclusion = excl.swap.getOrElse(StatePensionExclusionFiltered)

            exclusion shouldBe StatePensionExclusionFiltered(Exclusion.IsleOfMan)
        }
      }
    }

    "There is failed future from a MWRRE exclusion" should {
      "return a Left MWRRE Exclusion" in {

        when(mockNationalInsuranceConnector.getNationalInsurance(any(), any())(any()))
          .thenReturn(Future.successful(
            Right(Left(ForbiddenStatePensionExclusion(Exclusion.MarriedWomenReducedRateElection, Some("The customer cannot access the service, they should contact HMRC")))))
          )

        val result = await(nationalInsuranceService.getSummary(generateNino))

        result.map {
          excl =>
            val exclusion = excl.swap.getOrElse(StatePensionExclusionFiltered)

            exclusion shouldBe StatePensionExclusionFiltered(Exclusion.MarriedWomenReducedRateElection)
        }
      }
    }

    "There is a MWRRE Exclusion response for reducedRateElection = true" should {

      val mockNationalInsuranceRecord = NationalInsuranceRecord(
        qualifyingYears = 40,
        qualifyingYearsPriorTo1975 = 0,
        numberOfGaps = 2,
        numberOfGapsPayable = 1,
        Some(LocalDate.of(1973, 7, 7)),
        homeResponsibilitiesProtection = false,
        earningsIncludedUpTo = LocalDate.of(2016, 4, 5),
        taxYears = List(
          NationalInsuranceTaxYear(
            taxYear = "2015-16",
            qualifying = true,
            classOneContributions = 12345.45,
            classTwoCredits = 0,
            classThreeCredits = 0,
            otherCredits = 0,
            classThreePayable = 0,
            classThreePayableBy = None,
            classThreePayableByPenalty = None,
            payable = false,
            underInvestigation = false
          )
        ),
        reducedRateElection = true
      )

      "return a Left(Exclusion)" in {
        when(mockNationalInsuranceConnector.getNationalInsurance(any(), any())(any()))
          .thenReturn(Future.successful(Right(Right(mockNationalInsuranceRecord))))

        val niSummary = nationalInsuranceService.getSummary(generateNino)

        niSummary.futureValue shouldBe Right(Left(StatePensionExclusionFiltered(Exclusion.MarriedWomenReducedRateElection)))

      }

    }

    "There is a cope exclusion" should {

      "return Left(CopeProcessing)" in {
        val localDate = LocalDate.now()

        when(mockNationalInsuranceConnector.getNationalInsurance(any(), any())(any()))
          .thenReturn(Future.successful(
            Right(Left(CopeStatePensionExclusion(Exclusion.CopeProcessing, localDate, None))))
          )

        val result = nationalInsuranceService.getSummary(generateNino)

        result.futureValue shouldBe Right(Left(StatePensionExclusionFilteredWithCopeDate(Exclusion.CopeProcessing, localDate)))
      }

      "return Left(CopeProcessingFailed)" in {
        when(mockNationalInsuranceConnector.getNationalInsurance(any(), any())(any()))
          .thenReturn(Future.successful(
            Right(Left(ForbiddenStatePensionExclusion(Exclusion.CopeProcessingFailed, None))))
          )

        val result = nationalInsuranceService.getSummary(generateNino)

        result.futureValue shouldBe Right(Left(StatePensionExclusionFiltered(Exclusion.CopeProcessingFailed)))
      }
    }
  }
}
