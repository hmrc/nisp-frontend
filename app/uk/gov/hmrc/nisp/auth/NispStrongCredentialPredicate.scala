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

package uk.gov.hmrc.nisp.auth

import java.net.URI

import play.api.mvc.Results._
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.play.frontend.auth._
import uk.gov.hmrc.play.frontend.auth.connectors.domain.CredentialStrength

import scala.concurrent.Future

class NispStrongCredentialPredicate(twoFactorAuthenticationUri: URI) extends PageVisibilityPredicate {
  override def apply(authContext: AuthContext, request: Request[AnyContent]): Future[PageVisibilityResult] = {
    if (hasStrongCredentials(authContext)) {
      Future.successful(PageIsVisible)
    } else {
      Future.successful(PageBlocked(failedCredentialResult))
    }
  }

  private val failedCredentialResult = Future.successful(Redirect(twoFactorAuthenticationUri.toString))

  private def hasStrongCredentials(authContext: AuthContext) =
    authContext.user.credentialStrength == CredentialStrength.Strong
}
