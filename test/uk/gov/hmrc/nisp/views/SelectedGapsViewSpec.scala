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

import org.scalatest.BeforeAndAfter
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.LoginTimes
import uk.gov.hmrc.nisp.controllers.auth.{AuthDetails, AuthenticatedRequest, NispAuthedUser}
import uk.gov.hmrc.nisp.controllers.routes
import uk.gov.hmrc.nisp.fixtures.NispAuthedUserFixture
import uk.gov.hmrc.nisp.helpers.TestAccountBuilder
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.nisp.views.html.nirecordSelectedGaps

import java.time.Instant

class SelectedGapsViewSpec extends HtmlSpec with BeforeAndAfter {

  implicit val user: NispAuthedUser = NispAuthedUserFixture.user(TestAccountBuilder.regularNino)
  private val authDetails = AuthDetails(ConfidenceLevel.L200, LoginTimes(Instant.now(), None))
  implicit val authenticatedRequest: AuthenticatedRequest[AnyContentAsEmpty.type] =
    AuthenticatedRequest(FakeRequest(), user, authDetails)

  private val view = inject[nirecordSelectedGaps]
  private lazy val doc = asDocument(view.apply().toString())

  "Selected gaps view" should {

    "render the selected option and total cost" in {
      doc.select("h1").text() shouldBe messages("nisp.nirecord.selectedGaps.heading", 2)
      doc.select(".govuk-summary-card__title").text() shouldBe messages("nisp.nirecord.selectedGaps.option", 2, 8)
      doc.select(".govuk-table").first().text() should include("2015 to 2016 £105 Total cost £105")
    }

    "link back to gap options and forward to declaration" in {
      doc.select("a.govuk-link").attr("href") shouldBe routes.MultiClassNIRecordController.showGaps.url
      doc.select("a.govuk-button").attr("href") shouldBe "/declaration"
      doc.select("a.govuk-button").text() shouldBe messages("nisp.nirecord.selectedGaps.makePayment")
    }

    "render the State Pension comparison and payment requirements" in {
      doc.text() should include(messages("nisp.nirecord.selectedGaps.pensionImpact"))
      doc.text() should include("£158.19 per week")
      doc.text() should include(messages("nisp.nirecord.selectedGaps.paymentMessage1"))
      doc.text() should include(messages("nisp.nirecord.selectedGaps.paymentMessage2"))
    }

    "use the selected gaps page title" in {
      doc.select("head > title").text() shouldBe (
        messages("nisp.nirecord.selectedGaps.title") + Constants.titleSplitter +
          messages("nisp.title.extension") + Constants.titleSplitter + messages("nisp.gov-uk")
      )
    }
  }
}