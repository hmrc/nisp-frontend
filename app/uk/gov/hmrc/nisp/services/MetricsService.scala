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

package uk.gov.hmrc.nisp.services

import com.kenshoo.play.metrics.MetricsRegistry
import uk.gov.hmrc.nisp.models.enums.APIType

trait MetricsService {}

object MetricsService extends MetricsService {

  val timers = Map(
    APIType.SP -> MetricsRegistry.defaultRegistry.timer("sp-response-timer"),
    APIType.NI -> MetricsRegistry.defaultRegistry.timer("ni-response-timer")
  )

  val failedCounters = Map(
    APIType.SP -> MetricsRegistry.defaultRegistry.counter("sp-failed-counter"),
    APIType.NI -> MetricsRegistry.defaultRegistry.counter("ni-failed-counter")
  )

  val keystoreReadTimer = MetricsRegistry.defaultRegistry.timer("keystore-read-timer")
  val keystoreWriteTimer = MetricsRegistry.defaultRegistry.timer("keystore-write-timer")

  val keystoreReadFailed = MetricsRegistry.defaultRegistry.counter("keystore-read-failed-counter")
  val keystoreWriteFailed = MetricsRegistry.defaultRegistry.counter("keystore-write-failed-counter")

  val keystoreHitCounter = MetricsRegistry.defaultRegistry.counter("keystore-hit-counter")
  val keystoreMissCounter = MetricsRegistry.defaultRegistry.counter("keystore-miss-counter")

  val identityVerificationTimer = MetricsRegistry.defaultRegistry.timer("identity-verification-timer")
  val identityVerificationFailedCounter = MetricsRegistry.defaultRegistry.counter("identity-verification-failed-counter")

  val citizenDetailsTimer = MetricsRegistry.defaultRegistry.timer("citizen-details-timer")
  val citizenDetailsFailedCounter = MetricsRegistry.defaultRegistry.counter("citizen-details-failed-counter")

}
