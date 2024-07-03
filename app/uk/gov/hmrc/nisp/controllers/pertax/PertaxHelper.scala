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

package uk.gov.hmrc.nisp.controllers.pertax

import com.codahale.metrics.Timer
import com.google.inject.Inject
import play.api.mvc.Request
import uk.gov.hmrc.nisp.repositories.SessionCache
import uk.gov.hmrc.nisp.repositories.SessionCache.CacheKey
import uk.gov.hmrc.nisp.services.MetricsService

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class PertaxHelper @Inject() (
  sessionCacheNew: SessionCache,
  metricsService: MetricsService
)(implicit
  ec: ExecutionContext
) {

  def setFromPertax(implicit request: Request[_]): Unit = {
    val keystoreTimerContext: Timer.Context =
      metricsService.keystoreWriteTimer.time()
    val cacheF: Future[Unit]                =
      sessionCacheNew.put(CacheKey.PERTAX, true)

    cacheF.onComplete {
      case Success(_) =>
        keystoreTimerContext.stop()
      case Failure(_) =>
        metricsService.keystoreWriteFailed.inc()
    }
  }

  def isFromPertax(implicit request: Request[_]): Future[Boolean] = {
    val keystoreTimerContext =
      metricsService.keystoreReadTimer.time()

    sessionCacheNew.get(CacheKey.PERTAX).map { keystoreResult =>
      keystoreTimerContext.stop()
      keystoreResult match {
        case Some(isPertax) =>
          metricsService.keystoreHitCounter.inc()
          isPertax
        case None           =>
          metricsService.keystoreMissCounter.inc()
          false
      }
    } recover { case _ =>
      metricsService.keystoreReadFailed.inc()
      false
    }
  }
}
