/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.nisp.models.enums.APIType
import uk.gov.hmrc.nisp.models.enums.APIType._
import uk.gov.hmrc.nisp.services.MetricsService
import uk.gov.hmrc.nisp.utils.EitherReads.eitherReads

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait BackendConnector {

  def http: HttpClient

  def serviceUrl: String

  def sessionCache: SessionCache

  val metricsService: MetricsService

  implicit val executionContext: ExecutionContext

  implicit def reads[A: Reads]: Reads[Either[StatePensionExclusion, A]] =
    eitherReads[StatePensionExclusion, A]

  implicit def writes[A: Writes]: Writes[Either[StatePensionExclusion, A]] =
    Writes[Either[StatePensionExclusion, A]] {
      case Left(exclusion) =>
        Json.toJson(exclusion)
      case Right(a) =>
        Json.toJson(a)
    }

  implicit def formats[A: Format]: Format[Either[StatePensionExclusion, A]] =
    Format[Either[StatePensionExclusion, A]](reads[A], writes[A])

  implicit def httpReads[A: Reads]: HttpReads[Either[UpstreamErrorResponse, Either[StatePensionExclusion, A]]] =
    new HttpReads[Either[UpstreamErrorResponse, Either[StatePensionExclusion, A]]] {
      def reportAsStatus(statusCode: Int): Int =
        if (is4xx(statusCode)) Status.INTERNAL_SERVER_ERROR else Status.BAD_GATEWAY

      override def read(
        method: String,
        url: String,
        response: HttpResponse
      ): Either[UpstreamErrorResponse, Either[StatePensionExclusion, A]] =
        response.status match {
          case Status.FORBIDDEN =>
            Right(response.json.as[Either[StatePensionExclusion, A]])
          case status if is4xx(status) || is5xx(status) =>
            Left(UpstreamErrorResponse(response.body, response.status, reportAsStatus(status)))
          case _ =>
            Right(response.json.as[Either[StatePensionExclusion, A]])
        }
    }

  protected def retrieveFromCache[A](
    api: APIType,
    url: String,
    headers: Seq[(String, String)] = Seq(),
    delegationState: Boolean = false
  )(
    implicit hc: HeaderCarrier,
    formats: Format[A]
  ): Future[Either[UpstreamErrorResponse, Either[StatePensionExclusion, A]]] = {
    val keystoreTimerContext =
      metricsService.keystoreReadTimer.time()

    if (api == APIType.StatePension && delegationState) {
      metricsService.keystoreMissCounter.inc()
      connectToMicroservice(url, api, headers)
    } else {
      sessionCache.fetchAndGetEntry[A](api.toString).flatMap {
        keystoreResult =>
          keystoreTimerContext.stop()
          keystoreResult match {
            case Some(data) =>
              metricsService.keystoreHitCounter.inc()
              Future.successful(Right(Right(data)))
            case None =>
              metricsService.keystoreMissCounter.inc()
              connectToMicroservice(url, api, headers) map {
                case Right(Right(right)) =>
                  Right(Right(cacheResult(right, api.toString)))
                case errorResponse =>
                  errorResponse
              }
          }
      } recover {
        case e =>
          metricsService.keystoreReadFailed.inc()
          throw e
      }
    }
  }

  private def connectToMicroservice[A](
    urlToRead: String,
    apiType: APIType,
    headers: Seq[(String, String)]
  )(
    implicit hc: HeaderCarrier,
    formats: Format[A]
  ): Future[Either[UpstreamErrorResponse, Either[StatePensionExclusion, A]]] = {
    val timerContext = metricsService.startTimer(apiType)
    http.GET[Either[UpstreamErrorResponse, Either[StatePensionExclusion, A]]](
      url = urlToRead,
      queryParams = Seq(),
      headers = headers
    ) map {
      response =>
        timerContext.stop()
        response
    } recover {
      case e =>
        metricsService.incrementFailedCounter(apiType)
        throw e
    }
  }

  private def cacheResult[A](
    result: A,
    name: String
  )(
    implicit hc: HeaderCarrier,
    formats: Format[A]
  ): A = {
    val timerContext = metricsService.keystoreWriteTimer.time()

    sessionCache.cache[A](name, result).onComplete {
      case Success(_) =>
        timerContext.stop()
      case Failure(_) =>
        metricsService.keystoreWriteFailed.inc()
    }
    result
  }
}
