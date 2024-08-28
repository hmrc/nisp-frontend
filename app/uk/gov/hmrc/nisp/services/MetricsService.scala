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

package uk.gov.hmrc.nisp.services

import com.codahale.metrics.Timer.Context
import com.codahale.metrics.{Counter, Timer}
import com.google.inject.Inject
import uk.gov.hmrc.nisp.models.enums
import uk.gov.hmrc.nisp.models.enums.APIType
import uk.gov.hmrc.nisp.models.enums.APIType.APIType

class MetricsService @Inject() (metrics: com.codahale.metrics.MetricRegistry) {

  private val timers: Map[enums.APIType.Value, Timer] = Map(
    APIType.SP                -> metrics.timer("sp-response-timer"),
    APIType.NI                -> metrics.timer("ni-response-timer"),
    APIType.SchemeMembership  -> metrics.timer("scheme-membership-response-timer"),
    APIType.StatePension      -> metrics.timer("state-pension-response-timer"),
    APIType.NationalInsurance -> metrics.timer("national-insurance-response-timer")
  )

  private val failedCounters: Map[enums.APIType.Value, Counter] = Map(
    APIType.SP                -> metrics.counter("sp-failed-counter"),
    APIType.NI                -> metrics.counter("ni-failed-counter"),
    APIType.SchemeMembership  -> metrics.counter("scheme-membership-failed-counter"),
    APIType.StatePension      -> metrics.counter("state-pension-failed-counter"),
    APIType.NationalInsurance -> metrics.counter("national-insurance-failed-counter")
  )

  val keystoreReadTimer: Timer = metrics.timer("keystore-read-timer")
  val keystoreWriteTimer: Timer = metrics.timer("keystore-write-timer")
  val keystoreReadFailed: Counter = metrics.counter("keystore-read-failed-counter")
  val keystoreWriteFailed: Counter = metrics.counter("keystore-write-failed-counter")
  val keystoreHitCounter: Counter = metrics.counter("keystore-hit-counter")
  val keystoreMissCounter: Counter = metrics.counter("keystore-miss-counter")
  val identityVerificationTimer: Timer = metrics.timer("identity-verification-timer")
  val identityVerificationFailedCounter: Counter = metrics.counter("identity-verification-failed-counter")
  val citizenDetailsTimer: Timer = metrics.timer("citizen-details-timer")
  val citizenDetailsFailedCounter: Counter = metrics.counter("citizen-details-failed-counter")

  def startTimer(api: APIType): Context = timers(api).time()

  def incrementFailedCounter(api: APIType): Unit = failedCounters(api).inc()
}
