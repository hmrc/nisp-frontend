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

package uk.gov.hmrc.nisp.services

import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Injecting
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, Upstream4xxResponse}
import uk.gov.hmrc.nisp.connectors.NationalInsuranceConnector
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.models.enums.Exclusion
import uk.gov.hmrc.nisp.models.enums.Exclusion.Exclusion
import uk.gov.hmrc.play.test.UnitSpec
import scala.concurrent.Future
import scala.util.Random

class NationalInsuranceServiceSpec extends UnitSpec with MockitoSugar with ScalaFutures with GuiceOneAppPerSuite
with BeforeAndAfterEach with Injecting {

  def generateNino: Nino = new uk.gov.hmrc.domain.Generator(new Random()).nextNino

  implicit val headerCarrier = HeaderCarrier()
  val mockNationalInsuranceConnector = mock[NationalInsuranceConnector]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[NationalInsuranceConnector].toInstance(mockNationalInsuranceConnector)
    ).build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockNationalInsuranceConnector)
  }

  val nationalInsuranceService = inject[NationalInsuranceService]

  "NationalInsuranceConnection" when {

    "There is a successful response" should {

      val mockNationalInsuranceRecord = NationalInsuranceRecord(
        qualifyingYears = 40,
        qualifyingYearsPriorTo1975 = 39,
        numberOfGaps = 2,
        numberOfGapsPayable = 1,
        Some(new LocalDate(1973, 7, 7)),
        homeResponsibilitiesProtection = false,
        earningsIncludedUpTo = new LocalDate(2016, 4, 5),
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
          ),
          NationalInsuranceTaxYear(
            taxYear = "2014-15",
            qualifying = false,
            classOneContributions = 123,
            classTwoCredits = 1,
            classThreeCredits = 1,
            otherCredits = 1,
            classThreePayable = 456.58,
            classThreePayableBy = Some(new LocalDate(2019, 4, 5)),
            classThreePayableByPenalty = Some(new LocalDate(2023, 4, 5)),
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
            classThreePayableBy = Some(new LocalDate(2019, 4, 5)),
            classThreePayableByPenalty = Some(new LocalDate(2023, 4, 5)),
            payable = false,
            underInvestigation = false
          )
        ),
        reducedRateElection = false
      )

      "return a Right(NationalInsuranceRecord)" in {
        when(mockNationalInsuranceConnector.getNationalInsurance(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(mockNationalInsuranceRecord))

        nationalInsuranceService.getSummary(generateNino).isRight shouldBe true
        nationalInsuranceService.getSummary(generateNino).right.get shouldBe a[NationalInsuranceRecord]
      }

      "return unmodified data" in {

        when(mockNationalInsuranceConnector.getNationalInsurance(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(mockNationalInsuranceRecord))

        whenReady(nationalInsuranceService.getSummary(generateNino)) { record =>
          record shouldBe Right(mockNationalInsuranceRecord)
        }
      }

      "return the tax years in descending order" in {

        val jumbledRecord = mockNationalInsuranceRecord.copy(taxYears = List(
          NationalInsuranceTaxYear(
            taxYear = "2014-15",
            qualifying = false,
            classOneContributions = 123,
            classTwoCredits = 1,
            classThreeCredits = 1,
            otherCredits = 1,
            classThreePayable = 456.58,
            classThreePayableBy = Some(new LocalDate(2019, 4, 5)),
            classThreePayableByPenalty = Some(new LocalDate(2023, 4, 5)),
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
            classThreePayableBy = Some(new LocalDate(2019, 4, 5)),
            classThreePayableByPenalty = Some(new LocalDate(2023, 4, 5)),
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
        ))

        when(mockNationalInsuranceConnector.getNationalInsurance(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(jumbledRecord))

        whenReady(nationalInsuranceService.getSummary(generateNino)) { result =>
          result.right.get.taxYears shouldBe mockNationalInsuranceRecord.taxYears
        }

      }

    }

    "There is failed future from a Dead exclusion" should {
      "return a Left Dead Exclusion" in {

        when(mockNationalInsuranceConnector.getNationalInsurance(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.failed(new Upstream4xxResponse(
            message = "GET of 'http://url' returned 403. Response body: '{\"code\":\"EXCLUSION_DEAD\",\"message\":\"The customer needs to contact the National Insurance helpline\"}'",
            upstreamResponseCode = 403,
            reportAs = 500
          )))

        whenReady(nationalInsuranceService.getSummary(generateNino)) { ex =>
          ex shouldBe Left(Exclusion.Dead)
        }
      }
    }

    "There is failed future from a MCI exclusion" should {
      "return a Left MCI Exclusion" in {

        when(mockNationalInsuranceConnector.getNationalInsurance(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.failed(new Upstream4xxResponse(
            message = "GET of 'http://url' returned 403. Response body: '{\"code\":\"EXCLUSION_MANUAL_CORRESPONDENCE\",\"message\":\"The customer cannot access the service, they should contact HMRC\"}'",
            upstreamResponseCode = 403,
            reportAs = 500
          )))

        whenReady(nationalInsuranceService.getSummary(generateNino)) { ex =>
          ex shouldBe Left(Exclusion.ManualCorrespondenceIndicator)
        }
      }
    }

    "There is failed future from a Isle of Man exclusion" should {
      "return a Left Isle of Man Exclusion" in {

        when(mockNationalInsuranceConnector.getNationalInsurance(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.failed(new Upstream4xxResponse(
            message = "GET of 'http://url' returned 403. Response body: '{\"code\":\"EXCLUSION_ISLE_OF_MAN\",\"message\":\"The customer cannot access the service, they should contact HMRC\"}'",
            upstreamResponseCode = 403,
            reportAs = 500
          )))

        whenReady(nationalInsuranceService.getSummary(generateNino)) { ex =>
          ex shouldBe Left(Exclusion.IsleOfMan)
        }
      }
    }

    "There is failed future from a MWRRE exclusion" should {
      "return a Left MWRRE Exclusion" in {

        when(mockNationalInsuranceConnector.getNationalInsurance(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.failed(new Upstream4xxResponse(
            message = "GET of 'http://url' returned 403. Response body: '{\"code\":\"EXCLUSION_MARRIED_WOMENS_REDUCED_RATE\",\"message\":\"The customer cannot access the service, they should contact HMRC\"}'",
            upstreamResponseCode = 403,
            reportAs = 500
          )))

        whenReady(nationalInsuranceService.getSummary(generateNino)) { ex =>
          ex shouldBe Left(Exclusion.MarriedWomenReducedRateElection)
        }
      }
    }

    "There is a MWRRE Exclusion response for reducedRateElection = true" should {

      val mockNationalInsuranceRecord = NationalInsuranceRecord(
        qualifyingYears = 40,
        qualifyingYearsPriorTo1975 = 0,
        numberOfGaps = 2,
        numberOfGapsPayable = 1,
        Some(new LocalDate(1973, 7, 7)),
        homeResponsibilitiesProtection = false,
        earningsIncludedUpTo = new LocalDate(2016, 4, 5),
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



      "return a Left(Excelution)" in {
        when(mockNationalInsuranceConnector.getNationalInsurance(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(mockNationalInsuranceRecord))

        val niSummary = nationalInsuranceService.getSummary(generateNino)
        niSummary.isLeft shouldBe true
        niSummary.left.get shouldBe a[Exclusion]
      }

    }
  }
}
