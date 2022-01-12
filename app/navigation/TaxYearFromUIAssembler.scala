/*
 * Copyright 2022 HM Revenue & Customs
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

import java.time.LocalDate

case class TaxYearFromUIAssembler(checkboxYearOptions: List[String]) {

  private val claimingAllYears = List("option1", "option2", "option3")
  private val claiming2022And2021 = List("option1", "option2")
  private val claiming2022Only = List("option1")
  private val claiming2021AndPrev = List("option2", "option3")
  private val claiming2021Only = List("option2")
  private val claiming2022AndPrev = List("option1", "option3")
  private val claimingPrevOnly = List("option3")

  private val validateInputList = {
    if (checkboxYearOptions == null || checkboxYearOptions.isEmpty) {
      throw new IllegalArgumentException("Input checkbox list is either empty or invalid")
    }
  }

  val assemble: List[(LocalDate, LocalDate)] = {
    if(checkboxYearOptions == claimingAllYears) {
      List(
        (TAX_YEAR_2022_START_DATE, TAX_YEAR_2022_END_DATE),
        (TAX_YEAR_2021_START_DATE, TAX_YEAR_2021_END_DATE),
        (TAX_YEAR_2020_START_DATE, TAX_YEAR_2020_END_DATE)
      )
    }
    else if(checkboxYearOptions == claiming2022Only) {
      List((TAX_YEAR_2022_START_DATE, TAX_YEAR_2022_END_DATE))
    }
    else if(checkboxYearOptions == claiming2021Only) {
      List((TAX_YEAR_2021_START_DATE, TAX_YEAR_2021_END_DATE))
    }
    else if(checkboxYearOptions == claimingPrevOnly) {
      List((TAX_YEAR_2020_START_DATE, TAX_YEAR_2020_END_DATE))
    }
    else if(checkboxYearOptions == claiming2022And2021) {
      List(
        (TAX_YEAR_2022_START_DATE, TAX_YEAR_2022_END_DATE),
        (TAX_YEAR_2021_START_DATE, TAX_YEAR_2021_END_DATE)
      )
    }
    else if(checkboxYearOptions == claiming2022AndPrev) {
      List(
        (TAX_YEAR_2022_START_DATE, TAX_YEAR_2022_END_DATE),
        (TAX_YEAR_2020_START_DATE, TAX_YEAR_2020_END_DATE)
      )
    }
    else if(checkboxYearOptions == claiming2021AndPrev) {
      List(
        (TAX_YEAR_2021_START_DATE, TAX_YEAR_2021_END_DATE),
        (TAX_YEAR_2020_START_DATE, TAX_YEAR_2020_END_DATE)
      )
    }
    else {
      List.empty
    }

  }

  val isPreviousTaxYearSelected: Boolean = checkboxYearOptions.contains("option3")

  val showSecondTaxReliefMessageBlock: Boolean = checkboxYearOptions.contains("option2") || isPreviousTaxYearSelected

  val showFirstTaxReliefMessageBlock: Boolean = checkboxYearOptions.contains("option1")

}