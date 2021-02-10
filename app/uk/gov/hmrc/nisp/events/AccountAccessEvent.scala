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

package uk.gov.hmrc.nisp.events

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import uk.gov.hmrc.nisp.models.enums.Scenario.Scenario
import uk.gov.hmrc.http.HeaderCarrier

object AccountAccessEvent {
  def apply(nino: String, statePensionAge: LocalDate, statePensionAmount: BigDecimal,
            statePensionForecast: BigDecimal, dateOfBirth: LocalDate, name: String,
            contractedOutFlag: Boolean = false, forecastScenario: Scenario, copeAmount: BigDecimal,
            authenticationProvider: String)(implicit hc: HeaderCarrier): AccountAccessEvent =
    new AccountAccessEvent(
      nino,
      statePensionAge,
      statePensionAmount,
      statePensionForecast,
      dateOfBirth,
      name,
      contractedOutFlag,
      forecastScenario,
      copeAmount,
      authenticationProvider
    )
}
class AccountAccessEvent(nino: String, statePensionAge: LocalDate, statePensionAmount: BigDecimal,
                         statePensionForecast: BigDecimal, dateOfBirth: LocalDate, name: String, contractedOutFlag: Boolean, forecastScenario: Scenario,
                         copeAmount: BigDecimal, authenticationProvider: String)(implicit hc: HeaderCarrier)
  extends NispBusinessEvent("AccountPage",
    Map(
      "nino" -> nino,
      "StatePensionAge" -> DateTimeFormat.forPattern("dd/MM/yyyy").print(statePensionAge),
      "StatePensionAmount" -> statePensionAmount.toString(),
      "StatePensionForecast" -> statePensionForecast.toString(),
      "DateOfBirth" -> DateTimeFormat.forPattern("dd/MM/yyyy").print(dateOfBirth),
      "Name" -> name,
      "ContractedOut" -> contractedOutFlag.toString,
      "ForecastScenario" -> forecastScenario.toString,
      "COPEAmount" -> copeAmount.toString(),
      "AuthenticationProvider" -> authenticationProvider
    )
)
