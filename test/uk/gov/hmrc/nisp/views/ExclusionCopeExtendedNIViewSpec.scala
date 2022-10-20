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

import java.time.{Instant, LocalDate}
import org.jsoup.nodes.Document
import play.api.test.Helpers.contentAsString
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.LoginTimes
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.auth.{AuthDetails, ExcludedAuthenticatedRequest}
import uk.gov.hmrc.nisp.helpers.TestAccountBuilder
import uk.gov.hmrc.nisp.views.html.excluded_cope_extended_ni

import java.time.format.DateTimeFormatter

class ExclusionCopeExtendedNIViewSpec extends HtmlSpec with Injecting {

  implicit val fakeRequest = ExcludedAuthenticatedRequest(
    FakeRequest(),
    TestAccountBuilder.regularNino,
    AuthDetails(ConfidenceLevel.L200, LoginTimes(Instant.now(), None))
  )

  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  val today = LocalDate.now()

  val excludedCopeExtendedNIView = inject[excluded_cope_extended_ni]
  lazy val view: Document = asDocument(contentAsString(excludedCopeExtendedNIView(today)))

  "render correct h1" in {
    assertEqualsMessage(
      view,
      "[data-spec='excluded_cope_extended__h1']",
      "nisp.excluded.cope.extended.ni.h1"
    )
  }

  "render the backlog paragraph" in {
    assertEqualsMessage(
      view,
      "[data-spec='excluded_cope_extended__p1']",
      "nisp.excluded.cope.extended.backlog"
    )
  }

  "render the national insurance will be available paragraph" in {
    assertContainsDynamicMessage(
      view,
      "[data-spec='excluded_cope__p2']",
      "nisp.excluded.cope.returnDate.ni",
      today.format(DateTimeFormatter.ofPattern("d MMMM y")).replace(" ", "&nbsp;")
    )
  }
}
