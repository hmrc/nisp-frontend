/*
 * Copyright 2024 HM Revenue & Customs
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

import uk.gov.hmrc.nisp.models.Exclusion

class ExclusionFeatureFlagServiceHelperSpec extends UnitSpec {

  "filterExclusions" should {
    "return Dead" when {
      "List of Exclusions has Dead only" in {
        ExclusionHelper.filterExclusions(List(Exclusion.Dead)) shouldBe Exclusion.Dead
      }

      "List of Exclusions includes Dead and other Exclusions" in {
        ExclusionHelper.filterExclusions(
          List(Exclusion.IsleOfMan,
            Exclusion.PostStatePensionAge,
            Exclusion.Dead)) shouldBe Exclusion.Dead
      }
    }

    "return MCI" when {
      "List of Exclusions has MCI only" in {
        ExclusionHelper.filterExclusions(List(Exclusion.ManualCorrespondenceIndicator)) shouldBe Exclusion.ManualCorrespondenceIndicator
      }

      "List of Exclusions has multiple exclusion without Dead" in {
        ExclusionHelper.filterExclusions(
          List(Exclusion.IsleOfMan,
            Exclusion.PostStatePensionAge,
            Exclusion.ManualCorrespondenceIndicator)) shouldBe Exclusion.ManualCorrespondenceIndicator
      }
    }

    "return PostStatePensionAge" when {
      "List of Exclusions has PostStatePensionAge only" in {
        ExclusionHelper.filterExclusions(List(Exclusion.PostStatePensionAge)) shouldBe Exclusion.PostStatePensionAge
      }

      "List of Exclusions does not include Dead and MCI" in {
        ExclusionHelper.filterExclusions(
          List(Exclusion.IsleOfMan,
            Exclusion.PostStatePensionAge,
            Exclusion.AmountDissonance)) shouldBe Exclusion.PostStatePensionAge
      }
    }

    "return AmountDissonance" when {
      "List of Exclusions has AmountDissonance only" in {
        ExclusionHelper.filterExclusions(List(Exclusion.AmountDissonance)) shouldBe Exclusion.AmountDissonance
      }

      "List of Exclusions does not include Dead, MCI and PostStatePensionAge" in {
        ExclusionHelper.filterExclusions(
          List(Exclusion.IsleOfMan,
            Exclusion.MarriedWomenReducedRateElection,
            Exclusion.AmountDissonance)) shouldBe Exclusion.AmountDissonance
      }
    }

    "return IsleOfMan" when {
      "List of Exclusions is IsleOfMan only" in {
        ExclusionHelper.filterExclusions(List(Exclusion.IsleOfMan)) shouldBe Exclusion.IsleOfMan
      }

      "List of Exclusions does not include Dead, MCI, PostStatePensionAge and AmountDissonance" in {
        ExclusionHelper.filterExclusions(
          List(Exclusion.MarriedWomenReducedRateElection, Exclusion.IsleOfMan)) shouldBe Exclusion.IsleOfMan
      }
    }

    "return MarriedWomenRateReducedRateElection" when {
      "List of Exclusion is MarriedWomenRateReducedRateElection only" in {
        ExclusionHelper.filterExclusions(List(Exclusion.MarriedWomenReducedRateElection)) shouldBe Exclusion.MarriedWomenReducedRateElection
      }
    }

    "return RuntimeException" when {
      "List of Exclusion is empty" in {
        intercept[RuntimeException]{
          ExclusionHelper.filterExclusions(List())
        }.getMessage shouldBe "Un-accounted for exclusion in NispConnectionNI: List()"
      }
    }
  }

}
