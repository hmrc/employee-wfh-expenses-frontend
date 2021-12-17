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

import uk.gov.hmrc.time.TaxYear
import utils.TaxYearDates._


case class SelectedTaxYears(checkboxYearOptions: List[String]) {

  val claimingAllYears: SelectedTaxYears = SelectedTaxYears(List("option1", "option2", "option3"))
  val claiming2022Only: SelectedTaxYears = SelectedTaxYears(List("option1"))
  val claiming2021Only: SelectedTaxYears = SelectedTaxYears(List("option2"))
  val claimingPrevOnly: SelectedTaxYears = SelectedTaxYears(List("option3"))
  val claiming2022And2021: SelectedTaxYears = SelectedTaxYears(List("option1", "option2"))
  val claiming2022AndPrev: SelectedTaxYears = SelectedTaxYears(List("option1", "option3"))
  val claiming2021AndPrev: SelectedTaxYears = SelectedTaxYears(List("option2", "option3"))

  def dateListAllYears = List(
    (TaxYear(YEAR_2022).starts, TaxYear(YEAR_2022).finishes),
    (TaxYear(YEAR_2021).starts, TaxYear(YEAR_2021).finishes),
    (TaxYear(YEAR_2020).starts, TaxYear(YEAR_2020).finishes)
  )
  def dateList2022And2021 = List(
    (TaxYear(YEAR_2022).starts, TaxYear(YEAR_2022).finishes),
    (TaxYear(YEAR_2021).starts, TaxYear(YEAR_2021).finishes)
  )
  def dateList2022AndPrev = List(
    (TaxYear(YEAR_2022).starts, TaxYear(YEAR_2022).finishes),
    (TaxYear(YEAR_2020).starts, TaxYear(YEAR_2020).finishes)
  )
  def dateList2021AndPrev = List(
    (TaxYear(YEAR_2021).starts, TaxYear(YEAR_2021).finishes),
    (TaxYear(YEAR_2020).starts, TaxYear(YEAR_2020).finishes)
  )
  def dateList2022Only = List((TaxYear(YEAR_2022).starts, TaxYear(YEAR_2022).finishes))
  def dateList2021Only = List((TaxYear(YEAR_2021).starts, TaxYear(YEAR_2021).finishes))
  def dateListPrevOnly = List((TaxYear(YEAR_2020).starts, TaxYear(YEAR_2020).finishes))


}