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
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.nisp.controllers.auth.NispAuthedUser
import uk.gov.hmrc.nisp.fixtures.NispAuthedUserFixture
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.nisp.views.html.landing
import uk.gov.hmrc.renderer.TemplateRenderer

class LandingViewSpec extends HtmlSpec {

  val fakeRequest = FakeRequest("GET", "/")
  implicit val cachedStaticHtmlPartialRetriever = FakeCachedStaticHtmlPartialRetriever
  implicit val templateRenderer: TemplateRenderer = FakeTemplateRenderer
  implicit val user: NispAuthedUser = NispAuthedUserFixture.user(TestAccountBuilder.regularNino)

  val feedbackFrontendUrl: String = "/foo"

  "return correct page title on landing page" in {
    val html = inject[landing]
    val document = asDocument(html().toString)
    val title = document.title()
    val expected = messages("nisp.landing.title") + Constants.titleSplitter +
      messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk")
    title must include(expected)
  }

  "return correct title on the landing page" in {
    val html = inject[landing]
    val document = asDocument(html.apply().toString)
    val title = document.title()
    title must include(messages("nisp.landing.title"))
  }
}
