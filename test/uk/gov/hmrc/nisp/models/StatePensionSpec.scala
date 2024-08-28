/*
 * Copyright 2024 HM Revenue & Customs
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
import uk.gov.hmrc.nisp.models.enums.{MQPScenario, Scenario}
import uk.gov.hmrc.nisp.utils.UnitSpec

class StatePensionSpec extends UnitSpec {

  def createStatePension(
    cope: BigDecimal = 0,
    finalRelevantYear: String = "2018-19",
    earningsIncludedUpTo: Int = 2015,
    currentAmount: BigDecimal = 0,
    forecastAmount: BigDecimal = 0,
    maximumAmount: BigDecimal = 0,
    fullStatePensionAmount: BigDecimal = 155.65,
    qualifyingYears: Int = 30,
    statePensionAgeUnderConsideration: Boolean = false
  ) =
    StatePension(
      LocalDate.of(earningsIncludedUpTo + 1, 4, 5),
      amounts = StatePensionAmounts(
        false,
        StatePensionAmountRegular(currentAmount, currentAmount / 7 * 365.25 / 12, currentAmount / 7 * 365.25),
        StatePensionAmountForecast(0, forecastAmount, forecastAmount / 7 * 365.25 / 12, forecastAmount / 7 * 365.25),
        StatePensionAmountMaximum(0, 0, maximumAmount, maximumAmount / 7 * 365.25 / 12, maximumAmount / 7 * 365.25),
        cope = StatePensionAmountRegular(cope, cope / 7 * 365.25 / 12, cope / 7 * 365.25)
      ),
      65,
      LocalDate.of(2019, 5, 1),
      finalRelevantYear,
      qualifyingYears,
      false,
      fullStatePensionAmount,
      false,
      statePensionAgeUnderConsideration
    )

  "contractedOut" should {
    "return true when the user has a COPE amount more than 0" in {
      createStatePension(cope = 0.87).contractedOut shouldBe true
    }
    "return false when the user has a COPE amount of 0" in {
      createStatePension().contractedOut shouldBe false
    }
  }

  "finalRelevantStartYear" should {
    "should be 2017 when finalRelevantYear is \"2017-18\"" in {
      createStatePension(finalRelevantYear = "2017-18").finalRelevantStartYear shouldBe 2017
    }
    "should be 2000 when finalRelevantYear is \"2000-01\"" in {
      createStatePension(finalRelevantYear = "2000-01").finalRelevantStartYear shouldBe 2000
    }
    "should be 1911 when finalRelevantYear is \"1911-12\"" in {
      createStatePension(finalRelevantYear = "1911-12").finalRelevantStartYear shouldBe 1911
    }
  }

  "finalRelevantEndYear" should {
    "should be 2018 when finalRelevantYear is \"2017-18\"" in {
      createStatePension(finalRelevantYear = "2017-18").finalRelevantEndYear shouldBe 2018
    }
    "should be 2000 when finalRelevantYear is \"1999-00\"" in {
      createStatePension(finalRelevantYear = "1999-00").finalRelevantEndYear shouldBe 2000
    }
    "should be 2900 when finalRelevantYear is \"2899-00\"" in {
      createStatePension(finalRelevantYear = "2899-00").finalRelevantEndYear shouldBe 2900
    }
  }

  "mqpScenario" should {

    "should be an MQP Scenario if they have less than 10 years" in {
      createStatePension(
        qualifyingYears = 4,
        currentAmount = 50).mqpScenario.isDefined shouldBe true
    }

    "be None if they have a Current Amount of more than 0" in {
      createStatePension(currentAmount = 122.34).mqpScenario shouldBe None
    }
    "be ContinueWorking if they have a Current Amount of 0, ForecastAmount more than 0" in {
      createStatePension(forecastAmount = 89.34).mqpScenario shouldBe Some(
        MQPScenario.ContinueWorking
      )
    }
    "be CanGetWithGaps if they have a Current Amount of 0, Forecast Amount of 0 and a Maximum more than 0" in {
      createStatePension(maximumAmount = 250.99).mqpScenario shouldBe Some(
        MQPScenario.CanGetWithGaps
      )
    }
    "be CantGet if all the amounts are 0" in {
      createStatePension().mqpScenario shouldBe Some(
        MQPScenario.CantGet
      )
    }
  }

  "forecastScenario" should {
    "be ForecastOnly when Forecast Amount is less than the Current Amount" in {
      createStatePension(
        currentAmount = 20,
        forecastAmount = 10,
        maximumAmount = 10
      ).forecastScenario shouldBe Scenario.ForecastOnly
    }
    "be Reached when current, forecast and maximum are all the same" in {
      createStatePension(
        currentAmount = 10,
        forecastAmount = 10,
        maximumAmount = 10
      ).forecastScenario shouldBe Scenario.Reached
    }
    "be FillGaps when current and forecast are the same and maximum is greater" in {
      createStatePension(
        currentAmount = 10,
        forecastAmount = 10,
        maximumAmount = 20
      ).forecastScenario shouldBe Scenario.FillGaps
    }
    "be FillGaps when current and forecast are different and maximum is greater" in {
      createStatePension(
        currentAmount = 10,
        forecastAmount = 20,
        maximumAmount = 30
      ).forecastScenario shouldBe Scenario.FillGaps
      createStatePension(
        currentAmount = 20,
        forecastAmount = 10,
        maximumAmount = 30
      ).forecastScenario shouldBe Scenario.FillGaps
    }

    "be ContinueWorkingMax when forecast and maximum are the same and the value is the full amount" in {
      createStatePension(
        currentAmount = 100,
        forecastAmount = 155.65,
        maximumAmount = 155.65).forecastScenario shouldBe Scenario.ContinueWorkingMax
      createStatePension(
        currentAmount = 10,
        forecastAmount = 20,
        maximumAmount = 20,
        fullStatePensionAmount = 20
      ).forecastScenario shouldBe Scenario.ContinueWorkingMax
    }
    "be ContinueWorkingMax when forecast and maximum are the same and the value is more than full amount" in {
      createStatePension(
        currentAmount = 100,
        forecastAmount = 170.00,
        maximumAmount = 170.00).forecastScenario shouldBe Scenario.ContinueWorkingMax
      createStatePension(
        currentAmount = 100,
        forecastAmount = 155.66,
        maximumAmount = 155.66).forecastScenario shouldBe Scenario.ContinueWorkingMax
      createStatePension(
        currentAmount = 10,
        forecastAmount = 20.01,
        maximumAmount = 20.01,
        fullStatePensionAmount = 20
      ).forecastScenario shouldBe Scenario.ContinueWorkingMax
    }
    "be ContinueWorkingNonMax when forecast and maximum are the same and the value is less than full amount" in {
      createStatePension(
        currentAmount = 100,
        forecastAmount = 155.64,
        maximumAmount = 155.64).forecastScenario shouldBe Scenario.ContinueWorkingNonMax
      createStatePension(
        currentAmount = 10,
        forecastAmount = 20.01,
        maximumAmount = 20.01,
        fullStatePensionAmount = 21
      ).forecastScenario shouldBe Scenario.ContinueWorkingNonMax
    }
    "be CantGetPension when maximum is 0" in {
      createStatePension().forecastScenario shouldBe Scenario.CantGetPension
    }
  }

  "statePensionAgeUnderConsideration" should {
    "return true when the user has a date of birth within the correct range" in {
      createStatePension(statePensionAgeUnderConsideration = true).statePensionAgeUnderConsideration shouldBe true
    }
    "return true when the user does not have a date of birth within the correct range" in {
      createStatePension().statePensionAgeUnderConsideration shouldBe false
    }
  }

}
