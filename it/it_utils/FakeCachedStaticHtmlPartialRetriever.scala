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

package it_utils

import play.api.mvc.RequestHeader
import play.twirl.api.Html
import uk.gov.hmrc.http.HttpGet
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever

import scala.concurrent.duration.Duration

object FakeCachedStaticHtmlPartialRetriever extends CachedStaticHtmlPartialRetriever {
  override def httpGet: HttpGet = ???

  def getPartialContent(url: String, templateParameters: Map[String, String], errorMessage: Html)(implicit request: RequestHeader): Html =
    Html("")

  override def refreshAfter: Duration = ???

  override def expireAfter: Duration = ???

  override def maximumEntries: Int = ???
}