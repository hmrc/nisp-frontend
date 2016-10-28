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

package uk.gov.hmrc.nisp.models

import org.joda.time._

import uk.gov.hmrc.play.test.UnitSpec

class StatePensionSpec extends UnitSpec {

  "contractedOut" should {
    "return true when the user has a COPE amount more than 0" in {
      StatePension(
        new LocalDate(2016, 4, 5),
        amounts = StatePensionAmounts (
          false,
          StatePensionAmount(None, None, 0, 0, 0),
          StatePensionAmount(None, None, 0, 0, 0),
          StatePensionAmount(None, None, 0, 0, 0),
          cope = StatePensionAmount(None, None, 0.87, 26.48, 317.77)
        ),
        65,
        new LocalDate(2019, 5, 1),
        "2018-19",
        30,
        false,
        155.65
      ).contractedOut shouldBe true
    }
    "return false when the user has a COPE amount of 0" in {
      StatePension(
        new LocalDate(2016, 4, 5),
        amounts = StatePensionAmounts (
          false,
          StatePensionAmount(None, None, 0, 0, 0),
          StatePensionAmount(None, None, 0, 0, 0),
          StatePensionAmount(None, None, 0, 0, 0),
          cope = StatePensionAmount(None, None, 0, 0, 0)
        ),
        65,
        new LocalDate(2019, 5, 1),
        "2018-19",
        30,
        false,
        155.65
      ).contractedOut shouldBe false
    }
  }

}
