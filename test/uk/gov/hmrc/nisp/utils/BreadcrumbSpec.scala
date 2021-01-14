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

package uk.gov.hmrc.nisp.utils

import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import uk.gov.hmrc.nisp.helpers.MockBreadcrumb
import uk.gov.hmrc.play.test.UnitSpec

class BreadcrumbSpec extends UnitSpec with OneAppPerSuite {
  val fakeRequestSP = FakeRequest("GET", "/account")
  val fakeRequestNI = FakeRequest("GET", "/account/nirecord/gaps")
  val fakeRequestVolContribution = FakeRequest("GET", "/account/nirecord/voluntarycontribs")
  val fakeRequestHowToImproveGaps = FakeRequest("GET", "/account/nirecord/gapsandhowtocheck")
  val messages = applicationMessages

  "Breadcrumb utils" should {
    "return a item text as Account Home and State Pension" in {
      val bc = MockBreadcrumb.buildBreadCrumb(fakeRequestSP, messages)
      bc.lastItem.map(_.text) shouldBe Some("State Pension")
    }

    "return a item text as Account Home, State Pension and NI Record when URL is /account/nirecord/gaps" in {
      val bc = MockBreadcrumb.buildBreadCrumb(fakeRequestNI, messages)
      val breadcrumbItem = "Breadcrumb: BreadcrumbItem(Account home,http://localhost:9232/account), BreadcrumbItem(State Pension,/check-your-state-pension/account), lastItem: Some(BreadcrumbItem(NI record,/check-your-state-pension/account/nirecord))"
      bc.toString() shouldBe breadcrumbItem
    }

    "return a item text as Account Home, State Pension and NI Record when URL is /account/nirecord/voluntarycontribs" in {
      val bc = MockBreadcrumb.buildBreadCrumb(fakeRequestVolContribution, messages)
      val breadcrumbItem = "Breadcrumb: BreadcrumbItem(Account home,http://localhost:9232/account), BreadcrumbItem(State Pension,/check-your-state-pension/account), BreadcrumbItem(NI record,/check-your-state-pension/account/nirecord), lastItem: Some(BreadcrumbItem(Voluntary contributions,/check-your-state-pension/account/nirecord/voluntarycontribs))"
      bc.toString() shouldBe breadcrumbItem
    }

    "return a item text as Account Home, State Pension and NI Record when URL is /account/nirecord/gapsandhowtocheck" in {
      val bc = MockBreadcrumb.buildBreadCrumb(fakeRequestHowToImproveGaps, messages)
      val breadcrumbItem = "Breadcrumb: BreadcrumbItem(Account home,http://localhost:9232/account), BreadcrumbItem(State Pension,/check-your-state-pension/account), BreadcrumbItem(NI record,/check-your-state-pension/account/nirecord), lastItem: Some(BreadcrumbItem(Gaps in your record and how to check them,/check-your-state-pension/account/nirecord/gapsandhowtocheck))"
      bc.toString() shouldBe breadcrumbItem
    }
  }
}
