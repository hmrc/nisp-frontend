/*
 * Copyright 2017 HM Revenue & Customs
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

import com.codahale.metrics.{Counter, Timer}
import com.codahale.metrics.Timer.Context
import com.kenshoo.play.metrics.MetricsRegistry
import uk.gov.hmrc.nisp.models.enums.APIType
import uk.gov.hmrc.nisp.models.enums.APIType.APIType

object MetricsService extends MetricsService {

  val timers = Map(
    APIType.SP -> MetricsRegistry.defaultRegistry.timer("sp-response-timer"),
    APIType.NI -> MetricsRegistry.defaultRegistry.timer("ni-response-timer"),
    APIType.SchemeMembership-> MetricsRegistry.defaultRegistry.timer("scheme-membership-response-timer"),
    APIType.StatePension -> MetricsRegistry.defaultRegistry.timer("state-pension-response-timer")
  )

  val failedCounters = Map(
    APIType.SP -> MetricsRegistry.defaultRegistry.counter("sp-failed-counter"),
    APIType.NI -> MetricsRegistry.defaultRegistry.counter("ni-failed-counter"),
    APIType.SchemeMembership -> MetricsRegistry.defaultRegistry.counter("scheme-membership-failed-counter"),
    APIType.StatePension -> MetricsRegistry.defaultRegistry.counter("state-pension-failed-counter")
  )

  override val keystoreReadTimer = MetricsRegistry.defaultRegistry.timer("keystore-read-timer")
  override val keystoreWriteTimer = MetricsRegistry.defaultRegistry.timer("keystore-write-timer")

  override val keystoreReadFailed = MetricsRegistry.defaultRegistry.counter("keystore-read-failed-counter")
  override val keystoreWriteFailed = MetricsRegistry.defaultRegistry.counter("keystore-write-failed-counter")

  override val keystoreHitCounter = MetricsRegistry.defaultRegistry.counter("keystore-hit-counter")
  override val keystoreMissCounter = MetricsRegistry.defaultRegistry.counter("keystore-miss-counter")

  override val identityVerificationTimer = MetricsRegistry.defaultRegistry.timer("identity-verification-timer")
  override val identityVerificationFailedCounter = MetricsRegistry.defaultRegistry.counter("identity-verification-failed-counter")

  override val citizenDetailsTimer = MetricsRegistry.defaultRegistry.timer("citizen-details-timer")
  override val citizenDetailsFailedCounter = MetricsRegistry.defaultRegistry.counter("citizen-details-failed-counter")

  override def startTimer(api: APIType): Context = timers(api).time()

  override def incrementFailedCounter(api: APIType): Unit = failedCounters(api).inc()
}

trait MetricsService {
  def startTimer(api:APIType): Timer.Context
  def incrementFailedCounter(api: APIType): Unit

  val keystoreReadTimer: Timer
  val keystoreWriteTimer: Timer
  val keystoreReadFailed: Counter
  val keystoreWriteFailed: Counter
  val keystoreHitCounter: Counter
  val keystoreMissCounter: Counter
  val identityVerificationTimer: Timer
  val identityVerificationFailedCounter: Counter
  val citizenDetailsTimer: Timer
  val citizenDetailsFailedCounter: Counter
}
