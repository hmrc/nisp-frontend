/*
 * Copyright 2015 HM Revenue & Customs
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

import uk.gov.hmrc.play.http.HeaderCarrier

class QuestionnaireEvent(easyToUse: Int, useItByYourself: Int, likelyToUse: Int, likelyToSeek : Int,
                         recommend : Int, satisfied: Int, takePart: Int, nextSteps: String, name: String,
                         nino: String, abTest: String)(implicit hc: HeaderCarrier)
  extends NispBusinessEvent("Questionnaire",
    Map(
      "version" -> 3.toString,
      "easytouse" -> easyToUse.toString,
      "useitbyyourself" -> useItByYourself.toString,
      "likelytouse" -> likelyToUse.toString,
      "likelytoseek"-> likelyToSeek.toString,
      "recommend" -> recommend.toString,
      "satisfied"-> satisfied.toString,
      "takepart" -> takePart.toString,
      "nextsteps" -> nextSteps.toString,
      "Name" -> name,
      "nino" -> nino,
      "abtest" -> abTest
    )
  )
