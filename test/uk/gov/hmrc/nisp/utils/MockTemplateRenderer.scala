/*
 * Copyright 2017 HM Revenue & Customs
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

import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.nisp.config.{LocalTemplateRenderer, WsAllMethods}

import scala.concurrent.duration._

object MockTemplateRenderer extends LocalTemplateRenderer {
  override lazy val templateServiceBaseUrl = "http://example.com/template/mustache"
  override val refreshAfter = 10 minutes
  override val wsHttp = MockitoSugar.mock[WsAllMethods]

  override def renderDefaultTemplate(content: Html, extraArgs: Map[String, Any])(implicit messages: Messages) = {
    Html("<title>" + extraArgs("pageTitle") + "</title>"+ "<sidebar>"+extraArgs("sidebar")+"</sidebar>" + "<navLinks>"+extraArgs("navLinks")+"</navLinks>"+ "<mainContentHeader>"+extraArgs("mainContentHeader")+"</mainContentHeader>"+ content)
  }
}
