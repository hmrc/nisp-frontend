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

package uk.gov.hmrc.nisp.events

import uk.gov.hmrc.nisp.models.NpsDate
import uk.gov.hmrc.nisp.models.enums.ABTest.ABTest
import uk.gov.hmrc.nisp.models.enums.SPContextMessage.SPContextMessage
import uk.gov.hmrc.play.http.HeaderCarrier

object AccountAccessEvent {
  def apply(nino: String, contextMessage: Option[SPContextMessage], statePensionAge: NpsDate, statePensionAmount: BigDecimal, statePensionForecast: BigDecimal,
            dateOfBirth: NpsDate, name: Option[String], contractedOutFlag: Boolean = false, forecastOnlyFlag: Boolean = false, abTest: Option[ABTest],
            copeAmount: BigDecimal)(implicit hc: HeaderCarrier): AccountAccessEvent =
    new AccountAccessEvent(
      nino,
      contextMessage,
      statePensionAge,
      statePensionAmount,
      statePensionForecast,
      dateOfBirth,
      name.getOrElse("N/A"),
      contractedOutFlag,
      forecastOnlyFlag,
      abTest,
      copeAmount
    )
}
class AccountAccessEvent(nino: String, contextMessage: Option[SPContextMessage], statePensionAge: NpsDate, statePensionAmount: BigDecimal,
                         statePensionForecast: BigDecimal, dateOfBirth: NpsDate, name: String, contractedOutFlag: Boolean, forecastOnlyFlag: Boolean,
                         abTest: Option[ABTest], copeAmount: BigDecimal)(implicit hc: HeaderCarrier)
  extends NispBusinessEvent("AccountPage",
    Map(
      "nino" -> nino,
      "Scenario" -> contextMessage.fold("")(_.toString),
      "StatePensionAge" -> statePensionAge.toNpsString,
      "StatePensionAmount" -> statePensionAmount.toString(),
      "StatePensionForecast" -> statePensionForecast.toString(),
      "DateOfBirth" -> dateOfBirth.toNpsString,
      "Name" -> name,
      "ContractedOut" -> contractedOutFlag.toString,
      "ForecastOnly" -> forecastOnlyFlag.toString,
      "ABTest" -> abTest.map(_.toString).getOrElse("None"),
      "COPEAmount" -> copeAmount.toString()
    )
)
