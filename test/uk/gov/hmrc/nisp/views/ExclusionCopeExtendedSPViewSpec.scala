/*
 * Copyright 2026 HM Revenue & Customs
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
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers.contentAsString
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.LoginTimes
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.auth.{AuthDetails, ExcludedAuthenticatedRequest}
import uk.gov.hmrc.nisp.helpers.TestAccountBuilder
import uk.gov.hmrc.nisp.views.html.excluded_cope_extended_sp

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate}

class ExclusionCopeExtendedSPViewSpec extends HtmlSpec with Injecting {

  implicit val fakeRequest: ExcludedAuthenticatedRequest[AnyContentAsEmpty.type] = ExcludedAuthenticatedRequest(
    FakeRequest(),
    TestAccountBuilder.regularNino,
    AuthDetails(ConfidenceLevel.L200, LoginTimes(Instant.now(), None))
  )

  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  val today: LocalDate = LocalDate.now()

  val excludedCopeExtendedSPView: excluded_cope_extended_sp = inject[excluded_cope_extended_sp]
  lazy val view: Document = asDocument(contentAsString(excludedCopeExtendedSPView(today)))

  "render correct h1" in {
    assertEqualsText(
      view,
      "[data-spec='excluded_cope_extended__h1']",
      "Sorry, we are still working on updates to your forecast"
    )
  }

  "render specialist team paragraph" in {
    assertEqualsText(
      view,
      "[data-spec='excluded_cope_extended__inset_text']",
      "Our Specialist Team is checking your National Insurance record to ensure your State Pension forecast is accurate and up to date."
    )
  }

  "render the backlog paragraph" in {
    assertEqualsText(
      view,
      "[data-spec='excluded_cope__p1']",
      "Unfortunately, there is a backlog of work to manually process and it is taking longer to complete."
    )
  }

  "render the state pension forecast will be available paragraph" in {
    assertEqualsText(
      view,
      "[data-spec='excluded_cope__p2']",
      s"Your State Pension forecast will be available on ${today.format(DateTimeFormatter.ofPattern("d MMMM y"))}."
    )
  }
}
