/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.Mode.Mode
import play.api.{Configuration, Play}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpResponse}
import uk.gov.hmrc.nisp.config.wiring.WSHttp
import uk.gov.hmrc.nisp.models.citizen.CitizenDetailsResponse
import uk.gov.hmrc.nisp.services.MetricsService
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

object CitizenDetailsConnector extends CitizenDetailsConnector with ServicesConfig {
  override lazy val serviceUrl = baseUrl("citizen-details")
  override val metricsService: MetricsService = MetricsService
  override def http: HttpGet = WSHttp
  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration
}

trait CitizenDetailsConnector {
  val serviceUrl: String
  val metricsService: MetricsService
  def http: HttpGet

  def connectToGetPersonDetails(nino: Nino)(implicit hc: HeaderCarrier): Future[CitizenDetailsResponse] = {

    val context = metricsService.citizenDetailsTimer.time()
    val result = http.GET[HttpResponse](s"$serviceUrl/citizen-details/$nino/designatory-details").map {
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
