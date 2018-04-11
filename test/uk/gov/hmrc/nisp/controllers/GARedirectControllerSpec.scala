/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.controllers

import java.util.UUID

import org.mockito.Mockito
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.http._
import play.api.i18n.Messages.Implicits._
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.config.wiring.NispFormPartialRetriever
import uk.gov.hmrc.nisp.connectors.IdentityVerificationConnector
import uk.gov.hmrc.nisp.helpers.{MockAuthConnector, MockCachedStaticHtmlPartialRetriever, MockCitizenDetailsService, MockIdentityVerificationConnector}
import uk.gov.hmrc.nisp.services.CitizenDetailsService
import uk.gov.hmrc.nisp.utils.MockTemplateRenderer
import uk.gov.hmrc.nisp.views.html.{identity_verification_landing, landing}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.time.DateTimeUtils._

class GARedirectControllerSpec  extends PlaySpec with MockitoSugar with OneAppPerSuite {

  private implicit val fakeRequest = FakeRequest("GET", "/redirect")
  private implicit val lang = Lang("en")
  private implicit val retriever = MockCachedStaticHtmlPartialRetriever
  implicit val formPartialRetriever: uk.gov.hmrc.play.partials.FormPartialRetriever = NispFormPartialRetriever
  implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer

  val testGARedirectController = new GARedirectController {
    override val citizenDetailsService: CitizenDetailsService = MockCitizenDetailsService

    override val applicationConfig: ApplicationConfig = mock[ApplicationConfig]

    override val identityVerificationConnector: IdentityVerificationConnector = MockIdentityVerificationConnector

    override protected def authConnector: AuthConnector = MockAuthConnector

    override implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = retriever

    override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
  }

  "GET /redirect" should {
    "return 200" in {
      val result = testGARedirectController.show(fakeRequest)
      status(result) mustBe Status.OK
    }

  }
}
