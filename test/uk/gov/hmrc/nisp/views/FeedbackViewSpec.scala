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

package uk.gov.hmrc.nisp.views

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.mockito.MockitoSugar
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import play.api.test.Helpers._
import play.api.test.Injecting
import play.twirl.api.Html
import uk.gov.hmrc.nisp.helpers.{FakeCachedStaticHtmlPartialRetriever, FakePartialRetriever, FakeTemplateRenderer}
import uk.gov.hmrc.nisp.views.html.feedback
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}
import uk.gov.hmrc.renderer.TemplateRenderer

class FeedbackViewSpec extends HtmlSpec with MockitoSugar with Injecting {

  val mockFormPartialRetriever: FormPartialRetriever = mock[FormPartialRetriever]
  val partialUrl: String = "partialUrl"

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[FormPartialRetriever].toInstance(mockFormPartialRetriever),
      bind[CachedStaticHtmlPartialRetriever].toInstance(FakeCachedStaticHtmlPartialRetriever),
      bind[TemplateRenderer].to(FakeTemplateRenderer)
    ).build()

  "Feedback page" should {
    "assert correct feedback title page" in {
      val html = inject[feedback]
      val source = asDocument(html(partialUrl, Some(Html("sdfgh")))(messages, request).toString)
      val row = source.getElementsByTag("script").get(0).toString
      val expected = messages("nisp.feedback.title")
      row must include(s"""document.title = "$expected"""")
    }

    "assert passed in formBody is displayed" in {
      val testHtml = "<p> test html </p>"
      val html = inject[feedback]
      html(partialUrl, Some(Html(testHtml)))(messages, request).toString must include (testHtml)
      verify(mockFormPartialRetriever, times(0)).getPartialContent(
        ArgumentMatchers.eq(partialUrl), any(), any())(any(),any())
    }

    "assert correct html displayed from partialURL call when formBody is not provided" in {
      reset(mockFormPartialRetriever)
      val expected = "<p> Mock partial content </p>"
      when(mockFormPartialRetriever.getPartialContent(any(), any(), any())(any(), any()))
        .thenReturn(Html(expected))

      val html = inject[feedback]
      html(partialUrl, None)(messages, request).toString must include(expected)
      verify(mockFormPartialRetriever, times(1)).getPartialContent(
        url = ArgumentMatchers.eq(partialUrl), any(), any())(any(), any())
    }
  }
}
