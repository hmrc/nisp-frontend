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

import uk.gov.hmrc.http.HeaderCarrier

object NIRecordEvent {
  def apply(nino: String, yearsToContribute: Int, qualifyingYears: Int, nonQualifyingYears: Int, fillableGaps: Int, nonFillableGaps: Int, pre75Years: Int)(implicit hc: HeaderCarrier): NIRecordEvent =
    new NIRecordEvent(nino, yearsToContribute, qualifyingYears, nonQualifyingYears, fillableGaps, nonFillableGaps, pre75Years)
}

class NIRecordEvent(nino: String, yearsToContribute: Int, qualifyingYears: Int, nonQualifyingYears: Int, fillableGaps: Int, nonFillableGaps: Int,
                    pre75Years: Int)(implicit hc: HeaderCarrier)
  extends NispBusinessEvent("NIRecord",
    Map(
      "nino" -> nino,
      "yearsToContribute" -> yearsToContribute.toString,
      "qualifyingYears" -> qualifyingYears.toString,
      "nonQualifyingYears" -> nonQualifyingYears.toString,
      "fillableGaps" -> fillableGaps.toString,
      "nonFillableGaps" -> nonFillableGaps.toString,
      "pre75Years" -> pre75Years.toString
    )
  )
