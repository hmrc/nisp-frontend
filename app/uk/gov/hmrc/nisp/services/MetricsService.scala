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

package uk.gov.hmrc.nisp.services

import com.codahale.metrics.Timer.Context
import com.google.inject.Inject
import com.kenshoo.play.metrics.Metrics
import uk.gov.hmrc.nisp.models.enums.APIType
import uk.gov.hmrc.nisp.models.enums.APIType.APIType

class MetricsService @Inject()(metrics: Metrics){

  val timers = Map(
    APIType.SP -> metrics.defaultRegistry.timer("sp-response-timer"),
    APIType.NI -> metrics.defaultRegistry.timer("ni-response-timer"),
    APIType.SchemeMembership -> metrics.defaultRegistry.timer("scheme-membership-response-timer"),
    APIType.StatePension -> metrics.defaultRegistry.timer("state-pension-response-timer"),
    APIType.NationalInsurance -> metrics.defaultRegistry.timer("national-insurance-response-timer")
  )

  val failedCounters = Map(
    APIType.SP -> metrics.defaultRegistry.counter("sp-failed-counter"),
    APIType.NI -> metrics.defaultRegistry.counter("ni-failed-counter"),
    APIType.SchemeMembership -> metrics.defaultRegistry.counter("scheme-membership-failed-counter"),
    APIType.StatePension -> metrics.defaultRegistry.counter("state-pension-failed-counter"),
    APIType.NationalInsurance -> metrics.defaultRegistry.counter("national-insurance-failed-counter")
  )

  val keystoreReadTimer = metrics.defaultRegistry.timer("keystore-read-timer")
  val keystoreWriteTimer = metrics.defaultRegistry.timer("keystore-write-timer")
  val keystoreReadFailed = metrics.defaultRegistry.counter("keystore-read-failed-counter")
  val keystoreWriteFailed = metrics.defaultRegistry.counter("keystore-write-failed-counter")
  val keystoreHitCounter = metrics.defaultRegistry.counter("keystore-hit-counter")
  val keystoreMissCounter = metrics.defaultRegistry.counter("keystore-miss-counter")
  val identityVerificationTimer = metrics.defaultRegistry.timer("identity-verification-timer")
  val identityVerificationFailedCounter = metrics.defaultRegistry.counter("identity-verification-failed-counter")
  val citizenDetailsTimer = metrics.defaultRegistry.timer("citizen-details-timer")
  val citizenDetailsFailedCounter = metrics.defaultRegistry.counter("citizen-details-failed-counter")

  def startTimer(api: APIType): Context = timers(api).time()

  def incrementFailedCounter(api: APIType): Unit = failedCounters(api).inc()
}
