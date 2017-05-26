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

package uk.gov.hmrc.nisp.controllers

import play.api.mvc.{Request, Result}
import play.api.{Logger, PlayException}
import uk.gov.hmrc.nisp.config.{ApplicationGlobal, NispContext, NispContextImpl}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever

trait NispFrontendController extends FrontendController {
  val logger: Logger = Logger(this.getClass)

  implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever
  implicit val context: NispContext = NispContextImpl


  def onError(ex: Exception)(implicit request: Request[_]): Result = {
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
    InternalServerError(ApplicationGlobal.internalServerErrorTemplate)
  }

}
