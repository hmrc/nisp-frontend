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

package uk.gov.hmrc.nisp.helpers

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain.ConfidenceLevel.L500
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{CredentialStrength, Accounts, Authority, PayeAccount}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, UserId}
import uk.gov.hmrc.play.frontend.auth._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object MockAuthConnector extends AuthConnector {
  override val serviceUrl: String = ""
  override def http: HttpGet = ???

  val mockUserNino = TestAccountBuilder.regularNino
  val mockUsername = "mockuser"
  val mockUserId = userID(mockUsername)

  def userID(username: String): UserId = UserId(s"/auth/oid/$username")

  val usernameToNino = Map(
    userID("mockuser") -> TestAccountBuilder.regularNino,
    userID("mockexcluded") -> TestAccountBuilder.excludedNino,
    userID("mockfulluser") -> TestAccountBuilder.fullUserNino,
    userID("mockblank") -> TestAccountBuilder.blankNino,
    userID("mockcontractedout") -> TestAccountBuilder.contractedOutBTestNino,
    userID("mockmqp") -> TestAccountBuilder.mqpNino,
    userID("mockforecastonly") -> TestAccountBuilder.forecastOnlyNino
  )

  private def payeAuthority(id: String, nino: String): Option[Authority] =
    Some(Authority(id, Accounts(paye = Some(PayeAccount(s"/paye/$nino", Nino(nino)))), None, None, CredentialStrength.Strong, L500))

  override def currentAuthority(implicit hc: HeaderCarrier): Future[Option[Authority]] =
    Future(payeAuthority(hc.userId.getOrElse(mockUserId).value, usernameToNino(hc.userId.getOrElse(mockUserId))))
}
