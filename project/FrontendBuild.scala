import sbt._

object FrontendBuild extends Build with MicroService {

  val appName = "nisp-frontend"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {

  private val playHealthVersion = "2.0.0"
  private val frontendBootstrapVersion = "7.10.0"
  private val govukTemplateVersion = "5.0.0"
  private val playUiVersion = "5.2.0"
  private val playAuthorisedFrontendVersion = "6.2.0"
  private val playLanguageVersion = "3.0.0"
  private val httpCachingClientVersion = "6.1.0"
  private val cspClientVersion = "1.0.0"

  val compile = Seq(
    "uk.gov.hmrc" %% "frontend-bootstrap" % frontendBootstrapVersion,
    "uk.gov.hmrc" %% "play-authorised-frontend" % playAuthorisedFrontendVersion,
    "uk.gov.hmrc" %% "http-caching-client" % httpCachingClientVersion,
    "uk.gov.hmrc" %% "play-language" % playLanguageVersion,
    "uk.gov.hmrc" %% "govuk-template" % govukTemplateVersion,
    "uk.gov.hmrc" %% "play-health" % playHealthVersion,
    "uk.gov.hmrc" %% "play-ui" % playUiVersion,
    "uk.gov.hmrc" %% "tax-year" % "0.3.0",
    "uk.gov.hmrc" %% "csp-client" % cspClientVersion,
    "uk.gov.hmrc" %% "play-conditional-form-mapping" % "0.2.0"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    private val hmrcTestVersion = "2.2.0"
    private val pegdownVersion = "1.6.0"
    private val jsoupVersion = "1.10.2"
    private val scalaTestPlusVersion = "1.5.1"
    private val mockitoCoreVersion = "2.6.3"

    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "org.jsoup" % "jsoup" % jsoupVersion % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.mockito" % "mockito-core" % mockitoCoreVersion
      )
    }.test
  }

  def apply() = compile ++ Test()
}

