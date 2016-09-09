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

package uk.gov.hmrc.nisp.models

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.Json

case class SchemeMembership (
                              schemeStartDate: LocalDate,
                              schemeEndDate: LocalDate
                            ) {
  val dateFormat = DateTimeFormat.forPattern("MMMM yyyy")
  val startDateFormatted = schemeStartDate.toString(dateFormat)
  val endDateFormatted = schemeEndDate.toString(dateFormat)
}

object SchemeMembership {
  implicit val formats = Json.format[SchemeMembership]
}
