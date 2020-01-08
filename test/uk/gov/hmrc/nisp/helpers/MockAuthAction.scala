/*
 * Copyright 2020 HM Revenue & Customs
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

import org.joda.time.DateTime
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.LoginTimes
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.nisp.controllers.auth.{AuthAction, AuthDetails, AuthenticatedRequest}
import uk.gov.hmrc.nisp.fixtures.NispAuthedUserFixture

import scala.concurrent.Future

class MockAuthAction(ninoToReturn: Nino) extends AuthAction {
  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    block(AuthenticatedRequest(request, NispAuthedUserFixture.user(ninoToReturn),
      AuthDetails(ConfidenceLevel.L200, Some("GovernmentGateway"), LoginTimes(DateTime.now, None))))
  }
}
