/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.views.statePension

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{reset, when}
import org.scalatest.concurrent.ScalaFutures
import play.api.Application
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.{LoginTimes, Name}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlag
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.StatePensionController
import uk.gov.hmrc.nisp.controllers.auth._
import uk.gov.hmrc.nisp.controllers.pertax.PertaxHelper
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.models._
import uk.gov.hmrc.nisp.models.admin.NewStatePensionUIToggle
import uk.gov.hmrc.nisp.models.pertaxAuth.PertaxAuthResponseModel
import uk.gov.hmrc.nisp.repositories.SessionCache
import uk.gov.hmrc.nisp.services.{MetricsService, NIPayGapExtensionService, NationalInsuranceService, StatePensionService}
import uk.gov.hmrc.nisp.utils.Constants.ACCESS_GRANTED
import uk.gov.hmrc.nisp.utils.{Constants, PertaxAuthMockingHelper}
import uk.gov.hmrc.nisp.views.HtmlSpec
import uk.gov.hmrc.nisp.views.html.statePension.StatePensionCopeView
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.language.LanguageUtils

import java.time.{Instant, LocalDate}
import scala.concurrent.Future

class StatePensionCopeViewSpec
  extends HtmlSpec
    with ScalaFutures
    with Injecting
    with WireMockSupport
    with PertaxAuthMockingHelper {

  val mockUserNino: Nino = TestAccountBuilder.regularNino
  val mockUserNinoExcluded: Nino = TestAccountBuilder.excludedAll
  val mockUserNinoNotFound: Nino = TestAccountBuilder.blankNino

  implicit val user: NispAuthedUser =
    NispAuthedUser(mockUserNino, LocalDate.now(), UserName(Name(None, None)), None, None, isSa = false)
  val authDetails: AuthDetails = AuthDetails(ConfidenceLevel.L200, LoginTimes(Instant.now(), None))

  implicit val fakeRequest: AuthenticatedRequest[AnyContentAsEmpty.type] = AuthenticatedRequest(FakeRequest(), user, authDetails)

  val mockAuditConnector: AuditConnector                     = mock[AuditConnector]
  val mockNationalInsuranceService: NationalInsuranceService = mock[NationalInsuranceService]
  val mockStatePensionService: StatePensionService           = mock[StatePensionService]
  implicit val mockAppConfig: ApplicationConfig              = mock[ApplicationConfig]
  val mockPertaxHelper: PertaxHelper                         = mock[PertaxHelper]
  val mockMetricsService: MetricsService                     = mock[MetricsService]
  val mockSessionCache: SessionCache                         = mock[SessionCache]
  val mockNIPayGapExtensionService: NIPayGapExtensionService = mock[NIPayGapExtensionService]

  lazy val langUtils: LanguageUtils = inject[LanguageUtils]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockStatePensionService)
    reset(mockNationalInsuranceService)
    reset(mockAuditConnector)
    reset(mockAppConfig)
    reset(mockPertaxHelper)
    reset(mockFeatureFlagService)
    reset(mockNIPayGapExtensionService)

    wireMockServer.resetAll()
    when(mockPertaxHelper.isFromPertax(any()))
      .thenReturn(Future.successful(false))
    when(mockAppConfig.accessibilityStatementUrl(any()))
      .thenReturn("/foo")
    when(mockAppConfig.reportAProblemNonJSUrl)
      .thenReturn("/reportAProblem")
    when(mockAppConfig.contactFormServiceIdentifier)
      .thenReturn("/id")
    when(mockAppConfig.pertaxAuthBaseUrl)
      .thenReturn(s"http://localhost:${wireMockServer.port()}")
    mockPertaxAuth(PertaxAuthResponseModel(ACCESS_GRANTED, "", None, None), mockUserNino.nino)
    when(mockFeatureFlagService.get(NewStatePensionUIToggle))
      .thenReturn(Future.successful(FeatureFlag(NewStatePensionUIToggle, isEnabled = true)))
  }

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[AuthRetrievals].to[FakeAuthAction],
      bind[StatePensionService].toInstance(mockStatePensionService),
      bind[NationalInsuranceService].toInstance(mockNationalInsuranceService),
      bind[AuditConnector].toInstance(mockAuditConnector),
      bind[ApplicationConfig].toInstance(mockAppConfig),
      bind[PertaxHelper].toInstance(mockPertaxHelper),
      bind[NIPayGapExtensionService].toInstance(mockNIPayGapExtensionService),
      bind[PertaxAuthAction].to[FakePertaxAuthAction],
      featureFlagServiceBinding
    )
    .build()

  val statePensionController: StatePensionController = inject[StatePensionController]

  "Render Contracted Out View" should {
    lazy val sResult        = inject[StatePensionCopeView]
    lazy val htmlAccountDoc = asDocument(sResult(99.54).toString)

    "render with correct page title" in {
      assertElementContainsText(
        htmlAccountDoc,
        "head > title",
        messages("nisp.cope.youWereContractedOut")
          + Constants.titleSplitter
          + messages("nisp.title.extension")
          + Constants.titleSplitter
          + messages("nisp.gov-uk")
      )
    }

    "render page with heading you were contracted out " in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='state_pension_cope__h1']",
        "nisp.cope.youWereContractedOut"
      )
    }

    "render page with text 'In the past you’ve been part of one or more contracted out pension schemes, such as workplace or personal pension schemes.' " in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='state_pension_cope__p1']",
        "nisp.cope.inThePast"
      )
    }

    "render page with text 'when you were contracted out:'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='state_pension_cope__p2']",
        "nisp.cope.why"
      )
    }

    "render page with text 'you and your employers paid lower rate National Insurance contributions, or'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='state_pension_cope__list1__item1']",
        "nisp.cope.why.bullet1"
      )
    }

    "render page with text 'some of your National Insurance contributions were paid into another pension scheme, such as a personal or stakeholder pension'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='state_pension_cope__list1__item2']",
        "nisp.cope.why.bullet2"
      )
    }

    "render page with text 'The amount of additional State Pension you would have been paid if you had not been contracted out is known as the Contracted Out Pension Equivalent (COPE).'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='state_pension_cope__p3']",
        "nisp.cope.copeequivalent"
      )
    }

    "render page with text 'Contracted Out Pension Equivalent (COPE)'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='state_pension_cope__h2_1']",
        "nisp.cope.title2"
      )
    }

    "render page with test 'your cope estimate is'" in {
      assertElementsOwnMessage(
        htmlAccountDoc,
        "[data-spec='state_pension_cope__p4']",
        "nisp.cope.table.estimate.title"
      )
    }

    "render page with test 'your cope estimate is : £99.54 a week'" in {
      val sWeekMessage = "£99.54 " + Messages("nisp.main.chart.week")
      assertEqualsValue(
        htmlAccountDoc,
        "[data-spec='state_pension_cope__p4'] .bold-intext",
        sWeekMessage
      )
    }

    "render page with text 'This will not affect your State Pension forecast. The COPE amount is paid as part of your other pension schemes, not by the government.'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='state_pension_cope__p5']",
        "nisp.cope.definition"
      )
    }

    "render page with text 'In most cases the private pension scheme you were contracted out to:'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='state_pension_cope__p6']",
        "nisp.cope.definition.mostcases"
      )
    }

    "render page with text 'will include an amount equal to the COPE amount'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='state_pension_cope__list2__item1']",
        "nisp.cope.definition.mostcases.bullet1"
      )
    }

    "render page with text 'may not individually identify the COPE amount'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='state_pension_cope__list2__item2']",
        "nisp.cope.definition.mostcases.bullet2"
      )
    }

    "render page with text 'The total amount of pension paid by your workplace or personal pension schemes will depend on the scheme and on any investment choices.'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='state_pension_cope__p7']",
        "nisp.cope.workplace"
      )
    }

    "render page with link 'Find out more about COPE and contracting out'" in {
      assertEqualsValue(
        htmlAccountDoc,
        "[data-spec='state_pension_cope__p8']",
        "Find out more about COPE and contracting out (opens in new tab)."
      )
    }

    "render page with href link 'Find out more about COPE and contracting out'" in {
      assertLinkHasValue(
        htmlAccountDoc,
        "[data-spec='state_pension_cope__link1']",
        "https://www.gov.uk/government/publications/state-pension-fact-sheets/contracting-out-and-why-we-may-have-included-a-contracted-out-pension-equivalent-cope-amount-when-you-used-the-online-service"
      )
    }

    "render page with link 'Back'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='state_pension_cope__backlink']",
        "nisp.back"
      )
    }

    "render page with href link 'Back'" in {
      assertLinkHasValue(
        htmlAccountDoc,
        "[data-spec='state_pension_cope__backlink']",
        "/check-your-state-pension/account"
      )
    }
  }
}
