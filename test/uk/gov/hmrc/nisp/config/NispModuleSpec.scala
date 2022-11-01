/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.nisp.controllers.auth.{AuthAction, AuthActionImpl}
import uk.gov.hmrc.nisp.utils.UnitSpec

class NispModuleSpec extends UnitSpec {

  val identityVerificationProp = "microservice.services.features.identityVerification"

  "bindings" must {
    "bind an instance of AuthActionImpl" when {
      "the identity verification flag is set to true" in {
        val injector: Injector = GuiceApplicationBuilder()
          .configure(identityVerificationProp -> true)
          .injector()

        injector.instanceOf[AuthAction] shouldBe an[AuthActionImpl]
      }
    }
  }
}
