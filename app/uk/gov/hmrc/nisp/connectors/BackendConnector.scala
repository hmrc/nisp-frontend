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

package uk.gov.hmrc.nisp.connectors

import play.api.data.validation.ValidationError
import play.api.libs.json.{Format, JsObject, JsPath}
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.models.enums.APIType._
import uk.gov.hmrc.nisp.services.MetricsService
import uk.gov.hmrc.nisp.utils.JsonDepersonaliser
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future
import scala.util.{Failure, Success}
import uk.gov.hmrc.http.{ HeaderCarrier, HttpGet, HttpResponse }

trait BackendConnector {

  def http: HttpGet
  def serviceUrl: String
  def sessionCache: SessionCache
  val metricsService: MetricsService

  protected def retrieveFromCache[A](api: APIType, url: String)(implicit hc: HeaderCarrier, formats: Format[A]): Future[A] = {
    val keystoreTimerContext = metricsService.keystoreReadTimer.time()

    val sessionCacheF = sessionCache.fetchAndGetEntry[A](api.toString)
    sessionCacheF.onFailure {
      case _ => metricsService.keystoreReadFailed.inc()
    }
    sessionCacheF.flatMap { keystoreResult =>
      keystoreTimerContext.stop()
      keystoreResult match {
        case Some(data) =>
          metricsService.keystoreHitCounter.inc()
          Future.successful(data)
        case None =>
          metricsService.keystoreMissCounter.inc()
          connectToMicroservice[A](url, api) map {
            data: A => cacheResult(data, api.toString)
          }
      }
    }
  }

  private def connectToMicroservice[A](urlToRead: String, apiType: APIType)(implicit hc: HeaderCarrier, formats: Format[A]): Future[A] = {
    val timerContext = metricsService.startTimer(apiType)

    val httpResponseF = http.GET[HttpResponse](urlToRead)
    httpResponseF onSuccess {
      case _ => timerContext.stop()
    }
    httpResponseF onFailure {
      case _ => metricsService.incrementFailedCounter(apiType)
    }
    httpResponseF.map {
      httpResponse => httpResponse.json.validate[A].fold(
        errs => {
          val json = JsonDepersonaliser.depersonalise(httpResponse.json) match {
            case Success(s) => s"Depersonalised JSON\n$s"
            case Failure(e) => s"JSON could not be depersonalised\n${e.toString()}"
          }
          throw new JsonValidationException(s"Unable to deserialise $apiType: ${formatJsonErrors(errs)}\n$json")
        },
        valid => valid
      )
    }
  }

  private def cacheResult[A](a:A,name: String)(implicit hc: HeaderCarrier, formats: Format[A]): A = {
    val timerContext = metricsService.keystoreWriteTimer.time()
    val cacheF = sessionCache.cache[A](name, a)
    cacheF.onSuccess {
      case _ => timerContext.stop()
    }
    cacheF.onFailure {
      case _ => metricsService.keystoreWriteFailed.inc()
    }
    a
  }

  private def formatJsonErrors(errors: Seq[(JsPath, Seq[ValidationError])]): String = {
    errors.map(p => p._1 + " - " + p._2.map(e => removeJson(e.message)).mkString(",")).mkString(" | ")
  }

  private def removeJson(message: String): String = {
    message.indexOf("{") match {
      case i if i != -1  => message.substring(0, i - 1) + " [JSON removed]"
      case _ => message
    }
  }

  private[connectors] class JsonValidationException(message: String) extends Exception(message)
}
