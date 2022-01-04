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

package uk.gov.hmrc.nisp.controllers

import com.google.inject.Inject
import play.api.http.{Status => HttpStatus}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.api.Logging
import play.twirl.api.Html
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, HttpResponse}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.views.html.{feedback, feedback_thankyou}
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever, HeaderCarrierForPartialsConverter}
import uk.gov.hmrc.renderer.TemplateRenderer

import java.net.URLEncoder
import scala.concurrent.{ExecutionContext, Future}

class FeedbackController @Inject()(applicationConfig: ApplicationConfig,
                                   httpClient: HttpClient,
                                   executionContext: ExecutionContext,
                                   nispHeaderCarrierForPartialsConverter: HeaderCarrierForPartialsConverter,
                                   mcc: MessagesControllerComponents,
                                   feedbackThankYou: feedback_thankyou,
                                   feedback: feedback)
                                   (implicit val formPartialRetriever: FormPartialRetriever,
                                    val templateRenderer: TemplateRenderer,
                                    val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever,
                                    executor: ExecutionContext) extends NispFrontendController(mcc) with I18nSupport with Logging {


  def contactFormReferer(implicit request: Request[AnyContent]): String = request.headers.get(REFERER).getOrElse("")
  def localSubmitUrl(implicit request: Request[AnyContent]): String = routes.FeedbackController.submit.url

  private val TICKET_ID = "ticketId"

  private def feedbackFormPartialUrl(implicit request: Request[AnyContent]) =
    s"${applicationConfig.contactFrontendPartialBaseUrl}/contact/beta-feedback/form/?submitUrl=${urlEncode(localSubmitUrl)}" +
      s"&service=${urlEncode(applicationConfig.contactFormServiceIdentifier)}&referer=${urlEncode(contactFormReferer)}"

  private def feedbackHmrcSubmitPartialUrl(implicit request: Request[AnyContent]) =
    s"${applicationConfig.contactFrontendPartialBaseUrl}/contact/beta-feedback/form?resubmitUrl=${urlEncode(localSubmitUrl)}"

  private def feedbackThankYouPartialUrl(ticketId: String)(implicit request: Request[AnyContent]) =
    s"${applicationConfig.contactFrontendPartialBaseUrl}/contact/beta-feedback/form/confirmation?ticketId=${urlEncode(ticketId)}"

  def show: Action[AnyContent] = Action {
    implicit request =>
      (request.session.get(REFERER), request.headers.get(REFERER)) match {
        case (None, Some(ref)) =>
          Ok(feedback(feedbackFormPartialUrl, None)).withSession(request.session + (REFERER -> ref))
        case _ =>
          Ok(feedback(feedbackFormPartialUrl, None))
      }
  }

  def submit: Action[AnyContent] = Action.async {
    implicit request =>
      request.body.asFormUrlEncoded.map { formData =>
        httpClient.POSTForm[HttpResponse](feedbackHmrcSubmitPartialUrl, formData)(rds = PartialsFormReads.readPartialsForm, hc = partialsReadyHeaderCarrier, ec = executionContext).map {
          resp =>
            resp.status match {
              case HttpStatus.OK => Redirect(routes.FeedbackController.showThankYou).withSession(request.session + (TICKET_ID -> resp.body))
              case HttpStatus.BAD_REQUEST => BadRequest(feedback(feedbackFormPartialUrl, Some(Html(resp.body))))
              case status => logger.warn(s"Unexpected status code from feedback form: $status"); InternalServerError
            }
        }
      }.getOrElse {
        logger.warn("Trying to submit an empty feedback form")
        Future.successful(InternalServerError)
      }
  }

  def showThankYou: Action[AnyContent] = Action {
    implicit request =>
      val ticketId = request.session.get(TICKET_ID).getOrElse("N/A")
      val referer = request.session.get(REFERER).getOrElse("/")
      Ok(feedbackThankYou(feedbackThankYouPartialUrl(ticketId), referer)).withSession(request.session - REFERER)
  }

  private def urlEncode(value: String) = URLEncoder.encode(value, "UTF-8")

  private def partialsReadyHeaderCarrier(implicit request: Request[_]): HeaderCarrier = {
    val hc1 = nispHeaderCarrierForPartialsConverter.headerCarrierEncryptingSessionCookieFromRequest(request)
    nispHeaderCarrierForPartialsConverter.headerCarrierForPartialsToHeaderCarrier(hc1)
  }

}

object PartialsFormReads {
  implicit val readPartialsForm: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    def read(method: String, url: String, response: HttpResponse): HttpResponse = response
  }
}
