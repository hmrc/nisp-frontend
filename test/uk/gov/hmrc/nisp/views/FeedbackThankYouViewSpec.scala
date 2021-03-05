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
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.nisp.helpers.{FakePartialRetriever, FakeTemplateRenderer}
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}
import uk.gov.hmrc.renderer.TemplateRenderer

class FeedbackThankYouViewSpec extends HtmlSpec with MockitoSugar {

  implicit val cachedStaticHtmlPartialRetriever = mock[CachedStaticHtmlPartialRetriever]
  implicit val formPartialRetriever: FormPartialRetriever = FakePartialRetriever
  implicit val templateRenderer: TemplateRenderer = FakeTemplateRenderer

  val partialUrl = "partialUrl"
  def html = uk.gov.hmrc.nisp.views.html.feedback_thankyou(partialUrl, "/check-your-state-pension/account")
  def source = asDocument(contentAsString(html))

  "FeedbackThankYou" should {

    "assert correct page title" in {
      val title = contentAsString(html)
      title must include(messages("nisp.feedback.title"))
    }

    "assert correct html displayed from partialURL call" in {
      reset(cachedStaticHtmlPartialRetriever)
      when(cachedStaticHtmlPartialRetriever.getPartialContent(ArgumentMatchers.anyString(), any(), any())(any()))
        .thenReturn(Html("<p> Mock partial content </p>"))

      val content = contentAsString(html)
      content must include("<p> Mock partial content </p>")
      verify(cachedStaticHtmlPartialRetriever, times(1)).getPartialContent(
        url = ArgumentMatchers.eq(partialUrl),
        any(), any())(any())
    }

    "assert correct href on the start again button" in {
      when(cachedStaticHtmlPartialRetriever.getPartialContent(ArgumentMatchers.anyString(), any(), any())(any()))
        .thenReturn(Html("<p> Mock partial content </p>"))
      val redirect = source.getElementById("Start").attr("href")

      redirect must include("/check-your-state-pension/account")
    }
  }
}
