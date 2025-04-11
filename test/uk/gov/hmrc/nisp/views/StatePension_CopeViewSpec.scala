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

package uk.gov.hmrc.nisp.views

import org.apache.commons.text.StringEscapeUtils
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.concurrent.ScalaFutures
import play.api.Application
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers.contentAsString
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.{LoginTimes, Name}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlag
import uk.gov.hmrc.nisp.builders.NationalInsuranceTaxYearBuilder
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.StatePensionController
import uk.gov.hmrc.nisp.controllers.auth.{AuthDetails, AuthRetrievals, AuthenticatedRequest, GracePeriodAction, NispAuthedUser, PertaxAuthAction}
import uk.gov.hmrc.nisp.controllers.pertax.PertaxHelper
import uk.gov.hmrc.nisp.helpers.*
import uk.gov.hmrc.nisp.models.*
import uk.gov.hmrc.nisp.models.admin.NewStatePensionUIToggle
import uk.gov.hmrc.nisp.models.pertaxAuth.PertaxAuthResponseModel
import uk.gov.hmrc.nisp.repositories.SessionCache
import uk.gov.hmrc.nisp.services.{GracePeriodService, MetricsService, NationalInsuranceService, StatePensionService}
import uk.gov.hmrc.nisp.utils.Constants.ACCESS_GRANTED
import uk.gov.hmrc.nisp.utils.{Constants, PertaxAuthMockingHelper}
import uk.gov.hmrc.nisp.views.html.statepension_cope
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.language.LanguageUtils

import java.time.{Instant, LocalDate}
import scala.concurrent.Future

class StatePension_CopeViewSpec extends HtmlSpec with ScalaFutures with Injecting with WireMockSupport with PertaxAuthMockingHelper {

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
  val mocGracePeriodService: GracePeriodService              = mock[GracePeriodService]

  lazy val langUtils: LanguageUtils = inject[LanguageUtils]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockStatePensionService)
    reset(mockNationalInsuranceService)
    reset(mockAuditConnector)
    reset(mockAppConfig)
    reset(mockPertaxHelper)
    wireMockServer.resetAll()
    when(mockPertaxHelper.isFromPertax(any())).thenReturn(Future.successful(false))
    when(mockAppConfig.accessibilityStatementUrl(any())).thenReturn("/foo")
    when(mockAppConfig.reportAProblemNonJSUrl).thenReturn("/reportAProblem")
    when(mockAppConfig.contactFormServiceIdentifier).thenReturn("/id")
    when(mockAppConfig.pertaxAuthBaseUrl).thenReturn(s"http://localhost:${wireMockServer.port()}")
    mockPertaxAuth(PertaxAuthResponseModel(
      ACCESS_GRANTED, "", None, None
    ), mockUserNino.nino)
    when(mockFeatureFlagService.get(NewStatePensionUIToggle))
      .thenReturn(Future.successful(FeatureFlag(NewStatePensionUIToggle, isEnabled = false)))
  }

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[AuthRetrievals].to[FakeAuthAction],
      bind[StatePensionService].toInstance(mockStatePensionService),
      bind[NationalInsuranceService].toInstance(mockNationalInsuranceService),
      bind[AuditConnector].toInstance(mockAuditConnector),
      bind[ApplicationConfig].toInstance(mockAppConfig),
      bind[PertaxHelper].toInstance(mockPertaxHelper),
      bind[PertaxAuthAction].to[FakePertaxAuthAction],
      bind[GracePeriodAction].to[FakeGracePeriodAction],
      bind[GracePeriodService].toInstance(mocGracePeriodService),
      featureFlagServiceBinding
    )
    .build()

  val statePensionController: StatePensionController = inject[StatePensionController]

  "Render State Pension view with Contracted out User" should {

    def mockSetup: OngoingStubbing[Future[Either[UpstreamErrorResponse, Either[StatePensionExclusionFilter, NationalInsuranceRecord]]]] = {

      when(mockStatePensionService.getSummary(any())(any()))
        .thenReturn(Future.successful(Right(Right(StatePension(
          LocalDate.of(2014, 4, 5),
          StatePensionAmounts(
            protectedPayment = false,
            StatePensionAmountRegular(46.38, 201.67, 2420.04),
            StatePensionAmountForecast(3, 155.55, 622.35, 76022.24),
            StatePensionAmountMaximum(3, 0, 155.55, 622.35, 76022.24),
            StatePensionAmountRegular(50, 217.41, 2608.93))
          ,64, LocalDate.of(2021, 7, 18), "2017-18", 30, pensionSharingOrder = false, 155.65, reducedRateElection = false, statePensionAgeUnderConsideration = false)
        ))))

      when(mockNationalInsuranceService.getSummary(any())(any()))
        .thenReturn(Future.successful(Right(Right(NationalInsuranceRecord(
          qualifyingYears = 11,
          qualifyingYearsPriorTo1975 = 0,
          numberOfGaps = 2,
          numberOfGapsPayable = 2,
          Some(LocalDate.of(1954, 3, 6)),
          homeResponsibilitiesProtection = false,
          LocalDate.of(2017, 4, 5),
          List(

            NationalInsuranceTaxYearBuilder("2015-16", underInvestigation = false),
            NationalInsuranceTaxYearBuilder("2014-15", qualifying = false, underInvestigation = false),
            NationalInsuranceTaxYearBuilder("2013-14", underInvestigation = false)
          ),
          reducedRateElection = false
        )
        ))))

    }

    lazy val result         = statePensionController.show()(AuthenticatedRequest(FakeRequest(), user, authDetails))
    lazy val htmlAccountDoc = asDocument(contentAsString(result))

    "render with correct page title" in {
      mockSetup
      assertElementContainsText(
        htmlAccountDoc,
        "head > title",
        messages("nisp.main.h1.title")
          + Constants.titleSplitter
          + messages("nisp.title.extension")
          + Constants.titleSplitter
          + messages("nisp.gov-uk")
      )
    }

    "render page with heading 'Your State Pension' " in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='state_pension__pageheading'] [data-component='nisp_page_heading__h1']",
        "nisp.main.h1.title"
      )
    }

    "render page with text 'You can get your State Pension on 18 july 2012' " in {
      assertEqualsValue(
        htmlAccountDoc,
        "[data-spec='state_pension__panel1'] [data-component='nisp_panel__title']",
        Messages("nisp.main.basedOn") + " " + langUtils.Dates.formatDate(LocalDate.of(2021, 7, 18))
      )
    }

    "render page with text 'Your forecast is £155.55 a week, £622.35 a month, £76,022.24 a year' " in {
      val sMessage =
        Messages("nisp.main.caveats") + " " +
          Messages("nisp.is") + " £155.55 " +
          Messages("nisp.main.week") + ", £622.35 " +
          Messages("nisp.main.month") + ", £76,022.24 " +
          Messages("nisp.main.year")
      assertEqualsValue(
        htmlAccountDoc,
        "[data-spec='state_pension__panel1__caveats']",
        sMessage
      )
    }

    "render page with text ' Your forecast '" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='continue_working__p__caveats']",
        "nisp.main.caveats"
      )
    }

    "render page with text ' is not a guarantee and is based on the current law '" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='continue_working__ul__caveats__1']",
        "nisp.main.notAGuarantee"
      )
    }

    "render page with text ' does not include any increase due to inflation '" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='continue_working__ul__caveats__2']",
        "nisp.main.inflation"
      )
    }

    "render page with heading 'You need to continue to contribute National Insurance to reach your forecast'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='continue_working__h2_1']",
        "nisp.main.continueContribute"
      )
    }

    "render page with heading 'Estimate based on your National Insurance record up to 5 April 2014'" in {
      assertContainsDynamicMessage(
        htmlAccountDoc,
        "[data-spec='continue_working__chart1'] [data-component='nisp_chart__title']",
        "nisp.main.chart.lastprocessed.title",
        "2014"
      )
    }

    "render page with heading '£46.38 a week'" in {
      val sWeek = "£46.38 " + Messages("nisp.main.week")
      assertEqualsValue(
        htmlAccountDoc,
        "[data-spec='continue_working__chart1'] [data-component='nisp_chart__inner_text']",
        sWeek
      )
    }

    "render page with Heading '£155.55 is the most you can get'" in {
      val sMaxCanGet = "£155.55 " + StringEscapeUtils.unescapeHtml4(Messages("nisp.main.mostYouCanGet"))
      assertEqualsValue(
        htmlAccountDoc,
        "[data-spec='continue_working__h2_2']",
        sMaxCanGet
      )
    }

    "render page with text 'You cannot improve your forecast any further, unless you choose to put off claiming.'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='continue_working__p2']",
        "nisp.main.context.willReach"
      )
    }

    "render page with text 'If you’re working you may still need to pay National Insurance contributions until 18 " +
      "July 2021 as they fund other state benefits and the NHS.'" in {
        assertContainsDynamicMessage(
          htmlAccountDoc,
          "[data-spec='continue_working__p3']",
          "nisp.main.context.reachMax.needToPay",
          langUtils.Dates.formatDate(LocalDate.of(2021, 7, 18))
        )
      }

    "render page with link 'View your National Insurance Record'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='continue_working__link1']",
        "nisp.main.showyourrecord"
      )
    }

    "render page with href link 'View your National Insurance Record'" in {
      assertLinkHasValue(
        htmlAccountDoc,
        "[data-spec='continue_working__link1']",
        "/check-your-state-pension/account/nirecord"
      )
    }

    /*Contracting out affects*/
    "render page with text 'You’ve been in a contracted-out pension scheme'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='contracted_out__h2_1']",
        "nisp.cope.title1"
      )
    }

    "render page with text 'Like most people, you were contracted out of part of the State Pension.'" in {
      assertEqualsValue(
        htmlAccountDoc,
        "[data-spec='contracted_out__p1']",
        messages("nisp.cope.likeMostPeople", messages("nisp.cope.likeMostPeople.linktext")) + "."
      )
    }
    /*Ends*/

    "render page with heading 'Putting off claiming'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='deferral__h2_1']",
        "nisp.main.puttingOff"
      )
    }

    "render page with text 'You can put off claiming your State Pension from 18 July 2021. Doing this may mean you get extra State Pension when you do come to claim it. The extra amount, along with your State Pension, forms part of your taxable income.'" in {
      assertContainsDynamicMessage(
        htmlAccountDoc,
        "[data-spec='deferral__p1']",
        "nisp.main.puttingOff.line1",
        langUtils.Dates.formatDate(LocalDate.of(2021, 7, 18))
      )
    }

    "render page with link 'More on putting off claiming (opens in new tab)'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='deferral__link1']",
        "nisp.main.puttingOff.linkTitle"
      )
    }

    "render page with href link 'More on putting off claiming (opens in new tab)'" in {
      assertLinkHasValue(
        htmlAccountDoc,
        "[data-spec='deferral__link1']",
        "https://www.gov.uk/deferring-state-pension"
      )
    }

    /*Side bar help*/
    "render page with heading 'Get help'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='state_pension__sidebar_h2']",
        "nisp.nirecord.helpline.getHelp"
      )
    }

    "render page with text 'Helpline 0800 731 0181'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='state_pension__sidebar_p1']",
        "nisp.nirecord.helpline.number"
      )
    }

    "render page with text 'Textphone 0800 731 0176'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='state_pension__sidebar_p2']",
        "nisp.nirecord.helpline.textNumber"
      )
    }

    "render page with text 'Monday to Friday: 8am to 6pm'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='state_pension__sidebar_p3']",
        "nisp.nirecord.helpline.openTimes"
      )
    }
  }

  "Render Contracted Out View" should {
    lazy val sResult        = inject[statepension_cope]
    lazy val htmlAccountDoc = asDocument(sResult(99.54, isPertaxUrl = true).toString)

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

    /*Side bar help*/
    "render page with heading 'Get help'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='state_pension_cope__sidebar_h2']",
        "nisp.nirecord.helpline.getHelp"
      )
    }

    "render page with text 'Helpline 0800 731 0181'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='state_pension_cope__sidebar_p1']",
        "nisp.nirecord.helpline.number"
      )
    }

    "render page with text 'Textphone 0800 731 0176'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='state_pension_cope__sidebar_p2']",
        "nisp.nirecord.helpline.textNumber"
      )
    }

    "render page with text 'Monday to Friday: 8am to 6pm'" in {
      assertEqualsMessage(
        htmlAccountDoc,
        "[data-spec='state_pension_cope__sidebar_p3']",
        "nisp.nirecord.helpline.openTimes"
      )
    }
  }
}
