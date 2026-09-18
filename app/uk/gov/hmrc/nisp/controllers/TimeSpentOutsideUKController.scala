/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package uk.gov.hmrc.nisp.controllers

import com.google.inject.Inject
import play.api.data.Form
import play.api.data.Forms.*
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.nisp.views.html.timeSpentOutsideUK

class TimeSpentOutsideUKController @Inject()(
  mcc: MessagesControllerComponents,
  view: timeSpentOutsideUK
) extends NispFrontendController(mcc) with I18nSupport {

  private val answerSessionKey = "time-spent-outside-uk-answer"

  private val form: Form[String] = Form(
    single(
      "timeSpentOutsideUK" -> nonEmptyText.verifying(
        "nisp.timeSpentOutsideUK.error.required",
        answer => Set("yes", "no").contains(answer)
      )
    )
  )

  def show: Action[AnyContent] = Action { implicit request =>
    val filledForm = request.session.get(answerSessionKey).fold(form)(form.fill)
    Ok(view(filledForm))
  }

  def submit: Action[AnyContent] = Action { implicit request =>
    form.bindFromRequest().fold(
      formWithErrors => BadRequest(view(formWithErrors)),
      answer => {
        val result =
          if (answer == "yes") {
            Redirect(routes.SeekGuidanceOrFinancialAdviceController.showView)
          } else {
            Redirect(routes.NIRecordController.showVoluntaryContributions)
          }

        result.addingToSession(answerSessionKey -> answer)
      }
    )
  }
}
