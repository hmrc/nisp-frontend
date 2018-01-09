/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.builders

import org.joda.time.LocalDate
import uk.gov.hmrc.nisp.models.NationalInsuranceTaxYear


object NationalInsuranceTaxYearBuilder {
  def apply(taxYear: String, qualifying: Boolean = true, payable: Boolean = false ,underInvestigation :Boolean): NationalInsuranceTaxYear = {
    if(qualifying) {
      NationalInsuranceTaxYear(
        taxYear,
        true,
        classOneContributions = 12345.67,
        classTwoCredits = 10,
        classThreeCredits = 8,
        otherCredits = 12,
        0,
        None,
        None,
        false,
        underInvestigation = underInvestigation
      )
    } else {
      NationalInsuranceTaxYear(
        taxYear,
        false,
        classOneContributions = 1,
        classTwoCredits = 1,
        classThreeCredits = 1,
        otherCredits = 1,
        classThreePayable = 755.56,
        classThreePayableBy = Some( new LocalDate(2019, 4, 5)),
        classThreePayableByPenalty = Some( new LocalDate(2023, 4, 5)),
        payable = payable,
        underInvestigation = underInvestigation
      )
    }
  }
}
