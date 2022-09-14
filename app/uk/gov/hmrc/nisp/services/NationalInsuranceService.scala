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

import com.google.inject.Inject
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.nisp.connectors.NationalInsuranceConnectorImpl
import uk.gov.hmrc.nisp.models.StatePensionExclusion._
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.utils.ExclusionHelper

import scala.concurrent.{ExecutionContext, Future}

class NationalInsuranceService @Inject()(nationalInsuranceConnector: NationalInsuranceConnectorImpl)
                                        (implicit executor: ExecutionContext) {

  def getSummary(nino: Nino)(implicit hc: HeaderCarrier):
        Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]] = {
    nationalInsuranceConnector.getNationalInsurance(nino)
      .map {
        case Right(Right(ni)) =>
          if (ni.reducedRateElection) Right(Left(StatePensionExclusionFiltered(Exclusion.MarriedWomenReducedRateElection)))
          else Right(Right(
            ni.copy(
              taxYears = ni.taxYears.sortBy(_.taxYear)(Ordering[String].reverse).map(t => t.copy(convertTaxYear(t.taxYear))),
              qualifyingYearsPriorTo1975 = ni.qualifyingYears - ni.taxYears.count(_.qualifying)
            )
          ))

        case Right(Left(OkStatePensionExclusion(exclusions, _, _, _))) =>
          Right(Left(StatePensionExclusionFiltered(ExclusionHelper.filterExclusions(exclusions))))

        case Right(Left(ForbiddenStatePensionExclusion(exclusion, _))) =>
          Right(Left(StatePensionExclusionFiltered(exclusion)))

        case Right(Left(CopeStatePensionExclusion(exclusion, copeAvailableDate, previousDate))) =>
          Right(Left(StatePensionExclusionFilteredWithCopeDate(exclusion, copeAvailableDate, previousDate)))

        case Left(errorResponse) => Left(errorResponse)

        case value => throw new NotImplementedError(s"Match not implemented for: $value")
      }
  }

  private def convertTaxYear(taxYear: String): String = {
    val taxYearStart = taxYear.take(4).toInt
    s"$taxYearStart to ${taxYearStart + 1}"
  }
}
