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

package uk.gov.hmrc.nisp.connectors

import com.google.inject.Inject
import play.api.http.Status._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HttpReadsInstances.readEitherOf
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.services.MetricsService

import scala.concurrent.{ExecutionContext, Future}

sealed trait IdentityVerificationResponse
case object IdentityVerificationForbiddenResponse extends IdentityVerificationResponse
case object IdentityVerificationNotFoundResponse extends IdentityVerificationResponse
case class IdentityVerificationErrorResponse(cause: Throwable) extends IdentityVerificationResponse
case class IdentityVerificationSuccessResponse(result: String) extends IdentityVerificationResponse

object IdentityVerificationSuccessResponse {
  val Success              = "Success"
  val Incomplete           = "Incomplete"
  val FailedMatching       = "FailedMatching"
  val InsufficientEvidence = "InsufficientEvidence"
  val LockedOut            = "LockedOut"
  val UserAborted          = "UserAborted"
  val Timeout              = "Timeout"
  val TechnicalIssue       = "TechnicalIssue"
  val PreconditionFailed   = "PreconditionFailed"
  val FailedIV             = "FailedIV"
}

class IdentityVerificationConnector @Inject() (
  http: HttpClientV2,
  metricsService: MetricsService,
  appConfig: ApplicationConfig
)(implicit ec: ExecutionContext) {

  val serviceUrl: String = appConfig.identityVerificationServiceUrl

  private def url(journeyId: String) = url"$serviceUrl/mdtp/journey/journeyId/$journeyId"

  def identityVerificationResponse(
    journeyId: String
  )(implicit hc: HeaderCarrier): Future[IdentityVerificationResponse] = {
    val context  = metricsService.identityVerificationTimer.time()
    http
      .get(url(journeyId))
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .andThen{_ => context.stop()}
      .map {
        case Right(response) =>
          val result: String = (response.json \ "result").as[String]
          IdentityVerificationSuccessResponse(result)
        case Left(error) =>
          metricsService.identityVerificationFailedCounter.inc()
          error.statusCode match {
            case NOT_FOUND =>
              IdentityVerificationNotFoundResponse
            case FORBIDDEN =>
              IdentityVerificationForbiddenResponse
            case _ =>
              IdentityVerificationErrorResponse(error)
          }
      }
      .recover {
        case error =>
          metricsService.identityVerificationFailedCounter.inc()
          IdentityVerificationErrorResponse(error)
      }
  }
}
