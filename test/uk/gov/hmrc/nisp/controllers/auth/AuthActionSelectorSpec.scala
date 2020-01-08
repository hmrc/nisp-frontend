/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.controllers.auth

import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.play.test.UnitSpec

class AuthActionSelectorSpec extends UnitSpec with OneAppPerSuite with MockitoSugar {

  "The decide method" should {

    "use VerifyAuthActionImpl when IV disabled" in {
      val applicationConfig: ApplicationConfig = mock[ApplicationConfig]

      when(applicationConfig.identityVerification).thenReturn(false)
      AuthActionSelector.decide(applicationConfig) shouldBe a[VerifyAuthActionImpl]
    }

    "use AuthActionImpl when IV enabled" in {
      val applicationConfig: ApplicationConfig = mock[ApplicationConfig]

      when(applicationConfig.identityVerification).thenReturn(true)
      AuthActionSelector.decide(applicationConfig) shouldBe an[AuthActionImpl]
    }
  }
}
