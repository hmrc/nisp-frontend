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

package uk.gov.hmrc.nisp.events

import uk.gov.hmrc.nisp.models.enums.Exclusion.Exclusion
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nisp.models.UserName

object AccountExclusionEvent {
  def apply(nino: String, name: UserName, spExclusion: Exclusion)(implicit hc: HeaderCarrier): AccountExclusionEvent =
    new AccountExclusionEvent(nino, name.toString, spExclusion)
}
class AccountExclusionEvent(nino: String, name: String, spExclusion: Exclusion)(implicit hc: HeaderCarrier)
  extends NispBusinessEvent("Exclusion",
  Map(
    "nino" -> nino,
    "ExclusionReasons" -> spExclusion.toString,
    "Name" -> name
  )
)
