/*
 * Copyright 2016 HM Revenue & Customs
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

import org.joda.time.DateTime
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.nisp.exceptions.EmptyPayeException
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.play.frontend.auth.AuthContext

case class NispUser(authContext: AuthContext, name: Option[String], authProvider: String) {
  def nino: Nino = authContext.principal.accounts.paye.map(_.nino).getOrElse(throw new EmptyPayeException("AuthContext does not have PAYE Details"))
  def previouslyLoggedInAt: Option[DateTime] = authContext.user.previouslyLoggedInAt
  val authProviderOld = if(authContext.user.confidenceLevel.level == 500) Constants.verify else Constants.iv
  val confidenceLevel = authContext.user.confidenceLevel
}
