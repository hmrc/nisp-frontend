/*
 * Copyright 2018 HM Revenue & Customs
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

import org.joda.time.LocalDate
import uk.gov.hmrc.nisp.models.enums.Exclusion
import uk.gov.hmrc.play.test.UnitSpec


class StatePensionExclusionSpec extends UnitSpec {

  "finalRelevantStartYear" when {
    "there is no pension date" should {
      "be none" in {
        StatePensionExclusion(List(Exclusion.Dead), None, None).finalRelevantStartYear shouldBe None
      }
    }

    "the pensionDate is 5th April 2020" should {
      "be 2018" in {
        StatePensionExclusion(List(Exclusion.AmountDissonance), Some(67), Some(new LocalDate(2020, 4, 5))).finalRelevantStartYear shouldBe Some(2018)
      }
    }

    "the pensionDate is 6th April 2020" should {
      "be 2019" in {
        StatePensionExclusion(List(Exclusion.AmountDissonance), Some(67), Some(new LocalDate(2020, 4, 6))).finalRelevantStartYear shouldBe Some(2019)
      }
    }

    "the pensionDate is 6th April 2000" should {
      "be 1999" in {
        StatePensionExclusion(List(Exclusion.AmountDissonance, Exclusion.PostStatePensionAge), Some(65), Some(new LocalDate(2000, 4, 6))).finalRelevantStartYear shouldBe Some(1999)
      }
    }

    "the pensionDate is 5th April 2020 and there is no flag for state pension age under consideration" should {
      "be 2018" in {
        StatePensionExclusion(List(Exclusion.AmountDissonance), Some(67), Some(new LocalDate(2020, 4, 5))).finalRelevantStartYear shouldBe Some(2018)
      }
    }

    "the pensionDate is 5th April 2020 and there is a true flag for state pension age under consideration" should {
      "be 2018" in {
        StatePensionExclusion(List(Exclusion.AmountDissonance), Some(67), Some(new LocalDate(2020, 4, 5)), Some(true)).finalRelevantStartYear shouldBe Some(2018)
      }
    }

    "the pensionDate is 5th April 2020 and there is a false flag for state pension age under consideration" should {
      "be 2018" in {
        StatePensionExclusion(List(Exclusion.AmountDissonance), Some(67), Some(new LocalDate(2020, 4, 5)), Some(false)).finalRelevantStartYear shouldBe Some(2018)
      }
    }


  }

}
