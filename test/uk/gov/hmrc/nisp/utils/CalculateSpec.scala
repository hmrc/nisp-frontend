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

package uk.gov.hmrc.nisp.utils

import org.joda.time.LocalDate
import uk.gov.hmrc.nisp.models.StatePensionAmountRegular
import uk.gov.hmrc.play.test.UnitSpec

class CalculateSpec extends UnitSpec {

  "calculate chart widths" should {
    def calculateCharts(currentAmount: BigDecimal, forecastAmount: BigDecimal, personalMax: BigDecimal) =
      Calculate.calculateChartWidths(StatePensionAmountRegular(currentAmount, 0, 0), StatePensionAmountRegular(forecastAmount, 0, 0), StatePensionAmountRegular(personalMax, 0, 0))

    "current chart is 100 when current amount is higher" in {
      val (currentChart, _, _) = calculateCharts(70, 30, 0)
      currentChart.width shouldBe 100
    }

    "forecast chart is 100 when forecast amount is higher" in {
      val (_, forecastChart, personalMaxChart) = calculateCharts(70, 80, 80)
      forecastChart.width shouldBe 100
      personalMaxChart.width shouldBe 100
    }

    "current chart and forecast chart are 100 when amounts are equal" in {
      val (currentChart, forecastChart, personalMaxChart) = calculateCharts(70, 70, 70)
      currentChart.width shouldBe 100
      forecastChart.width shouldBe 100
      personalMaxChart.width shouldBe 100
    }

    "current chart is 66 when current amount is 2 and forecast is 3" in {
      val (currentChart, forecastChart, personalMaxChart) = calculateCharts(2, 3, 4)
      currentChart.width shouldBe 50
      forecastChart.width shouldBe 75
      personalMaxChart.width shouldBe 100
    }

    "forecast chart is 30 when forecast amount is 4 and current is 13" in {
      val (currentChart, forecastChart, personalMaxChart) = calculateCharts(13, 4, 20)
      forecastChart.width shouldBe 31
      currentChart.width shouldBe 65
      personalMaxChart.width shouldBe 100
    }
  }

  "calculateAge" should {
    "return 30 when the currentDate is 2016-11-2 their dateOfBirth is 1986-10-28" in {
      Calculate.calculateAge(new LocalDate(1986, 10, 28), new LocalDate(2016, 11, 2)) shouldBe 30
    }
    "return 30 when the currentDate is 2016-11-2 their dateOfBirth is 1986-11-2" in {
      Calculate.calculateAge(new LocalDate(1986, 11, 2), new LocalDate(2016, 11, 2)) shouldBe 30

    }
    "return 29 when the currentDate is 2016-11-2 their dateOfBirth is 1986-11-3" in {
      Calculate.calculateAge(new LocalDate(1986, 11, 3), new LocalDate(2016, 11, 2)) shouldBe 29
    }
  }

}
