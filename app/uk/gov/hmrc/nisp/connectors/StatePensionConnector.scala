/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.libs.json.{Format, Json, Writes}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.models.enums.APIType
import uk.gov.hmrc.nisp.models.{StatePension, StatePensionExclusion}
import uk.gov.hmrc.nisp.services.MetricsService
import uk.gov.hmrc.nisp.utils.EitherReads.eitherReads
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.domain.Nino
import scala.concurrent.{ExecutionContext, Future}

class StatePensionConnector @Inject() (
  val http: HttpClient,
  val sessionCache: SessionCache,
  val metricsService: MetricsService,
  val executionContext: ExecutionContext,
  appConfig: ApplicationConfig
) extends BackendConnector {

  val serviceUrl: String = appConfig.statePensionServiceUrl

  implicit val reads = eitherReads[StatePensionExclusion, StatePension]

  implicit val writes = Writes[Either[StatePensionExclusion, StatePension]] {
    case Left(exclusion)     => Json.toJson(exclusion)
    case Right(statePension) => Json.toJson(statePension)
  }

  implicit val formats = Format[Either[StatePensionExclusion, StatePension]](reads, writes)

  def getStatePension(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[StatePensionExclusion, StatePension]] = {
    val urlToRead = s"$serviceUrl/ni/$nino"
    val header    = Seq("Accept" -> "application/vnd.hmrc.1.0+json")
    retrieveFromCache[Either[StatePensionExclusion, StatePension]](APIType.StatePension, urlToRead, header)
  }
}
