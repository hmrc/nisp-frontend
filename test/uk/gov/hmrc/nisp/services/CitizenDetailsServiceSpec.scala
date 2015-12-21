/*
 * Copyright 2015 HM Revenue & Customs
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

import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.http.Status
import uk.gov.hmrc.nisp.helpers.{TestAccountBuilder, MockCitizenDetailsService}
import uk.gov.hmrc.nisp.models.citizen.{CidName, Citizen}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class CitizenDetailsServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfter with ScalaFutures with OneAppPerSuite {
  val nino = TestAccountBuilder.regularNino
  val nonamenino = TestAccountBuilder.noNameNino
  val nonExistentNino = TestAccountBuilder.nonExistentNino
  val badRequestNino = TestAccountBuilder.blankNino

  val mockHttp = mock[HttpPost]

  "CitizenDetailsService" should {
    "return something for valid NINO" in {
      val person: Future[Option[Citizen]] = MockCitizenDetailsService.retrievePerson(nino)(new HeaderCarrier())
      whenReady(person) {p =>
        p should not be None
      }
    }

    "return None for bad NINO" in {
      val person: Future[Option[Citizen]] = MockCitizenDetailsService.retrievePerson(nonExistentNino)(new HeaderCarrier())
      whenReady(person) {p =>
        p shouldBe None
      }
    }

    "return None for bad request" in {
      val person: Future[Option[Citizen]] = MockCitizenDetailsService.retrievePerson(badRequestNino)(new HeaderCarrier())
      whenReady(person) {p =>
        p shouldBe None
      }
    }

    "return correct name for NINO" in {
      val person: Future[Option[Citizen]] = MockCitizenDetailsService.retrievePerson(nino)(new HeaderCarrier())
      whenReady(person) {p =>
        p.map(_.copy(nino = nino)) shouldBe Some(Citizen(nino, Some(CidName(Some("Dorothy"), Some("Kovacic"))), Some("26121960")))
      }
    }

    "return formatted name of None if Citizen returns without a name" in {
      val person: Future[Option[Citizen]] = MockCitizenDetailsService.retrievePerson(nonamenino)(new HeaderCarrier())
      whenReady(person) {p =>
        p shouldBe Some(Citizen(nonamenino, None, Some("19111953")))
        p.get.getNameFormatted shouldBe None
      }
    }
  }
}
