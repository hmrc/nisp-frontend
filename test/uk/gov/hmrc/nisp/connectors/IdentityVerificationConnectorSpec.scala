/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.connectors

import org.scalatestplus.play.OneAppPerSuite
import uk.gov.hmrc.nisp.helpers.{MockIdentityVerificationHttp, MockIdentityVerificationConnector}
import uk.gov.hmrc.nisp.models.enums.IdentityVerificationResult
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

class IdentityVerificationConnectorSpec extends UnitSpec with OneAppPerSuite {
  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  "return success when identityVerification returns success" in {
    MockIdentityVerificationConnector.identityVerificationResponse(MockIdentityVerificationHttp.journeyIdSuccess) shouldBe IdentityVerificationResult.Success
  }
}
