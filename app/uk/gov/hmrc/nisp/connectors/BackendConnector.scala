/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.http.Status
import play.api.libs.json._
import uk.gov.hmrc.http.HttpErrorFunctions.{is4xx, is5xx}
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.nisp.models.StatePensionExclusion
import uk.gov.hmrc.nisp.models.enums.APIType._
import uk.gov.hmrc.nisp.services.MetricsService
import uk.gov.hmrc.nisp.utils.EitherReads.eitherReads

import scala.concurrent.{ExecutionContext, Future}

trait BackendConnector {

  def http: HttpClient
  def serviceUrl: String
  def sessionCache: SessionCache
  val metricsService: MetricsService
  implicit val executionContext: ExecutionContext

  implicit def reads[A: Reads]: Reads[Either[StatePensionExclusion, A]] = eitherReads[StatePensionExclusion, A]
  implicit def writes[A: Writes]: Writes[Either[StatePensionExclusion, A]] = Writes[Either[StatePensionExclusion, A]] {
    case Left(exclusion) => Json.toJson(exclusion)
    case Right(a) => Json.toJson(a)
  }

  implicit def formats[A: Format] = Format[Either[StatePensionExclusion, A]](reads[A], writes[A])

  implicit def httpReads[A: Reads]: HttpReads[Either[UpstreamErrorResponse, Either[StatePensionExclusion, A]]] =
    new HttpReads[Either[UpstreamErrorResponse, Either[StatePensionExclusion, A]]] {
      def reportAsStatus(statusCode: Int): Int = {
        if (is4xx(statusCode)) Status.INTERNAL_SERVER_ERROR else Status.BAD_GATEWAY
      }

      override def read(method: String, url: String, response: HttpResponse): Either[UpstreamErrorResponse, Either[StatePensionExclusion, A]] = {
        response.status match {
          case Status.FORBIDDEN => Right(response.json.as[Either[StatePensionExclusion, A]])
          case status if is4xx(status) || is5xx(status) => Left(UpstreamErrorResponse(response.body, response.status, reportAsStatus(status)))
          case _ => Right(response.json.as[Either[StatePensionExclusion, A]])
        }
      }
    }

  protected def retrieveFromCache[A](api: APIType, url: String, headers: Seq[(String, String)] = Seq())
                                    (implicit hc: HeaderCarrier, formats: Format[A]): Future[Either[UpstreamErrorResponse, Either[StatePensionExclusion, A]]] = {
    val keystoreTimerContext = metricsService.keystoreReadTimer.time()

    val sessionCacheF = sessionCache.fetchAndGetEntry[A](api.toString)
    sessionCacheF.onFailure { case _ =>
      metricsService.keystoreReadFailed.inc()
    }
    sessionCacheF.flatMap { keystoreResult =>
      keystoreTimerContext.stop()
      keystoreResult match {
        case Some(data) =>
          metricsService.keystoreHitCounter.inc()
          Future.successful(Right(Right(data)))
        case None =>
          metricsService.keystoreMissCounter.inc()
          connectToMicroservice(url, api, headers) map {
            case Right(Right(right)) => Right(Right(cacheResult(right, api.toString)))
            case errorResponse => errorResponse
          }
      }
    }
  }

  private def connectToMicroservice[A](urlToRead: String, apiType: APIType, headers: Seq[(String, String)] = Seq())
                                      (implicit hc: HeaderCarrier, formats: Format[A]): Future[Either[UpstreamErrorResponse, Either[StatePensionExclusion, A]]] = {
    val timerContext = metricsService.startTimer(apiType)

    val httpResponseF = http.GET[Either[UpstreamErrorResponse, Either[StatePensionExclusion, A]]](urlToRead, Seq(), headers)
    httpResponseF onSuccess {
      case _ => timerContext.stop()
    }
    httpResponseF onFailure {
      case _ => metricsService.incrementFailedCounter(apiType)
    }
    httpResponseF
  }

  private def cacheResult[A](a: A, name: String)
                            (implicit hc: HeaderCarrier, formats: Format[A]): A = {
    val timerContext = metricsService.keystoreWriteTimer.time()
    val cacheF       = sessionCache.cache[A](name, a)
    cacheF.onSuccess { case _ =>
      timerContext.stop()
    }
    cacheF.onFailure { case _ =>
      metricsService.keystoreWriteFailed.inc()
    }
    a
  }
}
