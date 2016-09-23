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

import play.api.data.validation.ValidationError
import play.api.http.Status._
import play.api.libs.json.{JsPath, JsValue}
import uk.gov.hmrc.nisp.config.wiring.WSHttp
import uk.gov.hmrc.nisp.services.MetricsService
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.http.{HeaderCarrier, Upstream4xxResponse, NotFoundException}

import scala.concurrent.Future

sealed trait IdentityVerificationResponse
case object IdentityVerificationForbiddenResponse extends IdentityVerificationResponse
case object IdentityVerificationNotFoundResponse extends IdentityVerificationResponse
case class IdentityVerificationErrorResponse(cause: Throwable) extends IdentityVerificationResponse
case class IdentityVerificationSuccessResponse(result: String) extends IdentityVerificationResponse
object IdentityVerificationSuccessResponse {
  val Success = "Success"
  val Incomplete = "Incomplete"
  val FailedMatching = "FailedMatching"
  val InsufficientEvidence = "InsufficientEvidence"
  val LockedOut = "LockedOut"
  val UserAborted = "UserAborted"
  val Timeout = "Timeout"
  val TechnicalIssue = "TechnicalIssue"
  val PreconditionFailed = "PreconditionFailed"
  val FailedIV = "FailedIV"
}

object IdentityVerificationConnector extends IdentityVerificationConnector with ServicesConfig {
  override val serviceUrl = baseUrl("identity-verification")
  override def http: HttpGet = WSHttp
}

trait IdentityVerificationConnector {
  val serviceUrl: String
  def http: HttpGet

  private def url(journeyId: String) = s"$serviceUrl/mdtp/journey/journeyId/$journeyId"

  def identityVerificationResponse(journeyId: String)(implicit hc: HeaderCarrier): Future[IdentityVerificationResponse] = {
    val context = MetricsService.identityVerificationTimer.time()
    val ivFuture = http.GET[HttpResponse](url(journeyId)).map { httpResponse =>
      context.stop()
      val result = (httpResponse.json \ "result").as[String]
      IdentityVerificationSuccessResponse(result)
    } recover {
       case e: NotFoundException =>
        MetricsService.identityVerificationFailedCounter.inc()
        IdentityVerificationNotFoundResponse
       case Upstream4xxResponse(_, FORBIDDEN, _, _) =>
        MetricsService.identityVerificationFailedCounter.inc()
        IdentityVerificationForbiddenResponse
      case e: Throwable =>
        MetricsService.identityVerificationFailedCounter.inc()
        IdentityVerificationErrorResponse(e)
    }

    ivFuture
  }
}
