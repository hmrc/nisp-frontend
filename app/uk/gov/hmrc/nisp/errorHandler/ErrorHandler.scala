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

package uk.gov.hmrc.nisp.errorHandler

import com.google.inject.Inject
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Request
import play.twirl.api.Html
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.play.bootstrap.http.FrontendErrorHandler
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}
import uk.gov.hmrc.renderer.TemplateRenderer

//TODO test
class ErrorHandler @Inject()(applicationConfig: ApplicationConfig)
                            (implicit templateRenderer: TemplateRenderer,
                             formPartialRetriever: FormPartialRetriever,
                             val partialRetriever: CachedStaticHtmlPartialRetriever,
                             val messagesApi: MessagesApi) extends FrontendErrorHandler {
  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html =
  uk.gov.hmrc.nisp.views.html.global_error(pageTitle, heading, message, applicationConfig)

  //TODO should bootstrap be dealing with this, test this
  override def internalServerErrorTemplate(implicit request: Request[_]): Html = uk.gov.hmrc.nisp.views.html.service_error_500()
}
