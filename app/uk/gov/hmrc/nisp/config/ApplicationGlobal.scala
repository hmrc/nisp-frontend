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

import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import play.api.Mode.Mode
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.Request
import play.api.{Application, Configuration, Play}
import play.twirl.api.Html
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.nisp.config.wiring.NispAuditConnector
import uk.gov.hmrc.nisp.controllers.NispFrontendController
import uk.gov.hmrc.nisp.controllers.partial.PartialRetriever
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, RunMode}
import uk.gov.hmrc.play.frontend.bootstrap.DefaultFrontendGlobal
import uk.gov.hmrc.play.frontend.filters.{FrontendAuditFilter, FrontendLoggingFilter, MicroserviceFilterSupport}

object ApplicationGlobal extends ApplicationGlobalTrait {
  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration
  override def messagesApi = Play.current.injector.instanceOf[MessagesApi]
}

trait ApplicationGlobalTrait extends DefaultFrontendGlobal with RunMode with PartialRetriever with NispFrontendController with I18nSupport {

  implicit lazy val app:Application = Play.current

  override val auditConnector = NispAuditConnector
  override val loggingFilter = NispLoggingFilter
  override val frontendAuditFilter = NispFrontendAuditFilter

  override def onStart(app: Application) {
    super.onStart(app)
    new ApplicationCrypto(Play.current.configuration.underlying).verifyConfiguration()
  }

  override def internalServerErrorTemplate(implicit request: Request[_]): Html =
    uk.gov.hmrc.nisp.views.html.service_error_500()

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html =
    uk.gov.hmrc.nisp.views.html.global_error(pageTitle, heading, message)

  override def notFoundTemplate(implicit request: Request[_]): Html = {
    uk.gov.hmrc.nisp.views.html.page_not_found_template()
  }

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = app.configuration.getConfig(s"microservice.metrics")
}

object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs = Play.current.configuration.underlying.as[Config]("controllers")
}

object NispLoggingFilter extends FrontendLoggingFilter with MicroserviceFilterSupport {
  override def controllerNeedsLogging(controllerName: String): Boolean = ControllerConfiguration.paramsForController(controllerName).needsLogging
}

object NispFrontendAuditFilter extends FrontendAuditFilter with RunMode with AppName with MicroserviceFilterSupport {
  override lazy val maskedFormFields = Seq.empty
  override lazy val applicationPort = None
  override lazy val auditConnector = NispAuditConnector
  override def controllerNeedsAuditing(controllerName: String): Boolean = ControllerConfiguration.paramsForController(controllerName).needsAuditing
  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration
  override protected def appNameConfiguration: Configuration = Play.current.configuration
}