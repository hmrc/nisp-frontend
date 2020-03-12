package uk.gov.hmrc.nisp.messages

import java.io.{File, FileNotFoundException}
import java.util.Properties

import uk.gov.hmrc.play.test.UnitSpec

import scala.io.Source

class MessagesSpec extends UnitSpec {

  "Messages" should {

    "assert equal messages key for English/Welsh" in {

      val englishFile = new File(".", "conf/messages")
      val welshFile = new File(".", "conf/messages.cy")
      val propertiesEnglish: Properties = new Properties()
      val propertiesWelsh: Properties = new Properties()
      val elementsEnglish = propertiesEnglish.propertyNames
      val elementsWelsh = propertiesWelsh.propertyNames

      if (englishFile.exists()) {
        val sourceEnglish = Source.fromURL(englishFile.toURL)
        propertiesEnglish.load(sourceEnglish.bufferedReader())
      }
      else {
        throw new FileNotFoundException("Messages file cannot be loaded")
      }
      if (welshFile.exists()) {
        val sourceWelsh = Source.fromURL(welshFile.toURL)
        propertiesWelsh.load(sourceWelsh.bufferedReader())
      }
      else {
        throw new FileNotFoundException("Messages.cy file can not be loaded")
      }
      while (elementsEnglish.hasMoreElements && elementsWelsh.hasMoreElements) {
        val nextEnglish = elementsEnglish.nextElement()
        val nextWelsh = elementsWelsh.nextElement()
        println(nextEnglish, nextWelsh)
        nextEnglish should be(nextWelsh)
      }
    }
  }
}

