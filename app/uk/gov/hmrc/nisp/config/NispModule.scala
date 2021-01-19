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

import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.http.{CorePost, HttpGet}
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.config.wiring.{MetricsService, NispAuditConnector, NispFormPartialRetriever, NispSessionCache, WSHttp}
import uk.gov.hmrc.nisp.connectors.NispAuthConnector
import uk.gov.hmrc.nisp.controllers.auth.{AuthAction, AuthActionImpl, VerifyAuthActionImpl}
import uk.gov.hmrc.nisp.services.MetricsService
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.partials.FormPartialRetriever

class NispModule extends Module {
    override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = Seq(
        //TODO Bootstrap should allow most of these to go
        //TODO look at the toInstance for further DI
        bind[PlayAuthConnector].to[NispAuthConnector],
        bind[HttpGet].to[WSHttp],
        bind[CorePost].to[WSHttp],
        bind[MetricsService].toInstance(MetricsService),
        bind[SessionCache].to[NispSessionCache],
        bind[AuditConnector].toInstance(NispAuditConnector),
        bind[FormPartialRetriever].toInstance(NispFormPartialRetriever),
        //TODO test
        if(configuration.getBoolean("microservice.services.features.identityVerification").getOrElse(false)){
            bind[AuthAction].to[AuthActionImpl]
        }else{
            bind[AuthAction].to[VerifyAuthActionImpl]
        }
    )
}
