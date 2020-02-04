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

package uk.gov.hmrc.nisp.services

import org.joda.time.LocalDate
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, Upstream5xxResponse}
import uk.gov.hmrc.nisp.helpers.{MockCitizenDetailsService, TestAccountBuilder}
import uk.gov.hmrc.nisp.models.citizen.{Address, Citizen, CitizenDetailsError, CitizenDetailsResponse}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class CitizenDetailsServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfter with ScalaFutures with OneAppPerSuite {
  val nino: Nino = TestAccountBuilder.regularNino
  val noNameNino: Nino = TestAccountBuilder.noNameNino
  val nonExistentNino: Nino = TestAccountBuilder.nonExistentNino
  val badRequestNino: Nino = TestAccountBuilder.blankNino

  "CitizenDetailsService" should {
    "return something for valid NINO" in {
      val person: Future[Either[CitizenDetailsError, CitizenDetailsResponse]] = MockCitizenDetailsService.retrievePerson(nino)(new HeaderCarrier())
      whenReady(person) {p =>
        p.isRight shouldBe true
      }
    }

    "return None for bad NINO" in {
      val person: Future[Either[CitizenDetailsError, CitizenDetailsResponse]] = MockCitizenDetailsService.retrievePerson(nonExistentNino)(new HeaderCarrier())
      whenReady(person) {p =>
        p.isLeft shouldBe true
      }
    }

    "return None for bad request" in {
      val person: Future[Either[CitizenDetailsError, CitizenDetailsResponse]] = MockCitizenDetailsService.retrievePerson(badRequestNino)(new HeaderCarrier())
      whenReady(person) {p =>
        p.isLeft shouldBe true
      }
    }

    "return a Failed Future for a 5XX error" in {
      val person: Future[Either[CitizenDetailsError, CitizenDetailsResponse]] = MockCitizenDetailsService.retrievePerson(TestAccountBuilder.internalServerError)(new HeaderCarrier())
      whenReady(person.failed) { ex =>
        ex shouldBe a [Upstream5xxResponse]
      }
    }

    "return correct name and Date of Birth for NINO" in {
      val person: Future[Either[CitizenDetailsError, CitizenDetailsResponse]] = MockCitizenDetailsService.retrievePerson(nino)(new HeaderCarrier())
      whenReady(person) {p =>
        p.right.map(_.person.copy(nino = nino)) shouldBe Right(Citizen(nino, Some("AHMED"), Some("BRENNAN"), new LocalDate(1954, 3, 9)))
        p.right.get.person.getNameFormatted shouldBe Some("AHMED BRENNAN")
      }
    }

    "return formatted name of None if Citizen returns without a name" in {
      val person: Future[Either[CitizenDetailsError, CitizenDetailsResponse]] = MockCitizenDetailsService.retrievePerson(noNameNino)(new HeaderCarrier())
      whenReady(person) {p =>
        p shouldBe Right(CitizenDetailsResponse(Citizen(noNameNino, None, None, new LocalDate(1954, 3, 9)), Some(Address(Some("GREAT BRITAIN")))))
        p.right.get.person.getNameFormatted shouldBe None
      }
    }
  }
}
