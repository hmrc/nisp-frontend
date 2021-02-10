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
import play.api.libs.json.{Format, Json}

case class NationalInsuranceTaxYear(
                                     taxYear: String,
                                     qualifying: Boolean,
                                     classOneContributions: BigDecimal,
                                     classTwoCredits: Int,
                                     classThreeCredits: Int,
                                     otherCredits: Int,
                                     classThreePayable: BigDecimal,
                                     classThreePayableBy: Option[LocalDate],
                                     classThreePayableByPenalty: Option[LocalDate],
                                     payable: Boolean,
                                     underInvestigation: Boolean
                                   ) {

  def currentDateAfterCutOff(currentDate: LocalDate): Boolean = {
    classThreePayableBy match {
      case Some(classThreeDate) => currentDate.isAfter(classThreeDate)
      case None => payable
    }
  }

}



object NationalInsuranceTaxYear {
  implicit val formats: Format[NationalInsuranceTaxYear] = Json.format[NationalInsuranceTaxYear]
}
