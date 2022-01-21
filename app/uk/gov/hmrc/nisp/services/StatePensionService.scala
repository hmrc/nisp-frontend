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
import java.time.{LocalDate, LocalDateTime, ZoneId}

import play.api.http.Status._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.UpstreamErrorResponse.WithStatusCode
import uk.gov.hmrc.http.{HeaderCarrier, Upstream4xxResponse}
import uk.gov.hmrc.nisp.connectors.StatePensionConnector
import uk.gov.hmrc.nisp.models.{Exclusion, _}
import uk.gov.hmrc.time.CurrentTaxYear

import scala.util.matching.Regex
import scala.concurrent.{ExecutionContext, Future}

class StatePensionService @Inject() (statePensionConnector: StatePensionConnector)(implicit executor: ExecutionContext)
    extends CurrentTaxYear {

  val exclusionCodeDead: String                 = "EXCLUSION_DEAD"
  val exclusionCodeManualCorrespondence: String = "EXCLUSION_MANUAL_CORRESPONDENCE"
  val exclusionCodeCopeProcessing: String       = "EXCLUSION_COPE_PROCESSING"
  val exclusionCodeCopeProcessingFailed: String = "EXCLUSION_COPE_PROCESSING_FAILED"

  def getSummary(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[StatePensionExcl, StatePension]] =
    statePensionConnector
      .getStatePension(nino)
      .map {
        case Right(statePension) => Right(statePension)

        case Left(spExclusion) =>
          Left(
            StatePensionExclusionFiltered(
              filterExclusions(spExclusion.exclusionReasons),
              spExclusion.pensionAge,
              spExclusion.pensionDate,
              spExclusion.statePensionAgeUnderConsideration
            )
          )
      }
      .recover {
        case ex @ WithStatusCode(FORBIDDEN) if ex.getMessage.contains(exclusionCodeDead)                 =>
          Left(StatePensionExclusionFiltered(Exclusion.Dead))
        case ex @ WithStatusCode(FORBIDDEN) if ex.getMessage.contains(exclusionCodeManualCorrespondence) =>
          Left(StatePensionExclusionFiltered(Exclusion.ManualCorrespondenceIndicator))
        case ex @ WithStatusCode(FORBIDDEN) if ex.getMessage.contains(exclusionCodeCopeProcessingFailed) =>
          Left(StatePensionExclusionFiltered(Exclusion.CopeProcessingFailed))
        case ex @ WithStatusCode(FORBIDDEN) if ex.getMessage.contains(exclusionCodeCopeProcessing)       =>
          Left(getCopeExclusionWithRegex(ex.getMessage))
      }

  def yearsToContributeUntilPensionAge(earningsIncludedUpTo: LocalDate, finalRelevantYearStart: Int): Int =
    finalRelevantYearStart - taxYearFor(earningsIncludedUpTo).startYear

  private[services] def filterExclusions(exclusions: List[Exclusion]): Exclusion =
    if (exclusions.contains(Exclusion.Dead)) {
      Exclusion.Dead
    } else if (exclusions.contains(Exclusion.ManualCorrespondenceIndicator)) {
      Exclusion.ManualCorrespondenceIndicator
    } else if (exclusions.contains(Exclusion.PostStatePensionAge)) {
      Exclusion.PostStatePensionAge
    } else if (exclusions.contains(Exclusion.AmountDissonance)) {
      Exclusion.AmountDissonance
    } else if (exclusions.contains(Exclusion.IsleOfMan)) {
      Exclusion.IsleOfMan
    } else if (exclusions.contains(Exclusion.MarriedWomenReducedRateElection)) {
      Exclusion.MarriedWomenReducedRateElection
    } else {
      throw new RuntimeException(s"Un-accounted for exclusion in NispConnectionNI: $exclusions")
    }

  private def getCopeExclusionWithRegex(copeResponse: String): StatePensionExclusionFilteredWithCopeDate = {
    val copeRegex: Regex =
      """(?:.*)(?:'\{"code":"EXCLUSION_COPE_PROCESSING","copeDataAvailableDate":\")(\d{4}-\d{2}-\d{2})(?:(?:\",\"previousAvailableDate\":\"(\d{4}-\d{2}-\d{2}))?)(?:\"}')""".r

    copeResponse match {
      case copeRegex(copeAvailableDate, null)                      =>
        StatePensionExclusionFilteredWithCopeDate(Exclusion.CopeProcessing, LocalDate.parse(copeAvailableDate), None)
      case copeRegex(copeAvailableDate, copePreviousAvailableDate) =>
        StatePensionExclusionFilteredWithCopeDate(
          Exclusion.CopeProcessing,
          LocalDate.parse(copeAvailableDate),
          Some(LocalDate.parse(copePreviousAvailableDate))
        )
      case _                                                       => throw new Exception("COPE date not matched with regex!")
    }
  }

  override def now: () => LocalDate = () => LocalDate.now(ZoneId.of("Europe/London"))
}
