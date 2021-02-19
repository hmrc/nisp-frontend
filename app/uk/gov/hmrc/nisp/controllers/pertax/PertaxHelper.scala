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

package uk.gov.hmrc.nisp.controllers.pertax

import com.google.inject.Inject
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.services.MetricsService
import uk.gov.hmrc.nisp.utils.Constants._
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier

class PertaxHelper @Inject()(sessionCache: SessionCache,
                             metricsService: MetricsService)
                            (implicit ec: ExecutionContext){

  def setFromPertax(implicit hc: HeaderCarrier): Unit = {
    val timerContext = metricsService.keystoreWriteTimer.time()
    val cacheF = sessionCache.cache(PERTAX, true)
    cacheF.onSuccess {
      case _ => timerContext.stop()
    }
    cacheF.onFailure {
      case _ => metricsService.keystoreWriteFailed.inc()
    }
  }

  def isFromPertax(implicit hc: HeaderCarrier): Future[Boolean] = {
    val keystoreTimerContext = metricsService.keystoreReadTimer.time()
    sessionCache.fetchAndGetEntry[Boolean](PERTAX).map { keystoreResult =>
      keystoreTimerContext.stop()
      keystoreResult match {
        case Some(isPertax) => metricsService.keystoreHitCounter.inc(); isPertax
        case None =>
          metricsService.keystoreMissCounter.inc()
          false
      }
    } recover {
      case _ =>
        metricsService.keystoreReadFailed.inc()
        false
    }
  }
}
