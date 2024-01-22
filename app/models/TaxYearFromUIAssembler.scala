/*
 * Copyright 2023 HM Revenue & Customs
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

package models

import java.time.LocalDate

import utils.TaxYearDates._

case class TaxYearFromUIAssembler(checkboxYearOptions: List[String]) {

  val claimingAllYears           = List("option1", "option2", "option3", "option4")
  val claiming2023Only           = List("option1")
  val claiming2022Only           = List("option2")
  val claiming2021Only           = List("option3")
  val claimingPrevOnly           = List("option4")
  val claiming2023And2022        = List("option1", "option2")
  val claiming2023And2021        = List("option1", "option3")
  val claiming2023AndPrev        = List("option1", "option4")
  val claiming2022And2021        = List("option2", "option3")
  val claiming2022AndPrev        = List("option2", "option4")
  val claiming2021AndPrev        = List("option3", "option4")
  val claiming2023And2022And2021 = List("option1", "option2", "option3")
  val claiming2023And2021AndPrev = List("option1", "option3", "option4")
  val claiming2022And2021AndPrev = List("option2", "option3", "option4")
  val claiming2023And2022AndPrev = List("option1", "option2", "option4")

  val assemble: List[(LocalDate, LocalDate)] = {

    if (checkboxYearOptions == null || checkboxYearOptions.isEmpty) {
      throw new IllegalArgumentException("Input checkbox list is either empty or invalid")
    }

    if (checkboxYearOptions == claimingAllYears) {
      List(
        (TAX_YEAR_2023_START_DATE, TAX_YEAR_2023_END_DATE),
        (TAX_YEAR_2022_START_DATE, TAX_YEAR_2022_END_DATE),
        (TAX_YEAR_2021_START_DATE, TAX_YEAR_2021_END_DATE),
        (TAX_YEAR_2020_START_DATE, TAX_YEAR_2020_END_DATE)
      )
    }
    else if (checkboxYearOptions == claiming2023Only) {
      List((TAX_YEAR_2023_START_DATE, TAX_YEAR_2023_END_DATE))
    }
    else if (checkboxYearOptions == claiming2022Only) {
      List((TAX_YEAR_2022_START_DATE, TAX_YEAR_2022_END_DATE))
    }
    else if (checkboxYearOptions == claiming2021Only) {
      List((TAX_YEAR_2021_START_DATE, TAX_YEAR_2021_END_DATE))
    }
    else if (checkboxYearOptions == claimingPrevOnly) {
      List((TAX_YEAR_2020_START_DATE, TAX_YEAR_2020_END_DATE))
    }

    else if(checkboxYearOptions == claiming2023And2022) {
      List(
        (TAX_YEAR_2023_START_DATE, TAX_YEAR_2023_END_DATE),
        (TAX_YEAR_2022_START_DATE, TAX_YEAR_2022_END_DATE)
      )
    }
    else if(checkboxYearOptions == claiming2023And2021) {
      List(
        (TAX_YEAR_2023_START_DATE, TAX_YEAR_2023_END_DATE),
        (TAX_YEAR_2021_START_DATE, TAX_YEAR_2021_END_DATE)
      )
    }
    else if(checkboxYearOptions == claiming2023AndPrev) {
      List(
        (TAX_YEAR_2023_START_DATE, TAX_YEAR_2023_END_DATE),
        (TAX_YEAR_2020_START_DATE, TAX_YEAR_2020_END_DATE)
      )
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
    else if(checkboxYearOptions == claiming2023And2022And2021) {
      List(
        (TAX_YEAR_2023_START_DATE, TAX_YEAR_2023_END_DATE),
        (TAX_YEAR_2022_START_DATE, TAX_YEAR_2022_END_DATE),
        (TAX_YEAR_2021_START_DATE, TAX_YEAR_2021_END_DATE)
      )
    }
    else if(checkboxYearOptions == claiming2023And2021AndPrev) {
      List(
        (TAX_YEAR_2023_START_DATE, TAX_YEAR_2023_END_DATE),
        (TAX_YEAR_2021_START_DATE, TAX_YEAR_2021_END_DATE),
        (TAX_YEAR_2020_START_DATE, TAX_YEAR_2020_END_DATE)
      )
    }
    else if(checkboxYearOptions == claiming2022And2021AndPrev) {
      List(
        (TAX_YEAR_2022_START_DATE, TAX_YEAR_2022_END_DATE),
        (TAX_YEAR_2021_START_DATE, TAX_YEAR_2021_END_DATE),
        (TAX_YEAR_2020_START_DATE, TAX_YEAR_2020_END_DATE)
      )
    }
    else if(checkboxYearOptions == claiming2023And2022AndPrev) {
      List(
        (TAX_YEAR_2023_START_DATE, TAX_YEAR_2023_END_DATE),
        (TAX_YEAR_2022_START_DATE, TAX_YEAR_2022_END_DATE),
        (TAX_YEAR_2020_START_DATE, TAX_YEAR_2020_END_DATE)
      )
    }
    else {
      List.empty
    }

  }


  val containsCurrent: Boolean = checkboxYearOptions.contains("option1")
  val contains2022: Boolean = checkboxYearOptions.contains("option2")
  val contains2021: Boolean = checkboxYearOptions.contains("option3")
  val containsPrevious: Boolean = checkboxYearOptions.contains("option4")
  val contains2021OrPrevious: Boolean = contains2021 || containsPrevious
  val contains2022OrAfter: Boolean = containsCurrent || contains2022
  val yearsClaimedByWeek: List[(LocalDate, LocalDate)] = List(
    (TAX_YEAR_2023_START_DATE, TAX_YEAR_2023_END_DATE)
  )

  val assembleWholeYears: List[(LocalDate, LocalDate)] = assemble diff yearsClaimedByWeek

}
