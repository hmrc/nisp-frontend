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

package uk.gov.hmrc.nisp.controllers.pertax

import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.services.MetricsService
import uk.gov.hmrc.nisp.utils.Constants._
import play.api.http.HeaderNames.REFERER
import uk.gov.hmrc.play.http.HeaderCarrier
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

trait PertaxHelper {

  val sessionCache: SessionCache

  def isFromPertax(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Boolean] = {
    val keystoreTimerContext = MetricsService.keystoreReadTimer.time()
    sessionCache.fetchAndGetEntry[Boolean](PERTAX).map { keystoreResult =>
      keystoreTimerContext.stop()
      keystoreResult match  {
        case Some(isPertax) => MetricsService.keystoreHitCounter.inc(); isPertax
        case None =>  {
          MetricsService.keystoreMissCounter.inc()
          val isPertax = request.headers.get(REFERER).getOrElse("").contains("personal-account")
          val timerContext = MetricsService.keystoreWriteTimer.time()
          val cacheFuture = sessionCache.cache(PERTAX, isPertax)
          cacheFuture.onSuccess {
            case _ => timerContext.stop()
          }
          cacheFuture.onFailure {
            case _ => MetricsService.keystoreWriteFailed.inc()
          }
          isPertax
        }
      }
    } recover {
      case ex =>
        MetricsService.keystoreReadFailed.inc()
        request.headers.get(REFERER).getOrElse("").contains("personal-account")
    }
  }
}
