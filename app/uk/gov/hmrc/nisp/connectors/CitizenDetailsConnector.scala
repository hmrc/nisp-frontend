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

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.nisp.config.wiring.WSHttp
import uk.gov.hmrc.nisp.models.citizen.{CitizenDetailsRequest, CitizenDetailsResponse}
import uk.gov.hmrc.nisp.services.MetricsService
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpResponse}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

object CitizenDetailsConnector extends CitizenDetailsConnector with ServicesConfig {
  override val serviceUrl = baseUrl("citizen-details")
  override def http: HttpPost = WSHttp
  override val metricsService: MetricsService = MetricsService
}

trait CitizenDetailsConnector {
  val serviceUrl: String
  def http: HttpPost
  val metricsService: MetricsService

  private def url(nino: Nino) = s"$serviceUrl/citizen-details/$nino/designatory-details/summary"

  def connectToGetPersonDetails(nino: Nino)(implicit hc: HeaderCarrier): Future[CitizenDetailsResponse] = {
    val jsonRequest = Json.toJson(CitizenDetailsRequest(Set(nino)))

    val context = metricsService.citizenDetailsTimer.time()

    val result = http.POST[JsValue, HttpResponse](url(nino), jsonRequest).map {
      context.stop()
      _.json.as[CitizenDetailsResponse]
    }

    result onFailure {
      case e: Exception =>
        metricsService.citizenDetailsFailedCounter.inc()
    }

    result
  }
}
