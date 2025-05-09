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

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.*
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.auth.GracePeriodAction
import uk.gov.hmrc.nisp.helpers.FakeGracePeriodAction
import uk.gov.hmrc.nisp.services.GracePeriodService
import uk.gov.hmrc.nisp.utils.UnitSpec

class TermsConditionsControllerSpec extends UnitSpec with GuiceOneAppPerSuite with Injecting with BeforeAndAfterEach {

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type]  = FakeRequest("GET", "/")
  val mockApplicationConfig: ApplicationConfig          = mock[ApplicationConfig]
  val mocGracePeriodService: GracePeriodService         = mock[GracePeriodService]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[ApplicationConfig].toInstance(mockApplicationConfig),
      bind[GracePeriodAction].to[FakeGracePeriodAction],
      bind[GracePeriodService].toInstance(mocGracePeriodService)
    )
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockApplicationConfig.reportAProblemNonJSUrl).thenReturn("/reportAProblem")
    when(mockApplicationConfig.contactFormServiceIdentifier).thenReturn("/id")
  }

  val termsConditionController = inject[TermsConditionsController]

  "GET /" should {

    "return 200" in {
      val result = termsConditionController.show(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return HTML" in {
      val result = termsConditionController.show(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result)     shouldBe Some("utf-8")
    }

    "load the T&Cs page without back link" in {
      val result = contentAsString(termsConditionController.show(fakeRequest))
      result should include(
        "The information given is based on details from your National Insurance record at the time you use the service. While we will make every effort to keep your record up to date, we do not guarantee that it will be or that it is error and omission free."
      )

      Jsoup.parse(result).getElementById("back") shouldBe null
    }

    "load the T&Cs page with back link" in {
      val fakeRequest = FakeRequest("GET", "/?showBackLink=true")
      val result      = contentAsString(termsConditionController.show(fakeRequest))
      result should include(
        "The information given is based on details from your National Insurance record at the time you use the service. While we will make every effort to keep your record up to date, we do not guarantee that it will be or that it is error and omission free."
      )

      val backLink: Element = Jsoup.parse(result).getElementById("back")

      backLink.attr("data-spec") shouldBe "terms_and_conditions__backlink"
      backLink.attr("href") shouldBe "/check-your-state-pension/account"
      backLink.attr("class") shouldBe "govuk-link"
      backLink.text() shouldBe "Back"
    }

  }

  "GET / with showBackLink query parameter" should {

    "return 200 with flag value" in {
      val result = termsConditionController.show(fakeRequest)
      status(result)        shouldBe Status.OK
      contentAsString(result) should include(
        "The information given is based on details from your National Insurance record at the time you use the service. While we will make every effort to keep your record up to date, we do not guarantee that it will be or that it is error and omission free."
      )

      Jsoup.parse(contentAsString(result)).getElementById("back") shouldBe null
    }

    "load the T&Cs page with back link" in {
      val fakeRequest = FakeRequest("GET", "/?showBackLink=true")
      val result      = contentAsString(termsConditionController.show(fakeRequest))
      result should include(
        "The information given is based on details from your National Insurance record at the time you use the service. While we will make every effort to keep your record up to date, we do not guarantee that it will be or that it is error and omission free."
      )

      val backLink: Element = Jsoup.parse(result).getElementById("back")

      backLink.attr("data-spec") shouldBe "terms_and_conditions__backlink"
      backLink.attr("href") shouldBe "/check-your-state-pension/account"
      backLink.attr("class") shouldBe "govuk-link"
      backLink.text() shouldBe "Back"
    }

  }
}
