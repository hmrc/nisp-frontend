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

package uk.gov.hmrc.nisp.services

import uk.gov.hmrc.nisp.models.enums.ABTest
import uk.gov.hmrc.play.test.UnitSpec

class ABServiceSpec extends UnitSpec {
  "test" should {
    "return A for NINO AA000000A" in {
      ABService.test("AA000000A") shouldBe ABTest.A
    }

    "return B for NINO AA000003A" in {
      ABService.test("AA000003A") shouldBe ABTest.B
    }

    "return B for NINO AA000001A" in {
      ABService.test("AA000001A") shouldBe ABTest.B
    }

    "return A for NINO AA000002A" in {
      ABService.test("AA000002A") shouldBe ABTest.A
    }

    "return A for NINO BB564246A" in {
      ABService.test("BB564246") shouldBe ABTest.A
    }

    "return B for NINO YN315615D" in {
      ABService.test("YN315615D") shouldBe ABTest.B
    }
  }
}
