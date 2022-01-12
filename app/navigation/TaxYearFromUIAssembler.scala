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
      throw new IllegalArgumentException("input checkbox list is either empty or invalid")
    }
  }

  val assemble: List[(LocalDate, LocalDate)] = {
    checkboxYearOptions match {
      case inputList if inputList == claimingAllYears =>
        List(
          (TaxYear(YEAR_2022).starts, TaxYear(YEAR_2022).finishes),
          (TaxYear(YEAR_2021).starts, TaxYear(YEAR_2021).finishes),
          (TaxYear(YEAR_2020).starts, TaxYear(YEAR_2020).finishes)
        )
      case inputList if inputList == claiming2022And2021 =>
        List(
          (TaxYear(YEAR_2022).starts, TaxYear(YEAR_2022).finishes),
          (TaxYear(YEAR_2021).starts, TaxYear(YEAR_2021).finishes)
        )
      case inputList if (inputList == claiming2022Only || inputList == claiming2022AndPrev) =>
        List((TaxYear(YEAR_2022).starts, TaxYear(YEAR_2022).finishes))
      case inputList if (inputList == claiming2021AndPrev || inputList == claiming2021Only) =>
        List(
          (TaxYear(YEAR_2021).starts, TaxYear(YEAR_2021).finishes),
          (TaxYear(YEAR_2020).starts, TaxYear(YEAR_2020).finishes)
        )
      case inputList if(inputList == claimingPrevOnly) =>
        List(
          (TaxYear(YEAR_2020).starts, TaxYear(YEAR_2020).finishes)
        )
      case _ => List.empty
    }
  }

  val isPreviousTaxYearSelected: Boolean = checkboxYearOptions.contains("option3")

  val showBothMessageBlocksOnTaxReliefPage = {
    checkboxYearOptions.contains("option2") || checkboxYearOptions.contains("option3")
  }

}