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

package uk.gov.hmrc.nisp.views

import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.{Call, PathBindable}
import play.api.test.FakeApplication
import play.api.test.Helpers.{contentAsString, _}
import uk.gov.hmrc.nisp.views.html.HtmlSpec
import uk.gov.hmrc.play.language.LanguageUtils.{English, Welsh}
import uk.gov.hmrc.play.test.UnitSpec

class LanguageSelectionSpec extends UnitSpec with MockitoSugar with HtmlSpec with BeforeAndAfter with OneAppPerSuite {

  def languageMap: Map[String, Lang] = Map("english" -> English,
    "cymraeg" -> Welsh)

  def langToUrl(lang: String): Call = Call("GET", "/language/" + implicitly[PathBindable[String]].unbind("lang", lang))

  "Language selection template view" should {

    "give a link to switch to Welsh when current language is English" in {
      val doc = html.includes.language_selection(languageMap, langToUrl(_), None, None)(English)
      contentAsString(doc) should include(Messages("id=\"cymraeg-switch\""))
      contentAsString(doc) should include("/language/cymraeg")
    }

    "show correct current language message when current language is English" in running(new FakeApplication) {
      val doc = html.includes.language_selection(languageMap, langToUrl(_), None, None)(English)
      contentAsString(doc) should include("English")
      contentAsString(doc) should not include ">English<"
    }

    "give a link to switch to English when current language is Welsh" in {
      val doc = html.includes.language_selection(languageMap, langToUrl(_), None, None)(Welsh)
      contentAsString(doc) should include(Messages("id=\"english-switch\""))
      contentAsString(doc) should include ("/language/english")
    }

    "show correct current language message when current language is Welsh" in running(new FakeApplication) {
      val doc = html.includes.language_selection(languageMap, langToUrl(_), None, None)(Welsh)
      contentAsString(doc) should include(Messages("Cymraeg"))
      contentAsString(doc) should not include ">Cymraeg<"
    }

    "show a custom class if it is set" in running(new FakeApplication) {
      val doc = html.includes.language_selection(languageMap, langToUrl(_), Some("align-right"), None)(Welsh)
      contentAsString(doc) should include("class=\"align-right\"")
    }

    "show a data-journey-click attribute for GA if it is set and language is Welsh" in running(new FakeApplication) {
      val doc = html.includes.language_selection(languageMap, langToUrl(_), Some("align-right"), Some("checkmystatepension"))(Welsh)
      contentAsString(doc) should include("data-journey-click=\"checkmystatepension:language: en\"")
    }

    "show a data-journey-click attribute for GA if it is set and language is English" in running(new FakeApplication) {
     val doc = html.includes.language_selection(languageMap, langToUrl(_), Some("align-right"), Some("checkmystatepension"))(English)
      contentAsString(doc) should include("data-journey-click=\"checkmystatepension:language: cy-GB\"")
    }
  }
}
