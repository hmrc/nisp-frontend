package uk.gov.hmrc.nisp.controllers

import org.slf4j.{Logger => Slf4JLogger}
import org.mockito.Mockito.{verify, when}
import org.mockito.ArgumentMatchers._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.Logger
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.FakeRequest
import uk.gov.hmrc.nisp.helpers.MockCachedStaticHtmlPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec

class NispFrontendControllerSpec extends UnitSpec with MockitoSugar with OneAppPerSuite {

  val mockLogger: Slf4JLogger = mock[Slf4JLogger]
  when(mockLogger.isErrorEnabled).thenReturn(true)

  val controller = new NispFrontendController {
    override val logger = new Logger(mockLogger)
    val cachedStaticHtmlPartialRetriever = MockCachedStaticHtmlPartialRetriever
  }

  implicit val request = FakeRequest()

  "onError" should {
    "should log error details" in {
      val result: Result =  controller.onError(new Exception())
      verify(mockLogger).error(anyString(), any[Exception])
    }

    "should return an Internal Server Error (500)" in {
      val result: Result =  controller.onError(new Exception())
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

}
