/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.views.formatting

import org.scalatestplus.play.OneAppPerSuite
import play.api.test.FakeApplication
import uk.gov.hmrc.play.test.UnitSpec

class TimeSpec extends UnitSpec with OneAppPerSuite {

  "years" should {
    "return 1 year when there is 1 year"  in {
      Time.years(1) shouldBe "1 year"
    }
    "return 5 years when there is 5 years" in {
      Time.years(5) shouldBe "5 years"
    }
    "return 0 years when there is 0 years" in {
      Time.years(0) shouldBe "0 years"
    }
    "return -1 year when there is -1 year" in {
      Time.years(-1) shouldBe "-1 year"
    }

  }
}
