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

package uk.gov.hmrc.nisp.services

import play.api.Logger
import play.api.http.Status
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.nisp.connectors.CitizenDetailsConnector
import uk.gov.hmrc.nisp.models.citizen.{Address, Citizen, CitizenDetailsResponse}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future
import uk.gov.hmrc.http.{ BadRequestException, HeaderCarrier, NotFoundException, Upstream4xxResponse }

object CitizenDetailsService extends CitizenDetailsService {
  override val citizenDetailsConnector = CitizenDetailsConnector
}

trait CitizenDetailsService {
  val citizenDetailsConnector: CitizenDetailsConnector

  def retrievePerson(nino: Nino)(implicit hc: HeaderCarrier): Future[Option[CitizenDetailsResponse]] = {
    citizenDetailsConnector.connectToGetPersonDetails(nino) map ( citizen => Some(citizen)) recover {
      case ex: Upstream4xxResponse if ex.upstreamResponseCode == Status.LOCKED =>
        Logger.warn(s"MCI Exclusion for $nino", ex)
        None
      case ex: BadRequestException =>
        Logger.error(s"Citizen Details: BadRequest for $nino", ex)
        None
      case ex: NotFoundException =>
        Logger.error(s"Citizen Details: NotFound for $nino", ex)
        None
    }
  }
}
