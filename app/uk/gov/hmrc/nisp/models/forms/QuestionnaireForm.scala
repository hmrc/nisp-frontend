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

case class QuestionnaireForm(easyToUse: Int, useItByYourself: Int, likelyToUse: Int, likelyToSeek : Int,
                             recommend : Int, satisfied: Int, takePart: Int, nextSteps: Option[String])

object QuestionnaireForm {
  val form = Form[QuestionnaireForm](
    // scalastyle:off magic.number
    mapping(
      "easytouse" -> number(0, 3),
      "useitbyyourself" -> number(0, 2),
      "likelytouse" -> number(0, 3),
      "likelytoseek" -> number(0, 3),
      "recommend" -> number(0, 1),
      "satisfied" -> number(0, 3),
      "takepart" -> number(0, 1),
      "nextsteps" -> optional(text(maxLength = 1200))
    )(QuestionnaireForm.apply)(QuestionnaireForm.unapply)
  )
}
