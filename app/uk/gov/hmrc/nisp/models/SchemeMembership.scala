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

import org.joda.time.format.DateTimeFormat
import play.api.libs.json.Json

case class SchemeMembership (
                              schemeStartDate: Option[NpsDate],
                              schemeEndDate: Option[NpsDate]
                            ) {
  val dateFormat = DateTimeFormat.forPattern("MMM yyyy")
  val startDateFormatted = schemeStartDate.map(_.localDate.toString(dateFormat)).getOrElse("Wrong")
  val endDateFormatted = schemeEndDate.map(_.localDate.toString(dateFormat)).getOrElse("April 2016")
}

object SchemeMembership {
  implicit val formats = Json.format[SchemeMembership]
}
