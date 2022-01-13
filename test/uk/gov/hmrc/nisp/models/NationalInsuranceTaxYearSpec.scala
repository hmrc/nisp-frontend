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

package uk.gov.hmrc.nisp.models

import java.time.LocalDate
import uk.gov.hmrc.nisp.utils.UnitSpec

class NationalInsuranceTaxYearSpec extends UnitSpec {
  "currentDateAfterCutOff" when {

    "the current date is before the payableBy date" should {
      "return false" in {
        val taxYear = NationalInsuranceTaxYear(
          "",
          false,
          0,
          0,
          0,
          0,
          123.45,
          Some(LocalDate.of(2019, 4, 5)),
          Some(LocalDate.of(2023, 4, 5)),
          true,
          false
        )
        taxYear.currentDateAfterCutOff(LocalDate.of(2019, 4, 4)) shouldBe false
      }
    }

    "the current date is equal to the payableBy date" should {
      "return false" in {
        val taxYear = NationalInsuranceTaxYear(
          "",
          false,
          0,
          0,
          0,
          0,
          123.45,
          Some(LocalDate.of(2019, 4, 5)),
          Some(LocalDate.of(2023, 4, 5)),
          true,
          false
        )
        taxYear.currentDateAfterCutOff(LocalDate.of(2019, 4, 5)) shouldBe false
      }
    }

    "the current date is after to the payableBy date" should {
      "return true" in {
        val taxYear = NationalInsuranceTaxYear(
          "",
          false,
          0,
          0,
          0,
          0,
          123.45,
          Some(LocalDate.of(2019, 4, 5)),
          Some(LocalDate.of(2023, 4, 5)),
          true,
          false
        )
        taxYear.currentDateAfterCutOff(LocalDate.of(2019, 4, 6)) shouldBe true
      }
    }

    "the payable date is missing and the year is not payable" should {
      "return false" in {
        val taxYear = NationalInsuranceTaxYear("", false, 0, 0, 0, 0, 123.45, None, None, false, false)
        taxYear.currentDateAfterCutOff(LocalDate.of(2019, 4, 6)) shouldBe false
      }
    }

    "the payable date is missing and the year is payable" should {
      "return true" in {
        val taxYear =
          NationalInsuranceTaxYear("", false, 0, 0, 0, 0, 123.45, None, Some(LocalDate.of(2023, 4, 5)), true, false)
        taxYear.currentDateAfterCutOff(LocalDate.of(2020, 4, 6)) shouldBe true
      }
    }
  }
}
