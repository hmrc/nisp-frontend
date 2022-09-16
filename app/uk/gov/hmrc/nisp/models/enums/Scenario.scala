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

package uk.gov.hmrc.nisp.models.enums

import play.api.libs.json._
import uk.gov.hmrc.nisp.models.enums

object Scenario extends Enumeration {
  type Scenario = Value

  val Reached: enums.Scenario.Value = Value
  val ContinueWorkingMax: enums.Scenario.Value = Value
  val ContinueWorkingNonMax: enums.Scenario.Value = Value
  val FillGaps: enums.Scenario.Value = Value
  val ForecastOnly: enums.Scenario.Value = Value
  val CantGetPension: enums.Scenario.Value = Value

  implicit val formats: Format[Scenario] = new Format[Scenario] {
    def reads(json: JsValue): JsResult[Scenario] = JsSuccess(Scenario.withName(json.as[String]))
    def writes(scenario: Scenario): JsValue      = JsString(scenario.toString)
  }
}
