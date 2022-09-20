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

package uk.gov.hmrc.nisp.utils

class FormattingSpec extends UnitSpec {
  "startYearToTaxYear" when {
    "input is 1999" should {
      "return 1999 to 2000" in {
        Formatting.startYearToTaxYear(1999) shouldBe "1999"
      }
    }
    "input is 2015" should {
      "return 2015 to 2016" in {
        Formatting.startYearToTaxYear(2015) shouldBe "2015"
      }
    }
    "input is 2009" should {
      "return 2009 to 2010" in {
        Formatting.startYearToTaxYear(2009) shouldBe "2009"

      }
    }
    "input is 1985" should {
      "return 1985 to 1986" in {
        Formatting.startYearToTaxYear(1985) shouldBe "1985"
      }
    }
  }

}
