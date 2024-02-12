/*
 * Copyright 2024 HM Revenue & Customs
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

import com.google.inject.Inject
import play.api.mvc.{BodyParsers, Request, Result}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.LoginTimes
import uk.gov.hmrc.nisp.controllers.auth.{AuthAction, AuthDetails, AuthenticatedRequest}
import uk.gov.hmrc.nisp.fixtures.NispAuthedUserFixture

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class FakeAuthAction @Inject() (val parser: BodyParsers.Default, val executionContext: ExecutionContext)
    extends AuthAction {
  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] =
    block(
      AuthenticatedRequest(
        request,
        NispAuthedUserFixture.user(TestAccountBuilder.regularNino),
        AuthDetails(ConfidenceLevel.L200, LoginTimes(Instant.now, None))
      )
    )
}
