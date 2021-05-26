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

package uk.gov.hmrc.nisp.controllers.auth

import org.joda.time.DateTime
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.LoginTimes
import uk.gov.hmrc.play.test.UnitSpec

class AuthDetailsSpec extends UnitSpec {

  val confidenceLevel = ConfidenceLevel.L200
  val loginTimes = LoginTimes(new DateTime(), None)

  "isGG" should {
    "return true" when {
      "the auth provider contains the GG flag" in {
        val authProvider = Some("GovernmentGateway")
        val authDetails = AuthDetails(confidenceLevel, authProvider, loginTimes)

        authDetails.isGG shouldBe true
      }
    }

    "return false" when {
      "the auth provider does not contain the GG flag" in {
        val authProviderNonGG = Some("Verify")
        val authDetails = AuthDetails(confidenceLevel, authProviderNonGG, loginTimes)

        authDetails.isGG shouldBe false
      }
    }
  }

  "isVerify" should {
    "return true" when {
      "the auth provider contains the verify flag" in {
        val authProvider = Some("Verify")
        val authDetails = AuthDetails(confidenceLevel, authProvider, loginTimes)

        authDetails.isVerify shouldBe true
      }
    }

    "return false" when {
      "the auth provider does not contain the verify flag" in {
        val authProviderNonGG = Some("GovernmentGateway")
        val authDetails = AuthDetails(confidenceLevel, authProviderNonGG, loginTimes)

        authDetails.isVerify shouldBe false
      }
    }
  }

}
