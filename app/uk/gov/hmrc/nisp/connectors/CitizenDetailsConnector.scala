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

import com.codahale.metrics.Timer
import com.google.inject.Inject
import play.api.http.Status.BAD_GATEWAY
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HttpReadsInstances.readEitherOf
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.models.citizen.CitizenDetailsResponse
import uk.gov.hmrc.nisp.services.MetricsService

import scala.concurrent.{ExecutionContext, Future}

class CitizenDetailsConnector @Inject() (
  http: HttpClientV2,
  metricsService: MetricsService,
  appConfig: ApplicationConfig
)(implicit
  executionContext: ExecutionContext
) {

  val serviceUrl: String = appConfig.citizenDetailsServiceUrl

  def connectToGetPersonDetails(
    nino: Nino
  )(implicit
    hc: HeaderCarrier
  ): Future[Either[UpstreamErrorResponse, CitizenDetailsResponse]] = {

    val context: Timer.Context = metricsService.citizenDetailsTimer.time()

    http
      .get(url"$serviceUrl/citizen-details/$nino/designatory-details")
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .andThen(_ => context.stop())
      .map {
        case Right(response) =>
          Right(response.json.as[CitizenDetailsResponse])
        case Left(error)     =>
          metricsService.citizenDetailsFailedCounter.inc()
          Left(error)
      } recover {
      case error: HttpException =>
        Left(UpstreamErrorResponse(error.message, BAD_GATEWAY))
      case error                =>
        metricsService.citizenDetailsFailedCounter.inc()
        throw error
    }
  }
}
