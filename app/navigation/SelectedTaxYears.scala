/*
 * Copyright 2021 HM Revenue & Customs
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

package navigation

case class SelectedTaxYears(checkboxYearOptions: List[String]) {

  private val Scenario1921: Boolean = checkboxYearOptions.contains("option1")

  private val Scenario202122: Boolean = checkboxYearOptions.contains("option2")

  private val Scenario202223: Boolean = checkboxYearOptions.contains("option3")

  val isScenario1921Only: Boolean = Scenario1921 && !Scenario202122 && !Scenario202223

  val isScenario202122Only: Boolean = !Scenario1921 && Scenario202122 && !Scenario202223

  val isScenario202223Only: Boolean = !Scenario1921 && !Scenario202122 && Scenario202223

  val areAllAvailableTaxYearsSelected = Scenario1921 && Scenario202122 && Scenario202223

  val debugAreAllAvailableTaxYearsSelected = Scenario1921 && Scenario202122

}