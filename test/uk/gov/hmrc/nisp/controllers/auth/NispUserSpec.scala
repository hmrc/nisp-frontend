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

package uk.gov.hmrc.nisp.controllers.auth

import uk.gov.hmrc.nisp.models.citizen.Address
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Accounts, ConfidenceLevel, CredentialStrength}
import uk.gov.hmrc.play.frontend.auth.{AuthContext, LoggedInUser, Principal}
import uk.gov.hmrc.play.test.UnitSpec

class NispUserSpec extends UnitSpec {

  private val fakeAuthContext = new AuthContext(LoggedInUser("", None, None, None, CredentialStrength.Strong,
    ConfidenceLevel.L0, "test oid"), Principal(None, Accounts()), None, None, None, None)

  def createNispUser(address: Option[Address] = None) = {
    NispUser(fakeAuthContext, None, "", None, None, address)
  }

  "livesAbroad" should {
    "should return false if Address does not exist" in {
      createNispUser(address = None).livesAbroad shouldBe false
    }

    "should return false if Address does not have a country" in {
      createNispUser(address = Some(Address(None))).livesAbroad shouldBe false
    }
  }
}
