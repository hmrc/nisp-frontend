/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.controllers.auth

import org.joda.time.LocalDate
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.nisp.utils.Constants
import uk.gov.hmrc.nisp.utils.Country
import uk.gov.hmrc.nisp.models.UserName
import uk.gov.hmrc.nisp.models.citizen.Address

case class NispAuthedUser(nino: Nino,
                          confidenceLevel: ConfidenceLevel,
                          dateOfBirth: LocalDate,
                          name: UserName,
                          address: Option[Address]) {

  lazy val livesAbroad: Boolean = address.fold(false)( co => co.country.exists(Country.isAbroad))
  val authProviderOld = if(confidenceLevel.level == 500) Constants.verify else Constants.iv
  val authProvider = if(confidenceLevel.level == 500) Constants.verify else Constants.iv

}
