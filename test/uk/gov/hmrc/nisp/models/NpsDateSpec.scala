/*
 * Copyright 2017 HM Revenue & Customs
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

import org.joda.time.LocalDate
import play.api.libs.json.{JsString, Json, JsError, JsNull}
import uk.gov.hmrc.nisp.models.NpsDate
import uk.gov.hmrc.play.test.UnitSpec

class NpsDateSpec extends UnitSpec {
  "NpsDate" when {
    "JSON parsing" should {
      "return a JSError for null date" in {
        JsNull.validate[NpsDate].isError shouldBe true
      }
    }

    "JSON serialisation" should {
      "return JSString in correct format" in {
        Json.toJson(NpsDate(new LocalDate(2015,1,1))) shouldBe JsString("01/01/2015")
      }
    }
  }
}
