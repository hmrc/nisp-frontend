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

package uk.gov.hmrc.nisp.models.admin

import uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlagName

case object ExcessiveTrafficToggle extends FeatureFlagName {
  override val name: String = "excessive-traffic-toggle"
  override val description: Option[String] = Some(
    "Enable/disable excessive traffic message displayed"
  )
}

case object ViewPayableGapsToggle extends FeatureFlagName {
  override val name: String = "view-payable-gaps-toggle"
  override val description: Option[String] = Some(
    "Enable/disable the View payable gaps button on NI record"
  )
}

case object FriendlyUserFilterToggle extends FeatureFlagName {
  override val name: String = "friendly-user-filter-toggle"
  override val description: Option[String] = Some(
    "Enable/disable allowing specific users through instead of everybody."
  )
}

case object NewStatePensionUIToggle extends FeatureFlagName {
  override val name: String = "new-state-pension-ui"
  override val description: Option[String] = Some(
    "Enable/disable new State Pension ui."
  )
}
