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

package uk.gov.hmrc.nisp.models.enums

import play.api.i18n.Messages
import play.api.libs.json._

object SPExclusion extends Enumeration {
  type SPExclusion = Value
  val Abroad = Value
  val MWRRE = Value
  val CustomerTooOld = Value
  val ContractedOut = Value
  val Dead = Value
  val IOM = Value
  val AmountDissonance = Value

  implicit val formats = new Format[SPExclusion] {
    def reads(json: JsValue): JsResult[SPExclusion] = JsSuccess(SPExclusion.withName(json.as[String]) )
    def writes(spExclusion: SPExclusion): JsValue = JsString(spExclusion.toString)
  }
}
