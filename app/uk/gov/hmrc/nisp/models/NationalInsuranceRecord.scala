/*
 * Copyright 2021 HM Revenue & Customs
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

import org.joda.time.LocalDate
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class NationalInsuranceRecord(
                                    qualifyingYears: Int,
                                    qualifyingYearsPriorTo1975: Int,
                                    numberOfGaps: Int,
                                    numberOfGapsPayable: Int,
                                    dateOfEntry: Option[LocalDate],
                                    homeResponsibilitiesProtection: Boolean,
                                    earningsIncludedUpTo: LocalDate,
                                    taxYears: List[NationalInsuranceTaxYear],
                                    reducedRateElection:Boolean
                                  )

object NationalInsuranceRecord {

  val readNullableBoolean: JsPath => Reads[Boolean] =
    jsPath => jsPath.readNullable[Boolean].map(_.getOrElse(false))

  implicit val reads: Reads[NationalInsuranceRecord] = (
    (JsPath \ "qualifyingYears").read[Int] and
      (JsPath \ "qualifyingYearsPriorTo1975").read[Int] and
      (JsPath \ "numberOfGaps").read[Int] and
      (JsPath \ "numberOfGapsPayable").read[Int] and
      (JsPath \ "dateOfEntry").readNullable[LocalDate] and
      (JsPath \ "homeResponsibilitiesProtection").read[Boolean] and
      (JsPath \ "earningsIncludedUpTo").read[LocalDate] and
      (JsPath \ "_embedded" \ "taxYears").read[JsValue].map {
        case obj: JsObject => List(obj.as[NationalInsuranceTaxYear])
        case other => other.as[List[NationalInsuranceTaxYear]]
      } and
      readNullableBoolean(JsPath \ "reducedRateElection")
    ) (NationalInsuranceRecord.apply _)

  implicit val writes: Writes[NationalInsuranceRecord] = (
    (JsPath \ "qualifyingYears").write[Int] and
      (JsPath \ "qualifyingYearsPriorTo1975").write[Int] and
      (JsPath \ "numberOfGaps").write[Int] and
      (JsPath \ "numberOfGapsPayable").write[Int] and
      (JsPath \ "dateOfEntry").writeNullable[LocalDate] and
      (JsPath \ "homeResponsibilitiesProtection").write[Boolean] and
      (JsPath \ "earningsIncludedUpTo").write[LocalDate] and
      (JsPath \ "_embedded" \ "taxYears").write[List[NationalInsuranceTaxYear]] and
      (JsPath \ "reducedRateElection").write[Boolean]
    ) (unlift(NationalInsuranceRecord.unapply))

  implicit val formats: Format[NationalInsuranceRecord] = Format(reads, writes)
}
