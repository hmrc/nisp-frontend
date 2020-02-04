/*
 * Copyright 2020 HM Revenue & Customs
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

import uk.gov.hmrc.nisp.common.FakePlayApplication
import uk.gov.hmrc.play.test.UnitSpec

class NispMoneySpec extends UnitSpec with FakePlayApplication {
  "pounds" should {
    "return HTML with £100 for value 100" in {
      NispMoney.pounds(100).toString().endsWith("&pound;100") shouldBe true
    }
    "return HTML with £100.12 for value 100.12" in {
      NispMoney.pounds(100.12).toString().endsWith("&pound;100.12") shouldBe true
    }
    "return HTML with £100.10 for value 100.1" in {
      NispMoney.pounds(100.1).toString().endsWith("&pound;100.10") shouldBe true
    }
  }
}
