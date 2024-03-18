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

package utils

import uk.gov.hmrc.time.TaxYear

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object TaxYearDates {

  val ONE_WEEK  = 1
  val MAXIMUM_WEEKS_IN_A_YEAR = 52

  val YEAR_2019: Int = 2019
  val YEAR_2020: Int = 2020
  val YEAR_2021: Int = 2021
  val YEAR_2022: Int = 2022
  val YEAR_2023: Int = 2023
  val YEAR_2024: Int = 2024
  val YEAR_2025: Int = 2025

  val TAX_YEAR_2019_START_DATE: LocalDate = TaxYear(YEAR_2019).starts

  val TAX_YEAR_2019_END_DATE: LocalDate = TaxYear(YEAR_2020).starts.minusDays(1)

  val TAX_YEAR_2020_START_DATE: LocalDate = TaxYear(YEAR_2020).starts

  val TAX_YEAR_2020_END_DATE: LocalDate = TaxYear(YEAR_2021).starts.minusDays(1)

  val TAX_YEAR_2021_START_DATE: LocalDate = TaxYear(YEAR_2021).starts

  val TAX_YEAR_2021_END_DATE: LocalDate = TaxYear(YEAR_2022).starts.minusDays(1)

  val TAX_YEAR_2022_START_DATE: LocalDate = TaxYear(YEAR_2022).starts

  val TAX_YEAR_2022_END_DATE: LocalDate = TaxYear(YEAR_2023).starts.minusDays(1)

  val TAX_YEAR_2023_START_DATE: LocalDate = TaxYear(YEAR_2023).starts

  val TAX_YEAR_2023_END_DATE: LocalDate = TaxYear(YEAR_2024).starts.minusDays(1)

  val TAX_YEAR_2024_START_DATE: LocalDate = TaxYear(YEAR_2024).starts

  val TAX_YEAR_2024_END_DATE: LocalDate = TaxYear(YEAR_2025).starts.minusDays(1)

  def numberOfWeeks(startDate:LocalDate, endDate:LocalDate): Long = {
    if (startDate.equals(endDate)) {
      ONE_WEEK
    } else {
      val numberOfDays: Long = ChronoUnit.DAYS.between(startDate, endDate).abs
      (numberOfDays + 6) / 7
    }
  }

}

