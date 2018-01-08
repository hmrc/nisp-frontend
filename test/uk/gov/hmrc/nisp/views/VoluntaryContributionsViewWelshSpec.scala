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

package uk.gov.hmrc.nisp.views

import uk.gov.hmrc.nisp.helpers._

class VoluntaryContributionsViewWelshSpec extends VoluntaryContributionsViewSpec {

  implicit override val lang = LanguageToggle.getLanguageCode(testInWelsh = true)
  implicit override val lanCookie = LanguageToggle.getLanguageCookie(testInWelsh = true)

  override val expectedMoneyServiceLink = "https://www.moneyadviceservice.org.uk/cy"
  override val expectedCitizensAdviceLink = "https://www.citizensadvice.org.uk/cymraeg/"

}
