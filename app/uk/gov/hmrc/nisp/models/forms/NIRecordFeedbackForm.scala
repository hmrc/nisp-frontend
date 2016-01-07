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

case class NIRecordFeedbackForm(comments: String, hasGaps: Boolean, updatingRecord: Option[String])

object NIRecordFeedbackForm {
  val form = Form[NIRecordFeedbackForm](
    // scalastyle:off magic.number
    mapping(
      "comments" -> text(minLength = 0,maxLength = 1200),
      "hasGaps" -> boolean,
      "updatingRecord" -> optional(text(minLength = 0,maxLength = 1200))
    )(NIRecordFeedbackForm.apply)(NIRecordFeedbackForm.unapply)
  )
}
