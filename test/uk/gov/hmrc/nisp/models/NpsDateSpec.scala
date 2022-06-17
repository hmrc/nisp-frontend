/*
 * Copyright 2022 HM Revenue & Customs
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

import java.time.LocalDate
import play.api.libs.json.{JsNull, JsString, Json}
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.nisp.utils.UnitSpec

class NpsDateSpec extends UnitSpec {
  "NpsDate" when {
    "JSON parsing" should {
      "return a JSError for null date" in {
        JsNull.validate[NpsDate].isError shouldBe true
      }
    }

    "JSON serialisation" should {
      "return JSString in correct format" in {
        Json.toJson(NpsDate(LocalDate.of(2015, 1, 1))) shouldBe JsString("01/01/2015")
      }
      "deserialise works" in {
        Json.fromJson[NpsDate](JsString("01/01/2015")).get shouldBe NpsDate(LocalDate.of(2015, 1, 1))
      }
    }

    "taxYearEndDate" should {
      "return tax year end date" in {
        NpsDate.taxYearEndDate(2015) shouldBe NpsDate(2016, Constants.taxYearsStartEndMonth, Constants.taxYearEndDay)
      }
    }

    "taxYearStartDate" should {
      "return tax year start date" in {
        NpsDate
          .taxYearStartDate(2015) shouldBe NpsDate(2015, Constants.taxYearsStartEndMonth, Constants.taxYearStartDay)
      }
    }
  }
}
