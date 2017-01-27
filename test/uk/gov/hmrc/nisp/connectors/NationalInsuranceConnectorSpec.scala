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

package uk.gov.hmrc.nisp.connectors

import org.joda.time.LocalDate
import uk.gov.hmrc.nisp.helpers.TestAccountBuilder
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.nisp.helpers.MockNationalInsuranceConnector
import uk.gov.hmrc.nisp.models.NationalInsuranceRecord
import uk.gov.hmrc.play.http.{HeaderCarrier, Upstream4xxResponse}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import uk.gov.hmrc.nisp.models


class NationalInsuranceConnectorSpec extends UnitSpec with ScalaFutures {

  implicit val headerCarrier = HeaderCarrier(extraHeaders = Seq("Accept" -> "application/vnd.hmrc.1.0+json"))

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  "getNationalInsuranceRecord" when {

    "there is a regular user" should {

      val nationalInsuranceRecord = MockNationalInsuranceConnector.getNationalInsurance(TestAccountBuilder.regularNino)(headerCarrier)

      "return a National Insurance Record with 28 qualifying years" in {
        nationalInsuranceRecord.qualifyingYears shouldBe 28
      }

      "return a National Insurance Record with 0 qualifyingYearsPriorTo1975" in {
        nationalInsuranceRecord.qualifyingYearsPriorTo1975 shouldBe 0
      }

      "return a National Insurance Record with 6 numberOfGaps" in {
        nationalInsuranceRecord.numberOfGaps shouldBe 6
      }

      "return a National Insurance Record with 4 numberOfGapsPayable" in {
        nationalInsuranceRecord.numberOfGapsPayable shouldBe 4
      }

      "return a National Insurance Record with false homeResponsibilitiesProtection" in {
        nationalInsuranceRecord.homeResponsibilitiesProtection shouldBe false
      }

      "return a National Insurance Record with earningsIncludedUpTo of 2016-04-05" in {
        nationalInsuranceRecord.earningsIncludedUpTo shouldBe new LocalDate(2016, 4, 5)
      }

      "return a National Insurance Record with 41 tax years" in {
        nationalInsuranceRecord.taxYears.length shouldBe 41
      }

      "the first tax year" should {

        val taxYear = nationalInsuranceRecord.taxYears.head
        "be the 2015-16 tax year" in {
          taxYear.taxYear shouldBe "2015-16"
        }

        "be qualifying" in {
          taxYear.qualifying shouldBe true
        }

        "have classOneContributions of 2430.24" in {
          taxYear.classOneContributions shouldBe 2430.24
        }

        "have classTwoCredits of 0" in {
          taxYear.classTwoCredits shouldBe 0
        }

        "have classThreeCredits of 0" in {
          taxYear.classThreeCredits shouldBe 0
        }

        "have otherCredits of 0" in {
          taxYear.otherCredits shouldBe 0
        }

        "have classThreePayable of 0" in {
          taxYear.classThreePayable shouldBe 0
        }

        "have no classThreePayableBy date" in {
          taxYear.classThreePayableBy shouldBe None
        }

        "have no classThreePayableByPenalty date" in {
          taxYear.classThreePayableByPenalty shouldBe None
        }

        "be not payable" in {
          taxYear.payable shouldBe false
        }

        "be not underInvestigation " in {
          taxYear.underInvestigation shouldBe false
        }


      }

      "a non qualifying year such as 2011-12" should {

        val taxYear = nationalInsuranceRecord.taxYears.find(p => p.taxYear == "2011-12").get

        "be the 2011-12 tax year" in {
          taxYear.taxYear shouldBe "2011-12"
        }

        "be non-qualifying" in {
          taxYear.qualifying shouldBe false
        }

        "have classOneContributions of 0" in {
          taxYear.classOneContributions shouldBe 0
        }

        "have classTwoCredits of 0" in {
          taxYear.classTwoCredits shouldBe 0
        }

        "have classThreeCredits of 0" in {
          taxYear.classThreeCredits shouldBe 0
        }

        "have otherCredits of 0" in {
          taxYear.otherCredits shouldBe 0
        }

        "have classThreePayable of 655.20" in {
          taxYear.classThreePayable shouldBe 655.20
        }

        "have classThreePayableBy date of 5th April 2019" in {
          taxYear.classThreePayableBy shouldBe Some(new LocalDate(2019, 4, 5))
        }

        "have classThreePayableByPenalty date of 5th April 2023" in {
          taxYear.classThreePayableByPenalty shouldBe Some(new LocalDate(2023, 4, 5))
        }

        "be payable" in {
          taxYear.payable shouldBe true
        }

        "be not underInvestigation " in {
          taxYear.underInvestigation shouldBe false
        }

      }
    }

    "there is a Dead Exclusion" should {
      "return a failed future with a 403 response code with a relevant message" in {
        whenReady(MockNationalInsuranceConnector.getNationalInsurance(TestAccountBuilder.excludedAll).failed) {
          case ex: Upstream4xxResponse =>
            ex.upstreamResponseCode shouldBe 403
            ex.message.contains("EXCLUSION_DEAD") shouldBe true
        }
      }
    }

    "there is a MCI Exclusion" should {
      "return a failed future with a 403 response code with a relevant message" in {
        whenReady(MockNationalInsuranceConnector.getNationalInsurance(TestAccountBuilder.excludedAllButDead).failed) {
          case ex: Upstream4xxResponse =>
            ex.upstreamResponseCode shouldBe 403
            ex.message.contains("EXCLUSION_MANUAL_CORRESPONDENCE") shouldBe true
        }
      }
    }

    "there is a IOM Exclusion" should {
      "return a failed future with a 403 response code with a relevant message" in {
        whenReady(MockNationalInsuranceConnector.getNationalInsurance(TestAccountBuilder.excludedAllButDeadMCI).failed) {
          case ex: Upstream4xxResponse =>
            ex.upstreamResponseCode shouldBe 403
            ex.message.contains("EXCLUSION_ISLE_OF_MAN") shouldBe true
        }
      }
    }

    "there is a MWRRE Exclusion" should {
      "return a failed future with a 403 response code with a relevant message" in {
        whenReady(MockNationalInsuranceConnector.getNationalInsurance(TestAccountBuilder.excludedIomMwrreAbroad).failed) {
          case ex: Upstream4xxResponse =>
            ex.upstreamResponseCode shouldBe 403
            ex.message.contains("EXCLUSION_MARRIED_WOMENS_REDUCED_RATE") shouldBe true
        }
      }
    }



  }


}
