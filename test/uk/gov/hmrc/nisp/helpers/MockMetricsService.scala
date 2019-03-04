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

package uk.gov.hmrc.nisp.helpers

import com.codahale.metrics.Timer.Context
import com.codahale.metrics.{Counter, Timer}
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.nisp.models.enums.APIType.APIType
import uk.gov.hmrc.nisp.services.MetricsService

object MockMetricsService extends MetricsService with MockitoSugar {


  val fakeTimerContext = mock[Timer.Context]
  val fakeTimer = new Timer()
  val fakeCounter = mock[Counter]

  override def startTimer(api: APIType): Context = fakeTimerContext

  override def incrementFailedCounter(api: APIType): Unit = {}

  override val keystoreReadTimer: Timer = fakeTimer
  override val keystoreWriteTimer: Timer = fakeTimer
  override val keystoreReadFailed: Counter = fakeCounter
  override val keystoreWriteFailed: Counter = fakeCounter
  override val keystoreHitCounter: Counter = fakeCounter
  override val keystoreMissCounter: Counter = fakeCounter
  override val identityVerificationTimer: Timer = fakeTimer
  override val identityVerificationFailedCounter: Counter = fakeCounter
  override val citizenDetailsTimer: Timer = fakeTimer
  override val citizenDetailsFailedCounter: Counter = fakeCounter



}
