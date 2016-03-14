/*
 * Copyright 2016 HM Revenue & Customs
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

import java.net.URLEncoder

import play.api.i18n.Messages
import play.api.mvc.Request
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.auth.NispUser
import uk.gov.hmrc.nisp.controllers.routes

object Breadcrumb extends Breadcrumb {
  override lazy val applicationConfig = ApplicationConfig
}

trait Breadcrumb {

  val applicationConfig: ApplicationConfig

  val initialBreadCrumbList = List((URLEncoder.encode(Messages("nisp.breadcrumb.account"),"UTF-8"), applicationConfig.pertaxFrontendUrl))
  lazy val mainContentHeaderPartialUrl = s"${applicationConfig.pertaxFrontendUrl}/integration/main-content-header"

  private def buildBreadCrumb(request: Request[_]): List[(String, String)] = {
    val links = Map(
      "account" -> ((URLEncoder.encode(Messages("nisp.breadcrumb.pension"), "UTF-8"), routes.AccountController.show().url)),
      "nirecord" -> ((URLEncoder.encode(Messages("nisp.breadcrumb.nirecord"), "UTF-8"), routes.NIRecordController.showGaps().url)),
      "voluntarycontribs" -> ((URLEncoder.encode(Messages("nisp.breadcrumb.nirecord.voluntaryContrib"), "UTF-8"), routes.NIRecordController.showVoluntaryContributions().url)),
      "gapsandhowtocheck" -> ((URLEncoder.encode(Messages("nisp.breadcrumb.nirecord.gapsandhowtocheck"), "UTF-8"), routes.NIRecordController.showGapsAndHowToCheckThem().url)),
      "exclusion" -> ((URLEncoder.encode(Messages("nisp.breadcrumb.account"), "UTF-8"), applicationConfig.pertaxFrontendUrl))
    )

    val items: List[Option[(String, String)]] = request.path.split("/").filter(!_.isEmpty).map(links.get).toList
    initialBreadCrumbList ::: items.flatten
  }

  def generateHeaderUrl() (implicit request:Request[_], user: NispUser): String = {
    mainContentHeaderPartialUrl + "?name=" + s"${URLEncoder.encode(user.name.getOrElse(""),"UTF-8")}" + "&" +
      user.previouslyLoggedInAt.map("lastLogin=" + _.getMillis + "&").getOrElse("") +
      buildBreadCrumb(request).map{case (name: String, url: String) => s"item_text=$name&item_url=$url"}.mkString("&") +
      "&showBetaBanner=true&deskProToken='NISP'"
  }
}
