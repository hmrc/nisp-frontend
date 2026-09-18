/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package uk.gov.hmrc.nisp.views

import play.api.data.Form
import play.api.data.Forms.*
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.nisp.views.html.timeSpentOutsideUK

class TimeSpentOutsideUKViewSpec extends HtmlSpec with Injecting {

  private val view = inject[timeSpentOutsideUK]
  private val form = Form(single("timeSpentOutsideUK" -> nonEmptyText))

  "Time spent outside UK view" should {
    "render the heading, radio options, continue button and back link" in {
      implicit val request = FakeRequest()
      val doc = asDocument(view(form).toString())

      doc.select("[data-spec=time_spent_outside_uk__h1]").text() shouldBe
        messages("nisp.timeSpentOutsideUK.heading")

      doc.select("label[for=timeSpentOutsideUK]").text() shouldBe
        messages("nisp.timeSpentOutsideUK.yes")

      doc.select("label[for=timeSpentOutsideUK-no]").text() shouldBe
        messages("nisp.timeSpentOutsideUK.no")

      doc.select("[data-spec=time_spent_outside_uk__continue]").text() shouldBe
        messages("nisp.continue")

      doc.select("[data-spec=time_spent_outside_uk__back]").attr("href") shouldBe
        "/check-your-state-pension/account/nirecord/gaps"
    }

    "render a previously selected answer" in {
      implicit val request = FakeRequest()
      val doc = asDocument(view(form.fill("yes")).toString())

      doc.getElementById("timeSpentOutsideUK").hasAttr("checked") shouldBe true
    }
  }
}
