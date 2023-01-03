/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.nisp.controllers.auth.NispAuthedUser
import uk.gov.hmrc.nisp.fixtures.NispAuthedUserFixture
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.nisp.views.html.iv.failurepages.locked_out

class LockedOutViewSpec extends HtmlSpec with Injecting {

  val fakeRequest                   = FakeRequest("GET", "/")
  implicit val user: NispAuthedUser = NispAuthedUserFixture.user(TestAccountBuilder.regularNino)

  val feedbackFrontendUrl: String = "/foo"
  lazy val html                   = inject[locked_out]
  lazy val source                 = asDocument(html().toString)

  "LockedOutView" should {

    "assert correct page title" in {
      val title    = source.title()
      val expected = messages("nisp.iv.failure.lockedout.title") + Constants.titleSplitter + messages(
        "nisp.title.extension"
      ) + Constants.titleSplitter + messages("nisp.gov-uk")
      title should include(expected)
    }

    "assert correct heading title on page" in {
      val title = source.getElementsByTag("h1").get(0).toString
      title should include(messages("nisp.iv.failure.lockedout.title"))
    }
  }
}
