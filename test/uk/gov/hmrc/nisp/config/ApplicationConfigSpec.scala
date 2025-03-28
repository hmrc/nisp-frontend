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

package uk.gov.hmrc.nisp.config

import com.typesafe.config.ConfigFactory
import play.api.Configuration
import uk.gov.hmrc.nisp.models.PayableGapExtensionDetails
import uk.gov.hmrc.nisp.utils.UnitSpec
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import com.typesafe.config.ConfigException

class ApplicationConfigSpec extends UnitSpec  {

  val validConfig: String =
    s"""
       |payableGapsExtensions.extensions.1.taxYear = 2026
       |payableGapsExtensions.extensions.1.payableGaps = 6
       |payableGapsExtensions.extensions.2.taxYear = 2024
       |payableGapsExtensions.extensions.2.payableGaps = 18
       |payableGapsExtensions.extensions.3.taxYear = 2025
       |payableGapsExtensions.extensions.3.payableGaps = 6
    """.stripMargin

  val invalidConfig: String =
    s"""
       |payableGapsExtensions.extensions.1.taxYear = "2024-05-04"
       |payableGapsExtensions.extensions.1.payableGaps = 18
      """.stripMargin

  val defaultGapConfig: String =
    s"""
       |payableGapsExtensions.defaults.prevTaxYearPayableGaps = 6
       |payableGapsExtensions.defaults.payableGaps = 6
       |""".stripMargin

  def appConfig(config: String): ApplicationConfig = {
    val configuration: Configuration = Configuration(ConfigFactory.parseString(config))
    val servicesConfig = new ServicesConfig(configuration)
    new ApplicationConfig(configuration, servicesConfig)
  }

  "Application config" should {
    "Return correctly formated values sorted by date order" when {
      "payableGapExtensions called with valid application.conf entries" in {
        val payableGaps1 = PayableGapExtensionDetails(2024, 18)
        val payableGaps2 = PayableGapExtensionDetails(2025, 6)
        val payableGaps3 = PayableGapExtensionDetails(2026, 6)
        val payableGapExtensions = appConfig(validConfig).payableGapExtensions

        payableGapExtensions.head shouldBe payableGaps1
        payableGapExtensions(1) shouldBe payableGaps2
        payableGapExtensions(2) shouldBe payableGaps3

        payableGapExtensions.size shouldBe 3
      }
    }

    "Return exception" when {
      "payableGapExtensions called with inval application.conf entry" in {
        val exception = intercept[ConfigException](appConfig(invalidConfig).payableGapExtensions)
        exception.getMessage shouldBe "String: 2: taxYear has type STRING rather than NUMBER"
      }
    }

    "Return defaults" when {
      "requesting value for payableGapDefault" in {
        appConfig(defaultGapConfig).payableGapDefault shouldBe 6
      }
    }
  }
}
