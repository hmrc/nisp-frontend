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

import javax.inject.Inject
import play.api.i18n.{Lang, MessagesApi}
import play.api.mvc.{Call, ControllerComponents}
import uk.gov.hmrc.play.language.{LanguageController, LanguageUtils}

class CustomLanguageController @Inject() (implicit
  override val messagesApi: MessagesApi,
  languageUtils: LanguageUtils,
  languageService: LanguageService,
  val cc: ControllerComponents
) extends LanguageController(languageUtils, cc) {

  /** Provides a fallback URL if there is no referrer in the request header. * */
  override protected def fallbackURL: String = routes.LandingController.show.url

  /** Returns a mapping between strings and the corresponding Lang object. * */
  override def languageMap: Map[String, Lang] = languageService.languageMap
}

class LanguageService @Inject() () {
  def routeToSwitchLanguage: String => Call = (lang: String) => routes.CustomLanguageController.switchToLanguage(lang)

  /** Returns a mapping between strings and the corresponding Lang object. * */
  def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy")
  )
}
