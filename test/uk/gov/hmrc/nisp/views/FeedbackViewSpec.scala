/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.test.Helpers.{contentAsString, _}
import play.twirl.api.Html
import uk.gov.hmrc.nisp.config.wiring.NispFormPartialRetriever
import uk.gov.hmrc.nisp.utils.MockTemplateRenderer
import uk.gov.hmrc.nisp.views.html.feedback
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}
import uk.gov.hmrc.renderer.TemplateRenderer

class FeedbackViewSpec extends HtmlSpec with MockitoSugar {

  implicit val cachedStaticHtmlPartialRetriever  = mock[CachedStaticHtmlPartialRetriever]
  implicit val formPartialRetriever: FormPartialRetriever = mock[FormPartialRetriever]
  implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer

  val partialUrl = "partialUrl"

  "Feedback page" should {
    "assert correct feedback title page" in {
      val html = feedback(partialUrl, Some(Html("sdfgh")))
      val source = asDocument(contentAsString(html))
      val row = source.getElementsByTag("script").get(0).toString
      val expected = messagesApi("nisp.feedback.title")
      row must include(s"document.title = \042$expected\042")
    }

    "assert passed in formBody is displayed" in {
      val testHtml = "<p> test html </p>"
      val html = feedback(partialUrl, Some(Html(testHtml)))
      contentAsString(html) must include (testHtml)
      verify(formPartialRetriever, times(0)).getPartialContent(
        ArgumentMatchers.eq(partialUrl), any(), any())(any())
    }

    "assert correct html displayed from partialURL call when formBody is not provided" in {
      reset(formPartialRetriever)
      val expected = "<p> Mock partial content </p>"
      when(formPartialRetriever.getPartialContent(any(), any(), any())(any()))
        .thenReturn(Html(expected))
      val html = feedback(partialUrl, None)
      val content = contentAsString(html)
      content must include(expected)
      verify(formPartialRetriever, times(1)).getPartialContent(
        url = ArgumentMatchers.eq(partialUrl), any(), any())(any())
    }
  }
}