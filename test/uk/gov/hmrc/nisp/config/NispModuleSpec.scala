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

package uk.gov.hmrc.nisp.config

import org.scalatestplus.play.PlaySpec
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.nisp.controllers.auth.{AuthAction, AuthActionImpl, VerifyAuthActionImpl}

class NispModuleSpec extends PlaySpec {

  val identityVerificationProp = "microservice.services.features.identityVerification"

  "bindings" must {
    "bind an instance of AuthActionImpl" when {
      "the identity verification flag is set to true" in {
        val injector: Injector = new GuiceApplicationBuilder()
          .configure(identityVerificationProp -> true)
          .injector()

        injector.instanceOf[AuthAction] mustBe an[AuthActionImpl]
      }
    }
    "bind an instance of VerifyAuthActionImpl" when {
      "the identity verification flag is set to false" in {
        val injector: Injector = new GuiceApplicationBuilder()
          .configure(identityVerificationProp -> false)
          .injector()

        injector.instanceOf[AuthAction] mustBe a[VerifyAuthActionImpl]
      }
    }
  }
}
