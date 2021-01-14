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

package uk.gov.hmrc.nisp.controllers.auth

import org.joda.time.LocalDate
import uk.gov.hmrc.auth.core.retrieve.v2.TrustedHelper
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.nisp.models.UserName
import uk.gov.hmrc.nisp.models.citizen.Address
import uk.gov.hmrc.nisp.utils.Country

final case class NispAuthedUser(nino: Nino,
                          dateOfBirth: LocalDate,
                          name: UserName,
                          address: Option[Address],
                          trustedHelper: Option[TrustedHelper],
                          isSa: Boolean) {

  lazy val livesAbroad: Boolean = address.fold(false)( co => co.country.exists(Country.isAbroad))

}
