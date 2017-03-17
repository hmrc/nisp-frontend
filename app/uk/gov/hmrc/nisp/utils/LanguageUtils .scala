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

package uk.gov.hmrc.nisp.utils

import com.ibm.icu.text.SimpleDateFormat
import com.ibm.icu.util.{TimeZone, ULocale}
import org.joda.time.{DateTime, LocalDate}
import play.api.Play
import play.api.i18n.{Lang, Messages}
import play.api.mvc._

/** This object provides access to common language utilities.
  *
  * This object contains language codes for English and Welsh and a
  * function to return the current language based on a request header.
  *
  * Additionally, a Dates object is provided which provides helper
  * functions to return correctly formatted dates in both English
  * and Welsh.
  *
  */
object LanguageUtils {

  import play.api.i18n.Messages.Implicits._

  val EnglishLangCode = "en"
  val WelshLangCode = "cy"

  val English = Lang(EnglishLangCode)
  val Welsh = Lang(WelshLangCode)

  val SwitchIndicatorKey = "switching-language"
  val FlashWithSwitchIndicator = Flash(Map(SwitchIndicatorKey -> "true"))

  /** Returns the current language as a Lang object.
    *
    * This function returns the current language as an i18n Lang object. It first checks
    * that the PLAY_LANG cookie exists from the request object and then gets the value from it.
    * If it does not exist then it returns the accepted language from the request object. If there
    * is no Play application then it just defaults to return the accepted language in the request or
    * use the default language.
    *
    * @param request The RequestHeader object to extract the language information from.
    * @return Lang object containing the current langugage.
    */
  def getCurrentLang(implicit request: RequestHeader): Lang = {
    Play.maybeApplication.map { implicit app =>
      val maybeLangFromCookie = request.cookies.get(Play.langCookieName).flatMap(c => Lang.get(c.value))
      maybeLangFromCookie.getOrElse(Lang.preferred(request.acceptLanguages))
    }.getOrElse(request.acceptLanguages.headOption.getOrElse(Lang.defaultLang))
  }

  /** Helper object to correctly display and format dates in both English and Welsh.
    *
    * This object provides a default implementation of the Dates trait in order to provide
    * support for Welsh and English dates.
    */
  object Dates extends Dates {
    import play.api.Play.current

    override def defaultTimeZone = TimeZone.getTimeZone("Europe/London")

    override def to(implicit messages:Messages) = messages("language.to")

    override def singular(implicit messages:Messages) = messages("language.day.singular")

    override def plural(implicit messages:Messages) = messages("language.day.plural")
  }

  /**
    * A trait that correctly displays and formats dates in multiple languages.
    *
    * This object contains helper methods to correctly format dates in any language supported
    * by the IBM ICU library.
    *
    * This trait requires a default timezone to be defined, as well as String values for the English words:
    * - to
    * - day
    * - days
    *
    * These values should come from a Messages file for each language that needs to be supported.
    *
    */
  trait Dates {

    /** The timezone to use when formatting dates */
    def defaultTimeZone: TimeZone

    /** The value of the word 'to' **/
    def to(implicit messages:Messages): String

    /** The value of the singular of the word 'day' **/
    def singular(implicit messages:Messages): String

    /** The value of the plural of the word 'day' **/
    def plural(implicit messages:Messages): String

    /** Helper methods to format dates using various patterns **/
    private def dateFormat(implicit messages: Messages) = createDateFormatForPattern("d MMMM y")

    private def dateFormatAbbrMonth(implicit messages: Messages) = createDateFormatForPattern("d MMM y")

    private def shortDateFormat(implicit messages: Messages) = createDateFormatForPattern("yyyy-MM-dd")

    private def easyReadingDateFormat(implicit messages: Messages) = createDateFormatForPattern("EEEE d MMMM yyyy")

    private def easyReadingTimestampFormat(implicit messages: Messages) = createDateFormatForPattern("h:mmaa")

    /**
      * Function that returns a simple date format object based on the locale defined in the Lang object.
      *
      * If the lang does not contain a value that is supported by the IBM ICU library then the default
      * Locale is used instead.
      *
      * @param pattern - The date format pattern as a String.
      * @param messages    - The implicit lang object.
      * @return - The SimpleDateFormat configured using the current language and pattern.
      */
    private def createDateFormatForPattern(pattern: String)(implicit messages: Messages): SimpleDateFormat = {
      val langCode: String = messages.lang.code
      val validLang: Boolean = ULocale.getAvailableLocales.contains(new ULocale(langCode))
      val locale: ULocale = if (validLang) new ULocale((langCode)) else ULocale.getDefault
      val sdf = new SimpleDateFormat(pattern, locale)
      sdf.setTimeZone(defaultTimeZone)
      sdf
    }

    /**
      * Converts a LocalDate object into a String with the format "D MMMM Y".
      *
      * This function will return a translated string based on the implicit lang object
      * that is passed through with it.
      *
      * Lang("en") example: 25 January 2015
      * Lang("cy") example: 25 Ionawr 2015
      *
      * @param date The LocalDate object to convert.
      * @param messages The implicit lang object.
      * @return The date as a "D MMMM Y" formatted string.
      */
    def formatDate(date: LocalDate)(implicit messages:Messages): String = dateFormat.format(date.toDate)

    /**
      * Converts an Option LocalDate object into a String with the format "D MMMM Y"
      *
      * This function will return a translated string based on the implicit lang object
      * that is passed through with it. If the option is None then the default value is
      * returned back to the caller.
      *
      * Lang("en") example: 25 January 2015
      * Lang("cy") example: 25 Ionawr 2015
      * None example: default
      *
      * @param date    The Optional LocalDate object to convert.
      * @param default A default value to return if the date option is not set.
      * @param messages    The implicit lang object.
      * @return Either the date as a "D MMMM Y" formatted string or the default value if not set.
      */
    def formatDate(date: Option[LocalDate], default: String)(implicit messages:Messages): String =
      date match {
        case Some(d) => formatDate(d)
        case None => default
      }

    /**
      * Converts a LocalDate object into a human readable String with the format "D MMM Y"
      *
      * This function will return a translated string based on the implicit lang object
      * that is passed through with it.
      *
      * Lang("en") example: 25 Jan 2015
      * Lang("cy") example: 25 Ion 2015
      *
      * @param date The LocalDate object to convert.
      * @param messages The implicit lang object.
      * @return The date as a "D MMM Y" formatted string.
      */
    def formatDateAbbrMonth(date: LocalDate)(implicit messages:Messages) = dateFormatAbbrMonth.format(date.toDate)

    /**
      * Converts an optional DateTime object into a human readable String with the format: "h:mmaa, EEEE d MMMM yyyy"
      *
      * This function will return a translated string based on the implicit lang object
      * that is passed through with it. If the option is None then the default value is
      * returned back to the caller.
      *
      * Lang("en") example: "3:45am, Sunday 25 January 2015"
      * Lang("cy" example: "3:45am, Dydd Sul 25 Ionawr 2015"
      *
      * @param date    The optional DateTime object to convert.
      * @param default The default value to return if the date is missing.
      * @param messages    The implicit lang object.
      * @return The date and time as a "h:mmaa, EEEE d MMMM yyyy" formatted string.
      */
    def formatEasyReadingTimestamp(date: Option[DateTime], default: String)(implicit messages:Messages) =
      date match {
        case Some(d) =>
          val time = easyReadingTimestampFormat.format(d.toDate).toLowerCase
          val date = easyReadingDateFormat.format(d.toDate)
          s"$time, $date"
        case None => default
      }

    /**
      * Converts a LocalDate object into a human readable String with the format: "yyyy-MM-dd"
      *
      * This function will return a translated string based on the implicit lang object
      * that is passed through with it.
      *
      * Lang("en") example: 2015-01-25
      * Lang("cy") example: 2015-01-25
      *
      * @param date - The LocalDate object to be converted.
      * @param messages - The implicit language object.
      * @return The date as a "yyyy-MM-dd" formatted string.
      */
    def shortDate(date: LocalDate)(implicit messages:Messages) = shortDateFormat.format(date.toDate)

    /**
      * Converts two LocalDate objects into a human readable String to show a date range.
      *
      * This function will return a translated string based on the implicit lang object.
      *
      * Lang("en") example: "25 January 2015 to 25 January 2015"
      * Lang("cy") example: "25 Ionawr 2015 i 25 Ionawr 2015"
      *
      * @param startDate The first date.
      * @param endDate   The second date.
      * @param messages      The implicit lang value.
      * @return A string in the format of "D MMMM Y to D MMMM Y"
      */
    def formatDateRange(startDate: LocalDate, endDate: LocalDate)(implicit messages:Messages) = {
      Seq(formatDate(startDate), to, formatDate(endDate)).mkString(" ")
    }

    /**
      * Converts an Int into a string appended by 'days'.
      *
      * This function will return a translated string based on the implicit lang object.
      * It checks to see if the number of days is equal to 1 or not, and then responds with
      * the correct plural or singular value for the word "day".
      *
      * 1, Lang("en") example: 1 day
      * 5, Lang("en") example: 5 days
      *
      * @param numberOfDays - The number of days.
      * @param messages         - The implicit language object.
      * @return A string denoting "x" days.
      */
    def formatDays(numberOfDays: Int)(implicit messages:Messages) = {
      val dayOrDays = if (numberOfDays == 1) singular else plural
      s"$numberOfDays $dayOrDays"
    }
  }

}