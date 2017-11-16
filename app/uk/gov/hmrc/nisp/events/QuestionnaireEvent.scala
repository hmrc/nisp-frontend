/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.events

import uk.gov.hmrc.http.HeaderCarrier

class QuestionnaireEvent(easyToUse: Option[Int], useItByYourself: Option[Int], likelyToUse: Option[Int],
                         satisfied: Option[Int],
                         understanding: Option[Int],
                         whatWillYouDoNext: Option[Int], otherFollowUp: Option[String], improve: Option[String],
                         research: Option[Int], email: Option[String], name: String, nino: String, contractedOut: String
                         )(implicit hc: HeaderCarrier)
extends NispBusinessEvent("Questionnaire",
    Map(
      "version" -> 7.toString,
      "easytouse" -> easyToUse.map(_.toString).getOrElse("N/A"),
      "useitbyyourself" -> useItByYourself.map(_.toString).getOrElse("N/A"),
      "likelytouse" -> likelyToUse.map(_.toString).getOrElse("N/A"),
      "satisfied"-> satisfied.map(_.toString).getOrElse("N/A"),
      "understanding"-> understanding.map(_.toString).getOrElse("N/A"),
      "whatWillYouDoNext" -> whatWillYouDoNext.map(_.toString).getOrElse("N/A"),
      "otherFollowUp" -> whatWillYouDoNext.flatMap(res => if(res == 8) otherFollowUp else None).map(_.toString).getOrElse("N/A"),
      "improve" -> improve.map(_.toString).getOrElse("N/A"),
      "research" -> research.map(_.toString).getOrElse("N/A"),
      "email" -> research.flatMap(res => if(res == 0) email else None).map(_.toString).getOrElse("N/A"),
      "Name" -> name,
      "nino" -> nino,
      "contractedout" -> contractedOut
    )
  )
