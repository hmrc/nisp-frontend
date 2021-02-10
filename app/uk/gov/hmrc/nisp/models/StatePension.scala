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
import play.api.libs.json.{JsPath, Json, Reads}
import uk.gov.hmrc.nisp.models.enums.{MQPScenario, Scenario}
import uk.gov.hmrc.nisp.models.enums.MQPScenario.MQPScenario
import uk.gov.hmrc.nisp.models.enums.Scenario.Scenario
import uk.gov.hmrc.nisp.utils.Constants
import play.api.libs.functional.syntax._

sealed trait StatePensionAmount {
  val weeklyAmount: BigDecimal
  val monthlyAmount: BigDecimal
  val annualAmount: BigDecimal
}

case class StatePensionAmountRegular(weeklyAmount: BigDecimal,
                                     monthlyAmount: BigDecimal,
                                     annualAmount: BigDecimal) extends StatePensionAmount

object StatePensionAmountRegular {
  implicit val formats = Json.format[StatePensionAmountRegular]
}

case class StatePensionAmountForecast(yearsToWork: Int,
                                      weeklyAmount: BigDecimal,
                                      monthlyAmount: BigDecimal,
                                      annualAmount: BigDecimal) extends StatePensionAmount {
}

object StatePensionAmountForecast {
  implicit val formats = Json.format[StatePensionAmountForecast]
}

case class StatePensionAmountMaximum(yearsToWork: Int,
                                     gapsToFill: Int,
                                     weeklyAmount: BigDecimal,
                                     monthlyAmount: BigDecimal,
                                     annualAmount: BigDecimal) extends StatePensionAmount

object StatePensionAmountMaximum {
  implicit val formats = Json.format[StatePensionAmountMaximum]
}

case class StatePensionAmounts(protectedPayment: Boolean,
                               current: StatePensionAmountRegular,
                               forecast: StatePensionAmountForecast,
                               maximum: StatePensionAmountMaximum,
                               cope: StatePensionAmountRegular)

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
                        currentFullWeeklyPensionAmount: BigDecimal,
                        reducedRateElection: Boolean,
                        statePensionAgeUnderConsideration: Boolean) {

  lazy val contractedOut: Boolean = amounts.cope.weeklyAmount > 0

  lazy val forecastScenario: Scenario = {
    if (amounts.maximum.weeklyAmount == 0) {
      Scenario.CantGetPension
    } else if(amounts.maximum.weeklyAmount > amounts.forecast.weeklyAmount) {
      Scenario.FillGaps
    } else {
      if(amounts.forecast.weeklyAmount > amounts.current.weeklyAmount) {

        if (amounts.forecast.weeklyAmount >= currentFullWeeklyPensionAmount)
          Scenario.ContinueWorkingMax
        else Scenario.ContinueWorkingNonMax

      } else if(amounts.forecast.weeklyAmount == amounts.current.weeklyAmount) {
        Scenario.Reached
      } else {
        Scenario.ForecastOnly
      }
    }
  }

  lazy val mqpScenario: Option[MQPScenario] = {
    if (amounts.current.weeklyAmount > 0 && numberOfQualifyingYears >= Constants.minimumQualifyingYearsNSP) {
      None
    } else {
      if (amounts.forecast.weeklyAmount > 0) {
        Some(MQPScenario.ContinueWorking)
      } else {
        if (amounts.maximum.weeklyAmount > 0) {
          Some(MQPScenario.CanGetWithGaps)
        } else {
          Some(MQPScenario.CantGet)
        }
      }
    }
  }

  lazy val finalRelevantStartYear: Int = Integer.parseInt(finalRelevantYear.substring(0, 4))
  lazy val finalRelevantEndYear: Int = finalRelevantStartYear + 1
}

object StatePension {
  val readNullableBoolean: JsPath => Reads[Boolean] =
    jsPath => jsPath.readNullable[Boolean].map(_.getOrElse(false))

  implicit val reads: Reads[StatePension] = (
    (JsPath \ "earningsIncludedUpTo").read[LocalDate] and
    (JsPath \ "amounts").read[StatePensionAmounts] and
    (JsPath \ "pensionAge").read[Int] and
    (JsPath \ "pensionDate").read[LocalDate] and
    (JsPath \ "finalRelevantYear").read[String] and
    (JsPath \ "numberOfQualifyingYears").read[Int] and
    (JsPath \ "pensionSharingOrder").read[Boolean] and
    (JsPath \ "currentFullWeeklyPensionAmount").read[BigDecimal] and
    readNullableBoolean(JsPath \ "reducedRateElection") and
    readNullableBoolean(JsPath \ "statePensionAgeUnderConsideration")
  ) (StatePension.apply _)

  implicit val writes = Json.writes[StatePension]
}
