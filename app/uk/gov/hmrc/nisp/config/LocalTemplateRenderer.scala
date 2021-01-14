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

import akka.actor.ActorSystem
import com.typesafe.config.Config
import play.api.{Configuration, Play}
import play.api.Mode.Mode
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.play.config.{AppName, RunMode, ServicesConfig}
import uk.gov.hmrc.nisp.config.wiring.{NispAuditConnector, WSHttp}
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future
import scala.concurrent.duration._
import uk.gov.hmrc.http.HeaderCarrier

trait LocalTemplateRenderer extends TemplateRenderer with ServicesConfig {
  override lazy val templateServiceBaseUrl = baseUrl("frontend-template-provider")
  override val refreshAfter: Duration = 10 minutes
  private implicit val hc = HeaderCarrier()
  val wsHttp: WSHttp

  override def fetchTemplate(path: String): Future[String] =  {
    wsHttp.GET(path).map(_.body)
  }
}

object LocalTemplateRenderer extends LocalTemplateRenderer {
  override val wsHttp = WsAllMethods
  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration
}

trait WsAllMethods extends WSHttp with HttpAuditing with AppName with RunMode

object WsAllMethods extends WsAllMethods {
  override lazy val auditConnector = NispAuditConnector
  override val hooks = Seq (AuditingHook)

  override protected def appNameConfiguration: Configuration = Play.current.configuration
  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration
  override protected def actorSystem: ActorSystem = Play.current.actorSystem
  override protected def configuration: Option[Config] = Option(Play.current.configuration.underlying)
}
