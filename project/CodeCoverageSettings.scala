/*
 * Copyright 2024 HM Revenue & Customs
 *
 */

import sbt.Setting
import scoverage.ScoverageKeys

object CodeCoverageSettings {
  def apply(): Seq[Setting[_]] = Seq(
    ScoverageKeys.coverageMinimumBranchTotal := 83,
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    ScoverageKeys.coverageExcludedPackages:= ".*Reverse.*;.*Routes.*;view.*",
  )
}
