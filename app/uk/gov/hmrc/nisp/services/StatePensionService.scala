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
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.nisp.connectors.StatePensionConnector
import uk.gov.hmrc.nisp.models.StatePensionExclusion.{CopeStatePensionExclusion, ForbiddenStatePensionExclusion, OkStatePensionExclusion}
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.utils.ExclusionHelper
import uk.gov.hmrc.time.CurrentTaxYear

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class StatePensionService @Inject()(
  statePensionConnector: StatePensionConnector
)(
  implicit executor: ExecutionContext
)
  extends CurrentTaxYear {

  def getSummary(
    nino: Nino
  )(
    implicit hc: HeaderCarrier
  ): Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, StatePension]]] =
    statePensionConnector.getStatePension(nino).map {
      case Right(Left(CopeStatePensionExclusion(exclusionReason, copeAvailableDate, previousAvailableDate))) =>
        Right(Left(StatePensionExclusionFilteredWithCopeDate(exclusionReason, copeAvailableDate, previousAvailableDate)))
      case Right(Left(ForbiddenStatePensionExclusion(exclusion, _))) =>
        Right(Left(StatePensionExclusionFiltered(exclusion)))
      case Right(Left(OkStatePensionExclusion(exclusionReasons, pensionAge, pensionDate, statePensionAgeUnderConsideration))) =>
        Right(Left(StatePensionExclusionFiltered(ExclusionHelper.filterExclusions(exclusionReasons),
          pensionAge, pensionDate, statePensionAgeUnderConsideration)))
      case Right(Left(x)) =>
        throw new Exception(s"Unknown Exclusion: $x")
      case Right(Right(statePension)) =>
        Right(Right(statePension))
      case Left(errorResponse) =>
        Left(errorResponse)
    }

  def yearsToContributeUntilPensionAge(earningsIncludedUpTo: LocalDate, finalRelevantYearStart: Int): Int =
    finalRelevantYearStart - taxYearFor(earningsIncludedUpTo).startYear

  override def now: () => LocalDate = () => LocalDate.now(ukTime)
}
