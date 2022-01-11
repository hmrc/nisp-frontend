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

package uk.gov.hmrc.nisp.controllers

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{FakeRequest, Injecting}
import play.api.test.Helpers._
import uk.gov.hmrc.nisp.helpers.{FakeCachedStaticHtmlPartialRetriever, FakePartialRetriever}
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}
import uk.gov.hmrc.nisp.utils.UnitSpec

class CustomLanguageControllerSpec extends UnitSpec with GuiceOneAppPerSuite with Injecting {

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[FormPartialRetriever].to[FakePartialRetriever],
      bind[CachedStaticHtmlPartialRetriever].toInstance(FakeCachedStaticHtmlPartialRetriever)
    ).build()

  val testLanguageController = inject[CustomLanguageController]

  "Hitting language selection endpoint" should {

    "redirect to English translated start page if English language is selected" in {
      val request = FakeRequest()
      val result = testLanguageController.switchToLanguage("english")(request.withHeaders("Referer" -> "myUrl"))
      cookies(result).get("PLAY_LANG").map(_.value) shouldBe Some("en")
      redirectLocation(result).get shouldBe "myUrl"
    }

    "redirect to Welsh translated start page if Welsh language is selected" in {
      val request = FakeRequest()
      val result = testLanguageController.switchToLanguage("cymraeg")(request.withHeaders("Referer" -> "myUrl"))
      cookies(result).get("PLAY_LANG").map(_.value) shouldBe Some("cy")
      redirectLocation(result).get shouldBe "myUrl"
    }

  }
}
