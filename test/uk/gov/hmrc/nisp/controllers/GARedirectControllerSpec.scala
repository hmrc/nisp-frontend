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
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.nisp.helpers.{FakeCachedStaticHtmlPartialRetriever, FakePartialRetriever, FakeTemplateRenderer}
import uk.gov.hmrc.nisp.utils.UnitSpec
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}
import uk.gov.hmrc.renderer.TemplateRenderer

class GARedirectControllerSpec extends UnitSpec with GuiceOneAppPerSuite with Injecting{

  private implicit val fakeRequest = FakeRequest("GET", "/redirect")

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[TemplateRenderer].toInstance(FakeTemplateRenderer),
      bind[FormPartialRetriever].to[FakePartialRetriever],
      bind[CachedStaticHtmlPartialRetriever].toInstance(FakeCachedStaticHtmlPartialRetriever)
    ).build()

  val testGARedirectController = inject[GARedirectController]

  "GET /redirect" should {
    "return 200" in {
      val result = testGARedirectController.show(fakeRequest)
      status(result) shouldBe Status.OK
    }
  }
}
