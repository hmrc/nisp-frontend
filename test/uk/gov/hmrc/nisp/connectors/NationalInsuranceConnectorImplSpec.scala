/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.connectors

import org.joda.time.LocalDate
import org.mockito.Mockito
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Injecting
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, Upstream4xxResponse}
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.helpers.{FakeSessionCache, MockNispHttp, TestAccountBuilder}
import uk.gov.hmrc.nisp.services.MetricsService
import uk.gov.hmrc.play.test.UnitSpec

class NationalInsuranceConnectorImplSpec extends UnitSpec with ScalaFutures with MockitoSugar with GuiceOneAppPerSuite with
  Injecting with BeforeAndAfterEach {

  implicit val headerCarrier = HeaderCarrier(extraHeaders = Seq("Accept" -> "application/vnd.hmrc.1.0+json"))

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val mockMetricService = mock[MetricsService](Mockito.RETURNS_DEEP_STUBS)
  val mockApplicationConfig = mock[ApplicationConfig]
  val mockHttp = MockNispHttp.mockHttp

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[HttpClient].toInstance(mockHttp),
      bind[MetricsService].toInstance(mockMetricService),
      bind[ApplicationConfig].toInstance(mockApplicationConfig),
      bind[SessionCache].toInstance(FakeSessionCache)
    ).build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockMetricService, mockApplicationConfig)
  }

  lazy val nationalInsuranceConnector = inject[NationalInsuranceConnectorImpl]

  "getNationalInsuranceRecord" when {

    "there is a regular user" should {
      when(mockApplicationConfig.nationalInsuranceServiceUrl).thenReturn("national-insurance")
      val nationalInsuranceRecord = await(nationalInsuranceConnector.getNationalInsurance(TestAccountBuilder.regularNino)(headerCarrier))

      "return a National Insurance Record with 28 qualifying years" in {
        nationalInsuranceRecord.qualifyingYears shouldBe 28
      }

      "return a National Insurance Record with 0 qualifyingYearsPriorTo1975" in {
        nationalInsuranceRecord.qualifyingYearsPriorTo1975 shouldBe 0
      }

      "return a National Insurance Record with 10 numberOfGaps" in {
        nationalInsuranceRecord.numberOfGaps shouldBe 10
      }

      "return a National Insurance Record with 4 numberOfGapsPayable" in {
        nationalInsuranceRecord.numberOfGapsPayable shouldBe 4
      }

      "return a National Insurance Record with false homeResponsibilitiesProtection" in {
        nationalInsuranceRecord.homeResponsibilitiesProtection shouldBe false
      }

      "return a National Insurance Record with earningsIncludedUpTo of 2014-04-05" in {
        nationalInsuranceRecord.earningsIncludedUpTo shouldBe new LocalDate(2014, 4, 5)
      }

      "return a National Insurance Record with 39 tax years" in {
        nationalInsuranceRecord.taxYears.length shouldBe 39
      }

      "the first tax year" should {

        val taxYear = nationalInsuranceRecord.taxYears.head
        "be the 2013-14 tax year" in {
          taxYear.taxYear shouldBe "2013-14"
        }

        "be qualifying" in {
          taxYear.qualifying shouldBe false
        }

        "have classOneContributions of 0" in {
          taxYear.classOneContributions shouldBe 0
        }

        "have classTwoCredits of 0" in {
          taxYear.classTwoCredits shouldBe 0
        }

        "have classThreeCredits of 0" in {
          taxYear.classThreeCredits shouldBe 0
        }

        "have otherCredits of 0" in {
          taxYear.otherCredits shouldBe 0
        }

        "have classThreePayable of 704.60" in {
          taxYear.classThreePayable shouldBe 704.60
        }

        "have no classThreePayableBy date" in {
          taxYear.classThreePayableBy shouldBe Some(new LocalDate(2019, 4, 5))
        }

        "have no classThreePayableByPenalty date" in {
          taxYear.classThreePayableByPenalty shouldBe Some(new LocalDate(2023, 4, 5))
        }

        "be not payable" in {
          taxYear.payable shouldBe true
        }

        "be not underInvestigation " in {
          taxYear.underInvestigation shouldBe false
        }

      }

      "a non qualifying year such as 2011-12" should {

        val taxYear = nationalInsuranceRecord.taxYears.find(p => p.taxYear == "2011-12").get

        "be the 2011-12 tax year" in {
          taxYear.taxYear shouldBe "2011-12"
        }

        "be non-qualifying" in {
          taxYear.qualifying shouldBe false
        }

        "have classOneContributions of 0" in {
          taxYear.classOneContributions shouldBe 0
        }

        "have classTwoCredits of 0" in {
          taxYear.classTwoCredits shouldBe 0
        }

        "have classThreeCredits of 0" in {
          taxYear.classThreeCredits shouldBe 0
        }

        "have otherCredits of 0" in {
          taxYear.otherCredits shouldBe 0
        }

        "have classThreePayable of 655.20" in {
          taxYear.classThreePayable shouldBe 655.20
        }

        "have classThreePayableBy date of 5th April 2019" in {
          taxYear.classThreePayableBy shouldBe Some(new LocalDate(2019, 4, 5))
        }

        "have classThreePayableByPenalty date of 5th April 2023" in {
          taxYear.classThreePayableByPenalty shouldBe Some(new LocalDate(2023, 4, 5))
        }

        "be payable" in {
          taxYear.payable shouldBe true
        }

        "be not underInvestigation " in {
          taxYear.underInvestigation shouldBe false
        }

      }
    }

    "there is a Dead Exclusion" should {
      "return a failed future with a 403 response code with a relevant message" in {
        when(mockApplicationConfig.nationalInsuranceServiceUrl).thenReturn("national-insurance")
        whenReady(nationalInsuranceConnector.getNationalInsurance(TestAccountBuilder.excludedAll).failed) {
          case ex: Upstream4xxResponse =>
            ex.upstreamResponseCode shouldBe 403
            ex.message.contains("EXCLUSION_DEAD") shouldBe true
        }
      }
    }

    "there is a MCI Exclusion" should {
      "return a failed future with a 403 response code with a relevant message" in {
        when(mockApplicationConfig.nationalInsuranceServiceUrl).thenReturn("national-insurance")
        whenReady(nationalInsuranceConnector.getNationalInsurance(TestAccountBuilder.excludedAllButDead).failed) {
          case ex: Upstream4xxResponse =>
            ex.upstreamResponseCode shouldBe 403
            ex.message.contains("EXCLUSION_MANUAL_CORRESPONDENCE") shouldBe true
        }
      }
    }

    "there is a IOM Exclusion" should {
      "return a failed future with a 403 response code with a relevant message" in {
        when(mockApplicationConfig.nationalInsuranceServiceUrl).thenReturn("national-insurance")
        whenReady(nationalInsuranceConnector.getNationalInsurance(TestAccountBuilder.excludedAllButDeadMCI).failed) {
          case ex: Upstream4xxResponse =>
            ex.upstreamResponseCode shouldBe 403
            ex.message.contains("EXCLUSION_ISLE_OF_MAN") shouldBe true
        }
      }
    }

    "there is a MWRRE Exclusion" should {
      "return a failed future with a 403 response code with a relevant message" in {
        when(mockApplicationConfig.nationalInsuranceServiceUrl).thenReturn("national-insurance")
        whenReady(nationalInsuranceConnector.getNationalInsurance(TestAccountBuilder.excludedIomMwrreAbroad).failed) {
          case ex: Upstream4xxResponse =>
            ex.upstreamResponseCode shouldBe 403
            ex.message.contains("EXCLUSION_MARRIED_WOMENS_REDUCED_RATE") shouldBe true
        }
      }
    }
  }
}