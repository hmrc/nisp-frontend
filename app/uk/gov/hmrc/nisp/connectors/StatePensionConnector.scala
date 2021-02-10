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

import play.api.libs.json.{Format, Json, Writes}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nisp.models.enums.APIType
import uk.gov.hmrc.nisp.models.{StatePension, StatePensionExclusion}
import uk.gov.hmrc.nisp.utils.EitherReads.eitherReads

import scala.concurrent.Future

trait StatePensionConnector extends BackendConnector {
  implicit val reads = eitherReads[StatePensionExclusion, StatePension]

  implicit val writes = Writes[Either[StatePensionExclusion, StatePension]] {
    case Left(exclusion) => Json.toJson(exclusion)
    case Right(statePension) => Json.toJson(statePension)
  }

  implicit val formats = Format[Either[StatePensionExclusion, StatePension]](reads, writes)

  val apiHeader = "Accept" -> "application/vnd.hmrc.1.0+json"

  def getStatePension(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[StatePensionExclusion, StatePension]] = {
    val urlToRead = s"$serviceUrl/ni/$nino"
    val headerCarrier = hc.copy(extraHeaders = hc.extraHeaders :+ apiHeader)
    retrieveFromCache[Either[StatePensionExclusion, StatePension]](APIType.StatePension, urlToRead)(headerCarrier, formats)
  }
}
