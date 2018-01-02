/*
 * Copyright 2018 HM Revenue & Customs
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
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.config.wiring.{NispSessionCache, WSHttp}
import uk.gov.hmrc.nisp.connectors.StatePensionConnector.baseUrl
import uk.gov.hmrc.nisp.models.NationalInsuranceRecord
import uk.gov.hmrc.nisp.models.enums.APIType
import uk.gov.hmrc.nisp.services.MetricsService

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpGet }

trait NationalInsuranceConnector extends BackendConnector {

  private val apiHeader = "Accept" -> "application/vnd.hmrc.1.0+json"

  def getNationalInsurance(nino: Nino)(implicit hc: HeaderCarrier): Future[NationalInsuranceRecord] = {
    val urlToRead = s"$serviceUrl/ni/$nino"
    val headerCarrier = hc.copy(extraHeaders = hc.extraHeaders :+ apiHeader)
    retrieveFromCache[NationalInsuranceRecord](APIType.NationalInsurance, urlToRead)(headerCarrier, NationalInsuranceRecord.formats)
  }

}

object NationalInsuranceConnector extends NationalInsuranceConnector {
  override val serviceUrl: String = baseUrl("national-insurance")
  override def http: HttpGet = WSHttp
  override def sessionCache: SessionCache = NispSessionCache
  override val metricsService: MetricsService = MetricsService
}
