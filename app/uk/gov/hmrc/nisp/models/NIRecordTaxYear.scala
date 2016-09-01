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
import play.api.libs.json.Json
import uk.gov.hmrc.nisp.utils.Constants

case class NIRecordTaxYear(taxYear: Int, qualifying: Boolean,classOneContributions: BigDecimal,
                           classTwoCredits: Int, classThreeCredits: Int, otherCredits: Int,
                           classThreePayable: Option[BigDecimal], classThreePayableBy: Option[NpsDate],
                           classThreePayableByPenalty: Option[NpsDate], payable: Boolean, underInvestigation: Boolean) {

  val displayableTaxYear: String = s"$taxYear-${(taxYear + 1).toString.substring(Constants.shortYearStartCharacter,Constants.shortYearEndCharacter)}"

   val checkCutOffDate: Boolean = classThreePayableBy match {

    case Some(localClassThreePayableBy) => localClassThreePayableBy.localDate.isAfter(new LocalDate())
    case None => false

  }

  def cutOffDate(classThreePayableBy: Option[NpsDate], currentDate: LocalDate): Boolean = { 
    classThreePayableBy match {
      case Some(classThreePayableByDate)  => classThreePayableByDate.localDate.isAfter(currentDate)
      case None => false
    }
  }

}

object NIRecordTaxYear {
  implicit val formats = Json.format[NIRecordTaxYear]

}
