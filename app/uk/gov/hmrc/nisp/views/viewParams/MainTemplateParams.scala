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

package uk.gov.hmrc.nisp.views.viewParams

import play.twirl.api.Html
import uk.gov.hmrc.nisp.config.ApplicationConfig

case class MainTemplateParams(
  browserTitle: Option[String] = None,
  pageInfo: Option[String] = None,
  pageTitle: Option[String] = None,
  h1Class: Option[String] = None,
  printableDocument: Boolean = false,
  sidebarLinks: Option[Html] = None,
  sidebarClasses: Option[String] = None,
  userLoggedIn: Boolean = false,
  applicationConfig: ApplicationConfig = ApplicationConfig,
  showTitleHeaderNav: Boolean = true,
  showBetaBanner: Boolean = false,
  pageScripts: Option[Html] = None,
  articleClasses: Option[String] = None,
  gaDimensions: Option[Map[String, Any]] = None,
  analyticsAdditionalJs: Option[Html] = None,
  articleEnabled: Boolean = true,
  hideBreadcrumb: Boolean = false,
  showUrBanner: Boolean = false,
  hideNavBar: Boolean = false
)
