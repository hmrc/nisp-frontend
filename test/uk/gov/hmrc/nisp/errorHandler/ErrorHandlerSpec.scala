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

package uk.gov.hmrc.nisp.errorHandler

import java.util.Locale

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Request
import play.api.test.{FakeRequest, Injecting}
import play.twirl.api.Html
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.helpers.{FakeCachedStaticHtmlPartialRetriever, FakePartialRetriever, FakeTemplateRenderer}
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}
import uk.gov.hmrc.nisp.utils.UnitSpec
import uk.gov.hmrc.renderer.TemplateRenderer

class ErrorHandlerSpec extends UnitSpec with MockitoSugar with GuiceOneAppPerSuite with Injecting{

  implicit val cachedRetriever: CachedStaticHtmlPartialRetriever = FakeCachedStaticHtmlPartialRetriever
  implicit val templateRenderer: TemplateRenderer = FakeTemplateRenderer
  implicit val request: Request[_] = FakeRequest()

  val mockApplicationConfig = mock[ApplicationConfig]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[TemplateRenderer].toInstance(templateRenderer),
      bind[FormPartialRetriever].to[FakePartialRetriever],
      bind[CachedStaticHtmlPartialRetriever].toInstance(cachedRetriever),
      bind[ApplicationConfig].toInstance(mockApplicationConfig)
    ).build()

  val errorHandler = inject[ErrorHandler]
  implicit val messages: MessagesImpl = MessagesImpl(Lang(Locale.getDefault), inject[MessagesApi])

  "standardErrorTemplate" must {
    "return the global error view" in {
      val title = "testTitle"
      val heading = "testHeading"
      val message = "testMessage"

      val standardErrorTemplate: Html = errorHandler.standardErrorTemplate(title, heading, message)
      val doc: Document = Jsoup.parse(standardErrorTemplate.toString())

      val docTitle = doc.select("title").text()
      val docHeading = doc.select("article > h1").text()
      val docMessage = doc.select("article > p").text()

      docTitle shouldBe title
      docHeading shouldBe heading
      docMessage shouldBe message
    }
  }

  "internalServerErrorTemplate" must {
    "return the service error 500 view" in {
      val serverErrorTemplate: Html = errorHandler.internalServerErrorTemplate
      val doc: Document = Jsoup.parse(serverErrorTemplate.toString())

      val docTitle = doc.select("title").text()
      docTitle should include(messages("global.error.InternalServerError500.title"))
    }
  }

  "notFoundTemplate" must {
    "return the page not found view" in {
      val notFoundTemplate: Html = errorHandler.notFoundTemplate
      val doc: Document = Jsoup.parse(notFoundTemplate.toString())

      val docTitle = doc.select("title").text()
      docTitle should include(messages("global.page.not.found.error.title"))
    }
  }
}