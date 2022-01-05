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

import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue, Json, JsonValidationError, OFormat, Reads, Writes}
import uk.gov.hmrc.time.TaxYear

trait StatePensionExclusion extends Exclusion

object StatePensionExclusion {
  case class OkStatePensionExclusion(
                                      exclusionReasons: List[Exclusion],
                                      pensionAge: Option[Int] = None,
                                      pensionDate: Option[LocalDate] = None,
                                      statePensionAgeUnderConsideration: Option[Boolean] = None) extends StatePensionExclusion {
    val finalRelevantStartYear: Option[Int] = pensionDate.map(TaxYear.taxYearFor(_).back(1).startYear)
  }

  object OkStatePensionExclusion {
    val dateWrites: Writes[LocalDate] = Writes[LocalDate] {
      date => JsString(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
    }

    implicit val dateFormats: Format[LocalDate] = Format[LocalDate](Reads.localDateReads("yyyy-MM-dd"), dateWrites)
    implicit val formats: OFormat[OkStatePensionExclusion] = Json.format[OkStatePensionExclusion]
  }

  case class ForbiddenStatePensionExclusion(code: Exclusion, message: Option[String]) extends StatePensionExclusion

  object ForbiddenStatePensionExclusion {
    implicit val formats: OFormat[ForbiddenStatePensionExclusion] = Json.format[ForbiddenStatePensionExclusion]
  }

  case class CopeStatePensionExclusion(code: Exclusion, copeDataAvailableDate: LocalDate, previousAvailableDate: Option[LocalDate])
    extends StatePensionExclusion

  object CopeStatePensionExclusion {
    val dateWrites: Writes[LocalDate] = Writes[LocalDate] {
      date => JsString(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
    }

    implicit val dateFormats: Format[LocalDate] = Format[LocalDate](Reads.localDateReads("yyyy-MM-dd"), dateWrites)
    implicit val formats: OFormat[CopeStatePensionExclusion] = Json.format[CopeStatePensionExclusion]
  }

  case class StatePensionExclusionFiltered(
                                            exclusion: Exclusion,
                                            pensionAge: Option[Int] = None,
                                            pensionDate: Option[LocalDate] = None,
                                            statePensionAgeUnderConsideration: Option[Boolean] = None
                                          ) extends StatePensionExclusion {
    val finalRelevantStartYear: Option[Int] = pensionDate.map(TaxYear.taxYearFor(_).back(1).startYear)
  }

  object StatePensionExclusionFiltered {
    val dateWrites: Writes[LocalDate] = Writes[LocalDate] {
      date => JsString(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
    }

    implicit val dateFormats: Format[LocalDate] = Format[LocalDate](Reads.localDateReads("yyyy-MM-dd"), dateWrites)
    implicit val formats: OFormat[StatePensionExclusionFiltered] = Json.format[StatePensionExclusionFiltered]
  }

  case class StatePensionExclusionFilteredWithCopeDate(
                                                        exclusion: Exclusion,
                                                        copeDataAvailableDate: LocalDate,
                                                        previousAvailableDate: Option[LocalDate] = None
                                                      ) extends StatePensionExclusion


  object StatePensionExclusionFilteredWithCopeDate {
    val dateWrites: Writes[LocalDate] = Writes[LocalDate] {
      date => JsString(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
    }

    implicit val dateFormats: Format[LocalDate] = Format[LocalDate](Reads.localDateReads("yyyy-MM-dd"), dateWrites)
    implicit val copeDataFormats: OFormat[StatePensionExclusionFilteredWithCopeDate] = Json.format[StatePensionExclusionFilteredWithCopeDate]
  }

  implicit object StatePensionExclusionFormats extends Format[StatePensionExclusion] {
    override def reads(json: JsValue): JsResult[StatePensionExclusion] = {
      println(s"\n\n\n\njson = $json\n\n\n")
      println(s"\n\n\n\nparsed = ${json.validate[ForbiddenStatePensionExclusion]}\n\n\n")
      if (json.validate[OkStatePensionExclusion].isSuccess) JsSuccess(json.as[OkStatePensionExclusion])
      else if (json.validate[CopeStatePensionExclusion].isSuccess) JsSuccess(json.as[CopeStatePensionExclusion])
      else if (json.validate[ForbiddenStatePensionExclusion].isSuccess) JsSuccess(json.as[ForbiddenStatePensionExclusion])
      else if (json.validate[StatePensionExclusionFiltered].isSuccess) JsSuccess(json.as[StatePensionExclusionFiltered])
      else if (json.validate[StatePensionExclusionFilteredWithCopeDate].isSuccess) JsSuccess(json.as[StatePensionExclusionFilteredWithCopeDate])
      else JsError(JsonValidationError("Unable to parse json as StatePensionExclusion"))
    }

    override def writes(spExclusion: StatePensionExclusion): JsValue = {
      spExclusion match {
        case okStatePensionExclusion: OkStatePensionExclusion => OkStatePensionExclusion.formats.writes(okStatePensionExclusion)
        case copeStatePensionExclusion: CopeStatePensionExclusion => CopeStatePensionExclusion.formats.writes(copeStatePensionExclusion)
        case forbiddenStatePensionExclusion: ForbiddenStatePensionExclusion =>
          ForbiddenStatePensionExclusion.formats.writes(forbiddenStatePensionExclusion)
        case statePensionExclusionFiltered: StatePensionExclusionFiltered =>
          StatePensionExclusionFiltered.formats.writes(statePensionExclusionFiltered)
        case statePensionExclusionFilteredWithCopeDate: StatePensionExclusionFilteredWithCopeDate =>
          StatePensionExclusionFilteredWithCopeDate.copeDataFormats.writes(statePensionExclusionFilteredWithCopeDate)
      }
    }
  }
}





