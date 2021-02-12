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

import org.scalatest.mockito.MockitoSugar
import play.api.test.Helpers._
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer

class TimeoutViewSpec extends HtmlSpec with MockitoSugar {

  implicit val cachedStaticHtmlPartialRetriever = FakeCachedStaticHtmlPartialRetriever
  implicit val formPartialRetriever: FormPartialRetriever = FakePartialRetriever
  implicit val templateRenderer: TemplateRenderer = FakeTemplateRenderer

  val feedbackFrontendUrl: String = "/foo"
  lazy val html = uk.gov.hmrc.nisp.views.html.iv.failurepages.timeout()
  lazy val source = asDocument(contentAsString(html))

  "TimeoutView" should {

    "assert correct page title" in {
      val title = source.title()
      val expected = "Some(" + messages("nisp.iv.failure.timeout.title") + Constants.titleSplitter +
        messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk") + ")"
      title must include(expected)
    }

    "assert correct heading title on page" in {
      val title = source.getElementsByTag("h1").get(0).toString
      val messageKey = "nisp.iv.failure.timeout.title"
      val expected = messages(messageKey)
      title must include(expected)
    }

    "assert correct paragraph one text on page" in {
      val title = source.getElementsByTag("p").get(1).toString
      val messageKey = "nisp.iv.failure.timeout.message"
      val expected = messages(messageKey)
      title must include(expected)
    }

    "assert correct paragraph two text on page" in {
      val title = source.getElementsByTag("p").get(2).toString
      val messageKey = "nisp.iv.failure.timeout.data"
      val expected = messages(messageKey)
      title must include(expected)
    }

    "assert correct button text on page" in {
      val title = source.getElementsByTag("a").get(1).toString
      val messageKey = "nisp.iv.failure.timeout.button"
      val expected = messages(messageKey)
      title must include(expected)
    }

    "assert correct href on the start again button" in {
      val redirect = source.getElementsByTag("a").get(1).attr("href")
      val expected = "/check-your-state-pension/account"
      redirect must include(expected)
    }
  }
}
