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

package uk.gov.hmrc.nisp.controllers.auth

import com.google.inject.Inject
import com.google.inject.name.Named
import uk.gov.hmrc.nisp.config.ApplicationConfig

class AuthActionSelector @Inject()(applicationConfig: ApplicationConfig,
                                   @Named("authAction") authAction: AuthAction,
                                   @Named("verifyAuthAction") verifyAuthAction: AuthAction) {

  def decide: AuthAction = if (applicationConfig.identityVerification) authAction else verifyAuthAction
}
