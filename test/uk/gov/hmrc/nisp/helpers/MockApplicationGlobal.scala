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

import play.api.{Configuration, Play}
import play.api.Mode.Mode
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.nisp.config.ApplicationGlobalTrait
import uk.gov.hmrc.nisp.utils.MockTemplateRenderer
import uk.gov.hmrc.renderer.TemplateRenderer

object MockApplicationGlobal extends ApplicationGlobalTrait with I18nSupport {
  override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration
  override def messagesApi = Play.current.injector.instanceOf[MessagesApi]
}
