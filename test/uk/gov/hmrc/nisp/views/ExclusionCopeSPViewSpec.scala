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

package uk.gov.hmrc.nisp.views

import org.jsoup.nodes.Document
import org.mockito.Mockito.when
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers.contentAsString
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.LoginTimes
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.auth.{AuthDetails, ExcludedAuthenticatedRequest}
import uk.gov.hmrc.nisp.helpers.TestAccountBuilder
import uk.gov.hmrc.nisp.views.html.excluded_cope_sp

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate}

class ExclusionCopeSPViewSpec extends HtmlSpec with Injecting {

  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]

  implicit val fakeRequest: ExcludedAuthenticatedRequest[AnyContentAsEmpty.type] = ExcludedAuthenticatedRequest(
    FakeRequest(),
    TestAccountBuilder.regularNino,
    AuthDetails(ConfidenceLevel.L200, LoginTimes(Instant.now(), None))
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockAppConfig.reportAProblemNonJSUrl).thenReturn("/reportAProblem")
    when(mockAppConfig.contactFormServiceIdentifier).thenReturn("/id")
  }

  val excludedCopeView: excluded_cope_sp = inject[excluded_cope_sp]
  val today: LocalDate = LocalDate.now()

  lazy val view: Document = asDocument(contentAsString(excludedCopeView(today)))

  "render correct h1" in {
    assertEqualsMessage(
      view,
      "[data-spec='excluded_cope__h1']",
      "nisp.excluded.cope.processing.h1"
    )
  }

  "render correct indent-panel div with text" in {
    assert(view.getElementsByClass("govuk-inset-text").size() == 1)
    assertEqualsMessage(
      view,
      "[data-spec='excluded_cope__inset_text']",
      "nisp.excluded.cope.processing"
    )
  }

  "render correct p tag with text" in {
    assertContainsDynamicMessage(
      view,
      "[data-spec='excluded_cope__p2']",
      "nisp.excluded.cope.returnDate",
      today.format(DateTimeFormatter.ofPattern("d MMMM y")).replace(" ", "&nbsp;")
    )
  }
}
