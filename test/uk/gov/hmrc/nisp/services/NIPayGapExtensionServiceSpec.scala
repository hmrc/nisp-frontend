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

import com.typesafe.config.ConfigFactory
import play.api.Configuration
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.models.{AfterDeadline, AfterDeadlineExtended, BeforeDeadline, PayableGapInfo}
import uk.gov.hmrc.nisp.utils.UnitSpec
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalDate

class NIPayGapExtensionServiceSpec extends UnitSpec {

  "Calling payableGapInfo" should {
    "Return status of 'BeforeDeadline' and start year of 2006" when {
      "yearsPayable includes years before 6 years ago, TaxYear is 2024 and day is 1st April" in new Setup {

        val yearsPayable: Seq[String] = Seq("2023", "2022","2010", "2006")

        val gapInfo: PayableGapInfo = service.payableGapInfo(yearsPayable)

        gapInfo.payableGapExtensionStatus shouldBe BeforeDeadline
        gapInfo.startYear shouldBe 2006
        gapInfo.numberOfGapYears shouldBe 18
      }
    }

    "Return status of 'BeforeDeadline' and start year of 2006" when {
      "yearsPayable includes some years before 6 years ago, TaxYear is 2024 and day is 1st April" in new Setup {

        val yearsPayable: Seq[String] = Seq("2023", "2022")

        val gapInfo: PayableGapInfo = service.payableGapInfo(yearsPayable)

        gapInfo.payableGapExtensionStatus shouldBe BeforeDeadline
        gapInfo.startYear shouldBe 2006
        gapInfo.numberOfGapYears shouldBe 18
      }
    }

    "Return status of 'AfterDeadline' and start year of 2019" when {
      "yearsPayable includes none before 6 years ago, TaxYear is 2025 and day is 6st April" in new Setup(6) {

        val yearsPayable: Seq[String] = Seq("2023", "2022")

        val gapInfo: PayableGapInfo = service.payableGapInfo(yearsPayable)

        gapInfo.payableGapExtensionStatus shouldBe AfterDeadline
        gapInfo.startYear shouldBe 2019
        gapInfo.numberOfGapYears shouldBe 6
      }
    }

    "Return status of 'AfterDeadlineExtended' and start year of 2006" when {
      "yearsPayable includes some years before 6 years ago, TaxYear is 2025 and day is 6st April" in new Setup(6) {

        val yearsPayable: Seq[String] = Seq("2023", "2022","2010", "2006")

        val gapInfo: PayableGapInfo = service.payableGapInfo(yearsPayable)

        gapInfo.payableGapExtensionStatus shouldBe AfterDeadlineExtended
        gapInfo.startYear shouldBe 2006
        gapInfo.numberOfGapYears shouldBe 19
      }
    }
  }

  class Setup(day: Int = 1) {

    val validConfig: String =
      s"""
         |payableGapsExtensions.extensions.2.taxYear = 2024
         |payableGapsExtensions.extensions.2.payableGaps = 18
         |
         |payableGapsExtensions.defaults.prevTaxYearPayableGaps = 6
         |payableGapsExtensions.defaults.payableGaps = 6
    """.stripMargin

    def appConfig(config: String): ApplicationConfig = {
      val configuration: Configuration = Configuration(ConfigFactory.parseString(config))
      val servicesConfig = new ServicesConfig(configuration)
      new ApplicationConfig(configuration, servicesConfig)
    }

    val service: NIPayGapExtensionService = new NIPayGapExtensionService(appConfig(validConfig)) {
      override def now: () => LocalDate = () => LocalDate.of(2025, 4, day)
    }
  }
}
