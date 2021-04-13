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

package uk.gov.hmrc.nisp.services

import com.google.inject.Inject
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, Upstream4xxResponse}
import uk.gov.hmrc.nisp.connectors.NationalInsuranceConnectorImpl
import uk.gov.hmrc.nisp.models.{Exclusion, NationalInsuranceRecord}

import scala.concurrent.{ExecutionContext, Future}

class NationalInsuranceService @Inject()(nationalInsuranceConnector: NationalInsuranceConnectorImpl)
                                        (implicit executor: ExecutionContext){

  final val ExclusionCodeDead = "EXCLUSION_DEAD"
  final val ExclusionCodeManualCorrespondence = "EXCLUSION_MANUAL_CORRESPONDENCE"
  final val ExclusionCodeIsleOfMan = "EXCLUSION_ISLE_OF_MAN"
  final val ExclusionCodeMarriedWomen = "EXCLUSION_MARRIED_WOMENS_REDUCED_RATE"
  final val ExclusionErrorCode = 403

  def getSummary(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[Exclusion, NationalInsuranceRecord]] = {
    nationalInsuranceConnector.getNationalInsurance(nino)
      .map { ni =>
        if (ni.reducedRateElection) Left(Exclusion.MarriedWomenReducedRateElection)
        else Right(
          ni.copy(
            taxYears = ni.taxYears.sortBy(_.taxYear)(Ordering[String].reverse),
            qualifyingYearsPriorTo1975 = ni.qualifyingYears - ni.taxYears.count(_.qualifying)
          )
        )
      }
      .recover {
        case Upstream4xxResponse(message, ExclusionErrorCode, _, _) if message.contains(ExclusionCodeDead) =>
          Left(Exclusion.Dead)
        case Upstream4xxResponse(message, ExclusionErrorCode, _, _) if message.contains(ExclusionCodeManualCorrespondence) =>
          Left(Exclusion.ManualCorrespondenceIndicator)
        case Upstream4xxResponse(message, ExclusionErrorCode, _, _) if message.contains(ExclusionCodeIsleOfMan) =>
          Left(Exclusion.IsleOfMan)
        case Upstream4xxResponse(message, ExclusionErrorCode, _, _) if message.contains(ExclusionCodeMarriedWomen) =>
          Left(Exclusion.MarriedWomenReducedRateElection)
      }
  }
}