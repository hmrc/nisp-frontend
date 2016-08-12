/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.models.forms

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.voa.play.form.ConditionalMappings._

case class QuestionnaireForm(easyToUse: Option[Int], useItByYourself: Option[Int], likelyToUse: Option[Int],
                             satisfied: Option[Int], followUpCall: Option[Int], otherFollowUp: Option[String],
                             improve: Option[String], research: Option[Int], email: Option[String], understanding: Option[Int])

object QuestionnaireForm {
  val form = Form[QuestionnaireForm](
    // scalastyle:off magic.number
    mapping(
      "easytouse" -> optional(number(0, 3)),
      "useitbyyourself" -> optional(number(0, 2)),
      "likelytouse" -> optional(number(0, 3)),
      "satisfied" -> optional(number(0, 4)),
      "followupcall" -> optional(number(0,10)),
      "otherfollowup" -> mandatoryIfEqual("followupcall", "10", nonEmptyText),
      "improve" -> optional(text(maxLength = 1200)),
      "research" -> optional(number(0,1)),
      "email" -> mandatoryIfEqual("research", "0", email),
      "understanding" -> optional(number(0,2))
    )(QuestionnaireForm.apply)(QuestionnaireForm.unapply)
  )
}
