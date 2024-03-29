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

package uk.gov.hmrc.nisp.models.enums

import play.api.libs.json._
import uk.gov.hmrc.nisp.models.enums

object MQPScenario extends Enumeration {
  type MQPScenario = Value

  val ContinueWorking: enums.MQPScenario.Value = Value
  val CantGet: enums.MQPScenario.Value = Value
  val CanGetWithGaps: enums.MQPScenario.Value = Value

  implicit val formats: Format[MQPScenario] = new Format[MQPScenario] {
    def reads(json: JsValue): JsResult[MQPScenario] = JsSuccess(MQPScenario.withName(json.as[String]))
    def writes(MQPScenario: MQPScenario): JsValue   = JsString(MQPScenario.toString)
  }
}
