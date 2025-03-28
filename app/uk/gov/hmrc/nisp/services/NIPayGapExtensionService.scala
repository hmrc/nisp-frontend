/*
 * Copyright 2025 HM Revenue & Customs
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
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.models.{AfterDeadline, AfterDeadlineExtended, BeforeDeadline, PayableGapExtensionDetails, PayableGapExtensionStatus, PayableGapInfo}
import uk.gov.hmrc.time.CurrentTaxYear

import java.time.LocalDate

class NIPayGapExtensionService @Inject() (
  appConfig: ApplicationConfig
) extends CurrentTaxYear {

  private val currentTaxYearGapDetails = taxYearGapDetails(current.startYear)
  private val previousTaxYearGapDetails = taxYearGapDetails(current.startYear - 1)
  private val postDeadline: Boolean =
    (today.getYear == current.startYear) && today.isAfter(LocalDate.of(current.startYear, 4, 5))

  def payableGapInfo(yearsPayable: Seq[String]): PayableGapInfo = {
    val status = payGapExtensionStatus(yearsPayable)
    PayableGapInfo(
      payableGapExtensionStatus = status,
      numberOfGapYears = numberOfGapYears(status),
      startYear = current.startYear - numberOfGapYears(status)
    )
  }

  private def numberOfGapYears(status: PayableGapExtensionStatus): Int =
    status match {
      case AfterDeadline         => currentTaxYearGapDetails.payableGaps
      case BeforeDeadline        => currentTaxYearGapDetails.payableGaps
      case AfterDeadlineExtended => previousTaxYearGapDetails.payableGaps + 1
    }

  private def payGapExtensionStatus(yearsPayable: Seq[String]): PayableGapExtensionStatus = {
    val gapsInExtension = yearsPayable.map(_.toInt).exists(_ < currentTaxYearGapDetails.taxYear - currentTaxYearGapDetails.payableGaps)
    (gapsInExtension, postDeadline) match {
      case (true, true)   => AfterDeadlineExtended
      case (false, true)  => AfterDeadline
      case (_, false)     => BeforeDeadline
    }
  }

  private def taxYearGapDetails(taxYear: Int): PayableGapExtensionDetails =
    appConfig.payableGapExtensions.find(_.taxYear == taxYear)
      .fold(
        PayableGapExtensionDetails(taxYear, appConfig.payableGapDefault)
      )(p => p)

  override def now: () => LocalDate = () => LocalDate.now(ukTime)
}
