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

package uk.gov.hmrc.nisp.services

import play.api.Logger
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.nisp.connectors.CitizenDetailsConnector
import uk.gov.hmrc.nisp.models.citizen.Citizen
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

object CitizenDetailsService extends CitizenDetailsService {
  override val citizenDetailsConnector = CitizenDetailsConnector
}

trait CitizenDetailsService {
  val citizenDetailsConnector: CitizenDetailsConnector

  def retrievePerson(nino: Nino)(implicit hc: HeaderCarrier): Future[Option[Citizen]] = {
    citizenDetailsConnector.connectToGetPersonDetails(nino) map ( citizen => Some(citizen.person)) recover {
      case ex =>
        Logger.error(s"Citizen details returned error: ${ex.getMessage}", ex)
        None
    }
  }
}
