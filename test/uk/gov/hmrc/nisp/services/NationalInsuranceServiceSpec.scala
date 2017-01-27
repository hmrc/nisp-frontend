/*
 * Copyright 2017 HM Revenue & Customs
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
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.nisp.connectors.NationalInsuranceConnector
import uk.gov.hmrc.nisp.models.{NationalInsuranceRecord, NationalInsuranceTaxYear}
import uk.gov.hmrc.nisp.models.enums.Exclusion
import uk.gov.hmrc.play.http.{HeaderCarrier, Upstream4xxResponse}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import scala.util.Random


class NationalInsuranceServiceSpec extends UnitSpec with MockitoSugar with ScalaFutures {

  def generateNino: Nino = new uk.gov.hmrc.domain.Generator(new Random()).nextNino

  implicit val headcarrier = HeaderCarrier()

  "NationalInsuranceConnection" when {

    "There is a successful response" should {

      val mockNationalInsuranceRecord = NationalInsuranceRecord(
        qualifyingYears = 40,
        qualifyingYearsPriorTo1975 = 0,
        numberOfGaps = 2,
        numberOfGapsPayable = 1,
        homeResponsibilitiesProtection = false,
        earningsIncludedUpTo = new LocalDate(2016, 4, 5),
        taxYears = List(
          NationalInsuranceTaxYear(
            taxYear = "2015-16",
            qualifying = true,
            classOneContributions = 12345.45,
            classTwoCredits = 0,
            classThreeCredits = 0,
            otherCredits =  0,
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
            otherCredits =  1,
            classThreePayable = 456.58,
            classThreePayableBy = Some(new LocalDate(2019, 4, 5)),
            classThreePayableByPenalty = Some(new LocalDate(2023, 4, 5)),
            payable = false,
            underInvestigation = false
          )
        )
      )

      val service = new NationalInsuranceService with NationalInsuranceConnection {
        override val nationalInsuranceConnector: NationalInsuranceConnector = mock[NationalInsuranceConnector]
      }
      when(service.nationalInsuranceConnector.getNationalInsurance(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(mockNationalInsuranceRecord))

      "return a Right(NationalInsuranceRecord)" in {
        service.getSummary(generateNino).isRight shouldBe true
        service.getSummary(generateNino).right.get shouldBe a [NationalInsuranceRecord]
      }

      "return unmodified data" in {
        whenReady(service.getSummary(generateNino)) { record =>
          record shouldBe Right(mockNationalInsuranceRecord)
        }
      }

    }

    "There is failed future from a Dead exclusion" should {
      "return a Left Dead Exclusion" in {
        val service = new NationalInsuranceService with NationalInsuranceConnection {
          override val nationalInsuranceConnector: NationalInsuranceConnector = mock[NationalInsuranceConnector]
        }
        when(service.nationalInsuranceConnector.getNationalInsurance(Matchers.any())(Matchers.any()))
          .thenReturn(Future.failed(new Upstream4xxResponse(
          message = "GET of 'http://url' returned 403. Response body: '{\"code\":\"EXCLUSION_DEAD\",\"message\":\"The customer needs to contact the National Insurance helpline\"}'",
          upstreamResponseCode = 403,
          reportAs = 500
        )))

        whenReady(service.getSummary(generateNino)) { ex =>
          ex shouldBe Left(Exclusion.Dead)
        }
      }
    }

    "There is failed future from a MCI exclusion" should {
      "return a Left MCI Exclusion" in {
        val service = new NationalInsuranceService with NationalInsuranceConnection {
          override val nationalInsuranceConnector: NationalInsuranceConnector = mock[NationalInsuranceConnector]
        }
        when(service.nationalInsuranceConnector.getNationalInsurance(Matchers.any())(Matchers.any()))
          .thenReturn(Future.failed(new Upstream4xxResponse(
            message = "GET of 'http://url' returned 403. Response body: '{\"code\":\"EXCLUSION_MANUAL_CORRESPONDENCE\",\"message\":\"The customer cannot access the service, they should contact HMRC\"}'",
            upstreamResponseCode = 403,
            reportAs = 500
          )))

        whenReady(service.getSummary(generateNino)) { ex =>
          ex shouldBe Left(Exclusion.ManualCorrespondenceIndicator)
        }
      }
    }

    "There is failed future from a Isle of Man exclusion" should {
      "return a Left Isle of Man Exclusion" in {
        val service = new NationalInsuranceService with NationalInsuranceConnection {
          override val nationalInsuranceConnector: NationalInsuranceConnector = mock[NationalInsuranceConnector]
        }
        when(service.nationalInsuranceConnector.getNationalInsurance(Matchers.any())(Matchers.any()))
          .thenReturn(Future.failed(new Upstream4xxResponse(
            message = "GET of 'http://url' returned 403. Response body: '{\"code\":\"EXCLUSION_ISLE_OF_MAN\",\"message\":\"The customer cannot access the service, they should contact HMRC\"}'",
            upstreamResponseCode = 403,
            reportAs = 500
          )))

        whenReady(service.getSummary(generateNino)) { ex =>
          ex shouldBe Left(Exclusion.IsleOfMan)
        }
      }
    }

    "There is failed future from a MWRRE exclusion" should {
      "return a Left MWRRE Exclusion" in {
        val service = new NationalInsuranceService with NationalInsuranceConnection {
          override val nationalInsuranceConnector: NationalInsuranceConnector = mock[NationalInsuranceConnector]
        }
        when(service.nationalInsuranceConnector.getNationalInsurance(Matchers.any())(Matchers.any()))
          .thenReturn(Future.failed(new Upstream4xxResponse(
            message = "GET of 'http://url' returned 403. Response body: '{\"code\":\"EXCLUSION_MARRIED_WOMENS_REDUCED_RATE\",\"message\":\"The customer cannot access the service, they should contact HMRC\"}'",
            upstreamResponseCode = 403,
            reportAs = 500
          )))

        whenReady(service.getSummary(generateNino)) { ex =>
          ex shouldBe Left(Exclusion.MarriedWomenReducedRateElection)
        }
      }
    }
  }

}
