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

package uk.gov.hmrc.nisp.errorHandler

import com.google.inject.Inject
import play.api.http.HeaderNames.CACHE_CONTROL
import play.api.i18n.MessagesApi
import play.api.mvc.Results.InternalServerError
import play.api.mvc.{Request, RequestHeader, Result}
import play.api.{Logger, PlayException}
import play.twirl.api.Html
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.nisp.models.admin.ExcessiveTrafficToggle
import uk.gov.hmrc.nisp.views.html.{global_error, page_not_found_template, service_error_500}
import uk.gov.hmrc.play.bootstrap.frontend.http.{ApplicationException, FrontendErrorHandler}

import scala.concurrent.{ExecutionContext, Future}

class ErrorHandler @Inject() (
  serviceError500: service_error_500,
  pageNotFound: page_not_found_template,
  globalError: global_error,
  featureFlagService: FeatureFlagService
)(implicit
  val messagesApi: MessagesApi,
  ec: ExecutionContext
) extends FrontendErrorHandler {

  private val logger = Logger(getClass)

  private def rhToRequest(rh: RequestHeader): Request[_] = Request(rh, "")

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit
    request: Request[_]
  ): Html =
    globalError(pageTitle, heading, message)

  def internalServerErrorTemplateFuture(implicit request: Request[_]): Future[Html] = {
    featureFlagService.get(ExcessiveTrafficToggle).map{featureFlag =>
      serviceError500(showExcessiveTrafficMessage = featureFlag.isEnabled)
    }
  }

  private def logError(request: RequestHeader, ex: Throwable): Unit =
    logger.error(
      """
        |
        |! %sInternal server error, for (%s) [%s] ->
        | """.stripMargin.format(ex match {
        case p: PlayException => "@" + p.id + " - "
        case _ => ""
      }, request.method, request.uri),
      ex
    )

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    logError(request, exception)
    resolveErrorFuture(request, exception)
  }

  private def resolveErrorFuture(rh: RequestHeader, ex: Throwable): Future[Result] = ex match {
    case ApplicationException(result, _) => Future.successful(result)
    case _ =>
      internalServerErrorTemplateFuture(rhToRequest(rh)).map(page => InternalServerError(page).withHeaders(CACHE_CONTROL -> "no-cache"))
  }

  override def notFoundTemplate(implicit request: Request[_]): Html = pageNotFound()

}
