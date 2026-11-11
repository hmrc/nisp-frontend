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

import org.mockito.Mockito.when
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.LoginTimes
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.auth.{AuthDetails, ExcludedAuthenticatedRequest, NispAuthedUser}
import uk.gov.hmrc.nisp.fixtures.NispAuthedUserFixture
import uk.gov.hmrc.nisp.helpers.TestAccountBuilder
import uk.gov.hmrc.nisp.models.Exclusion
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.nisp.views.html.{excluded_dead, excluded_mci, excluded_sp}
import uk.gov.hmrc.play.language.LanguageUtils

import java.time.{Instant, LocalDate}

class ExclusionViewSpec extends HtmlSpec with Injecting {

  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]

  implicit val fakeRequest: ExcludedAuthenticatedRequest[AnyContentAsEmpty.type] = ExcludedAuthenticatedRequest(
    FakeRequest(),
    TestAccountBuilder.regularNino,
    AuthDetails(ConfidenceLevel.L200, LoginTimes(Instant.now(), None))
  )
  implicit val user: NispAuthedUser = NispAuthedUserFixture.user(TestAccountBuilder.regularNino)
  lazy val langUtils: LanguageUtils = inject[LanguageUtils]

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockAppConfig.reportAProblemNonJSUrl).thenReturn("/reportAProblem")
    when(mockAppConfig.contactFormServiceIdentifier).thenReturn("/id")
  }

  val statePensionSummary = "Your State Pension summary"
  val whenWillReachSPA    = "When will I reach State Pension age?"
  val reachedSPASep2015   = "You reached State Pension age on 6 September 2015"
  val willReachSPASep2015 = "You will reach State Pension age on 6 September 2015"
  val willReachSPASep2019 = "You will reach State Pension age on 6 September 2019"
  val canClaimOnline      = "You can claim your State Pension online if you have not already claimed it."
  val canClaimOnlineLink  = "https://www.gov.uk/claim-state-pension-online"
  val cantFillGapsOnline  = "This means you are unable to pay for gaps in your National Insurance record online."
  val ExcludedIOMText  = "We’re unable to calculate your State Pension forecast. You have paid National Insurance in the Isle of Man."
  val isleOfManPensionText  = "You can find out more about your Isle of Man Pension from the Isle of Man Pension Centre."
  val isleOfManPensionLink  = "https://www.gov.im/categories/benefits-and-financial-support/pensions/manx-state-pension/"
  val contactFuturePensionsText  = "Contact the Future Pension Centre to find out if you’ll benefit from paying voluntary contributions."
  val contactFuturePensionsLink  = "https://www.gov.uk/future-pension-centre"
  val reducedRateMarriedWomanText  = "We’re unable to calculate your State Pension forecast. You have paid a reduced rate of National Insurance as a married woman."
  val reducedRateMarriedWomanLink  = "https://www.gov.uk/reduced-national-insurance-married-women"
  val checkNIForGapsText  = "You can view your National Insurance record to check for gaps that you may be able to fill and make count towards your State Pension."
  val accountNIRButtonText  = "View your National Insurance record"
  val accountNIRUrl  = "/check-your-state-pension/account/nirecord"
  val proposedChangeToSPAText  = "Proposed change to your State Pension age"
  val deferSPText  = "You can delay claiming your State Pension. This means you may get extra State Pension when you do claim it. Find out about delaying your State Pension."
  val deferSPLinkText  = "https://www.gov.uk/deferring-state-pension"

  "Exclusion Dead" should {
    lazy val sResult        = inject[excluded_dead]
    lazy val htmlAccountDoc = asDocument(sResult(Exclusion.Dead, Some(65)).toString)

    "not render the UR banner" in {
      val urBanner = htmlAccountDoc.getElementById("full-width-banner")
      assert(urBanner == null)
    }

    "render with correct page title" in {
      assertElementContainsText(
        htmlAccountDoc,
        "title",
        messages("nisp.excluded.title")
          + Constants.titleSplitter
          + messages("nisp.title.extension")
          + Constants.titleSplitter
          + messages("nisp.gov-uk")
      )
    }

    "render page with heading  Your State Pension summary " in {
      val h1 = htmlAccountDoc.select("[data-spec='excluded_dead__h1']")

      h1 should not be empty
      h1.first().tagName() shouldBe "h1"
      h1.text() shouldBe statePensionSummary
    }

    "render page with message 'Contact HMRC to find out about your State Pension forecast.'" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='exclusions__dead__p1']",
        "Contact HMRC to find out about your State Pension forecast."
      )

      assertLinkHasValue(
        htmlAccountDoc,
        "[data-spec='exclusions__dead__link1']",
        "https://www.gov.uk/find-hmrc-contacts/income-tax-enquiries"
      )
    }
  }

  "Exclusion Isle of Man : Can't see NI Record: State Pension Age under consideration - no flag" should {
    lazy val sResult        = inject[excluded_sp]
    lazy val htmlAccountDoc =
      asDocument(sResult(Exclusion.IsleOfMan, Some(40), Some(LocalDate.of(2019, 9, 6)), false, None).toString)

    "render the UR banner" in {
      assert(htmlAccountDoc.getElementsByClass("full-width-banner__title") != null)
    }

    "render with correct page title" in {
      assertElementContainsText(
        htmlAccountDoc,
        "title",
        messages("nisp.main.h1.title") +
          Constants.titleSplitter +
          messages("nisp.title.extension") +
          Constants.titleSplitter +
          messages("nisp.gov-uk")
      )
    }

    "render page with heading 'Your State Pension summary'" in {
      val h1 = htmlAccountDoc.select("[data-spec='excluded_sp__h1']")

      h1 should not be empty
      h1.first().tagName() shouldBe "h1"
      h1.text() shouldBe statePensionSummary
    }

    "render page with h2 'When will I reach State Pension age?'" in {
      val h2 = htmlAccountDoc.select("[data-spec='excluded_sp__h2']")

      h2 should not be empty
      h2.first().tagName() shouldBe "h2"
      h2.text() shouldBe whenWillReachSPA
    }

    "render page with text 'You will reach State Pension age on 6 sep 2019' " in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='excluded_sp__p_1b']",
        willReachSPASep2019
      )
    }

    "render page with message unable to pay gaps information text" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='exclusion_isleOfMan_cant_pay_gaps_sp_p1']",
        cantFillGapsOnline
      )
    }

    "render page with message `We’re unable to calculate your State Pension...` with Isle of Man descriptor message" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='exclusion_isleOfMan_sp__p1']",
        ExcludedIOMText
      )
    }

    "render page with message 'You can find out more about your Isle of Man Pension from the Isle of Man Pension Centre.' " in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='exclusion_isleOfMan_sp__p2']",
        isleOfManPensionText
      )

      assertLinkHasValue(
        htmlAccountDoc,
        "[data-spec='exclusion_isleOfMan_sp__link1']",
        isleOfManPensionLink
      )
    }

    "render page with message 'Contact the Future Pension Centre to find out if you'll benefit from paying voluntary contributions.' " in {
      htmlAccountDoc
        .select("[data-spec='excluded_sp__p1']")
        .text shouldBe
        contactFuturePensionsText

      assertLinkHasValue(
        htmlAccountDoc,
        "[data-spec='excluded_sp__link1']",
        contactFuturePensionsLink
      )
    }

    // No state pension age under consideration message
    "not render page with heading Proposed change to your State Pension age text" in {
      htmlAccountDoc
        .select("state_pension_age_under_consideration__h2_1")
        .isEmpty shouldBe true
    }

    "not render page with text 'You will reach State Pension age on 6 Sep 2015. Under government proposals this may increase by up to a year.'" in {
      htmlAccountDoc
        .select("[data-spec='state_pension_age_under_consideration__p1']")
        .isEmpty shouldBe true
    }

    "not render View your National Insurance Record help text" in {
      htmlAccountDoc
        .select("""[data-spec="excluded_sp__p2"]""")
        .isEmpty shouldBe true
    }

    "not render View your National Insurance Record button" in {
      htmlAccountDoc
        .select("""[data-spec="excluded_sp__govukButton"]""")
        .isEmpty shouldBe true
    }
  }

  "Exclusion Isle of Man : Can't see NI Record: State Pension Age under consideration - false" should {
    lazy val sResult        = inject[excluded_sp]
    lazy val htmlAccountDoc =
      asDocument(sResult(Exclusion.IsleOfMan, Some(40), Some(LocalDate.of(2019, 9, 6)), false, Some(false)).toString)

    "render the UR banner" in {
      assert(htmlAccountDoc.getElementsByClass("full-width-banner__title") != null)
    }

    "render with correct page title" in {
      assertElementContainsText(
        htmlAccountDoc,
        "title",
        messages("nisp.main.h1.title") +
          Constants.titleSplitter +
          messages("nisp.title.extension") +
          Constants.titleSplitter +
          messages("nisp.gov-uk")
      )
    }

    "render page with heading 'Your State Pension summary'" in {
      val h1 = htmlAccountDoc.select("[data-spec='excluded_sp__h1']")

      h1 should not be empty
      h1.first().tagName() shouldBe "h1"
      h1.text() shouldBe statePensionSummary
    }

    "render page with h2 'When will I reach State Pension age?'" in {
      val h2 = htmlAccountDoc.select("[data-spec='excluded_sp__h2']")

      h2 should not be empty
      h2.first().tagName() shouldBe "h2"
      h2.text() shouldBe whenWillReachSPA
    }

    "render page with text 'You will reach State Pension age on 6 sep 2019' " in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='excluded_sp__p_1b']",
        willReachSPASep2019
      )
    }

    "render page with message 'We’re unable to calculate your State Pension...' with Isle of Man descriptor text " in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='exclusion_isleOfMan_sp__p1']",
        ExcludedIOMText
      )
    }

    "render page with message unable to pay gaps information text" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='exclusion_isleOfMan_cant_pay_gaps_sp_p1']",
        cantFillGapsOnline
      )
    }

    "render page with message 'You can find out more about your Isle of Man Pension from the Isle of Man Pension Centre.' " in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='exclusion_isleOfMan_sp__p2']",
        isleOfManPensionText
      )

      assertLinkHasValue(
        htmlAccountDoc,
        "[data-spec='exclusion_isleOfMan_sp__link1']",
        isleOfManPensionLink
      )
    }

    "render page with message 'Contact the Future Pension Centre to find out if you'll benefit from paying voluntary contributions.' " in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='excluded_sp__p1']",
        contactFuturePensionsText
      )

      assertLinkHasValue(
        htmlAccountDoc,
        "[data-spec='excluded_sp__link1']",
        contactFuturePensionsLink
      )
    }

    // No state pension age under consideration message
    "not render page with heading Proposed change to your State Pension age text" in {
      htmlAccountDoc
        .select("[data-spec='state_pension_age_under_consideration__h2_1']")
        .isEmpty shouldBe true
    }

    "not render page with text  'You will reach State Pension age on 6 Sep 2015. Under government proposals this may increase by up to a year.'" in {
      htmlAccountDoc
        .select("[data-spec='state_pension_age_under_consideration__p1']")
        .isEmpty shouldBe true
    }

    "not render View your National Insurance Record help text" in {
      htmlAccountDoc
        .select("""[data-spec="excluded_sp__p2"]""")
        .isEmpty shouldBe true
    }

    "not render View your National Insurance Record button" in {
      htmlAccountDoc
        .select("""[data-spec="excluded_sp__govukButton"]""")
        .isEmpty shouldBe true
    }
  }

  "Exclusion Isle of Man : Can't see NI Record: State Pension Age under consideration - true" should {
    lazy val sResult        = inject[excluded_sp]
    lazy val htmlAccountDoc =
      asDocument(sResult(Exclusion.IsleOfMan, Some(40), Some(LocalDate.of(2019, 9, 6)), false, Some(true)).toString)

    "render the UR banner" in {
      assert(htmlAccountDoc.getElementsByClass("full-width-banner__title") != null)
    }

    "render with correct page title" in {
      assertElementContainsText(
        htmlAccountDoc,
        "title",
        messages("nisp.main.h1.title") +
          Constants.titleSplitter +
          messages("nisp.title.extension") +
          Constants.titleSplitter +
          messages("nisp.gov-uk")
      )
    }

    "render page with heading 'Your State Pension summary'" in {
      val h1 = htmlAccountDoc.select("[data-spec='excluded_sp__h1']")

      h1 should not be empty
      h1.first().tagName() shouldBe "h1"
      h1.text() shouldBe statePensionSummary
    }

    "render page with h2 'When will I reach State Pension age?'" in {
      val h2 = htmlAccountDoc.select("[data-spec='excluded_sp__h2']")

      h2 should not be empty
      h2.first().tagName() shouldBe "h2"
      h2.text() shouldBe whenWillReachSPA
    }

    "render page with text 'You will reach State Pension age on 6 Sep 2019' " in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='excluded_sp__p_1b']",
        willReachSPASep2019
      )
    }

    "render page with message 'We’re unable to calculate your State Pension...' with Isle of Man descriptor text " in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='exclusion_isleOfMan_sp__p1']",
        ExcludedIOMText
      )
    }

    "render page with message unable to pay gaps information text" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='exclusion_isleOfMan_cant_pay_gaps_sp_p1']",
        cantFillGapsOnline
      )
    }

    "render page with message 'You can find out more about your Isle of Man Pension from the Isle of Man Pension Centre.' " in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='exclusion_isleOfMan_sp__p2']",
        isleOfManPensionText
      )

      assertLinkHasValue(
        htmlAccountDoc,
        "[data-spec='exclusion_isleOfMan_sp__link1']",
        isleOfManPensionLink
      )
    }

    "render page with message 'Contact the Future Pension Centre to find out if you'll benefit from paying voluntary contributions.' " in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='excluded_sp__p1']",
        contactFuturePensionsText
      )

      assertLinkHasValue(
        htmlAccountDoc,
        "[data-spec='excluded_sp__link1']",
        contactFuturePensionsLink
      )
    }

    // state pension age under consideration message
    "render page with heading Proposed change to your State Pension age text" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='state_pension_age_under_consideration__h2_1']",
        proposedChangeToSPAText
      )
    }

    "render page with text  'You’ll reach State Pension age on 6 Sep 2019. Under government proposals this may increase by up to a year.'" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='state_pension_age_under_consideration__p1']",
        "You’ll reach State Pension age on 6 September 2019. Under government proposals this may increase by up to a year."
      )
    }

    "not render View your National Insurance Record help text" in {
      htmlAccountDoc
        .select("""[data-spec="excluded_sp__p2"]""")
        .isEmpty shouldBe true
    }

    "not render View your National Insurance Record button" in {
      htmlAccountDoc
        .select("""[data-spec="excluded_sp__govukButton"]""")
        .isEmpty shouldBe true
    }
  }

  "Exclusion Manual Correspondence Indicator(MCI)" should {
    lazy val sResult = inject[excluded_mci]

    lazy val htmlAccountDoc = asDocument(sResult(Exclusion.ManualCorrespondenceIndicator, Some(40)).toString)

    "render with correct page title" in {
      assertElementContainsText(
        htmlAccountDoc,
        "title",
        messages("nisp.excluded.mci.title") +
          Constants.titleSplitter +
          messages("nisp.title.extension") +
          Constants.titleSplitter +
          messages("nisp.gov-uk")
      )
    }

    "render page with heading 'You cannot access your account right now'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='excluded_mci__h1']",
        "nisp.excluded.mci.title"
      )

    }

    "render page with text  'We need to speak to you before you can log in to the service.' " in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='excluded_mci__p_lede']",
        "nisp.excluded.mci.info"
      )
    }

    "render page with text 'How to contact us'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='excluded_mci__h2']",
        "nisp.excluded.mci.howToFix"
      )
    }

    "render page with message 'Telephone: 0300 200 3300' " in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='excluded_mci__ul__how_to_fix__1']",
        "nisp.excluded.mci.howToFix.message1"
      )
    }

    "render page with message 'Textphone: 0300 200 3319'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='excluded_mci__ul__how_to_fix__2']",
        "nisp.excluded.mci.howToFix.message2"
      )
    }

    "render page with message 'Outside UK: +44 135 535 9022'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='excluded_mci__ul__how_to_fix__3']",
        "nisp.excluded.mci.howToFix.message3"
      )
    }

    "render page with message 'Phone lines are open:'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='excluded_mci__p2']",
        "nisp.excluded.mci.howToContact"
      )
    }

    "render page with message '8am to 8pm, Monday to Friday'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='excluded_mci__ul__how_to_contact__1']",
        "nisp.excluded.mci.howToContact.weekdays"
      )
    }

    "render page with message '8am to 4pm Saturdays'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='excluded_mci__ul__how_to_contact__2']",
        "nisp.excluded.mci.howToContact.saturday"
      )
    }

    "render page with message '9am to 5pm Sundays'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='excluded_mci__ul__how_to_contact__3']",
        "nisp.excluded.mci.howToContact.sunday"
      )
    }

    "render page with message 'Closed bank holidays.'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='excluded_mci__p3']",
        "nisp.excluded.mci.howToContact.bankholiday"
      )
    }

    "render page with message 'Find out about call charges (opens in a new window)'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='excluded_mci__link1']",
        "nisp.excluded.mci.howToContact.link"
      )
    }
  }

  "Exclusion Post State Pension Age: State Pension Age under consideration - no flag" should {

    lazy val sResult        = inject[excluded_sp]
    lazy val htmlAccountDoc =
      asDocument(sResult(Exclusion.PostStatePensionAge, Some(70), Some(LocalDate.of(2015, 9, 6)), true, None).toString)

    "render the UR banner" in {
      assert(htmlAccountDoc.getElementsByClass("full-width-banner__title") != null)
    }

    "render with correct page title" in {
      assertElementContainsText(
        htmlAccountDoc,
        "title",
        messages("nisp.main.h1.title") +
          Constants.titleSplitter +
          messages("nisp.title.extension") +
          Constants.titleSplitter +
          messages("nisp.gov-uk")
      )
    }

    "render page with heading 'Your State Pension summary'" in {
      val h1 = htmlAccountDoc.select("[data-spec='excluded_sp__h1']")

      h1 should not be empty
      h1.first().tagName() shouldBe "h1"
      h1.text() shouldBe statePensionSummary
    }

    "render page with h2 'When will I reach State Pension age?'" in {
      val h2 = htmlAccountDoc.select("[data-spec='excluded_sp__h2']")

      h2 should not be empty
      h2.first().tagName() shouldBe "h2"
      h2.text() shouldBe whenWillReachSPA
    }

    "render page with text You reached State Pension age on 6 September 2015" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='excluded_sp__p_1a']",
        reachedSPASep2015
      )
    }

    "render page with message  'You can claim your State Pension online if you have not already claimed it.' " in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='post_state_pension_age__p1']",
        canClaimOnline
      )

      assertLinkHasValue(
        htmlAccountDoc,
        "[data-spec='post_state_pension_age__link1']",
        canClaimOnlineLink
      )
    }

    "render page with help message for deferring state pension with link to delaying state pension page" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='post_state_pension_age_defer__p1']",
        deferSPText
      )

      assertLinkHasValue(
        htmlAccountDoc,
        "[data-spec='post_state_pension_age_defer__link1']",
        deferSPLinkText
      )
    }

    "not render page with help message for National Insurance Record button" in {
      htmlAccountDoc.select("[data-spec='excluded_sp__p2']")
        .isEmpty shouldBe true
    }

    "render page with view your National Insurance record button" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='excluded_sp__govukButton']",
        accountNIRButtonText
      )

      assertLinkHasValue(
        htmlAccountDoc,
        "[data-spec='excluded_sp__govukButton']",
        accountNIRUrl
      )
    }

    // No state pension age under consideration message
    "not render page with heading Proposed change to your State Pension age text" in {
      htmlAccountDoc
        .select("[data-spec='state_pension_age_under_consideration__h2_1']")
        .isEmpty shouldBe true
    }

    "not render page with text  'You’ll reach State Pension age on 6 Sep 2015. Under government proposals this may increase by up to a year.'" in {
      htmlAccountDoc
        .select("[data-spec='state_pension_age_under_consideration__p1']")
        .isEmpty shouldBe true
    }
  }

  "Exclusion Post State Pension Age: State Pension Age under consideration - false" should {

    lazy val sResult        = inject[excluded_sp]
    lazy val htmlAccountDoc = asDocument(
      sResult.apply(Exclusion.PostStatePensionAge, Some(70), Some(LocalDate.of(2015, 9, 6)), true, Some(false)).toString
    )

    "render the UR banner" in {
      assert(htmlAccountDoc.getElementsByClass("full-width-banner__title") != null)
    }

    "render with correct page title" in {
      assertElementContainsText(
        htmlAccountDoc,
        "title",
        messages("nisp.main.h1.title") +
          Constants.titleSplitter +
          messages("nisp.title.extension") +
          Constants.titleSplitter +
          messages("nisp.gov-uk")
      )
    }

    "render page with heading 'Your State Pension summary'" in {
      val h1 = htmlAccountDoc.select("[data-spec='excluded_sp__h1']")

      h1 should not be empty
      h1.first().tagName() shouldBe "h1"
      h1.text() shouldBe statePensionSummary
    }

    "render page with h2 'When will I reach State Pension age?'" in {
      val h2 = htmlAccountDoc.select("[data-spec='excluded_sp__h2']")

      h2 should not be empty
      h2.first().tagName() shouldBe "h2"
      h2.text() shouldBe whenWillReachSPA
    }

    "render page with text  You reached State Pension age on 6 September 2015" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='excluded_sp__p_1a']",
        reachedSPASep2015
      )
    }

    "render page with message  'You can claim your State Pension online if you have not already claimed it.' " in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='post_state_pension_age__p1']",
        canClaimOnline
      )

      assertLinkHasValue(
        htmlAccountDoc,
        "[data-spec='post_state_pension_age__link1']",
        canClaimOnlineLink
      )
    }

    "render page with help message for deferring state pension with link to delaying state pension page" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='post_state_pension_age_defer__p1']",
        deferSPText
      )

      assertLinkHasValue(
        htmlAccountDoc,
        "[data-spec='post_state_pension_age_defer__link1']",
        deferSPLinkText
      )
    }

    "not render page with help message for National Insurance Record button" in {
      htmlAccountDoc.select("[data-spec='excluded_sp__p2']")
        .isEmpty shouldBe true
    }

    "render page with view your National Insurance record button" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='excluded_sp__govukButton']",
        accountNIRButtonText
      )

      assertLinkHasValue(
        htmlAccountDoc,
        "[data-spec='excluded_sp__govukButton']",
        accountNIRUrl
      )
    }

    // No state pension age under consideration message
    "not render page with heading Proposed change to your State Pension age text" in {
      htmlAccountDoc
        .select("[data-spec='state_pension_age_under_consideration__h2_1']")
        .isEmpty shouldBe true
    }

    "not render page with text  'You’ll reach State Pension age on 6 Sep 2015. Under government proposals this may increase by up to a year.'" in {
      htmlAccountDoc
        .select("[data-spec='state_pension_age_under_consideration__p1']")
        .isEmpty shouldBe true
    }
  }

  "Exclusion Post State Pension Age: State Pension Age under consideration - true" should {

    lazy val sResult        = inject[excluded_sp]
    lazy val htmlAccountDoc = asDocument(
      sResult(Exclusion.PostStatePensionAge, Some(70), Some(LocalDate.of(2015, 9, 6)), true, Some(true)).toString
    )

    "render the UR banner" in {
      assert(htmlAccountDoc.getElementsByClass("full-width-banner__title") != null)
    }

    "render with correct page title" in {
      assertElementContainsText(
        htmlAccountDoc,
        "title",
        messages("nisp.main.h1.title") +
          Constants.titleSplitter +
          messages("nisp.title.extension") +
          Constants.titleSplitter +
          messages("nisp.gov-uk")
      )
    }

    "render page with heading 'Your State Pension summary'" in {
      val h1 = htmlAccountDoc.select("[data-spec='excluded_sp__h1']")

      h1 should not be empty
      h1.first().tagName() shouldBe "h1"
      h1.text() shouldBe statePensionSummary
    }

    "render page with h2 'When will I reach State Pension age?'" in {
      val h2 = htmlAccountDoc.select("[data-spec='excluded_sp__h2']")

      h2 should not be empty
      h2.first().tagName() shouldBe "h2"
      h2.text() shouldBe whenWillReachSPA
    }

    "render page with text  You reached State Pension age on 6 September 2015" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='excluded_sp__p_1a']",
        reachedSPASep2015
      )
    }

    "render page with message  'You can claim your State Pension online if you have not already claimed it.' " in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='post_state_pension_age__p1']",
        canClaimOnline
      )

      assertLinkHasValue(
        htmlAccountDoc,
        "[data-spec='post_state_pension_age__link1']",
        canClaimOnlineLink
      )
    }

    "render page with help message for deferring state pension with link to delaying state pension page" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='post_state_pension_age_defer__p1']",
        deferSPText
      )

      assertLinkHasValue(
        htmlAccountDoc,
        "[data-spec='post_state_pension_age_defer__link1']",
        deferSPLinkText
      )
    }

    "not render page with help message for National Insurance Record button" in {
      htmlAccountDoc.select("[data-spec='excluded_sp__p2']")
        .isEmpty shouldBe true
    }

    "render page with view your National Insurance record button" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='excluded_sp__govukButton']",
        accountNIRButtonText
      )

      assertLinkHasValue(
        htmlAccountDoc,
        "[data-spec='excluded_sp__govukButton']",
        accountNIRUrl
      )
    }

    // No state pension age under consideration message
    "not render page with heading Proposed change to your State Pension age text" in {
      htmlAccountDoc
        .select("state_pension_age_under_consideration__h2_1")
        .isEmpty shouldBe true
    }

    "not render page with text  'You’ll reach State Pension age on 6 Sep 2015. Under government proposals this may increase by up to a year.'" in {
      htmlAccountDoc
        .select("[data-spec='state_pension_age_under_consideration__p1']")
        .isEmpty shouldBe true
    }
  }

  "Exclusion Amount Dissonance: State Pension Age under consideration - no flag" should {
    lazy val sResult        = inject[excluded_sp]
    lazy val htmlAccountDoc =
      asDocument(sResult(Exclusion.AmountDissonance, Some(70), Some(LocalDate.of(2015, 9, 6)), true, None).toString)

    "render with correct page title" in {
      assertElementContainsText(
        htmlAccountDoc,
        "title",
        messages("nisp.main.h1.title") +
          Constants.titleSplitter +
          messages("nisp.title.extension") +
          Constants.titleSplitter +
          messages("nisp.gov-uk")
      )
    }

    "render page with heading 'Your State Pension summary'" in {
      val h1 = htmlAccountDoc.select("[data-spec='excluded_sp__h1']")

      h1 should not be empty
      h1.first().tagName() shouldBe "h1"
      h1.text() shouldBe statePensionSummary
    }

    "render page with h2 'When will I reach State Pension age?'" in {
      val h2 = htmlAccountDoc.select("[data-spec='excluded_sp__h2']")

      h2 should not be empty
      h2.first().tagName() shouldBe "h2"
      h2.text() shouldBe whenWillReachSPA
    }

    "render page with text 'You will reach State Pension age on 6 September 2015'" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='excluded_sp__p_1b']",
        willReachSPASep2015
      )
    }

    "render page with text  We’re unable to calculate your State Pension forecast. We are working on fixing this." in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='exclusions__sp__amountdissonance__p1']",
        "We’re unable to calculate your State Pension forecast. We are working on fixing this."
      )
    }

    "render page with text  Contact the Future Pension Centre to find out if you'll benefit from paying voluntary contributions." in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='excluded_sp__p1']",
        contactFuturePensionsText
      )

      assertLinkHasValue(
        htmlAccountDoc,
        "[data-spec='excluded_sp__link1']",
        contactFuturePensionsLink
      )
    }

    "render page with help message for National Insurance Record button for Dissonance" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='excluded_sp__p2']",
        checkNIForGapsText
      )
    }

    "render page with view your National Insurance record button" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='excluded_sp__govukButton']",
        accountNIRButtonText
      )

      assertLinkHasValue(
        htmlAccountDoc,
        "[data-spec='excluded_sp__govukButton']",
        accountNIRUrl
      )
    }

    // No state pension age under consideration message
    "not render page with heading Proposed change to your State Pension age text" in {
      htmlAccountDoc
        .select("state_pension_age_under_consideration__h2_1")
        .isEmpty shouldBe true
    }

    "not render page with text  'You’ll reach State Pension age on 6 Sep 2015. Under government proposals this may increase by up to a year.'" in {
      htmlAccountDoc
        .select("[data-spec='state_pension_age_under_consideration__p1']")
        .isEmpty shouldBe true
    }
  }

  "Exclusion Amount Dissonance: State Pension Age under consideration - false" should {
    lazy val sResult        = inject[excluded_sp]
    lazy val htmlAccountDoc = asDocument(
      sResult(Exclusion.AmountDissonance, Some(70), Some(LocalDate.of(2015, 9, 6)), true, Some(false)).toString
    )

    "render with correct page title" in {
      assertElementContainsText(
        htmlAccountDoc,
        "title",
        messages("nisp.main.h1.title") +
          Constants.titleSplitter +
          messages("nisp.title.extension") +
          Constants.titleSplitter +
          messages("nisp.gov-uk")
      )
    }

    "render page with heading 'Your State Pension summary'" in {
      val h1 = htmlAccountDoc.select("[data-spec='excluded_sp__h1']")

      h1 should not be empty
      h1.first().tagName() shouldBe "h1"
      h1.text() shouldBe statePensionSummary
    }

    "render page with h2 'When will I reach State Pension age?'" in {
      val h2 = htmlAccountDoc.select("[data-spec='excluded_sp__h2']")

      h2 should not be empty
      h2.first().tagName() shouldBe "h2"
      h2.text() shouldBe whenWillReachSPA
    }

    "render page with text 'You will reach your  State Pension age on 6 September 2015'" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='excluded_sp__p_1b']",
        willReachSPASep2015
      )
    }

    "render page with text 'We’re unable to calculate your State Pension forecast. We are working on fixing this.'" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='exclusions__sp__amountdissonance__p1']",
        "We’re unable to calculate your State Pension forecast. We are working on fixing this."
      )
    }

    "render page with text 'Contact the Future Pension Centre to find out if you'll benefit from paying voluntary contributions.'" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='excluded_sp__p1']",
        contactFuturePensionsText
      )

      assertLinkHasValue(
        htmlAccountDoc,
        "[data-spec='excluded_sp__link1']",
        contactFuturePensionsLink
      )
    }

    "render page with help message for National Insurance Record button for dissonance" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='excluded_sp__p2']",
        checkNIForGapsText
      )
    }

    "render page with view your National Insurance record button dissonance" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='excluded_sp__govukButton']",
        accountNIRButtonText
      )

      assertLinkHasValue(
        htmlAccountDoc,
        "[data-spec='excluded_sp__govukButton']",
        accountNIRUrl
      )
    }

    // No state pension age under consideration message
    "not render page with heading Proposed change to your State Pension age text" in {
      htmlAccountDoc
        .select("state_pension_age_under_consideration__h2_1")
        .isEmpty shouldBe true
    }

    "not render page with text  'You’ll reach State Pension age on 6 Sep 2015. Under government proposals this may increase by up to a year.'" in {
      htmlAccountDoc
        .select("[data-spec='state_pension_age_under_consideration__p1']")
        .isEmpty shouldBe true
    }
  }

  "Exclusion Amount Dissonance: State Pension Age under consideration - true" should {
    lazy val sResult        = inject[excluded_sp]
    lazy val htmlAccountDoc = asDocument(
      sResult(Exclusion.AmountDissonance, Some(70), Some(LocalDate.of(2015, 9, 6)), true, Some(true)).toString
    )

    "render with correct page title" in {
      assertElementContainsText(
        htmlAccountDoc,
        "title",
        messages("nisp.main.h1.title") +
          Constants.titleSplitter +
          messages("nisp.title.extension") +
          Constants.titleSplitter +
          messages("nisp.gov-uk")
      )
    }

    "render page with heading 'Your State Pension summary'" in {
      val h1 = htmlAccountDoc.select("[data-spec='excluded_sp__h1']")

      h1 should not be empty
      h1.first().tagName() shouldBe "h1"
      h1.text() shouldBe statePensionSummary
    }

    "render page with h2 'When will I reach State Pension age?'" in {
      val h2 = htmlAccountDoc.select("[data-spec='excluded_sp__h2']")

      h2 should not be empty
      h2.first().tagName() shouldBe "h2"
      h2.text() shouldBe whenWillReachSPA
    }

    "render page with text  You will reach your  State Pension age on 6 September 2015" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='excluded_sp__p_1b']",
        willReachSPASep2015
      )
    }

    "render page with text We’re unable to calculate your State Pension forecast. We are working on fixing this." in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='exclusions__sp__amountdissonance__p1']",
        "We’re unable to calculate your State Pension forecast. We are working on fixing this."
      )
    }

    "render page with text  Contact the Future Pension Centre to find out if you'll benefit from paying voluntary contributions." in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='excluded_sp__p1']",
        contactFuturePensionsText
      )

      assertLinkHasValue(
        htmlAccountDoc,
        "[data-spec='excluded_sp__link1']",
        contactFuturePensionsLink
      )
    }

    "render page with help message for National Insurance Record button" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='excluded_sp__p2']",
        checkNIForGapsText
      )
    }

    "render page with view your National Insurance record button" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='excluded_sp__govukButton']",
        accountNIRButtonText
      )

      assertLinkHasValue(
        htmlAccountDoc,
        "[data-spec='excluded_sp__govukButton']",
        accountNIRUrl
      )
    }

    // state pension age under consideration message
    "render page with heading Proposed change to your State Pension age text" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='state_pension_age_under_consideration__h2_1']",
        proposedChangeToSPAText
      )
    }

    "render page with text  'You’ll reach State Pension age on 6 Sep 2015. Under government proposals this may increase by up to a year.'" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='state_pension_age_under_consideration__p1']",
        "You’ll reach State Pension age on 6 September 2015. Under government proposals this may increase by up to a year."
      )
    }
  }

  "Exclusion Married Women: State Pension Age under consideration - no flag" should {
    lazy val sResult        = inject[excluded_sp]
    lazy val htmlAccountDoc = asDocument(
      sResult(Exclusion.MarriedWomenReducedRateElection, Some(60), Some(LocalDate.of(2015, 9, 6)), false, None).toString
    )

    "render with correct page title" in {
      assertElementContainsText(
        htmlAccountDoc,
        "title",
        messages("nisp.main.h1.title") +
          Constants.titleSplitter +
          messages("nisp.title.extension") +
          Constants.titleSplitter +
          messages("nisp.gov-uk")
      )
    }

    "render page with heading 'Your State Pension summary'" in {
      val h1 = htmlAccountDoc.select("[data-spec='excluded_sp__h1']")

      h1 should not be empty
      h1.first().tagName() shouldBe "h1"
      h1.text() shouldBe statePensionSummary
    }

    "render page with h2 'When will I reach State Pension age?'" in {
      val h2 = htmlAccountDoc.select("[data-spec='excluded_sp__h2']")

      h2 should not be empty
      h2.first().tagName() shouldBe "h2"
      h2.text() shouldBe whenWillReachSPA
    }

    "render page with text - You will reach your  State Pension age on 6 September 2015" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='excluded_sp__p_1b']",
        willReachSPASep2015
      )
    }

    "render page text `We’re unable to calculate your State Pension forecast...` with MWRRE help text and page link" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='exclusions__sp__mwrre__p1']",
        reducedRateMarriedWomanText
      )

      assertLinkHasValue(
        htmlAccountDoc,
        "[data-spec='exclusions__sp__mwrre__link1']",
        reducedRateMarriedWomanLink
      )
    }

    "render page with message unable to pay gaps information text" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='exclusion_mwrre_cant_pay_gaps_sp_p1']",
        cantFillGapsOnline
      )
    }

    "render page with text  Contact the Future Pension Centre to find out if you'll benefit from paying voluntary contributions." in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='excluded_sp__p1']",
        contactFuturePensionsText
      )

      assertLinkHasValue(
        htmlAccountDoc,
        "[data-spec='excluded_sp__link1']",
        contactFuturePensionsLink
      )
    }

    // No state pension age under consideration message
    "not render page with heading 'Proposed change to your State Pension age'" in {
      htmlAccountDoc
        .select("state_pension_age_under_consideration__h2_1")
        .isEmpty shouldBe true
    }

    "not render page with text  'You’ll reach State Pension age on 6 Sep 2015. Under government proposals this may increase by up to a year.'" in {
      htmlAccountDoc
        .select("[data-spec='state_pension_age_under_consideration__p1']")
        .isEmpty shouldBe true
    }

    "not render View your National Insurance Record help text" in {
      htmlAccountDoc
        .select("""[data-spec="excluded_sp__p2"]""")
        .isEmpty shouldBe true
    }

    "not render View your National Insurance Record button" in {
      htmlAccountDoc
        .select("""[data-spec="excluded_sp__govukButton"]""")
        .isEmpty shouldBe true
    }
  }

  "Exclusion Married Women: State Pension Age under consideration - false" should {
    lazy val sResult        = inject[excluded_sp]
    lazy val htmlAccountDoc = asDocument(
      sResult(
        Exclusion.MarriedWomenReducedRateElection,
        Some(60),
        Some(LocalDate.of(2015, 9, 6)),
        false,
        Some(false)
      ).toString
    )

    "render with correct page title" in {
      assertElementContainsText(
        htmlAccountDoc,
        "title",
        messages("nisp.main.h1.title") +
          Constants.titleSplitter +
          messages("nisp.title.extension") +
          Constants.titleSplitter +
          messages("nisp.gov-uk")
      )
    }

    "render page with heading 'Your State Pension summary'" in {
      val h1 = htmlAccountDoc.select("[data-spec='excluded_sp__h1']")

      h1 should not be empty
      h1.first().tagName() shouldBe "h1"
      h1.text() shouldBe statePensionSummary
    }

    "render page with h2 'When will I reach State Pension age?'" in {
      val h2 = htmlAccountDoc.select("[data-spec='excluded_sp__h2']")

      h2 should not be empty
      h2.first().tagName() shouldBe "h2"
      h2.text() shouldBe whenWillReachSPA
    }

    "render page with text 'You will reach your  State Pension age on 6 September 2015'" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='excluded_sp__p_1b']",
        willReachSPASep2015
      )
    }

    "render page text `We’re unable to calculate your State Pension forecast...` with MWRRE help text and page link" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='exclusions__sp__mwrre__p1']",
        reducedRateMarriedWomanText
      )

      assertLinkHasValue(
        htmlAccountDoc,
        "[data-spec='exclusions__sp__mwrre__link1']",
        reducedRateMarriedWomanLink
      )
    }

    "render page with text 'Contact the Future Pension Centre to find out if you'll benefit from paying voluntary contributions.'" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='excluded_sp__p1']",
        contactFuturePensionsText
      )

      assertLinkHasValue(
        htmlAccountDoc,
        "[data-spec='excluded_sp__link1']",
        contactFuturePensionsLink
      )
    }

    // No state pension age under consideration message
    "not render page with heading 'Proposed change to your State Pension age'" in {
      htmlAccountDoc
        .select("state_pension_age_under_consideration__h2_1")
        .isEmpty shouldBe true
    }

    "not render page with text  'You’ll reach State Pension age on 6 Sep 2015. Under government proposals this may increase by up to a year.'" in {
      htmlAccountDoc
        .select("[data-spec='state_pension_age_under_consideration__p1']")
        .isEmpty shouldBe true
    }

    "not render View your National Insurance Record help text" in {
      htmlAccountDoc
        .select("""[data-spec="excluded_sp__p2"]""")
        .isEmpty shouldBe true
    }

    "not render View your National Insurance Record button" in {
      htmlAccountDoc
        .select("""[data-spec="excluded_sp__govukButton"]""")
        .isEmpty shouldBe true
    }
  }

  "Exclusion Married Women: State Pension Age under consideration - true" should {
    lazy val sResult        = inject[excluded_sp]
    lazy val htmlAccountDoc = asDocument(
      sResult(
        Exclusion.MarriedWomenReducedRateElection,
        Some(60),
        Some(LocalDate.of(2015, 9, 6)),
        false,
        Some(true)
      ).toString
    )

    "render with correct page title" in {
      assertElementContainsText(
        htmlAccountDoc,
        "title",
        messages("nisp.main.h1.title") +
          Constants.titleSplitter +
          messages("nisp.title.extension") +
          Constants.titleSplitter +
          messages("nisp.gov-uk")
      )
    }

    "render page with heading 'Your State Pension summary'" in {
      val h1 = htmlAccountDoc.select("[data-spec='excluded_sp__h1']")

      h1 should not be empty
      h1.first().tagName() shouldBe "h1"
      h1.text() shouldBe statePensionSummary
    }

    "render page with h2 'When will I reach State Pension age?'" in {
      val h2 = htmlAccountDoc.select("[data-spec='excluded_sp__h2']")

      h2 should not be empty
      h2.first().tagName() shouldBe "h2"
      h2.text() shouldBe whenWillReachSPA
    }

    "render page with text - You will reach your  State Pension age on 6 September 2015" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='excluded_sp__p_1b']",
        willReachSPASep2015
      )
    }

    "render page text `We’re unable to calculate your State Pension forecast...` with MWRRE help text and page link" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='exclusions__sp__mwrre__p1']",
        reducedRateMarriedWomanText
      )

      assertLinkHasValue(
        htmlAccountDoc,
        "[data-spec='exclusions__sp__mwrre__link1']",
        reducedRateMarriedWomanLink
      )
    }

    "render page with message unable to pay gaps information text" in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='exclusion_mwrre_cant_pay_gaps_sp_p1']",
        cantFillGapsOnline
      )
    }

    "render page with text  Contact the Future Pension Centre to find out if you'll benefit from paying voluntary contributions." in {
      assertEqualsText(
        htmlAccountDoc,
        "[data-spec='excluded_sp__p1']",
        contactFuturePensionsText
      )

      assertLinkHasValue(
        htmlAccountDoc,
        "[data-spec='excluded_sp__link1']",
        contactFuturePensionsLink
      )
    }

    // No state pension age under consideration message
    "not render page with heading 'Proposed change to your State Pension age'" in {
      htmlAccountDoc
        .select("state_pension_age_under_consideration__h2_1")
        .isEmpty shouldBe true
    }

    "not render page with text  'You’ll reach State Pension age on 6 Sep 2015. Under government proposals this may increase by up to a year.'" in {
      htmlAccountDoc
        .select("[data-spec='state_pension_age_under_consideration__p1']")
        .isEmpty shouldBe true
    }

    "not render View your National Insurance Record help text" in {
      htmlAccountDoc
        .select("""[data-spec="excluded_sp__p2"]""")
        .isEmpty shouldBe true
    }

    "not render View your National Insurance Record button" in {
      htmlAccountDoc
        .select("""[data-spec="excluded_sp__govukButton"]""")
        .isEmpty shouldBe true
    }
  }
}
