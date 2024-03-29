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

package uk.gov.hmrc.nisp.models

import play.api.libs.json.Json
import uk.gov.hmrc.nisp.models.StatePensionExclusion.{CopeStatePensionExclusion, ForbiddenStatePensionExclusion, OkStatePensionExclusion}
import uk.gov.hmrc.nisp.utils.UnitSpec

import java.time.LocalDate

class StatePensionExclusionSpec extends UnitSpec {

  "finalRelevantStartYear" when {
    "there is no pension date" should {
      "be none" in {
        OkStatePensionExclusion(List(Exclusion.Dead), None, None).finalRelevantStartYear shouldBe None
      }
    }

    "the pensionDate is 5th April 2020" should {
      "be 2018" in {
        OkStatePensionExclusion(List(Exclusion.AmountDissonance), Some(67), Some(LocalDate.of(2020, 4, 5))).finalRelevantStartYear shouldBe Some(2018)
      }
    }

    "the pensionDate is 6th April 2020" should {
      "be 2019" in {
        OkStatePensionExclusion(List(Exclusion.AmountDissonance), Some(67), Some(LocalDate.of(2020, 4, 6))).finalRelevantStartYear shouldBe Some(2019)
      }
    }

    "the pensionDate is 6th April 2000" should {
      "be 1999" in {
        OkStatePensionExclusion(List(Exclusion.AmountDissonance, Exclusion.PostStatePensionAge), Some(65), Some(LocalDate.of(2000, 4, 6))).finalRelevantStartYear shouldBe Some(1999)
      }
    }

    "the pensionDate is 5th April 2020 and there is no flag for state pension age under consideration" should {
      "be 2018" in {
        OkStatePensionExclusion(List(Exclusion.AmountDissonance), Some(67), Some(LocalDate.of(2020, 4, 5))).finalRelevantStartYear shouldBe Some(2018)
      }
    }

    "the pensionDate is 5th April 2020 and there is a true flag for state pension age under consideration" should {
      "be 2018" in {
        OkStatePensionExclusion(List(Exclusion.AmountDissonance), Some(67), Some(LocalDate.of(2020, 4, 5)), Some(true)).finalRelevantStartYear shouldBe Some(2018)
      }
    }

    "the pensionDate is 5th April 2020 and there is a false flag for state pension age under consideration" should {
      "be 2018" in {
        OkStatePensionExclusion(List(Exclusion.AmountDissonance), Some(67), Some(LocalDate.of(2020, 4, 5)), Some(false)).finalRelevantStartYear shouldBe Some(2018)
      }
    }

    "OkStatePensionExclusion" should {
      "de-serialise correctly" in {
        val okStatePensionExclusion = OkStatePensionExclusion(
          exclusionReasons = List(Exclusion.ManualCorrespondenceIndicator),
          pensionAge = Some(65),
          pensionDate = Some(LocalDate.parse("2030-01-01")),
          statePensionAgeUnderConsideration = Some(true)
        )

        val okStatePensionExclusionJson = Json.obj(
          "exclusionReasons" -> Json.arr("EXCLUSION_MANUAL_CORRESPONDENCE"),
          "pensionAge" -> 65,
          "pensionDate" -> "2030-01-01",
          "statePensionAgeUnderConsideration" -> true
        )

        okStatePensionExclusionJson.as[OkStatePensionExclusion] shouldBe okStatePensionExclusion
      }

      "serialise correctly" in {
        val okStatePensionExclusion = OkStatePensionExclusion(
          exclusionReasons = List(Exclusion.ManualCorrespondenceIndicator),
          pensionAge = Some(65),
          pensionDate = Some(LocalDate.parse("2030-01-01")),
          statePensionAgeUnderConsideration = Some(true)
        )

        val okStatePensionExclusionJson = Json.obj(
          "exclusionReasons" -> Json.arr("ManualCorrespondenceIndicator"),
          "pensionAge" -> 65,
          "pensionDate" -> "2030-01-01",
          "statePensionAgeUnderConsideration" -> true
        )

        Json.toJson(okStatePensionExclusion) shouldBe okStatePensionExclusionJson
      }
    }

    "CopeStatePensionExclusion" should {
      "de-serialise correctly" in {
        val copeStatePensionExclusion = CopeStatePensionExclusion(
          code = Exclusion.CopeProcessing,
          copeDataAvailableDate = LocalDate.parse("2030-01-01"),
          previousAvailableDate = Some(LocalDate.parse("2035-05-11"))
        )

        val copeStatePensionExclusionJson = Json.obj(
          "code" -> "EXCLUSION_COPE_PROCESSING",
          "copeDataAvailableDate" -> "2030-01-01",
          "previousAvailableDate" -> "2035-05-11"
        )

        copeStatePensionExclusionJson.as[CopeStatePensionExclusion] shouldBe copeStatePensionExclusion
      }

      "serialise correctly" in {
        val copeStatePensionExclusion = CopeStatePensionExclusion(
          code = Exclusion.CopeProcessing,
          copeDataAvailableDate = LocalDate.parse("2030-01-01"),
          previousAvailableDate = Some(LocalDate.parse("2035-05-11"))
        )

        val copeStatePensionExclusionJson = Json.obj(
          "code" -> "CopeProcessing",
          "copeDataAvailableDate" -> "2030-01-01",
          "previousAvailableDate" -> "2035-05-11"
        )

        Json.toJson(copeStatePensionExclusion) shouldBe copeStatePensionExclusionJson
      }
    }

    "ForbiddenStatePensionExclusion" should {
      "de-serialise correctly" in {
        val forbiddenStatePensionExclusion = ForbiddenStatePensionExclusion(
          code = Exclusion.Dead,
          message = Some("To be or not to be ? That is the question !!")
        )

        val forbiddenStatePensionExclusionJson = Json.obj(
          "code" -> "EXCLUSION_DEAD",
          "message" -> "To be or not to be ? That is the question !!"
        )

        forbiddenStatePensionExclusionJson.as[ForbiddenStatePensionExclusion] shouldBe forbiddenStatePensionExclusion
      }

      "serialise correctly" in {
        val forbiddenStatePensionExclusion = ForbiddenStatePensionExclusion(
          code = Exclusion.Dead,
          message = Some("To be or not to be ? That is the question !!")
        )

        val forbiddenStatePensionExclusionJson = Json.obj(
          "code" -> "Dead",
          "message" -> "To be or not to be ? That is the question !!"
        )

        Json.toJson(forbiddenStatePensionExclusion) shouldBe forbiddenStatePensionExclusionJson
      }
    }

  }
}
