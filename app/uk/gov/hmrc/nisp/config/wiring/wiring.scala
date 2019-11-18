/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.config.wiring

import org.joda.time.DateTime
import play.api.Mode.Mode
import play.api.{Configuration, Play}
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.http.{CorePost, HttpGet}
import uk.gov.hmrc.nisp.connectors.{CitizenDetailsConnector, IdentityVerificationConnector, NationalInsuranceConnector, StatePensionConnector}
import uk.gov.hmrc.nisp.controllers.connectors.CustomAuditConnector
import uk.gov.hmrc.nisp.services.{MetricsService, NationalInsuranceConnection, NationalInsuranceService, StatePensionConnection, StatePensionService}
import uk.gov.hmrc.play.config.ServicesConfig

object ServicesConfigToBeDId extends ServicesConfig {
  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration
}

class NispAuthConnector extends PlayAuthConnector {
  override lazy val serviceUrl: String = ServicesConfigToBeDId.baseUrl("auth")

  override def http: CorePost = WSHttp
}

object NispAuthConnector extends NispAuthConnector

object CustomAuditConnector extends CustomAuditConnector {
  override lazy val auditConnector: NispAuditConnector.type = NispAuditConnector
}

object CitizenDetailsConnector extends CitizenDetailsConnector {
  override lazy val serviceUrl: String = ServicesConfigToBeDId.baseUrl("citizen-details")
  override val metricsService: MetricsService = MetricsService
  override def http: HttpGet = WSHttp
}

object IdentityVerificationConnector extends IdentityVerificationConnector {
  override val serviceUrl: String = ServicesConfigToBeDId.baseUrl("identity-verification")
  override def http: HttpGet = WSHttp
  override val metricsService: MetricsService = MetricsService
}

object NationalInsuranceConnector extends NationalInsuranceConnector {
  override val serviceUrl: String = ServicesConfigToBeDId.baseUrl("national-insurance")
  override def http: HttpGet = WSHttp
  override def sessionCache: SessionCache = NispSessionCache
  override val metricsService: MetricsService = MetricsService
}

object NationalInsuranceService extends NationalInsuranceService with NationalInsuranceConnection {
  override val nationalInsuranceConnector: NationalInsuranceConnector = NationalInsuranceConnector
}

object StatePensionConnector extends StatePensionConnector {
  override val serviceUrl: String = ServicesConfigToBeDId.baseUrl("state-pension")
  override def http: HttpGet = WSHttp
  override def sessionCache: SessionCache = NispSessionCache
  override val metricsService: MetricsService = MetricsService
}

object StatePensionService extends StatePensionService with StatePensionConnection {
  override def now: () => DateTime = () => DateTime.now(ukTime)
  override val statePensionConnector: StatePensionConnector = StatePensionConnector
}

