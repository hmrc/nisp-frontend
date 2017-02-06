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

package uk.gov.hmrc.nisp.models

import org.joda.time.LocalDate
import play.api.libs.json.{Format, Json, Reads, Writes}

case class NationalInsuranceRecord(
                                    qualifyingYears: Int,
                                    qualifyingYearsPriorTo1975: Int,
                                    numberOfGaps: Int,
                                    numberOfGapsPayable: Int,
                                    dateOfEntry: LocalDate,
                                    homeResponsibilitiesProtection: Boolean,
                                    earningsIncludedUpTo: LocalDate,
                                    taxYears: List[NationalInsuranceTaxYear]
                                  )

object NationalInsuranceRecord {
  implicit val reads: Reads[NationalInsuranceRecord] = Reads[NationalInsuranceRecord] { json =>
    for {
      qualifyingYears <- (json \ "qualifyingYears").validate[Int]
      qualifyingYearsPriorTo1975 <- (json \ "qualifyingYearsPriorTo1975").validate[Int]
      numberOfGaps <- (json \ "numberOfGaps").validate[Int]
      numberOfGapsPayable <- (json \ "numberOfGapsPayable").validate[Int]
      dateOfEntry <- (json \ "dateOfEntry").validate[LocalDate]
      homeResponsibilitiesProtection <- (json \ "homeResponsibilitiesProtection").validate[Boolean]
      earningsIncludedUpTo <- (json \ "earningsIncludedUpTo").validate[LocalDate]
      taxYears <- (json \ "_embedded" \ "taxYears").validate[List[NationalInsuranceTaxYear]]
    } yield {
      NationalInsuranceRecord(
        qualifyingYears,
        qualifyingYearsPriorTo1975,
        numberOfGaps,
        numberOfGapsPayable,
        dateOfEntry,
        homeResponsibilitiesProtection,
        earningsIncludedUpTo,
        taxYears
      )
    }
  }
  implicit val writes: Writes[NationalInsuranceRecord] = Json.writes[NationalInsuranceRecord]
  implicit val formats: Format[NationalInsuranceRecord] = Format(reads, writes)
}
