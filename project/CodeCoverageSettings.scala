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
    ScoverageKeys.coverageMinimumBranchTotal := 81,
    ScoverageKeys.coverageMinimumStmtTotal := 85,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    ScoverageKeys.coverageExcludedPackages:= excludedPackages.mkString(",")
  )
}
