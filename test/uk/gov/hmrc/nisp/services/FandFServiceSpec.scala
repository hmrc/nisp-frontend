/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.services

import cats.data.EitherT
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.GET
import uk.gov.hmrc.auth.core.retrieve.v2.TrustedHelper
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.nisp.connectors.FandFConnector
import uk.gov.hmrc.nisp.utils.UnitSpec
import play.api.http.Status.INTERNAL_SERVER_ERROR

import scala.concurrent.{ExecutionContext, Future}

class FandFServiceSpec extends UnitSpec with ScalaFutures {

  val mockFandFConnector: FandFConnector = mock[FandFConnector]
  val nino: Nino = generateNino

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "/")

  val fandfService: FandFService = new FandFService(mockFandFConnector)

  val trustedHelper: TrustedHelper = TrustedHelper("principal Name", "attorneyName", "returnLink", Some(nino.nino))

  "FandFService calling getTrustedHelper" should {
    "return Some(TrustedHelper) when connector call successful" in {
      when(mockFandFConnector.getTrustedHelper()(any()))
        .thenReturn(EitherT[Future, UpstreamErrorResponse, TrustedHelper]
          (Future.successful(Right(trustedHelper))))

      val result = fandfService.getTrustedHelper()
      result.futureValue shouldBe Some(trustedHelper)
    }

    "return None when connector call fails" in {
      when(mockFandFConnector.getTrustedHelper()(any()))
        .thenReturn(EitherT[Future, UpstreamErrorResponse, TrustedHelper]
          (Future.successful(Left(UpstreamErrorResponse("failed call", INTERNAL_SERVER_ERROR)))))

      val result = fandfService.getTrustedHelper()
      result.futureValue shouldBe None
    }
  }
}
