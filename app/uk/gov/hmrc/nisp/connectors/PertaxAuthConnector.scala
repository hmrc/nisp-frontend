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

package uk.gov.hmrc.nisp.connectors

import cats.data.EitherT
import cats.implicits.catsStdInstancesForFuture
import com.google.inject.ImplementedBy
import play.api.Logging
import play.api.http.HeaderNames
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.connectors.httpParsers.PertaxAuthenticationHttpParser._
import uk.gov.hmrc.nisp.models.pertaxAuth.PertaxAuthResponseModel
import uk.gov.hmrc.play.partials.HtmlPartial

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PertaxAuthConnectorImpl @Inject()(
  http: HttpClientV2,
  appConfig: ApplicationConfig,
  httpClientResponse: HttpClientResponse
)(
  implicit
  ec: ExecutionContext
) extends PertaxAuthConnector with Logging {

  override def authorise(nino: String)(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, PertaxAuthResponseModel]] = {
    val authUrl = url"${appConfig.pertaxAuthBaseUrl}/pertax/$nino/authorise"
    http
      .get(authUrl)
      .setHeader(HeaderNames.ACCEPT -> "application/vnd.hmrc.1.0+json")
      .execute[Either[UpstreamErrorResponse, PertaxAuthResponseModel]]
  }

  def pertaxPostAuthorise(implicit
                          hc: HeaderCarrier,
                          ec: ExecutionContext
                         ): EitherT[Future, UpstreamErrorResponse, PertaxAuthResponseModel] = {
    httpClientResponse
      .read(
        http
          .post(url"${appConfig.pertaxAuthBaseUrl}/pertax/authorise")
          .setHeader(HeaderNames.ACCEPT -> "application/vnd.hmrc.2.0+json")
          .execute[Either[UpstreamErrorResponse, HttpResponse]]
      )
      .map(_.json.as[PertaxAuthResponseModel])
  }

  override def loadPartial(partialContextUrl: String)(implicit hc: HeaderCarrier): Future[HtmlPartial] = {
    val partialUrl =
      appConfig.pertaxAuthBaseUrl + s"${if (partialContextUrl.charAt(0).toString == "/") partialContextUrl else s"/$partialContextUrl"}"

    http
      .get(url"$partialUrl")
      .execute[HtmlPartial]
      .map {
        case partialSuccess: HtmlPartial.Success => partialSuccess
        case partialFailure: HtmlPartial.Failure =>
          logger.error(s"[PertaxAuthConnector][loadPartial] Failed to load Partial from partial url '$partialUrl'. " +
            s"Partial info: $partialFailure, body: ${partialFailure.body}")
          partialFailure
      }.recover {
        case exception: HttpException => HtmlPartial.Failure(Some(exception.responseCode))
        case _ => HtmlPartial.Failure(None)
      }
  }

}

@ImplementedBy(classOf[PertaxAuthConnectorImpl])
trait PertaxAuthConnector {
  def authorise(nino: String)(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, PertaxAuthResponseModel]]

  def pertaxPostAuthorise(implicit hc: HeaderCarrier, ec: ExecutionContext): EitherT[Future, UpstreamErrorResponse, PertaxAuthResponseModel]

  def loadPartial(partialContextUrl: String)(implicit hc: HeaderCarrier): Future[HtmlPartial]
}
