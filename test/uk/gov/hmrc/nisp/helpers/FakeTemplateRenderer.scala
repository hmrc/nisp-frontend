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

package uk.gov.hmrc.nisp.helpers

import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.renderer.TemplateRenderer
import scala.concurrent.duration._
import scala.concurrent.Future

object FakeTemplateRenderer extends TemplateRenderer {

  override lazy val templateServiceBaseUrl = "http://example.com/template/mustache"
  override val refreshAfter = 10 minutes

  override def renderDefaultTemplate(path:String, content: Html, extraArgs: Map[String, Any])(implicit messages: Messages): Html = {
    Html(
      "<title>" + extraArgs("pageTitle") + "</title>"
        + "<sidebar>"+extraArgs("sidebar")+"</sidebar>"
        + "<navLinks>"+extraArgs("navLinks")+"</navLinks>"
        + displayUrBanner(extraArgs) +
        "<mainContentHeader>" +extraArgs("mainContentHeader")+ "</mainContentHeader>"
        + "<mainContent>" + content + "</mainContent>")
  }

    def displayUrBanner(extraArgs: Map[String, Any]): String ={
      if(extraArgs.contains("fullWidthBannerTitle")){
        "<div id=full-width-banner>" + "<div class = \"full-width-banner__title\">" + extraArgs("fullWidthBannerTitle") + "</div>" + "<div id = fullWidthBannerLink>" + extraArgs("fullWidthBannerLink") +  "</div>"+ "<div>" + extraArgs("fullWidthBannerText")+ "</div>"+ "<div id = fullWidthBannerDismissText>"+extraArgs("fullWidthBannerDismissText")+"</div>"
      }
      else ""
    }

  override def fetchTemplate(path: String): Future[String] = ???
}
