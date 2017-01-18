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

package uk.gov.hmrc.nisp.services

import org.joda.time.LocalDate
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import uk.gov.hmrc.nisp.helpers.{MockCitizenDetailsService, TestAccountBuilder}
import uk.gov.hmrc.nisp.models.citizen.{Address, Citizen, CitizenDetailsResponse}
import uk.gov.hmrc.play.http
import uk.gov.hmrc.play.http.{HeaderCarrier, Upstream5xxResponse}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class CitizenDetailsServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfter with ScalaFutures with OneAppPerSuite {
  val nino = TestAccountBuilder.regularNino
  val nonamenino = TestAccountBuilder.noNameNino
  val nonExistentNino = TestAccountBuilder.nonExistentNino
  val badRequestNino = TestAccountBuilder.blankNino

  "CitizenDetailsService" should {
    "return something for valid NINO" in {
      val person: Future[Option[CitizenDetailsResponse]] = MockCitizenDetailsService.retrievePerson(nino)(new HeaderCarrier())
      whenReady(person) {p =>
        p should not be None
      }
    }

    "return None for bad NINO" in {
      val person: Future[Option[CitizenDetailsResponse]] = MockCitizenDetailsService.retrievePerson(nonExistentNino)(new HeaderCarrier())
      whenReady(person) {p =>
        p shouldBe None
      }
    }

    "return None for bad request" in {
      val person: Future[Option[CitizenDetailsResponse]] = MockCitizenDetailsService.retrievePerson(badRequestNino)(new HeaderCarrier())
      whenReady(person) {p =>
        p shouldBe None
      }
    }

    "return a Failed Future for a 5XX error" in {
      val person: Future[Option[CitizenDetailsResponse]] = MockCitizenDetailsService.retrievePerson(TestAccountBuilder.internalServerError)(new HeaderCarrier())
      whenReady(person.failed) { ex =>
        ex shouldBe a [Upstream5xxResponse]
      }
    }

    "return correct name, gender and Date of Birth for NINO" in {
      val person: Future[Option[CitizenDetailsResponse]] = MockCitizenDetailsService.retrievePerson(nino)(new HeaderCarrier())
      whenReady(person) {p =>
        p.map(_.person.copy(nino = nino)) shouldBe Some(Citizen(nino, Some("AHMED"), Some("BRENNAN"), Some("M"), new LocalDate(1954, 3, 9)))
      }
    }

    "return formatted name of None if Citizen returns without a name" in {
      val person: Future[Option[CitizenDetailsResponse]] = MockCitizenDetailsService.retrievePerson(nonamenino)(new HeaderCarrier())
      whenReady(person) {p =>
        p shouldBe Some(CitizenDetailsResponse(Citizen(nonamenino, None, None, None, new LocalDate(1954, 3, 9)), Some(Address(Some("GREAT BRITAIN")))))
        p.get.person.getNameFormatted shouldBe None
      }
    }
  }
}
