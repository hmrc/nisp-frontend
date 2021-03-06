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

import java.util.Locale

import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.play.test.UnitSpec
import org.mockito.Mockito.{when, reset}
import org.scalatest.BeforeAndAfterEach

class BreadcrumbSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar with Injecting with BeforeAndAfterEach {

  val fakeRequestSP = FakeRequest("GET", "/account")
  val fakeRequestNI = FakeRequest("GET", "/account/nirecord/gaps")
  val fakeRequestVolContribution = FakeRequest("GET", "/account/nirecord/voluntarycontribs")
  val fakeRequestHowToImproveGaps = FakeRequest("GET", "/account/nirecord/gapsandhowtocheck")
  val mockApplicationConfig = mock[ApplicationConfig]

  val pertaxFrontendUrl = "pertaxFrontendUrl"

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[ApplicationConfig].toInstance(mockApplicationConfig)
    ).build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockApplicationConfig)
    when(mockApplicationConfig.pertaxFrontendUrl).thenReturn(pertaxFrontendUrl)
  }

  val messages: MessagesImpl = MessagesImpl(Lang(Locale.getDefault), inject[MessagesApi])
  val breadCrumb = inject[NispBreadcrumb]

  "Breadcrumb utils" should {
    "return a item text as Account Home and State Pension" in {
      val bc = breadCrumb.buildBreadCrumb(fakeRequestSP, messages)
      bc.lastItem.map(_.text) shouldBe Some("State Pension")
    }

    "return a item text as Account Home, State Pension and NI Record when URL is /account/nirecord/gaps" in {
      val bc = breadCrumb.buildBreadCrumb(fakeRequestNI, messages)
      val breadcrumbItem = s"Breadcrumb: BreadcrumbItem(Account home,$pertaxFrontendUrl), BreadcrumbItem(State Pension,/check-your-state-pension/account), lastItem: Some(BreadcrumbItem(NI record,/check-your-state-pension/account/nirecord))"
      bc.toString() shouldBe breadcrumbItem
    }

    "return a item text as Account Home, State Pension and NI Record when URL is /account/nirecord/voluntarycontribs" in {
      val bc = breadCrumb.buildBreadCrumb(fakeRequestVolContribution, messages)
      val breadcrumbItem = s"Breadcrumb: BreadcrumbItem(Account home,$pertaxFrontendUrl), BreadcrumbItem(State Pension,/check-your-state-pension/account), BreadcrumbItem(NI record,/check-your-state-pension/account/nirecord), lastItem: Some(BreadcrumbItem(Voluntary contributions,/check-your-state-pension/account/nirecord/voluntarycontribs))"
      bc.toString() shouldBe breadcrumbItem
    }

    "return a item text as Account Home, State Pension and NI Record when URL is /account/nirecord/gapsandhowtocheck" in {
      val bc = breadCrumb.buildBreadCrumb(fakeRequestHowToImproveGaps, messages)
      val breadcrumbItem = s"Breadcrumb: BreadcrumbItem(Account home,$pertaxFrontendUrl), BreadcrumbItem(State Pension,/check-your-state-pension/account), BreadcrumbItem(NI record,/check-your-state-pension/account/nirecord), lastItem: Some(BreadcrumbItem(Gaps in your record and how to check them,/check-your-state-pension/account/nirecord/gapsandhowtocheck))"
      bc.toString() shouldBe breadcrumbItem
    }
  }
}
