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

import com.google.inject.Inject
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.models.citizen.CitizenDetailsResponse
import uk.gov.hmrc.nisp.services.MetricsService
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.domain.Nino


import scala.concurrent.{ExecutionContext, Future}

//TODO test this class
class CitizenDetailsConnector @Inject()(http: HttpClient,
                                        metricsService: MetricsService,
                                        appConfig: ApplicationConfig
                                        )(implicit executionContext: ExecutionContext) {

  val serviceUrl: String = appConfig.citizenDetailsServiceUrl

  def connectToGetPersonDetails(nino: Nino)(implicit hc: HeaderCarrier): Future[CitizenDetailsResponse] = {

    val context = metricsService.citizenDetailsTimer.time()
    val result = http.GET[HttpResponse](s"$serviceUrl/citizen-details/$nino/designatory-details").map {
      context.stop()
      _.json.as[CitizenDetailsResponse]
    }

    result onFailure {
      case _: Exception =>
        metricsService.citizenDetailsFailedCounter.inc()
    }

    result
  }
}
