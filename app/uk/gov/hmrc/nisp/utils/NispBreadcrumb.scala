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

import play.api.i18n.Messages
import play.api.mvc.Request
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.routes
import uk.gov.hmrc.play.breadcrumb.model.{Breadcrumb, BreadcrumbItem}

object NispBreadcrumb extends NispBreadcrumb {
  override lazy val applicationConfig = ApplicationConfig
}

trait NispBreadcrumb{

  val applicationConfig: ApplicationConfig

  def initialBreadCrumbList(implicit messages: Messages) = List((messages("nisp.breadcrumb.account"), applicationConfig.pertaxFrontendUrl))

  lazy val mainContentHeaderPartialUrl = applicationConfig.breadcrumbPartialUrl

   def buildBreadCrumb(implicit request: Request[_], messages: Messages): Breadcrumb = {
    val links = Map(
      "account" -> (Messages("nisp.breadcrumb.pension"), routes.StatePensionController.show().url),
      "nirecord" -> (Messages("nisp.breadcrumb.nirecord"), routes.NIRecordController.showFull().url),
      "voluntarycontribs" -> (Messages("nisp.breadcrumb.nirecord.voluntaryContrib"), routes.NIRecordController.showVoluntaryContributions().url),
      "gapsandhowtocheck" -> (Messages("nisp.breadcrumb.nirecord.gapsandhowtocheck"), routes.NIRecordController.showGapsAndHowToCheckThem().url),
      "exclusion" -> (Messages("nisp.breadcrumb.excluded"), routes.ExclusionController.showSP().url),
      "exclusionni" -> (Messages("nisp.breadcrumb.excluded"), routes.ExclusionController.showNI().url),
      "cope" -> (Messages("nisp.breadcrumb.cope"), routes.StatePensionController.showCope().url)
    )

    val items: List[Option[(String, String)]] = request.path.split("/").filter(!_.isEmpty).map(links.get).toList
    val breacrumList = initialBreadCrumbList ::: items.flatten

    val bcItems: Seq[BreadcrumbItem] = breacrumList.map( { case(label, url) => BreadcrumbItem(label, url) })
    Breadcrumb(bcItems.toVector)
  }

}
