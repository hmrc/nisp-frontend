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

case class StatePensionAmount(yearsToWork: Option[Int],
                              gapsToFill: Option[Int],
                              weeklyAmount: BigDecimal,
                              monthlyAmount: BigDecimal,
                              annualAmount: BigDecimal) {

}
object StatePensionAmount {
  implicit val formats = Json.format[StatePensionAmount]
}


case class StatePensionAmounts(protectedPayment: Boolean,
                               current: StatePensionAmount,
                               forecast: StatePensionAmount,
                               maximum: StatePensionAmount,
                               cope: StatePensionAmount)

object StatePensionAmounts {
  implicit val formats = Json.format[StatePensionAmounts]
}


case class StatePension(earningsIncludedUpTo: LocalDate,
                        amounts: StatePensionAmounts,
                        pensionAge: Int,
                        pensionDate: LocalDate,
                        finalRelevantYear: String,
                        numberOfQualifyingYears: Int,
                        pensionSharingOrder: Boolean,
                        currentFullWeeklyPensionAmount: BigDecimal)

object StatePension {
  implicit val formats = Json.format[StatePension]
}
