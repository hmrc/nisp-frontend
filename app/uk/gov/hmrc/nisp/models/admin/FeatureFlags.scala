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

case object SCAWrapperToggle extends FeatureFlagName {
  override val name: String = "SCAWrapperToggle"
  override val description: Option[String] = Some(
    "Enable/Disable the sca page wrapper"
  )
}

case object PertaxBackendToggle extends FeatureFlagName {
  override val name: String            = "pertax-backend-toggle"
  override val description: Option[String] = Some(
    "Enable/disable pertax backend during auth"
  )
}

case object ExcessiveTrafficToggle extends FeatureFlagName {
  override val name: String = "excessive-traffic-toggle"
  override val description: Option[String] = Some(
    "Enable/disable excessive traffic message displayed"
  )
}
