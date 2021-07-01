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

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import org.joda.time.DateTime
import org.jsoup.nodes.Document
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.test.{FakeRequest, Helpers, Injecting}
import play.api.test.Helpers.{contentAsString, stubMessages}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.LoginTimes
import uk.gov.hmrc.nisp.controllers.auth.{AuthDetails, ExcludedAuthenticatedRequest}
import uk.gov.hmrc.nisp.helpers.{FakeCachedStaticHtmlPartialRetriever, FakePartialRetriever, FakeTemplateRenderer, TestAccountBuilder}
import uk.gov.hmrc.nisp.models.Exclusion
import uk.gov.hmrc.nisp.views.html.excluded_cope
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer

class ExclusionCopeViewSpec extends HtmlSpec with MockitoSugar with Injecting {

  implicit val cachedStaticHtmlPartialRetriever = FakeCachedStaticHtmlPartialRetriever
  implicit val formPartialRetriever: FormPartialRetriever = FakePartialRetriever
  implicit val templateRenderer: TemplateRenderer = FakeTemplateRenderer
  implicit val fakeRequest = ExcludedAuthenticatedRequest(FakeRequest(), TestAccountBuilder.regularNino,
    AuthDetails(ConfidenceLevel.L200, Some("GovernmentGateway"), LoginTimes(DateTime.now(), None)))

  val excludedCopeView = inject[excluded_cope]
  val today: LocalDate = LocalDate.now()

  lazy val view: Document = asDocument(contentAsString(excludedCopeView(today)))

  "render correct h1" in {
    assertEqualsMessage(view, "h1", "nisp.excluded.cope.processing.h1")
  }

  "render correct indent-panel div with text" in {
    assert(view.getElementsByClass("panel-indent").size() == 1)
    assertEqualsMessage(view, ".panel-indent", "nisp.excluded.cope.processing")
  }

  "render correct p tag with text" in {
    assertContainsDynamicMessage(view, "article p:last-of-type",
      "nisp.excluded.cope.returnDate",
      today.format(DateTimeFormatter.ofPattern("d MMMM y")))
  }
}
