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

package uk.gov.hmrc.nisp.config

import com.google.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.renderer.TemplateRenderer
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class LocalTemplateRenderer @Inject() (appConfig: ApplicationConfig, http: HttpClient)(implicit
  val executionContext: ExecutionContext
) extends TemplateRenderer {
  override val templateServiceBaseUrl = appConfig.frontEndTemplateProviderBaseUrl
  override val refreshAfter: Duration = 10 minutes
  private implicit val hc             = HeaderCarrier()

  override def fetchTemplate(path: String): Future[String] = http.GET(path).map(_.body)
}
