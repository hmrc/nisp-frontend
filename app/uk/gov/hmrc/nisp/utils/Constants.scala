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

package uk.gov.hmrc.nisp.utils

object Constants {

  val GovernmentGatewayId = "GGW"
  val AnyAuthenticationProviderId = "IDAorGGW"
  val VerifyProviderId = "IDA"

  // scalastyle:off magic.number
  val baseUrl = "/check-your-state-pension"

  val loginUrl = s"$baseUrl/account"

  val logoutUrl = s"$baseUrl/signout"

  val finishedPageUrl = s"$baseUrl/finished"

  val taxYearStartDay = 6
  val taxYearEndDay = 5
  val taxYearsStartEndMonth = 4

  val minimumQualifyingYearsNSP = 10

  val NAME = "customerName"
  val NINO = "customerNino"
  val CONTRACTEDOUT = "contractedOutFlag"
  val PERTAX = "customerPERTAX"

  val ninoWithoutSuffixLength: Int = 8

  val shortYearStartCharacter = 2
  val shortYearEndCharacter = 4
  val verify = "verify"
  val iv = "iv"
  val chartWidthMinimum = 31
  val deferralCutOffAge = 53

  val niRecordStartYear = 1975
  val niRecordMinAge = 16

  var yearStringLength = 4

  val titleSplitter = " - "
}
