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

package uk.gov.hmrc.nisp.views.utils

object BarChartCalculator {

  def barPercentage(greenBar: BigDecimal, greyBar: BigDecimal): BigDecimal = {
    if ((greenBar / greyBar) * 100 > 100) 100 else (greenBar / greyBar) * 100
  }

  def forecastMax(greenBar: BigDecimal, greyBar: BigDecimal): BigDecimal = {
    if (greenBar > greyBar) greyBar else greenBar
  }

  def findMaxPensionAmount(spMaximumAmount: BigDecimal, vnicPensionAmount: BigDecimal): BigDecimal =
    Math.max(spMaximumAmount.toDouble, vnicPensionAmount.toDouble)

}
