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

package uk.gov.hmrc.nisp.models

import com.typesafe.config.Config
import play.api.ConfigLoader
import play.api.libs.json.{Json, OFormat}

case class PayableGapExtensionDetails (taxYear: Int, payableGaps: Int)

object PayableGapExtensionDetails {

  implicit val format: OFormat[PayableGapExtensionDetails] = Json.format[PayableGapExtensionDetails]

  implicit val PGEDs: ConfigLoader[Seq[PayableGapExtensionDetails]] = new ConfigLoader[Seq[PayableGapExtensionDetails]] {
    override def load(rootConfig: Config, path: String): Seq[PayableGapExtensionDetails] = {
      import scala.jdk.CollectionConverters._

      val config = rootConfig.getConfig(path)

      rootConfig.getObject(path).keySet().asScala.map { key =>
        val value = config.getConfig(key)

        PayableGapExtensionDetails(
          value.getInt("taxYear"),
          value.getInt("payableGaps")
        )
      }.toSeq.sortBy(_.taxYear)
    }
  }
}
