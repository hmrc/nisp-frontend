/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.i18n.MessagesApi
import play.api.mvc.Request
import play.twirl.api.Html
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.views.html.{global_error, page_not_found_template, service_error_500}
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}
import uk.gov.hmrc.renderer.TemplateRenderer

class ErrorHandler @Inject()(applicationConfig: ApplicationConfig, serviceError500: service_error_500, pageNotFound: page_not_found_template,
                             globalError: global_error)
                            (implicit templateRenderer: TemplateRenderer,
                             formPartialRetriever: FormPartialRetriever,
                             partialRetriever: CachedStaticHtmlPartialRetriever,
                             val messagesApi: MessagesApi) extends FrontendErrorHandler {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html =
    globalError(pageTitle, heading, message, applicationConfig)

  override def internalServerErrorTemplate(implicit request: Request[_]): Html = serviceError500()

  override def notFoundTemplate(implicit request: Request[_]): Html = pageNotFound()

}
