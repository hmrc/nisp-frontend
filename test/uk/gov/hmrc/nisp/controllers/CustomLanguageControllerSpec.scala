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

package uk.gov.hmrc.nisp.controllers

import java.util.UUID

import org.scalatestplus.play.OneAppPerSuite
import play.api.mvc.Result
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec
import play.api.Play
import play.api.Play._
import play.api.i18n.Lang
import play.api.libs.ws.WS
import play.api.mvc.Cookie
import play.api.test._
import play.api.test.Helpers._
import uk.gov.hmrc.nisp.builders.ApplicationConfigBuilder
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.play.language.LanguageController
import uk.gov.hmrc.play.language.LanguageUtils._


class CustomLanguageControllerSpec extends UnitSpec with OneAppPerSuite {
  val fakeRequest = FakeRequest()

  private val refererValue  = "http://gov.uk"
  private val fallbackValue = "http://gov.uk/fallback"

  val mockLanguageController = new CustomLanguageController {
    override val applicationConfig: ApplicationConfig = ApplicationConfigBuilder()
    override protected def fallbackURL: String  = fallbackValue
    override def languageMap: Map[String, Lang] = Map("english" -> English,
      "cymraeg" -> Welsh)
  }

  abstract class ServerWithConfig(conf: Map[String, String] = Map.empty) extends
    WithServer(FakeApplication(additionalConfiguration = conf))

  "The switch language endpoint" should {

    "respond with a See Other (303) status when a referer is in the header." in
      new ServerWithConfig() {
        val request = FakeRequest().withHeaders(REFERER -> refererValue)
        val res = mockLanguageController.switchToLanguage("english")(request)
        status(res) should be (SEE_OTHER)
      }

    "respond with a See Other (303) status when no referer is in the header." in
      new ServerWithConfig() {
        val res = mockLanguageController.switchToLanguage("english")(FakeRequest())
        status(res) should be (SEE_OTHER)
      }

    "set the redirect location to the value of the referer header." in
      new ServerWithConfig() {
        val request = FakeRequest().withHeaders(REFERER -> refererValue)
        val res = mockLanguageController.switchToLanguage("english")(request)
        redirectLocation(res) should be (Some(refererValue))
      }

    "set the redirect location to the fallback value when no referer is in the header." in
      new ServerWithConfig() {
        val res = mockLanguageController.switchToLanguage("english")(FakeRequest())
        redirectLocation(res) should be (Some(fallbackValue))
      }

    "should set the language in a cookie." in
      new ServerWithConfig() {
        val res = mockLanguageController.switchToLanguage("english")(FakeRequest())
        cookies(res).get(Play.langCookieName) match {
          case Some(c: Cookie) => c.value should be (EnglishLangCode)
          case _ => fail("PLAY_LANG cookie was not found.")
        }
      }

    "should not change to welsh with feature flag is set to false" in {

      val mockController = new CustomLanguageController {
        override val applicationConfig: ApplicationConfig = ApplicationConfigBuilder(isWelshEnabled = false)
      }

      new ServerWithConfig() {
        val res = mockController.switchToLanguage("cymraeg")(FakeRequest())
        cookies(res).get(Play.langCookieName) match {
          case Some(c: Cookie) => c.value should be (EnglishLangCode)
          case _ => fail("PLAY_LANG cookie was not found.")
        }
      }
    }
  }
}
