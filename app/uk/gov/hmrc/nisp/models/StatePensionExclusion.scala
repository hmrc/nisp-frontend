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
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, Reads}
import uk.gov.hmrc.nisp.models.enums.Exclusion
import uk.gov.hmrc.time.TaxYear

case class StatePensionExclusion(exclusionReasons: List[Exclusion.Exclusion],
                                 pensionAge: Option[Int] = None,
                                 pensionDate: Option[LocalDate] = None,
                                 statePensionAgeUnderConsideration: Option[Boolean] = None) {
  val finalRelevantStartYear: Option[Int] = pensionDate.map(TaxYear.taxYearFor(_).back(1).startYear)
}

object StatePensionExclusion {
  implicit val formats = Json.format[StatePensionExclusion]
}

