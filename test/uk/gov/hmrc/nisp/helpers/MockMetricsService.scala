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

package uk.gov.hmrc.nisp.helpers

import uk.gov.hmrc.nisp.models.enums.ABTest.ABTest
import uk.gov.hmrc.nisp.models.enums.SPContextMessage.SPContextMessage
import uk.gov.hmrc.nisp.models.enums.SPExclusion.SPExclusion
import uk.gov.hmrc.nisp.services.MetricsService

object MockMetricsService extends MetricsService {
  override def mainPage(forecast: BigDecimal, current: BigDecimal, scenario: Option[SPContextMessage],
                        contractedOutFlag: Boolean, forecastOnlyFlag: Boolean, age: Int, abTest: Option[ABTest]): Unit = ()
  override def niRecord(gaps: Int, payableGaps: Int, pre75Years: Int, qualifyingYears: Int, yearsUntilSPA: Int): Unit = ()
  override def exclusion(exclusions: List[SPExclusion]): Unit = ()

}
