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

import play.api.libs.json.Json
import uk.gov.hmrc.nisp.config.wiring.WSHttp
import uk.gov.hmrc.nisp.models.enums.IdentityVerificationResult.IdentityVerificationResult
import uk.gov.hmrc.nisp.services.MetricsService
import uk.gov.hmrc.play.http.{HttpGet, HeaderCarrier}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

object IdentityVerificationConnector extends IdentityVerificationConnector with ServicesConfig {
  override val serviceUrl = baseUrl("identity-verification")
  override def http: HttpGet = WSHttp
}

trait IdentityVerificationConnector {
  val serviceUrl: String
  def http: HttpGet

  private def url(journeyId: String) = s"$serviceUrl/mdtp/journey/journeyId/$journeyId"

  private[connectors] case class IdentityVerificationResponse(result: IdentityVerificationResult)
  private implicit val formats = Json.format[IdentityVerificationResponse]

  def identityVerificationResponse(journeyId: String)(implicit hc: HeaderCarrier): Future[IdentityVerificationResult] = {
    val context = MetricsService.identityVerificationTimer.time()
    val ivFuture = http.GET[IdentityVerificationResponse](url(journeyId)).map {
      context.stop()
      _.result
    }

    ivFuture onFailure {
      case e: Exception =>
        MetricsService.identityVerificationFailedCounter.inc()
    }

    ivFuture
  }
}
