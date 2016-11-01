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

import org.joda.time.{DateTime, LocalDate}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.nisp.connectors.NispConnector
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.time.CurrentTaxYear


trait StatePensionService extends CurrentTaxYear {
  def getSummary(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[StatePensionExclusion, StatePension]]

  def yearsToContributeUntilPensionAge(earningsIncludedUpTo: LocalDate, finalRelevantYearStart: Int): Int = {
    finalRelevantYearStart - taxYearFor(earningsIncludedUpTo).startYear
  }
}

trait NispConnection {
  val nisp: NispConnector

  def getSummary(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[StatePensionExclusion, StatePension]] = {
    nisp.connectToGetSPResponse(nino) map {
      case SPResponseModel(Some(spSummary: SPSummaryModel), None, None) => Right(StatePension(
        earningsIncludedUpTo = spSummary.lastProcessedDate.localDate,
        amounts = StatePensionAmounts(
          protectedPayment = spSummary.forecast.oldRulesCustomer,
          current = StatePensionAmountRegular(
            spSummary.statePensionAmount.week,
            spSummary.statePensionAmount.month,
            spSummary.statePensionAmount.year
          ),
          forecast = StatePensionAmountForecast(
            spSummary.forecast.yearsLeftToWork,
            spSummary.forecast.forecastAmount.week,
            spSummary.forecast.forecastAmount.month,
            spSummary.forecast.forecastAmount.year
          ),
          maximum = StatePensionAmountMaximum(
            spSummary.forecast.yearsLeftToWork,
            spSummary.forecast.minGapsToFillToReachMaximum,
            spSummary.forecast.personalMaximum.week,
            spSummary.forecast.personalMaximum.month,
            spSummary.forecast.personalMaximum.year),
          cope = StatePensionAmountRegular(
            spSummary.copeAmount.week,
            spSummary.copeAmount.month,
            spSummary.copeAmount.year
          )
        ),
        pensionAge = spSummary.statePensionAge.age,
        pensionDate = spSummary.statePensionAge.date.localDate,
        finalRelevantYear = spSummary.finalRelevantYear.toString,
        numberOfQualifyingYears = spSummary.numberOfQualifyingYears,
        pensionSharingOrder = spSummary.hasPsod,
        currentFullWeeklyPensionAmount = spSummary.fullNewStatePensionAmount
      ))

      case SPResponseModel(Some(spSummary: SPSummaryModel), Some(spExclusions: ExclusionsModel), _) =>
        Left(StatePensionExclusion(
          exclusionReasons = spExclusions.exclusions,
          pensionAge = spSummary.statePensionAge.age,
          pensionDate = spSummary.statePensionAge.date.localDate
        ))

      case _ => throw new RuntimeException("SP Response Model is unmatchable. This is probably a logic error.")
    }
  }
}

object StatePensionService extends StatePensionService with NispConnection {
  override val nisp: NispConnector = NispConnector
  override def now: () => DateTime = () => DateTime.now(ukTime)
}
