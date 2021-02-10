/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.views.formatting

import javax.inject.Inject

import org.scalatestplus.play.OneAppPerSuite
import uk.gov.hmrc.play.test.UnitSpec
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.i18n.{I18nSupport, Lang, Messages, MessagesApi}

class TimeSpec extends UnitSpec with OneAppPerSuite {
  val messagesApi = app.injector.instanceOf[MessagesApi]
  implicit val messages = new Messages(new Lang("en"), messagesApi)

  "years" should {
    "return 1 year when there is 1 year"  in {
      Time.years(1)(messages) shouldBe "1 year"
    }
    "return 5 years when there is 5 years" in {
      Time.years(5)(messages) shouldBe "5 years"
    }
    "return 0 years when there is 0 years" in {
      Time.years(0)(messages) shouldBe "0 years"
    }
    "return -1 year when there is -1 year" in {
      Time.years(-1)(messages) shouldBe "-1 year"
    }

  }
}
