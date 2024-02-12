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

import play.api.libs.json._

trait Exclusion

object Exclusion {

  case object MarriedWomenReducedRateElection extends Exclusion
  case object ContractedOut extends Exclusion
  case object Dead extends Exclusion
  case object IsleOfMan extends Exclusion
  case object AmountDissonance extends Exclusion
  case object PostStatePensionAge extends Exclusion
  case object ManualCorrespondenceIndicator extends Exclusion
  case object CopeProcessing extends Exclusion
  case object CopeProcessingFailed extends Exclusion

  implicit object ExclusionFormat extends Format[Exclusion] {
    override def reads(json: JsValue): JsResult[Exclusion] = json match {
      case JsString("EXCLUSION_MARRIED_WOMEN_REDUCED_RATE_ELECTION") => JsSuccess(Exclusion.MarriedWomenReducedRateElection)
      case JsString("MarriedWomenReducedRateElection") => JsSuccess(Exclusion.MarriedWomenReducedRateElection)
      case JsString("EXCLUSION_CONTRACTED_OUT") => JsSuccess(Exclusion.ContractedOut)
      case JsString("EXCLUSION_DEAD") => JsSuccess(Exclusion.Dead)
      case JsString("EXCLUSION_ISLE_OF_MAN") => JsSuccess(Exclusion.IsleOfMan)
      case JsString("IsleOfMan") => JsSuccess(Exclusion.IsleOfMan)
      case JsString("AmountDissonance") => JsSuccess(Exclusion.AmountDissonance)
      case JsString("PostStatePensionAge") => JsSuccess(Exclusion.PostStatePensionAge)
      case JsString("EXCLUSION_MANUAL_CORRESPONDENCE") => JsSuccess(Exclusion.ManualCorrespondenceIndicator)
      case JsString("EXCLUSION_COPE_PROCESSING") => JsSuccess(Exclusion.CopeProcessing)
      case JsString("EXCLUSION_COPE_PROCESSING_FAILED") => JsSuccess(Exclusion.CopeProcessingFailed)
      case _ => JsError("Exclusion not valid!")
    }

    override def writes(ex: Exclusion): JsValue = JsString(ex.toString)
  }

}
