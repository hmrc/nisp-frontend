/*
 * Copyright 2019 HM Revenue & Customs
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

import org.joda.time.{LocalDate, Period}
import uk.gov.hmrc.nisp.models.{SPChartModel, StatePensionAmount}

object Calculate {

  def calculateChartWidths(current: StatePensionAmount, forecast: StatePensionAmount, personalMaximum: StatePensionAmount): (SPChartModel, SPChartModel, SPChartModel) = {
    // scalastyle:off magic.number
    if (personalMaximum.weeklyAmount > forecast.weeklyAmount) {
      val currentChart = SPChartModel((current.weeklyAmount / personalMaximum.weeklyAmount * 100).toInt.max(Constants.chartWidthMinimum), current)
      val forecastChart = SPChartModel((forecast.weeklyAmount / personalMaximum.weeklyAmount * 100).toInt.max(Constants.chartWidthMinimum), forecast)
      val personalMaxChart = SPChartModel(100, personalMaximum)
      (currentChart, forecastChart, personalMaxChart)
    } else {
      if (forecast.weeklyAmount > current.weeklyAmount) {
        val currentPercentage = (current.weeklyAmount / forecast.weeklyAmount * 100).toInt
        val currentChart = SPChartModel(currentPercentage.max(Constants.chartWidthMinimum), current)
        val forecastChart = SPChartModel(100, forecast)
        (currentChart, forecastChart, forecastChart)
      } else {
        val currentChart = SPChartModel(100, current)
        val forecastChart = SPChartModel((forecast.weeklyAmount / current.weeklyAmount * 100).toInt, forecast)
        (currentChart, forecastChart, forecastChart)
      }
    }
  }

  def calculateAge(dateOfBirth: LocalDate, currentDate: LocalDate): Int = {
    new Period(dateOfBirth, currentDate).getYears
  }

}
