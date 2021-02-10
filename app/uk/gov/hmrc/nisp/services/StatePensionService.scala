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

import org.joda.time.LocalDate
import play.api.http.Status._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, Upstream4xxResponse}
import uk.gov.hmrc.nisp.connectors.StatePensionConnector
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.models.enums.Exclusion
import uk.gov.hmrc.nisp.models.enums.Exclusion._
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.time.CurrentTaxYear

import scala.concurrent.Future


trait StatePensionService extends CurrentTaxYear {
  def getSummary(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[StatePensionExclusionFiltered, StatePension]]

  def yearsToContributeUntilPensionAge(earningsIncludedUpTo: LocalDate, finalRelevantYearStart: Int): Int = {
    finalRelevantYearStart - taxYearFor(earningsIncludedUpTo).startYear
  }

  private[services] def filterExclusions(exclusions: List[Exclusion]): Exclusion = {
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
  }
}

trait StatePensionConnection extends StatePensionService {
  val statePensionConnector: StatePensionConnector

  final val exclusionCodeDead = "EXCLUSION_DEAD"
  final val exclusionCodeManualCorrespondence = "EXCLUSION_MANUAL_CORRESPONDENCE"

  def getSummary(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[StatePensionExclusionFiltered, StatePension]] = {
    statePensionConnector.getStatePension(nino)
      .map {
        case Right(statePension) => Right(statePension)

        case Left(spExclusion) => Left(StatePensionExclusionFiltered(
            filterExclusions(spExclusion.exclusionReasons),
            spExclusion.pensionAge,
            spExclusion.pensionDate,
            spExclusion.statePensionAgeUnderConsideration
          ))
      }
      .recover {
      case ex: Upstream4xxResponse if ex.upstreamResponseCode == FORBIDDEN && ex.message.contains(exclusionCodeDead) =>
        Left(StatePensionExclusionFiltered(Exclusion.Dead))
      case ex: Upstream4xxResponse if ex.upstreamResponseCode == FORBIDDEN && ex.message.contains(exclusionCodeManualCorrespondence) =>
        Left(StatePensionExclusionFiltered(Exclusion.ManualCorrespondenceIndicator))
    }
  }
}
