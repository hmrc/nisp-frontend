/*
 * Copyright 2017 HM Revenue & Customs
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
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Accounts, Authority, CredentialStrength, PayeAccount}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, UserId}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object MockAuthConnector extends AuthConnector {
  override val serviceUrl: String = ""

  override def http: HttpGet = ???

  val mockUserId = userID("mockuser")

  def userID(username: String): UserId = UserId(s"/auth/oid/$username")

  val usernameToNino = Map(
    userID("mockuser") -> TestAccountBuilder.regularNino,
    userID("mockfulluser") -> TestAccountBuilder.fullUserNino,
    userID("mockblank") -> TestAccountBuilder.blankNino,
    userID("mockcontractedout") -> TestAccountBuilder.contractedOutBTestNino,
    userID("mockmqp") -> TestAccountBuilder.mqpNino,
    userID("mockforecastonly") -> TestAccountBuilder.forecastOnlyNino,
    userID("mockweak") -> TestAccountBuilder.weakNino,
    userID("mockabroad") -> TestAccountBuilder.abroadNino,
    userID("mockmqpabroad") -> TestAccountBuilder.mqpAbroadNino,
    userID("mockhomeresponsibilitiesprotection") -> TestAccountBuilder.hrpNino,
    userID("mockfillgapsmultiple") -> TestAccountBuilder.fillGapsMultiple,
    userID("mockfillgapssingle") -> TestAccountBuilder.fillGapSingle,
    userID("mocknoqualifyingyears") -> TestAccountBuilder.noQualifyingYears,
    userID("mockbackendnotfound") -> TestAccountBuilder.backendNotFound,
    userID("mockstatepensionageunderconsideration") -> TestAccountBuilder.statePensionAgeUnderConsiderationNino,
    userID("mockstatepensionageunderconsiderationnoflag") -> TestAccountBuilder.statePensionAgeUnderConsiderationNoFlagNino,

    userID("mockexcludedall") -> TestAccountBuilder.excludedAll,
    userID("mockexcludedallbutdead") -> TestAccountBuilder.excludedAllButDead,
    userID("mockexcludedallbutdeadmci") -> TestAccountBuilder.excludedAllButDeadMCI,
    userID("mockexcludeddissonanceiommwrreabroad") -> TestAccountBuilder.excludedDissonanceIomMwrreAbroad,
    userID("mockexcludediommwrreabroad") -> TestAccountBuilder.excludedIomMwrreAbroad,
    userID("mockexcludedmwrreabroad") -> TestAccountBuilder.excludedMwrreAbroad,
    userID("mockexcludedmwrre") -> TestAccountBuilder.excludedMwrre,
    userID("mockexcludedabroad") -> TestAccountBuilder.excludedAbroad
  )

  private def payeAuthority(id: String, nino: Nino): Option[Authority] =
    Some(Authority(id, Accounts(paye = Some(PayeAccount(s"/paye/$nino", nino))), None, None,
      testCredentialStrength(nino), L500, None, None, None, "test oid"))

  private def testCredentialStrength(nino: Nino): CredentialStrength =
    if (nino == TestAccountBuilder.weakNino) CredentialStrength.Weak else CredentialStrength.Strong

  override def currentAuthority(implicit hc: HeaderCarrier): Future[Option[Authority]] =
    Future(payeAuthority(hc.userId.getOrElse(mockUserId).value, usernameToNino(hc.userId.getOrElse(mockUserId))))
}
