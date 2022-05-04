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

package uk.gov.hmrc.nisp.services

import javax.inject.Inject
import play.api.Logging
import play.api.http.Status
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nisp.connectors.CitizenDetailsConnector
import uk.gov.hmrc.nisp.models.citizen._

import scala.concurrent.{ExecutionContext, Future}

class CitizenDetailsService @Inject() (citizenDetailsConnector: CitizenDetailsConnector)(implicit
  val executor: ExecutionContext
) extends Logging {

  def retrievePerson(
    nino: Nino
  )(implicit hc: HeaderCarrier): Future[Either[CitizenDetailsError, CitizenDetailsResponse]] =
    citizenDetailsConnector
      .connectToGetPersonDetails(nino)
      .map {
        case Right(citizen) => Right(citizen)
        case Left(error) => error.statusCode match {
          case Status.LOCKED =>
            logger.warn(s"MCI Exclusion for $nino", error)
            Left(MCI_EXCLUSION)
          case Status.NOT_FOUND =>
            logger.error(s"Citizen Details: NotFound for $nino", error)
            Left(NOT_FOUND)
          case Status.BAD_REQUEST =>
            logger.error(s"Citizen Details: BadRequest for $nino", error)
            Left(TECHNICAL_DIFFICULTIES)
          case status =>
            logger.error(s"Citizen Details: $status status for $nino", error)
            Left(TECHNICAL_DIFFICULTIES)
        }
      }
}
