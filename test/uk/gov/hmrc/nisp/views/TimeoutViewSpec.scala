/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.test.Injecting
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.renderer.TemplateRenderer

class TimeoutViewSpec extends HtmlSpec with Injecting {

  implicit val cachedStaticHtmlPartialRetriever = FakeCachedStaticHtmlPartialRetriever
  implicit val templateRenderer: TemplateRenderer = FakeTemplateRenderer

  val feedbackFrontendUrl: String = "/foo"
  lazy val html = inject[uk.gov.hmrc.nisp.views.html.iv.failurepages.timeout]
  lazy val source = asDocument(html().toString)

  "TimeoutView" should {

    "assert correct page title" in {
      val title = source.title()
      val expected = "Some(" + messages("nisp.iv.failure.timeout.title") + Constants.titleSplitter +
        messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk") + ")"
      title should include(expected)
    }

    "assert correct heading title on page" in {
      val heading = source.getElementsByTag("h1").get(0).toString
      heading should include(messages("nisp.iv.failure.timeout.title"))
    }

    "assert correct paragraph one text on page" in {
      val paragraph = source.getElementsByTag("p").get(1).toString
      paragraph should include(messages("nisp.iv.failure.timeout.message"))
    }

    "assert correct paragraph two text on page" in {
      val paragraph = source.getElementsByTag("p").get(2).toString
      paragraph should include(messages("nisp.iv.failure.timeout.data"))
    }

    "assert correct button text on page" in {
      val button = source.getElementsByClass("button").text
      button should include(messages("nisp.iv.failure.timeout.button"))
    }

    "assert correct href on the start again button" in {
      val buttonHref = source.getElementsByClass("button").attr("href")
      buttonHref should include("/check-your-state-pension/account")
    }
  }
}
