/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.services

import com.google.inject.Inject
import uk.gov.hmrc.nisp.config.ApplicationConfig

import java.time.{LocalDate, ZoneId}

class GracePeriodService @Inject()(appConfig: ApplicationConfig) {
  
  private val currentYear: Int = LocalDate.now(ZoneId.of("Europe/London")).getYear
  private def now: LocalDate = LocalDate.now(ZoneId.of("Europe/London"))
  
  private val graceStartDate: LocalDate = LocalDate.of(currentYear, appConfig.gracePeriodStartMonth, appConfig.gracePeriodStartDay)
  private val graceEndDate: LocalDate = LocalDate.of(currentYear, appConfig.gracePeriodEndMonth, appConfig.gracePeriodEndDay)
  
  def inGracePeriod: Boolean = now.isAfter(graceStartDate) && now.isBefore(graceEndDate)
}
