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

package uk.gov.hmrc.nisp.config.wiring

import com.codahale.metrics.Timer.Context
import org.joda.time.DateTime
import play.api.Mode.Mode
import play.api.{Configuration, Play}
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.http.{CorePost, HttpGet}
import uk.gov.hmrc.nisp.connectors.{CitizenDetailsConnector, IdentityVerificationConnector, NationalInsuranceConnector, StatePensionConnector}
import uk.gov.hmrc.nisp.controllers.connectors.CustomAuditConnector
import uk.gov.hmrc.nisp.models.enums.APIType
import uk.gov.hmrc.nisp.models.enums.APIType.APIType
import uk.gov.hmrc.nisp.services.{MetricsService, NationalInsuranceConnection, NationalInsuranceService, StatePensionConnection, StatePensionService}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.graphite.MicroserviceMetrics

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

object MetricsService extends MetricsService with MicroserviceMetrics {

  val timers = Map(
    APIType.SP -> metrics.defaultRegistry.timer("sp-response-timer"),
    APIType.NI -> metrics.defaultRegistry.timer("ni-response-timer"),
    APIType.SchemeMembership -> metrics.defaultRegistry.timer("scheme-membership-response-timer"),
    APIType.StatePension -> metrics.defaultRegistry.timer("state-pension-response-timer"),
    APIType.NationalInsurance -> metrics.defaultRegistry.timer("national-insurance-response-timer")
  )

  val failedCounters = Map(
    APIType.SP -> metrics.defaultRegistry.counter("sp-failed-counter"),
    APIType.NI -> metrics.defaultRegistry.counter("ni-failed-counter"),
    APIType.SchemeMembership -> metrics.defaultRegistry.counter("scheme-membership-failed-counter"),
    APIType.StatePension -> metrics.defaultRegistry.counter("state-pension-failed-counter"),
    APIType.NationalInsurance -> metrics.defaultRegistry.counter("national-insurance-failed-counter")
  )

  override val keystoreReadTimer = metrics.defaultRegistry.timer("keystore-read-timer")
  override val keystoreWriteTimer = metrics.defaultRegistry.timer("keystore-write-timer")

  override val keystoreReadFailed = metrics.defaultRegistry.counter("keystore-read-failed-counter")
  override val keystoreWriteFailed = metrics.defaultRegistry.counter("keystore-write-failed-counter")

  override val keystoreHitCounter = metrics.defaultRegistry.counter("keystore-hit-counter")
  override val keystoreMissCounter = metrics.defaultRegistry.counter("keystore-miss-counter")

  override val identityVerificationTimer = metrics.defaultRegistry.timer("identity-verification-timer")
  override val identityVerificationFailedCounter = metrics.defaultRegistry.counter("identity-verification-failed-counter")

  override val citizenDetailsTimer = metrics.defaultRegistry.timer("citizen-details-timer")
  override val citizenDetailsFailedCounter = metrics.defaultRegistry.counter("citizen-details-failed-counter")

  override def startTimer(api: APIType): Context = timers(api).time()

  override def incrementFailedCounter(api: APIType): Unit = failedCounters(api).inc()
}
