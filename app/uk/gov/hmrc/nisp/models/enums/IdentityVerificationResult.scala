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

import play.api.libs.json._

object IdentityVerificationResult extends Enumeration {
  type IdentityVerificationResult = Value
  val Success = Value
  val Incomplete = Value
  val FailedMatching = Value
  val InsufficientEvidence = Value
  val LockedOut = Value
  val UserAborted = Value
  val Timeout = Value
  val TechnicalIssue = Value
  val PreconditionFailed = Value

  implicit val formats = new Format[IdentityVerificationResult] {
    def reads(json: JsValue): JsResult[IdentityVerificationResult] = JsSuccess(IdentityVerificationResult.withName(json.as[String]) )
    def writes(result: IdentityVerificationResult): JsValue = JsString(result.toString)
  }
}
