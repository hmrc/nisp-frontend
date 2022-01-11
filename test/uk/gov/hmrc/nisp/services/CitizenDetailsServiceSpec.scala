/*
 * Copyright 2022 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.{any => mockAny, eq => mockEQ}
import org.mockito.Mockito.{mock, reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Injecting
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, NotFoundException, Upstream5xxResponse}
import uk.gov.hmrc.nisp.connectors.CitizenDetailsConnector
import uk.gov.hmrc.nisp.helpers.{FakeCachedStaticHtmlPartialRetriever, FakePartialRetriever, TestAccountBuilder}
import uk.gov.hmrc.nisp.models.citizen.{Address, Citizen, CitizenDetailsError, CitizenDetailsResponse}
import uk.gov.hmrc.nisp.utils.UnitSpec
import uk.gov.hmrc.play.partials.{CachedStaticHtmlPartialRetriever, FormPartialRetriever}

import java.time.LocalDate
import scala.concurrent.Future
class CitizenDetailsServiceSpec extends UnitSpec with BeforeAndAfterEach with ScalaFutures with
  GuiceOneAppPerSuite with Injecting {

  val nino: Nino = TestAccountBuilder.regularNino
  val noNameNino: Nino = TestAccountBuilder.noNameNino
  val nonExistentNino: Nino = TestAccountBuilder.nonExistentNino
  val badRequestNino: Nino = TestAccountBuilder.blankNino
  val mockCitizenDetailsConnector = mock[CitizenDetailsConnector]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector),
      bind[FormPartialRetriever].to[FakePartialRetriever],
      bind[CachedStaticHtmlPartialRetriever].toInstance(FakeCachedStaticHtmlPartialRetriever)
    ).build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockCitizenDetailsConnector)
  }

  def citizenDetailsResponseForNino(nino: Nino): CitizenDetailsResponse =
    TestAccountBuilder.directJsonResponse(nino, "citizen-details")

  val citizenDetailsService = inject[CitizenDetailsService]

  "CitizenDetailsService" should {
    "return something for valid NINO" in {
      when(mockCitizenDetailsConnector.connectToGetPersonDetails(mockEQ(nino))(mockAny())).thenReturn(
        Future.successful(citizenDetailsResponseForNino(nino))
      )

      val person: Future[Either[CitizenDetailsError, CitizenDetailsResponse]] = citizenDetailsService.retrievePerson(nino)(new HeaderCarrier())
      whenReady(person) {p =>
        p.isRight shouldBe true
      }
    }

    "return None for bad NINO" in {
      when(mockCitizenDetailsConnector.connectToGetPersonDetails(mockEQ(nonExistentNino))(mockAny())).thenReturn(
        Future.failed(new NotFoundException(""))
      )

      val person: Future[Either[CitizenDetailsError, CitizenDetailsResponse]] = citizenDetailsService.retrievePerson(nonExistentNino)(new HeaderCarrier())
      whenReady(person) {p =>
        p.isLeft shouldBe true
      }
    }

    "return None for bad request" in {
      when(mockCitizenDetailsConnector.connectToGetPersonDetails(mockEQ(badRequestNino))(mockAny())).thenReturn(
        Future.failed(new BadRequestException(""))
      )

      val person: Future[Either[CitizenDetailsError, CitizenDetailsResponse]] = citizenDetailsService.retrievePerson(badRequestNino)(new HeaderCarrier())
      whenReady(person) {p =>
        p.isLeft shouldBe true
      }
    }

    "return a Failed Future for a 5XX error" in {
      when(mockCitizenDetailsConnector.connectToGetPersonDetails(mockEQ(TestAccountBuilder.internalServerError))(mockAny())).thenReturn(
        Future.failed(new Upstream5xxResponse("CRITICAL FAILURE", 500, 500))
      )

      val person: Future[Either[CitizenDetailsError, CitizenDetailsResponse]] = citizenDetailsService.retrievePerson(TestAccountBuilder.internalServerError)(new HeaderCarrier())
      whenReady(person.failed) { ex =>
        ex shouldBe a [Upstream5xxResponse]
      }
    }

    "return correct name and Date of Birth for NINO" in {
      when(mockCitizenDetailsConnector.connectToGetPersonDetails(mockEQ(nino))(mockAny())).thenReturn(
        Future.successful(citizenDetailsResponseForNino(nino))
      )

      val person: Future[Either[CitizenDetailsError, CitizenDetailsResponse]] = citizenDetailsService.retrievePerson(nino)(new HeaderCarrier())
      whenReady(person) {p =>
        p.right.map(_.person.copy(nino = nino)) shouldBe Right(Citizen(nino, Some("AHMED"), Some("BRENNAN"), LocalDate.of(1954, 3, 9)))
        p.right.get.person.getNameFormatted shouldBe Some("AHMED BRENNAN")
      }
    }

    "return formatted name of None if Citizen returns without a name" in {
      when(mockCitizenDetailsConnector.connectToGetPersonDetails(mockEQ(noNameNino))(mockAny())).thenReturn(
        Future.successful(citizenDetailsResponseForNino(noNameNino))
      )

      val person: Future[Either[CitizenDetailsError, CitizenDetailsResponse]] = citizenDetailsService.retrievePerson(noNameNino)(new HeaderCarrier())
      whenReady(person) {p =>
        p shouldBe Right(CitizenDetailsResponse(Citizen(noNameNino, None, None, LocalDate.of(1954, 3, 9)), Some(Address(Some("GREAT BRITAIN")))))
        p.right.get.person.getNameFormatted shouldBe None
      }
    }
  }
}
