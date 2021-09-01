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

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.nisp.helpers.{FakeCachedStaticHtmlPartialRetriever, FakePartialRetriever}
import uk.gov.hmrc.nisp.utils.UnitSpec
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}

class RedirectControllerSpec extends UnitSpec with GuiceOneAppPerSuite {

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[FormPartialRetriever].to[FakePartialRetriever],
      bind[CachedStaticHtmlPartialRetriever].toInstance(FakeCachedStaticHtmlPartialRetriever)
    ).build()

  "GET /checkmystatepension" should {
    "redirect to /check-your-state-pension" in {
      val request = FakeRequest("GET", "/checkmystatepension")
      val result = Helpers.route(app ,request).get
      redirectLocation(result) shouldBe Some("/check-your-state-pension")
    }
  }

  "GET /checkmystatepension + query string" should {
    "redirect to /check-your-state-pension + query string" in {
      val request = FakeRequest("GET", "/checkmystatepension?p=123&q=456")
      val result = Helpers.route(app, request).get
      redirectLocation(result) shouldBe Some("/check-your-state-pension?p=123&q=456")
    }
  }

  "GET /checkmystatepension/account" should {
    "redirect to /check-your-state-pension/account" in {
      val request = FakeRequest("GET", "/checkmystatepension/account")
      val result = Helpers.route(app, request).get
      redirectLocation(result) shouldBe Some("/check-your-state-pension/account")
    }
  }

  "GET /checkmystatepension/account + query string" should {
    "redirect to /check-your-state-pension/account" in {
      val request = FakeRequest("GET", "/checkmystatepension/account?p=123&q=456")
      val result = Helpers.route(app, request).get
      redirectLocation(result) shouldBe Some("/check-your-state-pension/account?p=123&q=456")
    }
  }

  "GET /checkmystatepension//account" should {
    "redirect to /check-your-state-pension/account" in {
      val request = FakeRequest("GET", "/checkmystatepension//account")
      val result = Helpers.route(app, request).get
      redirectLocation(result) shouldBe Some("/check-your-state-pension/account")
    }
  }
}
