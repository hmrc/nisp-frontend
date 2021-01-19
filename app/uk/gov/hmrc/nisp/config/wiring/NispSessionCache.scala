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

import com.google.inject.Inject
import play.api.Mode.Mode
import play.api.{Configuration, Play}
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}

//TODO inject or allow bootstrap to deal with this
class NispSessionCache @Inject()(appConfig: ApplicationConfig,
                                 val http: HttpClient) extends SessionCache{
  override val defaultSource: String = appConfig.appName
  override val baseUri = appConfig.sessionCacheURL
  override val domain = appConfig.sessionCacheDomain
}
