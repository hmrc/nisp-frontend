/*
 * Copyright 2023 HM Revenue & Customs
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

import com.google.inject.ImplementedBy
import play.api.Logging
import play.api.i18n.Messages
import play.api.mvc.Request
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.controllers.auth.AuthenticatedRequest
import uk.gov.hmrc.nisp.controllers.routes
import uk.gov.hmrc.nisp.models.admin.SCAWrapperToggle
import uk.gov.hmrc.nisp.views.html.oldMain
import uk.gov.hmrc.nisp.views.html.components.{additionalScripts, additionalStylesheets}
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.ptafrontend.views.html.PtaHead
import uk.gov.hmrc.sca.models.BannerConfig
import uk.gov.hmrc.sca.services.WrapperService

import javax.inject.Inject
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}
import scala.util.{Failure, Success, Try}

@ImplementedBy(classOf[MainImpl])
trait Main {
  def apply(
             pageTitle: String,
             showUrBanner: Boolean = true,
             extendedTitle: Boolean = true,
             optCustomLayout: Option[Html => Html] = None,
             sidebar: Option[Html] = None,
             hideNavBar: Boolean = false
           )(
             contentBlock: Html
           )(implicit
             request: Request[_],
             messages: Messages
           ): HtmlFormat.Appendable
}

class MainImpl @Inject() (
                           appConfig: ApplicationConfig,
                           featureFlagService: FeatureFlagService,
                           wrapperService: WrapperService,
                           oldMain: oldMain,
                           additionalStyles: additionalStylesheets,
                           additionalScripts: additionalScripts,
                           ptaHead: PtaHead
                         ) extends Main with Logging {

  //noinspection ScalaStyle
  override def apply(
                      pageTitle: String,
                      showUrBanner: Boolean,
                      extendedTitle: Boolean,
                      optCustomLayout: Option[Html => Html],
                      sidebar: Option[Html],
                      hideNavBar: Boolean
                    )(
                      contentBlock: Html
                    )(implicit request: Request[_], messages: Messages): HtmlFormat.Appendable = {
    val scaWrapperToggle = Await.result(featureFlagService.get(SCAWrapperToggle), Duration(appConfig.scaWrapperFutureTimeout, SECONDS))
    val fullPageTitle    = if(extendedTitle) {
      s"$pageTitle - ${Messages("nisp.title.extension")} - GOV.UK"
    } else pageTitle
    val trustedHelper    = Try(request.asInstanceOf[AuthenticatedRequest[_]]) match {
      case Failure(_: java.lang.ClassCastException) => None
      case Success(value)                           => value.nispAuthedUser.trustedHelper
      case Failure(exception)                       => throw exception
    }

    if (scaWrapperToggle.isEnabled) {
      logger.debug(s"SCA Wrapper layout used for request `${request.uri}``")

      wrapperService.layout(
        content = contentBlock,
        pageTitle = Some(fullPageTitle),
        serviceNameKey = Some("nisp.title"),
        serviceNameUrl = Some("/check-your-state-pension/account"),
        sidebarContent = sidebar,
        signoutUrl = routes.StatePensionController.signOut.url,
        //timeOutUrl = appConfig.timeOutUrl,
        keepAliveUrl = routes.TimeoutController.keep_alive.url,
        showBackLinkJS = true,
        //backLinkUrl: Option[String] = None,
        showSignOutInHeader = true,
        scripts = Seq(additionalScripts()),
        styleSheets = Seq(
          additionalStyles(),
          ptaHead()
        ),
        bannerConfig = BannerConfig(
          showChildBenefitBanner = false,
          showAlphaBanner = false,
          showBetaBanner = false,
          showHelpImproveBanner = showUrBanner
        ),
        optTrustedHelper = trustedHelper,
        fullWidth = false,
        hideMenuBar = hideNavBar,
        //disableSessionExpired = disableSessionExpired
      )(messages, HeaderCarrierConverter.fromRequest(request), request)

    } else {
      logger.debug(s"Old layout used for request `${request.uri}``")

      oldMain(
        pageTitle,
        showUrBanner = showUrBanner,
        sidebar = sidebar,
        optCustomLayout = optCustomLayout,
        hideNavBar = hideNavBar
      )(
        contentBlock
      )
    }
  }
}
