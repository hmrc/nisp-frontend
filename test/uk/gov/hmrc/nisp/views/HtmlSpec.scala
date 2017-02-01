/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.views.html



import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.i18n.Messages
import org.scalatest._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.nisp.config.ApplicationConfig
import uk.gov.hmrc.nisp.connectors.NispConnector
import uk.gov.hmrc.nisp.controllers.connectors.CustomAuditConnector
import uk.gov.hmrc.nisp.helpers._
import uk.gov.hmrc.nisp.models.{SPAmountModel, StatePensionAmount, StatePensionAmountRegular}
import uk.gov.hmrc.nisp.services.CitizenDetailsService
import uk.gov.hmrc.play.frontend.auth.{AuthContext, AuthenticationProviderIds, LoggedInUser, Principal}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.SessionKeys
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.time.DateTimeUtils.now
import play.twirl.api.Html
import org.apache.commons.lang3.StringEscapeUtils
import uk.gov.hmrc.nisp.controllers.auth.NispUser
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Accounts, ConfidenceLevel, CredentialStrength, PayeAccount}

trait HtmlSpec extends UnitSpec  {

        implicit val request = FakeRequest()

       implicit val authContext = {
        val user = LoggedInUser("", None, None, None, CredentialStrength.None , ConfidenceLevel.L500)
               val principal = Principal(None, accounts = Accounts(paye = Some(PayeAccount("", TestAccountBuilder.regularNino))))
        AuthContext(user, principal, None, None, None)
       }

        implicit val nispUser = NispUser(
                authContext = authContext,
                Some("First Last"),
                "",
                None,
                None,
                None
        )

        def asDocument(html: String): Document = Jsoup.parse(html)

        def optionalAttr(doc : Document, cssSelector : String, attributeKey:String) : Option[String] = {
        val element = doc.select(cssSelector).first()

        if (element != null && element.hasAttr(attributeKey)) Some(element.attr(attributeKey)) else None
        }

        def assertEqualsMessage(doc : Document, cssSelector : String, expectedMessageKey: String) = {
        val elements = doc.select(cssSelector)

        if(elements.isEmpty) throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

        assertMessageKeyHasValue(expectedMessageKey)
        //<p> HTML elements are rendered out with a carriage return on some pages, so discount for comparison
        assert(elements.first().html().replace("\n", "") == Html(Messages(expectedMessageKey)).toString())
        }

        def assertEqualsValue(doc : Document, cssSelector : String, expectedValue: String) = {
        val elements = doc.select(cssSelector)

        if(elements.isEmpty) throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

        //<p> HTML elements are rendered out with a carriage return on some pages, so discount for comparison
        assert(elements.first().html().replace("\n", "") == expectedValue)
        }

        def assertMessageKeyHasValue(expectedMessageKey: String): Unit = {
        assert(expectedMessageKey != Html(Messages(expectedMessageKey)).toString(), s"$expectedMessageKey has no messages file value setup")
        }

        def assertContainsMessage(doc : Document, cssSelector : String, expectedMessageKey: String) = {
        assertContainsDynamicMessage(doc, cssSelector, expectedMessageKey)
        }

        def assertContainsDynamicMessage(doc : Document, cssSelector : String, expectedMessageKey: String, messageArgs: String*) = {
        val elements = doc.select(cssSelector)

        if(elements.isEmpty) throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

        assertMessageKeyHasValue(expectedMessageKey)
        //<p> HTML elements are rendered out with a carriage return on some pages, so discount for comparison
        val expectedString = Html(Messages(expectedMessageKey, messageArgs: _*)).toString()

        assert(elements.toArray(new Array[Element](elements.size())).exists { element =>
        element.html().replace("\n", "").contains(StringEscapeUtils.escapeHtml4(expectedString))
        })
        }

        def assertRenderedByCssSelector(doc: Document, cssSelector: String) = {
        assert(!doc.select(cssSelector).isEmpty, "Element " + cssSelector + " was not rendered on the page.")
        }

        def assertNotRenderedByCssSelector(doc: Document, cssSelector: String) = {
        assert(doc.select(cssSelector).isEmpty, "\n\nElement " + cssSelector + " was rendered on the page.\n")
        }

        def assertLinkHasValue(doc: Document, id: String, linkValue: String) = {
        assert(doc.select(s"#$id").attr("href") === linkValue)
        }

        def assertRenderedById(doc: Document, id: String) = {
        assert(doc.getElementById(id) != null, "\n\nElement " + id + " was not rendered on the page.\n")
        }

        def assertNotRenderedById(doc: Document, id: String) = {
        assert(doc.getElementById(id) == null, "\n\nElement " + id + " was rendered when not expected.\n")
        }

        def assertElementHasValue(doc: Document, id: String, value: String) = {
        assertRenderedById(doc, id)

        assert(doc.getElementById(id).attr("value") == value, s"\n\nElement $id has incorrect value. Expected '$value', found '${doc.getElementById(id).attr("value")}'.")
        }

        def assertContainsText(doc:Document, text: String) = assert(doc.toString.contains(text), "\n\ntext " + text + " was not rendered on the page.\n")

        def assertNotContainsText(doc:Document, text: String) = assert(!doc.toString.contains(text), "\n\ntext " + text + " was rendered on the page.\n")

        def assertElementContainsText(doc: Document, cssSelector: String, text: String) = {
        val elements = doc.select(cssSelector)

        if(elements.isEmpty)
        throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

        assert(elements.first().html().contains(text), s"\n\nText '$text' was not rendered inside '$cssSelector'.\n")
        }

        def assertElementNotContainsText(doc: Document, cssSelector: String, text: String) = {
        val elements = doc.select(cssSelector)

        if(elements.isEmpty)
        throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

        assert(!elements.first().html().contains(text), s"\n\nText '$text' was rendered inside '$cssSelector'.\n")
        }

        def assertHasClass(doc: Document, cssSelector: String, className: String) = {
        val elements = doc.select(cssSelector)

        if(elements.isEmpty)
        throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

        assert(elements.first().hasClass(className), s"\n\nElement '$cssSelector' doesn't have '$className' class.\n")
        }

        def assertDoesNotHaveClass(doc: Document, cssSelector: String, className: String) = {
        val elements = doc.select(cssSelector)

        if(elements.isEmpty)
        throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

        assert(!elements.first().hasClass(className), s"\n\nElement '$cssSelector' has '$className' class.\n")
        }
        }
