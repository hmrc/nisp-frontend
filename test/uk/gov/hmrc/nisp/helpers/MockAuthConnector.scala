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

package uk.gov.hmrc.nisp.helpers

import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain.ConfidenceLevel.L500
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Accounts, Authority, CredentialStrength, PayeAccount}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, UserId}
import uk.gov.hmrc.nisp.controllers.auth.NispAuthConnector

object MockAuthConnector extends NispAuthConnector {
  val mockUserId: UserId = userID("mockuser")

  def userID(username: String): UserId = UserId(s"/auth/oid/$username")

  val usernameToNino: Map[UserId, Nino] = Map(
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
    userID("mockspaunderconsideration") -> TestAccountBuilder.spaUnderConsiderationNino,
    userID("mockspaunderconsiderationnoflag") -> TestAccountBuilder.spaUnderConsiderationNoFlagNino,
    userID("mockspaunderconsiderationexclusionamountdis") -> TestAccountBuilder.spaUnderConsiderationExclusionAmountDisNino,
    userID("mockspaunderconsiderationexclusioniom") -> TestAccountBuilder.spaUnderConsiderationExclusionIoMNino,
    userID("mockspaunderconsiderationexclusionmwree") -> TestAccountBuilder.spaUnderConsiderationExclusionMwrreNino,
    userID("mockspaunderconsiderationexclusionoverspa") -> TestAccountBuilder.spaUnderConsiderationExclusionOverSpaNino,
    userID("mockspaunderconsiderationexclusionmultiple") -> TestAccountBuilder.spaUnderConsiderationExclusionMultipleNino,
    userID("mockspaunderconsiderationexclusionnoflag") -> TestAccountBuilder.spaUnderConsiderationExclusionNoFlagNino,

    userID("showurbanner") -> TestAccountBuilder.urBannerNino,
    userID("hideurbanner") -> TestAccountBuilder.noUrBannerNino,

    userID("mockexcludedall") -> TestAccountBuilder.excludedAll,
    userID("mockexcludedallbutdead") -> TestAccountBuilder.excludedAllButDead,
    userID("mockexcludedallbutdeadmci") -> TestAccountBuilder.excludedAllButDeadMCI,
    userID("mockexcludeddissonanceiommwrreabroad") -> TestAccountBuilder.excludedDissonanceIomMwrreAbroad,
    userID("mockexcludediommwrreabroad") -> TestAccountBuilder.excludedIomMwrreAbroad,
    userID("mockexcludedmwrreabroad") -> TestAccountBuilder.excludedMwrreAbroad,
    userID("mockexcludedmwrre") -> TestAccountBuilder.excludedMwrre,
    userID("mockexcludedabroad") -> TestAccountBuilder.excludedAbroad
  )

  private def testCredentialStrength(nino: Nino): CredentialStrength =
    if (nino == TestAccountBuilder.weakNino) CredentialStrength.Weak else CredentialStrength.Strong

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = {
    ???
  }
}
