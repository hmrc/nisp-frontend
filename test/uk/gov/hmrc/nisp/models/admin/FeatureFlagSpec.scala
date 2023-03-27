/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.models.admin

import play.api.libs.json.{JsResultException, JsString}
import uk.gov.hmrc.nisp.utils.UnitSpec

class FeatureFlagSpec extends UnitSpec {

  "Valid string" must {
    "build a FeatureFlagName" in {
      val result = JsString("pertax-backend-toggle").as[FeatureFlagName]
      result shouldBe PertaxBackendToggle
    }
  }

  "Invalid string" must {
    "throw an exception" in {
      val result = intercept[JsResultException] {
        JsString("invalid").as[FeatureFlagName]
      }
      result.errors.toString() shouldBe "List((,List(JsonValidationError(List(Unknown FeatureFlagName),ArraySeq()))))"
    }
  }
}
