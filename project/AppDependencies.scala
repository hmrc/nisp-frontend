import sbt._

object AppDependencies {

  val frontendBootstrapVersion = "12.9.0"
  val playLanguageVersion = "3.4.0"
  val httpCachingClientVersion = "8.1.0"
  val cspClientVersion = "3.1.0"
  val localTemplateRendererVersion = "2.3.0"
  val taxYearVersion="0.5.0"
  val playConditionalFormMappingVersion="0.2.0"
  val playBreadCrumbVersion="1.0.0"

  val compile = Seq(
    "uk.gov.hmrc" %% "frontend-bootstrap" % frontendBootstrapVersion,
    "uk.gov.hmrc" %% "http-caching-client" % httpCachingClientVersion,
    "uk.gov.hmrc" %% "play-language" % playLanguageVersion,
    "uk.gov.hmrc" %% "tax-year" % taxYearVersion,
    "uk.gov.hmrc" %% "csp-client" % cspClientVersion,
    "uk.gov.hmrc" %% "play-conditional-form-mapping" % playConditionalFormMappingVersion,
    "uk.gov.hmrc" %% "local-template-renderer"  % localTemplateRendererVersion,
    "uk.gov.hmrc" %% "play-breadcrumb"  %  playBreadCrumbVersion
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    val hmrcTestVersion = "2.3.0"
    val pegdownVersion = "1.6.0"
    val jsoupVersion = "1.10.2"
    val scalaTestPlusVersion = "1.5.1"
    val mockitoCoreVersion = "2.6.3"

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
  
  val all: Seq[ModuleID] = compile ++ Test()

}

