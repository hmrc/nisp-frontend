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

import org.joda.time.{DateTime, LocalDate}
import org.scalatest.mockito.MockitoSugar
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.contentAsString
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.LoginTimes
import uk.gov.hmrc.nisp.controllers.auth.{AuthDetails, ExcludedAuthenticatedRequest}
import uk.gov.hmrc.nisp.helpers.{FakeCachedStaticHtmlPartialRetriever, FakePartialRetriever, FakeTemplateRenderer, TestAccountBuilder}
import uk.gov.hmrc.nisp.models.Exclusion
import uk.gov.hmrc.nisp.views.html.excluded_cope_failed
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer

class ExclusionCopeFailedViewSpec extends HtmlSpec with MockitoSugar with Injecting {

  implicit val cachedStaticHtmlPartialRetriever = FakeCachedStaticHtmlPartialRetriever
  implicit val formPartialRetriever: FormPartialRetriever = FakePartialRetriever
  implicit val templateRenderer: TemplateRenderer = FakeTemplateRenderer
  implicit val fakeRequest = ExcludedAuthenticatedRequest(FakeRequest(), TestAccountBuilder.regularNino,
    AuthDetails(ConfidenceLevel.L200, Some("GovernmentGateway"), LoginTimes(DateTime.now(), None)))

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure("future-pension-link.url" -> "pensionUrl")
    .build()

  val excludedCopeFailedView = inject[excluded_cope_failed]
  val today = LocalDate.now()

  lazy val view = asDocument(contentAsString(excludedCopeFailedView()))

  "render correct h1" in {
    assertEqualsMessage(view, "h1", "nisp.excluded.cope.failed.h1")
  }

  "render correct p tag and text" in {
    assertEqualsMessage(view, "article > div > p", "nisp.excluded.cope.processing.failed")
  }

  "render correct a tag text and href" in {
    assertEqualsMessage(view, "article > div > a", "nisp.excluded.cope.processing.failed.link")
    assert(view.getElementById("futurePensionLink").attr("href") == "pensionUrl")
  }
}
