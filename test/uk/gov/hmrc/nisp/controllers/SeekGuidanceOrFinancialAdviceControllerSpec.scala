/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.controllers

import org.jsoup.Jsoup
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http._
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers.{charset, contentAsString, contentType, defaultAwaitTimeout}
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.nisp.utils.UnitSpec

class SeekGuidanceOrFinancialAdviceControllerSpec extends UnitSpec with GuiceOneAppPerSuite with Injecting {

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type]  = FakeRequest("GET", "/")
  val SeekFinancialAdviceController: SeekGuidanceOrFinancialAdviceController = inject[SeekGuidanceOrFinancialAdviceController]

  "showView" should {

    "return 200" in {
      val result = SeekFinancialAdviceController.showView(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return HTML" in {
      val result = SeekFinancialAdviceController.showView(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result)     shouldBe Some("utf-8")
    }

    "return Seek guidance or financial advice page" in {
      val result = SeekFinancialAdviceController.showView(fakeRequest)
      val doc    = Jsoup.parse(contentAsString(result))
      doc.getElementById("mainTitle").text shouldBe "Seek guidance or financial advice"
    }
  }
}
