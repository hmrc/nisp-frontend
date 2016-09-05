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

import org.mockito.Mockito._
import org.mockito.Matchers
import org.scalatest.mock.MockitoSugar
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.nisp.models.enums.IdentityVerificationResult
import uk.gov.hmrc.play.http.{HttpResponse, HttpGet}

import scala.concurrent.Future
import scala.io.Source

object MockIdentityVerificationHttp extends MockitoSugar {
  val mockHttp = mock[HttpGet]

  val possibleJournies = Map (
    "success-journey-id" -> "test/resources/identity-verification/success.json",
    "incomplete-journey-id" -> "test/resources/identity-verification/incomplete.json",
    "failed-matching-journey-id" -> "test/resources/identity-verification/failed-matching.json",
    "insufficient-evidence-journey-id" -> "test/resources/identity-verification/insufficient-evidence.json",
    "locked-out-journey-id" -> "test/resources/identity-verification/locked-out.json",
    "user-aborted-journey-id" -> "test/resources/identity-verification/user-aborted.json",
    "timeout-journey-id" -> "test/resources/identity-verification/timeout.json",
    "technical-issue-journey-id" -> "test/resources/identity-verification/technical-issue.json",
    "precondition-failed-journey-id" -> "test/resources/identity-verification/precondition-failed.json",
    "invalid-journey-id" -> "test/resources/identity-verification/invalid-result.json",
    "invalid-fields-journey-id" -> "test/resources/identity-verification/invalid-fields.json"
  )

  def mockJourneyId(journeyId: String): Unit = {
    val fileContents = Source.fromFile(possibleJournies(journeyId)).mkString
    when(mockHttp.GET[HttpResponse](Matchers.contains(journeyId))(Matchers.any(), Matchers.any())).
      thenReturn(Future.successful(HttpResponse(Status.OK, responseJson = Some(Json.parse(fileContents)))))
  }

  possibleJournies.keys.foreach(mockJourneyId)
}
