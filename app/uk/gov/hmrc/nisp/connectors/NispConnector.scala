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

package uk.gov.hmrc.nisp.connectors

import play.Logger
import play.api.data.validation.ValidationError
import play.api.libs.json.{Format, JsPath}
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.config.wiring.{NispSessionCache, WSHttp}
import uk.gov.hmrc.nisp.models.enums.APIType
import uk.gov.hmrc.nisp.models.enums.APIType.APIType
import uk.gov.hmrc.nisp.models.{NIResponse, SPResponseModel}
import uk.gov.hmrc.nisp.services.MetricsService
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpResponse}

import scala.concurrent.Future
import scala.util.{Success, Try, Failure}

object NispConnector extends NispConnector with ServicesConfig {
  override val serviceUrl = baseUrl("nisp")

  override def http: HttpGet = WSHttp
  override def sessionCache: SessionCache = NispSessionCache
}

trait NispConnector {

  def http: HttpGet
  def serviceUrl: String
  def sessionCache: SessionCache

  def connectToGetSPResponse(nino: String)(implicit hc: HeaderCarrier): Future[SPResponseModel] = {
    val urlToRead = s"$serviceUrl/nisp/$nino/spsummary"
    retrieveFromCache[SPResponseModel](APIType.SP, urlToRead)
  }

  def connectToGetNIResponse(nino: String)(implicit hc: HeaderCarrier): Future[NIResponse] = {
    val urlToRead = s"$serviceUrl/nisp/$nino/nirecord"
    retrieveFromCache[NIResponse](APIType.NI, urlToRead)
  }

  private def retrieveFromCache[A](api: APIType, url: String)(implicit hc: HeaderCarrier, formats: Format[A]): Future[A] = {
    val keystoreTimerContext = MetricsService.keystoreReadTimer.time()

    val sessionCacheF = sessionCache.fetchAndGetEntry[A](api.toString)
    sessionCacheF.onFailure {
      case _ => MetricsService.keystoreReadFailed.inc()
    }
    sessionCacheF.flatMap { keystoreResult =>
      keystoreTimerContext.stop()
      keystoreResult match {
        case Some(data) =>
          MetricsService.keystoreHitCounter.inc()
          Future.successful(data)
        case None =>
          MetricsService.keystoreMissCounter.inc()
          connectToMicroservice[A](url, api) map {
            data: A => cacheResult(data, api.toString)
          }
      }
    }
  }

  private def connectToMicroservice[A](urlToRead: String, apiType: APIType)(implicit hc: HeaderCarrier, formats: Format[A]): Future[A] = {
    val timerContext = MetricsService.timers(apiType).time()

    val httpResponseF = http.GET[HttpResponse](urlToRead)
    httpResponseF onSuccess {
      case _ => timerContext.stop()
    }
    httpResponseF onFailure {
      case _ => MetricsService.failedCounters(apiType).inc()
    }
    httpResponseF.map {
      httpResponse => httpResponse.json.validate[A].fold(
        errs => throw new JsonValidationException(s"Unable to deserialise: ${formatJsonErrors(errs)}"), valid => valid
      )
    }
  }

  private def cacheResult[A](a:A,name: String)(implicit hc: HeaderCarrier, formats: Format[A]): A = {
    val timerContext = MetricsService.keystoreWriteTimer.time()
    val cacheF = sessionCache.cache[A](name, a)
    cacheF.onSuccess {
      case _ => timerContext.stop()
    }
    cacheF.onFailure {
      case _ => MetricsService.keystoreWriteFailed.inc()
    }
    a
  }

  private def formatJsonErrors(errors: Seq[(JsPath, Seq[ValidationError])]): String = {
    errors.map(p => p._1 + " - " + p._2.map(_.message).mkString(",")).mkString(" | ")
  }

  private class JsonValidationException(message: String) extends Exception(message)
}
