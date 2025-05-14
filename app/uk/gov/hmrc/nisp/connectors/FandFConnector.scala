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
import com.google.inject.Inject
import play.api.libs.json.Reads
import uk.gov.hmrc.auth.core.retrieve.v2.TrustedHelper
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.nisp.config.ApplicationConfig

import scala.concurrent.{ExecutionContext, Future}

class FandFConnector @Inject()(
  http: HttpClientV2,
  appConfig: ApplicationConfig
)(implicit
  executionContext: ExecutionContext
) {

  private val serviceUrl: String = appConfig.fandfServiceUrl
  private implicit val reads: Reads[TrustedHelper] = TrustedHelper.reads

  def getTrustedHelper()(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, TrustedHelper] = {
    EitherT (
      http
        .get(url"$serviceUrl/delegation/get")
        .execute[Either[UpstreamErrorResponse, TrustedHelper]]
        .recover {
          case error: HttpException =>
            Left(UpstreamErrorResponse(error.message, error.responseCode))
          case error =>
            throw error
        }
    )
  }
}
