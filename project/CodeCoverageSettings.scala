/*
 * Copyright 2024 HM Revenue & Customs
 *
 */

import sbt.Setting
import scoverage.ScoverageKeys

object CodeCoverageSettings {
  val excludedPackages: Seq[String] = Seq(
    ".*Reverse.*",
    ".*Routes.*",
    "view.*",
    ".*ErrorHandler.*",
    ".*\\$anon.*",
    ".*PertaxErrorView.*"
  )
  def apply(): Seq[Setting[?]] = Seq(
    ScoverageKeys.coverageMinimumBranchTotal := 83,
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    ScoverageKeys.coverageExcludedPackages:= excludedPackages.mkString(",")
  )
}
