/*
 * Copyright 2022 HM Revenue & Customs
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
import java.time.format.DateTimeFormatter
import play.api.libs.json._
import uk.gov.hmrc.nisp.models.NpsDate.{dateFormat, pattern}
import uk.gov.hmrc.nisp.utils.Constants

case class NpsDate(localDate: LocalDate) {
  val toNpsString: String = NpsDate.dateFormat.writes(dateFormat(localDate)).toString

  val taxYear: Int = {
    val year = localDate.getYear
    if (localDate.isBefore(LocalDate.of(year, Constants.taxYearsStartEndMonth, Constants.taxYearStartDay))) year - 1
    else year
  }
}

object NpsDate {

  private val pattern = "dd/MM/yyyy"
  private def writes(pattern: String): Writes[LocalDate] = {
    val datePattern = DateTimeFormatter.ofPattern(pattern)

    Writes[LocalDate] { localDate => JsString(localDate.format(datePattern))}
  }

  implicit val dateFormat: Format[LocalDate] = Format[LocalDate](Reads.localDateReads(pattern), writes(pattern))
  private val npsDateRegex = """^(\d\d)/(\d\d)/(\d\d\d\d)$""".r

  implicit val reads = new Reads[NpsDate] {
    override def reads(json: JsValue): JsResult[NpsDate] =
      json match {
        case JsString(npsDateRegex(d, m, y)) => JsSuccess(NpsDate(LocalDate.of(y.toInt, m.toInt, d.toInt)))
        case JsNull                          => JsError(JsonValidationError("Null date cannot convert to NpsDate"))
      }
  }

  implicit val writes: Writes[NpsDate] = (date: NpsDate) => JsString(date.toNpsString)
  def taxYearEndDate(taxYear: Int): NpsDate           =
    NpsDate(taxYear + 1, Constants.taxYearsStartEndMonth, Constants.taxYearEndDay)
  def taxYearStartDate(taxYear: Int): NpsDate         =
    NpsDate(taxYear, Constants.taxYearsStartEndMonth, Constants.taxYearStartDay)
  def apply(year: Int, month: Int, day: Int): NpsDate = NpsDate(LocalDate.of(year, month, day))
}
