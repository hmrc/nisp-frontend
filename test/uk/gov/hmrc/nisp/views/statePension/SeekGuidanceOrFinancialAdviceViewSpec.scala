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

package uk.gov.hmrc.nisp.views.statePension

import org.jsoup.nodes.Document
import play.api.test.Injecting
import uk.gov.hmrc.nisp.controllers.auth.NispAuthedUser
import uk.gov.hmrc.nisp.fixtures.NispAuthedUserFixture
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.nisp.views.HtmlSpec
import uk.gov.hmrc.nisp.views.html.statePension.SeekGuidanceOrFinancialAdvice



class SeekGuidanceOrFinancialAdviceViewSpec extends HtmlSpec with Injecting {

  implicit val user: NispAuthedUser = NispAuthedUserFixture.user(TestAccountBuilder.regularNino)

  lazy val view: SeekGuidanceOrFinancialAdvice = inject[uk.gov.hmrc.nisp.views.html.statePension.SeekGuidanceOrFinancialAdvice]
  lazy val doc: Document = asDocument(view().toString)

  "Seek guidance or financial advice" should {

    "render with correct page title" in {
      assertElementContainsText(
        doc,
        "head > title",
        messages("nisp.seekGuidanceOrFinancialAdvice.title") + Constants.titleSplitter + messages(
          "nisp.title.extension"
        ) + Constants.titleSplitter + messages("nisp.gov-uk")
      )
    }

    "render page with heading 'Seek guidance or financial advice'" in {
      assertEqualsText(
        doc,
        "[data-spec='seek_financial_advice__h1']",
        "Seek guidance or financial advice"
      )
    }

    "render page with the paying voluntary contributions paragraph" in {
      assertEqualsText(
        doc,
        "[data-spec='seek_financial_advice__p1']",
        "Paying voluntary contributions may not be your best option when planning for your retirement."
      )
    }

    "render page with the free impartial guidance paragraph" in {
      assertEqualsText(
        doc,
        "[data-spec='seek_financial_advice__p2']",
        "For free impartial guidance (not on GOV.UK):"
      )
    }

    "render page with bullet - citizens advice link " in {
      assertEqualsText(
        doc,
        "[data-spec='seek_financial_advice__list__item1']",
        "Citizens Advice"
      )

      assertLinkHasValue(
        doc,
        "[data-spec='seek_financial_advice__citizen_advice__link']",
        "https://www.citizensadvice.org.uk/"
      )
    }

    "render page with bullet - moneyHelper service link" in {
      assertEqualsText(
        doc,
        "[data-spec='seek_financial_advice__list__item2']",
        "MoneyHelper Service"
      )

      assertLinkHasValue(
        doc,
        "[data-spec='seek_financial_advice__moneyHelper_service__link']",
        "https://www.moneyadviceservice.org.uk/en"
      )
    }

    "render page with bullet - pensionWise link" in {
      assertEqualsText(
        doc,
        "[data-spec='seek_financial_advice__list__item3']",
        "Pension Wise — advice on private pensions for over 50s"
      )

      assertLinkHasValue(
        doc,
        "[data-spec='seek_financial_advice__pension_wise__link']",
        "https://www.pensionwise.gov.uk/"
      )
    }
  }
}
