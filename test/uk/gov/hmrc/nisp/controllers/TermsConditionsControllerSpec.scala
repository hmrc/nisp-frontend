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
import play.api.http._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{FakeRequest, Injecting}
import play.api.test.Helpers._
import uk.gov.hmrc.nisp.helpers.{FakeTemplateRenderer, _}
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}
import uk.gov.hmrc.renderer.TemplateRenderer

class TermsConditionsControllerSpec extends PlaySpec with GuiceOneAppPerSuite with Injecting {

  val fakeRequest = FakeRequest("GET", "/")

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[TemplateRenderer].toInstance(FakeTemplateRenderer),
      bind[FormPartialRetriever].toInstance(FakePartialRetriever),
      bind[CachedStaticHtmlPartialRetriever].toInstance(FakeCachedStaticHtmlPartialRetriever)
    ).build()

  val termsConditionController = inject[TermsConditionsController]

  "GET /" should {

    "return 200" in {
      val result = termsConditionController.show(fakeRequest)
      status(result) mustBe Status.OK
    }

    "return HTML" in {
      val result = termsConditionController.show(fakeRequest)
      contentType(result) mustBe Some("text/html")
      charset(result) mustBe Some("utf-8")
    }

    "load the T&Cs page without back link" in {
      val result = contentAsString(termsConditionController.show(fakeRequest))
      result must include("The information given is based on details from your National Insurance record at the time you use the service. While we will make every effort to keep your record up to date, we do not guarantee that it will be or that it is error and omission free.")
      result must not include("<p class=\"backlink\"><a href=\"/check-your-state-pension/account\">Back</a></p>")
    }

    "load the T&Cs page with back link" in {
      val fakeRequest = FakeRequest("GET", "/?showBackLink=true")
      val result = contentAsString(termsConditionController.show(fakeRequest))
      result must include("The information given is based on details from your National Insurance record at the time you use the service. While we will make every effort to keep your record up to date, we do not guarantee that it will be or that it is error and omission free.")
      result must include("<p class=\"backlink\"><a href=\"/check-your-state-pension/account\">Back</a></p>")
    }

  }

  "GET / with showBackLink query parameter" should {

    "return 200 with flag value" in {
      val result = termsConditionController.show(fakeRequest)
      status(result) mustBe Status.OK
      contentAsString(result) must include("The information given is based on details from your National Insurance record at the time you use the service. While we will make every effort to keep your record up to date, we do not guarantee that it will be or that it is error and omission free.")
      contentAsString(result) must not include("<p class=\"backlink\"><a href=\"/check-your-state-pension/account\">Back</a></p>")
    }

    "load the T&Cs page with back link" in {
      val fakeRequest = FakeRequest("GET", "/?showBackLink=true")
      val result = contentAsString(termsConditionController.show(fakeRequest))
      result must include("The information given is based on details from your National Insurance record at the time you use the service. While we will make every effort to keep your record up to date, we do not guarantee that it will be or that it is error and omission free.")
      result must include("<p class=\"backlink\"><a href=\"/check-your-state-pension/account\">Back</a></p>")
    }

  }
}
