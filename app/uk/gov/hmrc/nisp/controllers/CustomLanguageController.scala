/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.Application
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.mvc.Call
import uk.gov.hmrc.play.language.LanguageController
import play.api.mvc._

class CustomLanguageController @Inject()(implicit override val messagesApi: MessagesApi, application: Application) extends LanguageController with I18nSupport {

  private val englishLang = Lang("en")
  private val welshLang = Lang("cy")

  /** Converts a string to a URL, using the route to this controller. **/
  def langToCall(lang: String): Call = uk.gov.hmrc.nisp.controllers.routes.CustomLanguageController.switchToLanguage(lang)

  /** Provides a fallback URL if there is no referrer in the request header. **/
  override protected def fallbackURL: String = routes.LandingController.show().url

  /** Returns a mapping between strings and the corresponding Lang object. **/
  override def languageMap: Map[String, Lang] = Map("english" -> englishLang,
    "cymraeg" -> welshLang)

  def switchToWelshLandingPage: Action[AnyContent] = Action { implicit request =>
    val enabled =  application.configuration.getBoolean("microservice.services.features.welsh-translation").getOrElse(true)
    val lang =  if (enabled) welshLang
    else englishLang
    Redirect(uk.gov.hmrc.nisp.controllers.routes.LandingController.show()).withLang(lang)
  }
}
