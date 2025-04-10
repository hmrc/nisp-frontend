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

package uk.gov.hmrc.nisp.utils

import org.mockito.ArgumentMatcher
import org.scalactic.{Prettifier, source}
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.nisp.controllers.auth.AuthenticatedRequest
import scala.language.implicitConversions

case class EqualsAuthenticatedRequest(n: AuthenticatedRequest[?])
    extends ArgumentMatcher[AuthenticatedRequest[?]]
    with Matchers {

  class LocalPrettifier extends Prettifier {
    override def apply(o: Any): String =
      o match {
        case request: AuthenticatedRequest[?] =>
          s"AuthenticatedRequest { request: ${request.request}, nispAuthedUser: ${request.nispAuthedUser}, authDetails: ${request.authDetails}"
        case _                                => o.toString
      }
  }

  implicit def convertToAnyShouldWrapper[T](o: T)(
    implicit pos: source.Position, prettifier: Prettifier): AnyShouldWrapper[T] =
    new AnyShouldWrapper(o, pos, new LocalPrettifier)

  override def matches(argument: AuthenticatedRequest[?]): Boolean = {
    withClue(s"Argument doesn't match: ") {
      argument shouldBe n
    }
    true
  }
}
