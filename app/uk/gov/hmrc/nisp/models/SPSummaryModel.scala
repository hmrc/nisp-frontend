/*
 * Copyright 2015 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.models

import uk.gov.hmrc.nisp.models.enums.SPContextMessage
import SPContextMessage.SPContextMessage
import play.api.libs.json.Json

case class SPSummaryModel( nino: String,
                           lastProcessedDate: NpsDate,
                           statePensionAmount: SPAmountModel,
                           statePensionAge: SPAgeModel,
                           contextMessage: Option[SPContextMessage],
                           finalRelevantYear: Int,
                           numberOfQualifyingYears: Int,
                           numberOfGaps: Int,
                           numberOfGapsPayable: Int,
                           yearsToContributeUntilPensionAge: Int,
                           hasPsod: Boolean,
                           dateOfBirth: NpsDate,
                           forecastAmount:SPAmountModel,
                           fullNewStatePensionAmount: BigDecimal,
                           contractedOutFlag: Boolean,
                           customerAge: Int,
                           copeAmount: SPAmountModel)

object SPSummaryModel {
  implicit val formats = Json.format[SPSummaryModel]
}
