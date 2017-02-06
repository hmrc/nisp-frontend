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
import uk.gov.hmrc.nisp.connectors.{NationalInsuranceConnector, NispConnector}
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.models.enums.Exclusion
import uk.gov.hmrc.play.http.{HeaderCarrier, Upstream4xxResponse}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import scala.util.Random


class NationalInsuranceServiceSpec extends UnitSpec with MockitoSugar with ScalaFutures {

  def generateNino: Nino = new uk.gov.hmrc.domain.Generator(new Random()).nextNino

  implicit val headerCarrier = HeaderCarrier()

  "NationalInsuranceConnection" when {

    "There is a successful response" should {

      val mockNationalInsuranceRecord = NationalInsuranceRecord(
        qualifyingYears = 40,
        qualifyingYearsPriorTo1975 = 0,
        numberOfGaps = 2,
        numberOfGapsPayable = 1,
        new LocalDate(1973, 7, 7),
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
        )
      )

      val service = new NationalInsuranceService with NationalInsuranceConnection {
        override val nationalInsuranceConnector: NationalInsuranceConnector = mock[NationalInsuranceConnector]
      }
      when(service.nationalInsuranceConnector.getNationalInsurance(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(mockNationalInsuranceRecord))

      "return a Right(NationalInsuranceRecord)" in {
        service.getSummary(generateNino).isRight shouldBe true
        service.getSummary(generateNino).right.get shouldBe a[NationalInsuranceRecord]
      }

      "return unmodified data" in {
        whenReady(service.getSummary(generateNino)) { record =>
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

        when(service.nationalInsuranceConnector.getNationalInsurance(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(jumbledRecord))

        whenReady(service.getSummary(generateNino)) { result =>
          result.right.get.taxYears shouldBe mockNationalInsuranceRecord.taxYears
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

  "NispConnectionNI" when {

    "There is all of the exclusions (Dead, MCI, Isle of Man and MWRRE)" should {

      "return the dead exclusion" in {
        val service = new NationalInsuranceService with NispConnectionNI {
          override val nispConnector: NispConnector = mock[NispConnector]
        }

        when(service.nispConnector.connectToGetNIResponse(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(NIResponse(
            None,
            None,
            niExclusions = Some(ExclusionsModel(List(
              Exclusion.Dead,
              Exclusion.ManualCorrespondenceIndicator,
              Exclusion.IsleOfMan,
              Exclusion.MarriedWomenReducedRateElection
            )))
          )))

        whenReady(service.getSummary(generateNino)(headerCarrier)) { ex =>
          ex shouldBe Left(Exclusion.Dead)
        }

      }

    }

    "There is the exclusions MCI, Isle of Man and MWRRE" should {

      "return the MCI exclusion" in {
        val service = new NationalInsuranceService with NispConnectionNI {
          override val nispConnector: NispConnector = mock[NispConnector]
        }

        when(service.nispConnector.connectToGetNIResponse(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(NIResponse(
            None,
            None,
            niExclusions = Some(ExclusionsModel(List(
              Exclusion.ManualCorrespondenceIndicator,
              Exclusion.IsleOfMan,
              Exclusion.MarriedWomenReducedRateElection
            )))
          )))

        whenReady(service.getSummary(generateNino)(headerCarrier)) { ex =>
          ex shouldBe Left(Exclusion.ManualCorrespondenceIndicator)
        }

      }

    }

    "There is the exclusions Isle of Man and MWRRE" should {

      "return the Isle of Man exclusion" in {
        val service = new NationalInsuranceService with NispConnectionNI {
          override val nispConnector: NispConnector = mock[NispConnector]
        }

        when(service.nispConnector.connectToGetNIResponse(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(NIResponse(
            None,
            None,
            niExclusions = Some(ExclusionsModel(List(
              Exclusion.IsleOfMan,
              Exclusion.MarriedWomenReducedRateElection
            )))
          )))

        whenReady(service.getSummary(generateNino)(headerCarrier)) { ex =>
          ex shouldBe Left(Exclusion.IsleOfMan)
        }

      }

    }

    "There is the MWRRE exclusion" should {

      "return the MWRRE exclusion" in {
        val service = new NationalInsuranceService with NispConnectionNI {
          override val nispConnector: NispConnector = mock[NispConnector]
        }

        when(service.nispConnector.connectToGetNIResponse(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(NIResponse(
            None,
            None,
            niExclusions = Some(ExclusionsModel(List(
              Exclusion.MarriedWomenReducedRateElection
            )))
          )))

        whenReady(service.getSummary(generateNino)(headerCarrier)) { ex =>
          ex shouldBe Left(Exclusion.MarriedWomenReducedRateElection)
        }

      }

    }

    "there is a regular response" should {

      val service = new NationalInsuranceService with NispConnectionNI {
        override val nispConnector: NispConnector = mock[NispConnector]
      }

      val niResponse = NIResponse(
        Some(NIRecord(List(
          NIRecordTaxYear(
            2015,
            qualifying = true,
            classOneContributions = 12345.60,
            classTwoCredits = 27,
            classThreeCredits = 12,
            otherCredits = 2,
            classThreePayableBy = None,
            classThreePayableByPenalty = None,
            classThreePayable = None,
            payable = false,
            underInvestigation = true
          ),
          NIRecordTaxYear(
            1999,
            qualifying = false,
            classOneContributions = 52.12,
            classTwoCredits = 1,
            classThreeCredits = 1,
            otherCredits = 1,
            classThreePayableBy = Some(NpsDate(2019, 4, 5)),
            classThreePayableByPenalty = Some(NpsDate(2023, 4, 5)),
            classThreePayable = Some(311.1),
            payable = true,
            underInvestigation = false
          ),
          NIRecordTaxYear(
            1981,
            qualifying = false,
            classOneContributions = 0,
            classTwoCredits = 18,
            classThreeCredits = 0,
            otherCredits = 20,
            classThreePayableBy = None,
            classThreePayableByPenalty = None,
            classThreePayable = Some(0),
            payable = false,
            underInvestigation = true
          )

        ))),
        Some(NISummary(
          noOfQualifyingYears = 33,
          noOfNonQualifyingYears = 5,
          yearsToContributeUntilPensionAge = 10,
          spaYear = 2017,
          earningsIncludedUpTo = NpsDate(2016, 4, 5),
          unavailableYear = 2016,
          pre75QualifyingYears = Some(5),
          numberOfNonPayableGaps = 3,
          numberOfPayableGaps = 2,
          canImproveWithGaps = true,
          isAbroad = false,
          recordEnd = None,
          finalRelevantYear = 2020,
          homeResponsibilitiesProtection = true,
          dateOfEntry = NpsDate(1977, 4, 6)
        )),
        None
      )

      when(service.nispConnector.connectToGetNIResponse(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(niResponse))

      val serviceResponse: NationalInsuranceRecord = service.getSummary(generateNino).right.get

      "map qualifyingYears from niSummary.noOfQualifyingYears" in {
        serviceResponse.qualifyingYears shouldBe 33
      }

      "map qualifyingYearsPriorTo1975 from niSummary.pre75QualifyingYears" in {
        serviceResponse.qualifyingYearsPriorTo1975 shouldBe 5
      }

      "map numberOfGaps from niSummary.noOfNonQualifyingYears" in {
        serviceResponse.numberOfGaps shouldBe 5
      }

      "map numberOfGapsPayable from niSummary.numberOfPayableGaps" in {
        serviceResponse.numberOfGapsPayable shouldBe 2
      }


      "map dateOfEntry from niSummary.dateOfEntry" in {
        serviceResponse.dateOfEntry shouldBe new LocalDate(1977, 4, 6)
      }

      "map homeResponsibilitiesProtection from niSummary.homeResponsibilitiesProtection" in {
        serviceResponse.homeResponsibilitiesProtection shouldBe true
      }

      "map earningsIncludedUpTo from niSummary.earningsIncludedUpTo" in {
        serviceResponse.earningsIncludedUpTo shouldBe new LocalDate(2016, 4, 5)
      }

      "should pull the list of tax years from niRecord response" in {
        serviceResponse.taxYears.size shouldBe 3
      }

      "map the first tax year correctly which" should {

        "have a tax year of 2015-16" in {
          serviceResponse.taxYears.head.taxYear shouldBe "2015-16"
        }

        "be qualifying" in {
          serviceResponse.taxYears.head.qualifying shouldBe true
        }

        "have 12345.60 class one contributions" in {
          serviceResponse.taxYears.head.classOneContributions shouldBe 12345.60
        }

        "have 27 class two credits" in {
          serviceResponse.taxYears.head.classTwoCredits shouldBe 27
        }

        "have 12 class three credits" in {
          serviceResponse.taxYears.head.classThreeCredits shouldBe 12
        }

        "have 2 other credits" in {
          serviceResponse.taxYears.head.otherCredits shouldBe 2
        }

        "have no class three payable" in {
          serviceResponse.taxYears.head.classThreePayable shouldBe 0
        }

        "have no class three payable by" in {
          serviceResponse.taxYears.head.classThreePayableBy shouldBe None
        }

        "have no class three payable by penalty" in {
          serviceResponse.taxYears.head.classThreePayableByPenalty shouldBe None
        }

        "be not payable" in {
          serviceResponse.taxYears.head.payable shouldBe false
        }

        "be under investigation" in {
          serviceResponse.taxYears.head.underInvestigation shouldBe true
        }

      }

      "map the second tax year correctly which" should {

        "have a tax year of 1999-00" in {
          serviceResponse.taxYears(1).taxYear shouldBe "1999-00"
        }

        "be not qualifying" in {
          serviceResponse.taxYears(1).qualifying shouldBe false
        }

        "have 52.12 class one contributions" in {
          serviceResponse.taxYears(1).classOneContributions shouldBe 52.12
        }

        "have 1 class two credits" in {
          serviceResponse.taxYears(1).classTwoCredits shouldBe 1
        }

        "have 1 class three credits" in {
          serviceResponse.taxYears(1).classThreeCredits shouldBe 1
        }

        "have 1 other credits" in {
          serviceResponse.taxYears(1).otherCredits shouldBe 1
        }

        "have 311.10 class three payable" in {
          serviceResponse.taxYears(1).classThreePayable shouldBe 311.10
        }

        "have class three payable by of 5/4/2019" in {
          serviceResponse.taxYears(1).classThreePayableBy shouldBe Some(new LocalDate(2019, 4, 5))
        }

        "have class three payable by penalty of 5/4/2023" in {
          serviceResponse.taxYears(1).classThreePayableByPenalty shouldBe Some(new LocalDate(2023, 4, 5))
        }

        "be payable" in {
          serviceResponse.taxYears(1).payable shouldBe true
        }

        "be not under investigation" in {
          serviceResponse.taxYears(1).underInvestigation shouldBe false
        }

      }

      "map the last tax year correctly which" should {

        "have a tax year of 1981-82" in {
          serviceResponse.taxYears(2).taxYear shouldBe "1981-82"
        }

        "be not qualifying" in {
          serviceResponse.taxYears(2).qualifying shouldBe false
        }

        "have 0 class one contributions" in {
          serviceResponse.taxYears(2).classOneContributions shouldBe 0
        }

        "have 18 class two credits" in {
          serviceResponse.taxYears(2).classTwoCredits shouldBe 18
        }

        "have 0 class three credits" in {
          serviceResponse.taxYears(2).classThreeCredits shouldBe 0
        }

        "have 20 other credits" in {
          serviceResponse.taxYears(2).otherCredits shouldBe 20
        }

        "have no class three payable" in {
          serviceResponse.taxYears(2).classThreePayable shouldBe 0
        }

        "have no class three payable by" in {
          serviceResponse.taxYears(2).classThreePayableBy shouldBe None
        }

        "have no class three payable by penalty" in {
          serviceResponse.taxYears(2).classThreePayableByPenalty shouldBe None
        }

        "be not payable" in {
          serviceResponse.taxYears(2).payable shouldBe false
        }

        "be under investigation" in {
          serviceResponse.taxYears(2).underInvestigation shouldBe true
        }

      }

      "return all the tax years in descending order" in {
        when(service.nispConnector.connectToGetNIResponse(Matchers.any())(Matchers.any()))
          .thenReturn(Future.successful(niResponse.copy(niRecord = Some(
            NIRecord(List(
              NIRecordTaxYear(
                1981,
                qualifying = false,
                classOneContributions = 0,
                classTwoCredits = 18,
                classThreeCredits = 0,
                otherCredits = 20,
                classThreePayableBy = None,
                classThreePayableByPenalty = None,
                classThreePayable = Some(0),
                payable = false,
                underInvestigation = true
              ),
              NIRecordTaxYear(
                2015,
                qualifying = true,
                classOneContributions = 12345.60,
                classTwoCredits = 27,
                classThreeCredits = 12,
                otherCredits = 2,
                classThreePayableBy = None,
                classThreePayableByPenalty = None,
                classThreePayable = None,
                payable = false,
                underInvestigation = true
              ),
              NIRecordTaxYear(
                1999,
                qualifying = false,
                classOneContributions = 52.12,
                classTwoCredits = 1,
                classThreeCredits = 1,
                otherCredits = 1,
                classThreePayableBy = Some(NpsDate(2019, 4, 5)),
                classThreePayableByPenalty = Some(NpsDate(2023, 4, 5)),
                classThreePayable = Some(311.1),
                payable = true,
                underInvestigation = false
              )
            ))
          ))))

        service.getSummary(generateNino).right.get.taxYears shouldBe List(
          NationalInsuranceTaxYear(
            "2015-16",
            qualifying = true,
            classOneContributions = 12345.60,
            classTwoCredits = 27,
            classThreeCredits = 12,
            otherCredits = 2,
            classThreePayable = 0,
            classThreePayableBy = None,
            classThreePayableByPenalty = None,
            payable = false,
            underInvestigation = true
          ),
          NationalInsuranceTaxYear(
            "1999-00",
            qualifying = false,
            classOneContributions = 52.12,
            classTwoCredits = 1,
            classThreeCredits = 1,
            otherCredits = 1,
            classThreePayable = 311.1,
            classThreePayableBy = Some(new LocalDate(2019, 4, 5)),
            classThreePayableByPenalty = Some(new LocalDate(2023, 4, 5)),
            payable = true,
            underInvestigation = false
          ),
          NationalInsuranceTaxYear(
            "1981-82",
            qualifying = false,
            classOneContributions = 0,
            classTwoCredits = 18,
            classThreeCredits = 0,
            otherCredits = 20,
            classThreePayable = 0,
            classThreePayableBy = None,
            classThreePayableByPenalty = None,
            payable = false,
            underInvestigation = true
          )
        )
      }
    }

    "startYearToTaxYear" when {
      "startYear is 1999" should {
        "return 1999-00" in {
          val connection = new NispConnectionNI {
            override lazy val nispConnector: NispConnector = ???
          }
          connection.startYearToTaxYear(1999) shouldBe "1999-00"
        }
      }

      "startYear is 2015" should {
        "return 2015-16" in {
          val connection = new NispConnectionNI {
            override lazy val nispConnector: NispConnector = ???
          }
          connection.startYearToTaxYear(2015) shouldBe "2015-16"
        }
      }

      "startYear is 2009" should {
        "return 2009-10" in {
          val connection = new NispConnectionNI {
            override lazy val nispConnector: NispConnector = ???
          }
          connection.startYearToTaxYear(2009) shouldBe "2009-10"
        }
      }
    }

  }


}
