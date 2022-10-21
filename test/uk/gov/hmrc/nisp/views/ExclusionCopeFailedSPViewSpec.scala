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

import org.jsoup.nodes.Document
import org.mockito.Mockito.when
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.contentAsString
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.LoginTimes
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.auth.{AuthDetails, ExcludedAuthenticatedRequest}
import uk.gov.hmrc.nisp.helpers.TestAccountBuilder
import uk.gov.hmrc.nisp.views.html.excluded_cope_failed_sp

import java.time.{Instant, LocalDate}

class ExclusionCopeFailedSPViewSpec extends HtmlSpec with Injecting {

  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  val urResearchURL                    =
    "https://signup.take-part-in-research.service.gov.uk/?utm_campaign=checkyourstatepensionPTA&utm_source=Other&utm_medium=other&t=HMRC&id=183"

  implicit val fakeRequest = ExcludedAuthenticatedRequest(
    FakeRequest(),
    TestAccountBuilder.regularNino,
    AuthDetails(ConfidenceLevel.L200, LoginTimes(Instant.now(), None))
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockAppConfig.urBannerUrl).thenReturn(urResearchURL)
    when(mockAppConfig.pertaxFrontendUrl).thenReturn("/pert")
    when(mockAppConfig.reportAProblemNonJSUrl).thenReturn("/reportAProblem")
    when(mockAppConfig.contactFormServiceIdentifier).thenReturn("/id")
  }

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure("future-pension-link.url" -> "pensionUrl")
    .overrides(
      bind[ApplicationConfig].toInstance(mockAppConfig)
    )
    .build()

  val excludedCopeFailedView = inject[excluded_cope_failed_sp]
  val today: LocalDate       = LocalDate.now()

  lazy val view: Document = asDocument(contentAsString(excludedCopeFailedView()))

  "render correct h1" in {
    assertEqualsMessage(
      view,
      "[data-spec='excluded_cope_failed__h1']",
      "nisp.excluded.cope.failed.h1"
    )
  }

  "render correct h2" in {
    assertEqualsMessage(
      view,
      "[data-spec='excluded_cope_failed__h2_1']",
      "nisp.excluded.cope.failed.h2"
    )
  }

  "render correct Telephone h3" in {
    assertElementContainsText(
      view,
      "[data-spec='excluded_cope_failed__h3_1']",
      "Telephone"
    )
  }

  "render correct Textphone H3" in {
    assertElementContainsText(
      view,
      "[data-spec='excluded_cope_failed__h3_2']",
      "Textphone"
    )
  }

  "render correct Relay UK H3" in {
    assertElementContainsText(
      view,
      "[data-spec='excluded_cope_failed__h3_3']",
      "Relay UK"
    )
  }

  "render correct British Sign Language H3" in {
    assertElementContainsText(
      view,
      "[data-spec='excluded_cope_failed__h3_4']",
      "British Sign Language"
    )
  }

  "render correct Opening Times H3" in {
    assertElementContainsText(
      view,
      "[data-spec='excluded_cope_failed__h3_5']",
      "Opening Times"
    )
  }
}
