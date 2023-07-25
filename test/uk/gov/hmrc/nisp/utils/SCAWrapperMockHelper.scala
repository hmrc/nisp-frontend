/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.utils

import org.mockito.ArgumentMatchers.{eq => mockEq}
import org.mockito.Mockito.{mock, when}
import org.mockito.stubbing.OngoingStubbing
import play.api.inject.{Binding, bind}
import uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlag
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.nisp.models.admin.SCAWrapperToggle

import scala.concurrent.Future

trait SCAWrapperMockHelper {

  lazy val mockFeatureFlagService: FeatureFlagService = mock(classOf[FeatureFlagService])

  lazy val featureFlagServiceBinding: Binding[FeatureFlagService] = bind[FeatureFlagService].toInstance(mockFeatureFlagService)

  def featureFlagSCAWrapperMock(isEnabled: Boolean = false): OngoingStubbing[Future[FeatureFlag]] = {
    when(mockFeatureFlagService.get(mockEq(SCAWrapperToggle)))
      .thenReturn(Future.successful(FeatureFlag(SCAWrapperToggle, isEnabled = isEnabled)))
  }

}
