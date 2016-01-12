/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.nisp.services

import com.codahale.metrics.Meter
import com.kenshoo.play.metrics.MetricsRegistry
import uk.gov.hmrc.nisp.models.enums.ABTest.ABTest
import uk.gov.hmrc.nisp.models.enums.{APIType, ABTest, SPExclusion, SPContextMessage}
import uk.gov.hmrc.nisp.models.enums.SPContextMessage.SPContextMessage
import uk.gov.hmrc.nisp.models.enums.SPExclusion.SPExclusion

trait MetricsService {
  def mainPage(forecast: BigDecimal, current: BigDecimal, scenario: Option[SPContextMessage], contractedOutFlag: Boolean, age: Int, abTest: Option[ABTest])
  def niRecord(gaps: Int, payableGaps: Int, pre75Years: Int, qualifyingYears: Int, yearsUntilSPA: Int)
  def exclusion(exclusions: List[SPExclusion])
}

object MetricsService extends MetricsService {
  val mainPageMeter = MetricsRegistry.defaultRegistry.meter("main-page")
  val niRecordPageMeter = MetricsRegistry.defaultRegistry.meter("ni-record-page")
  val contractedOutMeter = MetricsRegistry.defaultRegistry.meter("contracted-out")
  val notContractedOutMeter = MetricsRegistry.defaultRegistry.meter("not-contracted-out")
  val ageUpTo30 = MetricsRegistry.defaultRegistry.meter("age-upto-30")
  val age31To45 = MetricsRegistry.defaultRegistry.meter("age-31-to-45")
  val age46To55 = MetricsRegistry.defaultRegistry.meter("age-46-to-55")
  val age56To65 = MetricsRegistry.defaultRegistry.meter("age-56-to-65")
  val age66AndAbove = MetricsRegistry.defaultRegistry.meter("age-66-and-above")

  val gapsMeter = MetricsRegistry.defaultRegistry.histogram("gaps")
  val payableGapsMeter = MetricsRegistry.defaultRegistry.histogram("payable-gaps")
  val pre75YearsMeter = MetricsRegistry.defaultRegistry.histogram("pre75-years")
  val qualifyingYearsMeter = MetricsRegistry.defaultRegistry.histogram("qualifying-years")
  val forecastAmountMeter = MetricsRegistry.defaultRegistry.histogram("forecast-amount")
  val currentAmountMeter = MetricsRegistry.defaultRegistry.histogram("current-amount")
  val yearsUntilSPAMeter = MetricsRegistry.defaultRegistry.histogram("years-until-spa")

  val scenarioMeters: Map[SPContextMessage, Meter] = Map(
    SPContextMessage.ScenarioOne -> MetricsRegistry.defaultRegistry.meter("scenario-1"),
    SPContextMessage.ScenarioTwo -> MetricsRegistry.defaultRegistry.meter("scenario-2"),
    SPContextMessage.ScenarioThree -> MetricsRegistry.defaultRegistry.meter("scenario-3"),
    SPContextMessage.ScenarioFour -> MetricsRegistry.defaultRegistry.meter("scenario-4"),
    SPContextMessage.ScenarioFive -> MetricsRegistry.defaultRegistry.meter("scenario-5"),
    SPContextMessage.ScenarioSix -> MetricsRegistry.defaultRegistry.meter("scenario-6"),
    SPContextMessage.ScenarioSeven -> MetricsRegistry.defaultRegistry.meter("scenario-7"),
    SPContextMessage.ScenarioEight -> MetricsRegistry.defaultRegistry.meter("scenario-8")
  )

  val exclusionMeters: Map[SPExclusion, Meter] = Map(
    SPExclusion.Abroad -> MetricsRegistry.defaultRegistry.meter("exclusion-abroad"),
    SPExclusion.MWRRE -> MetricsRegistry.defaultRegistry.meter("exclusion-mwrre"),
    SPExclusion.CustomerTooOld -> MetricsRegistry.defaultRegistry.meter("exclusion-too-old"),
    SPExclusion.ContractedOut -> MetricsRegistry.defaultRegistry.meter("exclusion-contracted-out"),
    SPExclusion.Dead -> MetricsRegistry.defaultRegistry.meter("exclusion-dead"),
    SPExclusion.IOM -> MetricsRegistry.defaultRegistry.meter("exclusion-isle-of-man"),
    SPExclusion.AmountDissonance -> MetricsRegistry.defaultRegistry.meter("amount-dissonance")
  )

  val abTestA = MetricsRegistry.defaultRegistry.meter("abtest-a")
  val abTestB = MetricsRegistry.defaultRegistry.meter("abtest-b")

  val timers = Map(
    APIType.SP -> MetricsRegistry.defaultRegistry.timer("sp-response-timer"),
    APIType.NI -> MetricsRegistry.defaultRegistry.timer("ni-response-timer")
  )

  val successCounters = Map(
    APIType.SP -> MetricsRegistry.defaultRegistry.counter("sp-success-counter"),
    APIType.NI -> MetricsRegistry.defaultRegistry.counter("ni-success-counter")
  )

  val failedCounters = Map(
    APIType.SP -> MetricsRegistry.defaultRegistry.counter("sp-failed-counter"),
    APIType.NI -> MetricsRegistry.defaultRegistry.counter("ni-failed-counter")
  )

  val keystoreReadTimer = MetricsRegistry.defaultRegistry.timer("keystore-read-timer")
  val keystoreWriteTimer = MetricsRegistry.defaultRegistry.timer("keystore-write-timer")

  val keystoreHitCounter = MetricsRegistry.defaultRegistry.counter("keystore-hit-counter")
  val keystoreMissCounter = MetricsRegistry.defaultRegistry.counter("keystore-miss-counter")

  override def mainPage(forecast: BigDecimal, current: BigDecimal, scenario: Option[SPContextMessage],
                        contractedOutFlag: Boolean, age: Int, abTest: Option[ABTest]): Unit = {
    mainPageMeter.mark()
    forecastAmountMeter.update(forecast.toInt)
    currentAmountMeter.update(current.toInt)
    scenario.foreach(scenarioMeters(_).mark())
    if(contractedOutFlag) contractedOutMeter.mark() else notContractedOutMeter.mark()
    mapToAgeMeter(age)
    abTest.foreach(mapToABTestMeter)
  }

  private def mapToAgeMeter(age: Int): Unit = {
    if (56 to 65 contains age)
      age56To65.mark()
    else if (age > 65)
      age66AndAbove.mark()
    else if (46 to 55 contains age)
      age46To55.mark()
    else if (31 to 45 contains age)
      age31To45.mark()
    else
      ageUpTo30.mark()
  }

  private def mapToABTestMeter(abTest: ABTest) = abTest match {
    case ABTest.A => abTestA.mark()
    case ABTest.B => abTestB.mark()
  }

  override def niRecord(gaps: Int, payableGaps: Int, pre75Years: Int, qualifyingYears: Int, yearsUntilSPA: Int): Unit = {
    niRecordPageMeter.mark()
    gapsMeter.update(gaps)
    payableGapsMeter.update(payableGaps)
    pre75YearsMeter.update(pre75Years)
    qualifyingYearsMeter.update(qualifyingYears)
    yearsUntilSPAMeter.update(yearsUntilSPA)
  }

  override def exclusion(exclusions: List[SPExclusion]): Unit = exclusions.foreach(exclusionMeters(_).mark())
}
