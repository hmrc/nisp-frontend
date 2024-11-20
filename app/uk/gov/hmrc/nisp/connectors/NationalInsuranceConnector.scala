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

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.models.enums.APIType
import uk.gov.hmrc.nisp.models.{NationalInsuranceRecord, StatePensionExclusion}
import uk.gov.hmrc.nisp.services.MetricsService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NationalInsuranceConnector @Inject()(
  val http: HttpClientV2,
  val appConfig: ApplicationConfig,
  val metricsService: MetricsService,
  implicit val ec: ExecutionContext
) extends BackendConnector {

  val serviceUrl: String = appConfig.nationalInsuranceServiceUrl

  def getNationalInsurance(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, Either[StatePensionExclusion, NationalInsuranceRecord]]] = {
    val urlToRead = s"$serviceUrl/ni/mdtp/$nino"
    val header    = Seq("Accept" -> "application/vnd.hmrc.1.0+json")
    connectToMicroservice[NationalInsuranceRecord](APIType.NationalInsurance, urlToRead, header)
  }
}
