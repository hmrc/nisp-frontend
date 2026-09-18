/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package uk.gov.hmrc.nisp.controllers

import org.jsoup.Jsoup
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.nisp.utils.UnitSpec

class TimeSpentOutsideUKControllerSpec extends UnitSpec with GuiceOneAppPerSuite with Injecting {

  private val controller = inject[TimeSpentOutsideUKController]

  "GET /time-spent-outside-uk" should {
    "render the page" in {
      val result = controller.show(FakeRequest(GET, "/time-spent-outside-uk"))

      status(result) shouldBe Status.OK
      contentType(result) shouldBe Some("text/html")
      Jsoup.parse(contentAsString(result))
        .select("[data-spec=time_spent_outside_uk__h1]")
        .text() shouldBe "Have you lived or worked outside the UK since 2015?"
    }

    "restore the previously selected answer from session" in {
      val result = controller.show(
        FakeRequest(GET, "/time-spent-outside-uk")
          .withSession("time-spent-outside-uk-answer" -> "yes")
      )

      Jsoup.parse(contentAsString(result))
        .getElementById("timeSpentOutsideUK")
        .hasAttr("checked") shouldBe true
    }
  }

  "POST /time-spent-outside-uk" should {
    "show validation errors when no answer is selected" in {
      val result = controller.submit(
        FakeRequest(POST, "/time-spent-outside-uk")
          .withFormUrlEncodedBody()
      )

      status(result) shouldBe Status.BAD_REQUEST

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("[data-spec=time_spent_outside_uk__error_summary]").size() shouldBe 1
      doc.select(".govuk-error-message").text() should include("Select yes if you have lived or worked outside the UK since 2015")
    }

    "store yes and continue to contact guidance" in {
      val result = controller.submit(
        FakeRequest(POST, "/time-spent-outside-uk")
          .withFormUrlEncodedBody("timeSpentOutsideUK" -> "yes")
      )

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.SeekGuidanceOrFinancialAdviceController.showView.url)
      session(result).get("time-spent-outside-uk-answer") shouldBe Some("yes")
    }

    "store no and continue to voluntary contributions" in {
      val result = controller.submit(
        FakeRequest(POST, "/time-spent-outside-uk")
          .withFormUrlEncodedBody("timeSpentOutsideUK" -> "no")
      )

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.NIRecordController.showVoluntaryContributions.url)
      session(result).get("time-spent-outside-uk-answer") shouldBe Some("no")
    }
  }
}
