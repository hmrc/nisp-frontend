/*
 * Copyright 2019 HM Revenue & Customs
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

import uk.gov.hmrc.play.test.UnitSpec


class FormattingSpec extends UnitSpec {
  "startYearToTaxYear" when {
    "input is 1999" should {
      "return 1999-00" in {
        Formatting.startYearToTaxYear(1999) shouldBe "1999-00"
      }
    }
    "input is 2015" should {
      "return 2015-16" in {
        Formatting.startYearToTaxYear(2015) shouldBe "2015-16"
      }
    }
    "input is 2009" should {
      "return 2009-10" in {
        Formatting.startYearToTaxYear(2009) shouldBe "2009-10"

      }
    }
    "input is 1985" should {
      "return 1985-86" in {
        Formatting.startYearToTaxYear(1985) shouldBe "1985-86"
      }
    }
  }

}
