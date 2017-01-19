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

import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

class QuestionnaireEventSpec extends UnitSpec {

  implicit val hc = HeaderCarrier()

   "QuestionnaireEvent" should {

     "not store the email address if research is No" in {
       val event = new QuestionnaireEvent(None, None, None, None, None, None, None, None, Some(1), Some("test@test.com"), "Name", "NINO", "")
       event.detail("email") shouldBe "N/A"
     }

     "not store the email address if research is Blank" in {
       val event = new QuestionnaireEvent(None, None, None, None, None, None, None, None, None, Some("test@test.com"), "Name", "NINO", "")
       event.detail("email") shouldBe "N/A"
     }

     "not store the [Please state] textbox if [Other] is not selected" in {
       val event = new QuestionnaireEvent(None, None, None, None, None, Some(5), Some("Lorem ipsum"), None, None, None, "Name", "NINO", "")
       event.detail("otherFollowUp") shouldBe "N/A"
     }

     "store the [Please state] textbox content, if [Other] is selected" in {
       val event = new QuestionnaireEvent(None, None, None, None, None, Some(8), Some("Lorem ipsum"), None, None, None, "Name", "NINO", "")
       event.detail("otherFollowUp") shouldBe "Lorem ipsum"
     }

     "store the email address if research is Yes" in {
       val event = new QuestionnaireEvent(None, None, None, None, None, None, None, None, Some(0), Some("test@test.com"), "Name", "NINO", "")
       event.detail("email") shouldBe "test@test.com"
     }

   }
}
