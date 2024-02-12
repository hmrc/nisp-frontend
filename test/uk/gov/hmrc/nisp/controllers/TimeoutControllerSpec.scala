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

package uk.gov.hmrc.nisp.controllers


import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.nisp.utils.UnitSpec

class TimeoutControllerSpec extends UnitSpec with BeforeAndAfterEach with GuiceOneAppPerSuite with Injecting {

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val timeoutController: TimeoutController = inject[TimeoutController]

  "GET /" should {
    "return 200" in {
      val result = timeoutController.keep_alive(fakeRequest)
      status(result) shouldBe OK
    }
  }
}
