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

package uk.gov.hmrc.nisp.controllers

import org.scalatestplus.play.OneAppPerSuite
import play.api.test.{Helpers, FakeRequest}
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.{WithFakeApplication, UnitSpec}

import scala.concurrent.ExecutionContext.Implicits.global

class RedirectControllerSpec extends UnitSpec with OneAppPerSuite with WithFakeApplication {

  "GET /yourstatepension" should {
    "redirect to /checkmystatepension" in {
      val request = FakeRequest("GET", "/yourstatepension")
      val result = Helpers.route(request).get.map { result => result }
      redirectLocation(result) shouldBe Some("/checkmystatepension")
    }
  }

  "GET /yourstatepension + query string" should {
    "redirect to /checkmystatepension + query string" in {
      val request = FakeRequest("GET", "/yourstatepension?p=123&q=456")
      val result = Helpers.route(request).get.map { result => result }
      redirectLocation(result) shouldBe Some("/checkmystatepension?p=123&q=456")
    }
  }


  "GET /yourstatepension/account" should {
    "redirect to /checkmystatepension/account" in {
      val request = FakeRequest("GET", "/yourstatepension/account")
      val result = Helpers.route(request).get.map { result => result }
      redirectLocation(result) shouldBe Some("/checkmystatepension/account")
    }
  }

  "GET /yourstatepension/account + query string" should {
    "redirect to /checkmystatepension/account" in {
      val request = FakeRequest("GET", "/yourstatepension/account?p=123&q=456")
      val result = Helpers.route(request).get.map { result => result }
      redirectLocation(result) shouldBe Some("/checkmystatepension/account?p=123&q=456")
    }
  }

  "GET /yourstatepension//account" should {
    "redirect to /checkmystatepension/account" in {
      val request = FakeRequest("GET", "/yourstatepension//account")
      val result = Helpers.route(request).get.map { result => result }
      redirectLocation(result) shouldBe Some("/checkmystatepension/account")
    }
  }

}
