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

import utils.TaxYearDates._

import java.time.LocalDate

case class TaxYearFromUIAssembler(checkboxYearOptions: List[String]) {

  val mapping: Map[String, (LocalDate, LocalDate)] = Map(
    "option1" -> (TAX_YEAR_2024_START_DATE, TAX_YEAR_2024_END_DATE),
    "option2" -> (TAX_YEAR_2023_START_DATE, TAX_YEAR_2023_END_DATE),
    "option3" -> (TAX_YEAR_2022_START_DATE, TAX_YEAR_2022_END_DATE),
    "option4" -> (TAX_YEAR_2021_START_DATE, TAX_YEAR_2021_END_DATE),
    "option5" -> (TAX_YEAR_2020_START_DATE, TAX_YEAR_2020_END_DATE)
  )

  val assemble: List[(LocalDate, LocalDate)] = {
    checkboxYearOptions.filter(mapping.keySet.contains) match {
      case Nil => throw new IllegalArgumentException("Input checkbox list is either empty or invalid")
      case list => list.map(mapping)
    }
  }


  val containsCurrent: Boolean = checkboxYearOptions.contains("option1")
  val contains2023: Boolean = checkboxYearOptions.contains("option2")
  val contains2022: Boolean = checkboxYearOptions.contains("option3")
  val contains2021: Boolean = checkboxYearOptions.contains("option4")
  val contains2020: Boolean = checkboxYearOptions.contains("option5")
  val containsPrevious: Boolean = contains2023 || contains2022 || contains2021 || contains2020
  val contains2021Or2020: Boolean = contains2021 || contains2020
  val contains2022OrAfter: Boolean = containsCurrent || contains2023 || contains2022
  val yearsClaimedByWeek: List[(LocalDate, LocalDate)] = List(
    (TAX_YEAR_2024_START_DATE, TAX_YEAR_2024_END_DATE),
    (TAX_YEAR_2023_START_DATE, TAX_YEAR_2023_END_DATE)
  )

  val assembleWholeYears: List[(LocalDate, LocalDate)] = assemble diff yearsClaimedByWeek

}
