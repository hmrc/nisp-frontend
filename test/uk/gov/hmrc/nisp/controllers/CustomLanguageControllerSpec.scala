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

package uk.gov.hmrc.nisp.controllers

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.UnitSpec

class CustomLanguageControllerSpec extends UnitSpec with GuiceOneAppPerSuite {

  val testLanguageController = app.injector.instanceOf[CustomLanguageController]

  "Hitting language selection endpoint" should {

    "redirect to English translated start page if English language is selected" in {
      val request = FakeRequest()
      val result = testLanguageController.switchToLanguage("english")(request)
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=en; Path=/; HTTPOnly;;PLAY_FLASH=switching-language=true; Path=/; HTTPOnly")
    }

    "redirect to Welsh translated start page if Welsh language is selected" in {
      val request = FakeRequest()
      val result = testLanguageController.switchToLanguage("cymraeg")(request)
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=cy; Path=/; HTTPOnly;;PLAY_FLASH=switching-language=true; Path=/; HTTPOnly")
    }

  }
}
