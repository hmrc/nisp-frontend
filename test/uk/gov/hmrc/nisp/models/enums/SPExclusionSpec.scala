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

package uk.gov.hmrc.nisp.models.enums

import Exclusion.Exclusion
import play.api.libs.json.{JsString, Json}
import uk.gov.hmrc.play.test.UnitSpec

/**
 * Created by callum on 02/03/15.
 */
class SPExclusionSpec extends UnitSpec {

  "SP Exclusion" when {
    "serialised into JSON" should {
      "output Dead when Dead is formatted" in {
        Json.toJson(Exclusion.Dead) shouldBe JsString("Dead")
      }

      "parse Dead when Dead is read" in {
        Json.fromJson[Exclusion](JsString("Dead")).get shouldBe Exclusion.Dead
      }
    }
  }
}
