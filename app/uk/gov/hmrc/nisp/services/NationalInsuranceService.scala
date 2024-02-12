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

package uk.gov.hmrc.nisp.services

import com.google.inject.Inject
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.nisp.connectors.NationalInsuranceConnector
import uk.gov.hmrc.nisp.models.StatePensionExclusion._
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.utils.ExclusionHelper

import scala.concurrent.{ExecutionContext, Future}

class NationalInsuranceService @Inject()(
  nationalInsuranceConnector: NationalInsuranceConnector
)(
  implicit executor: ExecutionContext
) {

  def getSummary(
    nino: Nino
  )(
    implicit hc: HeaderCarrier
  ): Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]] =
    nationalInsuranceConnector.getNationalInsurance(nino).map {
      case Right(Right(ni)) if ni.reducedRateElection =>
        Right(Left(StatePensionExclusionFiltered(Exclusion.MarriedWomenReducedRateElection)))
      case Right(Right(ni)) =>
          Right(Right(wrangleNiRecord(ni)))
      case Right(Left(OkStatePensionExclusion(exclusions, _, _, _))) =>
        Right(Left(StatePensionExclusionFiltered(ExclusionHelper.filterExclusions(exclusions))))
      case Right(Left(ForbiddenStatePensionExclusion(exclusion, _))) =>
        Right(Left(StatePensionExclusionFiltered(exclusion)))
      case Right(Left(CopeStatePensionExclusion(exclusion, copeAvailableDate, previousDate))) =>
        Right(Left(StatePensionExclusionFilteredWithCopeDate(exclusion, copeAvailableDate, previousDate)))
      case Left(errorResponse) =>
        Left(errorResponse)
      case value =>
        Left(UpstreamErrorResponse(s"Match not implemented for: $value", INTERNAL_SERVER_ERROR))
    }

  private def wrangleNiRecord(ni: NationalInsuranceRecord): NationalInsuranceRecord =
    ni.copy(
      qualifyingYearsPriorTo1975 =
        ni.qualifyingYears - ni.taxYears.count(_.qualifying),
      taxYears                   =
        ni.taxYears
          .sortBy(_.taxYear)(Ordering[String].reverse)
          .map(niTaxYear => niTaxYear.copy(niTaxYear.taxYear.take(4)))
    )
}
