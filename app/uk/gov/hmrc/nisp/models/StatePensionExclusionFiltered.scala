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

import java.time.LocalDate
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.time.TaxYear
import play.api.libs.json.JodaReads._
import play.api.libs.json.JodaWrites._

trait StatePensionExcl {
  def exclusion: Exclusion
}

trait CopeData {
  def copeAvailableDate: LocalDate
  def previousAvailableDate: Option[LocalDate]
}

case class StatePensionExclusionFiltered(
  exclusion: Exclusion,
  pensionAge: Option[Int] = None,
  pensionDate: Option[LocalDate] = None,
  statePensionAgeUnderConsideration: Option[Boolean] = None
) extends StatePensionExcl {
  val finalRelevantStartYear: Option[Int] = pensionDate.map(TaxYear.taxYearFor(_).back(1).startYear)
}

case class StatePensionExclusionFilteredWithCopeDate(
  exclusion: Exclusion,
  copeAvailableDate: LocalDate,
  previousAvailableDate: Option[LocalDate] = None
) extends StatePensionExcl with CopeData


object StatePensionExclusionFiltered {
  implicit val statePensionExclusionFilteredFormats: OFormat[StatePensionExclusionFiltered] = Json.format[StatePensionExclusionFiltered]
}

object StatePensionExclusionFilteredWithCopeDate {
  implicit val copeDataFormats: OFormat[StatePensionExclusionFilteredWithCopeDate] = Json.format[StatePensionExclusionFilteredWithCopeDate]
}
