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

package services

import java.time.temporal.ChronoUnit

import base.SpecBase
import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar
import utils.TaxYearDates._

// scalastyle:off magic.number
class TaxYearDatesSpec extends SpecBase with MockitoSugar {
  "calculateWeeks" when {
    "rounding up the days to the next whole week" when {
      val tests = Seq(
        (TAX_YEAR_2019_END_DATE.minus(1, ChronoUnit.DAYS), TAX_YEAR_2019_END_DATE, 1),
        (TAX_YEAR_2019_END_DATE.minus(2, ChronoUnit.DAYS), TAX_YEAR_2019_END_DATE, 1),
        (TAX_YEAR_2019_END_DATE.minus(3, ChronoUnit.DAYS), TAX_YEAR_2019_END_DATE, 1),
        (TAX_YEAR_2019_END_DATE.minus(4, ChronoUnit.DAYS), TAX_YEAR_2019_END_DATE, 1),
        (TAX_YEAR_2019_END_DATE.minus(5, ChronoUnit.DAYS), TAX_YEAR_2019_END_DATE, 1),
        (TAX_YEAR_2019_END_DATE.minus(6, ChronoUnit.DAYS), TAX_YEAR_2019_END_DATE, 1),
        (TAX_YEAR_2019_END_DATE.minus(7, ChronoUnit.DAYS), TAX_YEAR_2019_END_DATE, 1),
        (TAX_YEAR_2019_END_DATE.minus(8, ChronoUnit.DAYS), TAX_YEAR_2019_END_DATE, 2),
        (TAX_YEAR_2019_END_DATE.minus(9, ChronoUnit.DAYS), TAX_YEAR_2019_END_DATE, 2),
        (TAX_YEAR_2019_END_DATE.minus(10, ChronoUnit.DAYS), TAX_YEAR_2019_END_DATE, 2),
        (TAX_YEAR_2019_END_DATE.minus(11, ChronoUnit.DAYS), TAX_YEAR_2019_END_DATE, 2),
        (TAX_YEAR_2019_END_DATE.minus(12, ChronoUnit.DAYS), TAX_YEAR_2019_END_DATE, 2),
        (TAX_YEAR_2019_END_DATE.minus(13, ChronoUnit.DAYS), TAX_YEAR_2019_END_DATE, 2),
        (TAX_YEAR_2019_END_DATE.minus(14, ChronoUnit.DAYS), TAX_YEAR_2019_END_DATE, 2),
        (TAX_YEAR_2019_END_DATE.minus(15, ChronoUnit.DAYS), TAX_YEAR_2019_END_DATE, 3),
        (TAX_YEAR_2020_START_DATE, TAX_YEAR_2020_END_DATE, 52),
        (TAX_YEAR_2019_START_DATE, TAX_YEAR_2019_END_DATE, 53)
      )

      for ( (startDate, endDate, numOfWeeks) <- tests) {
        s"calculate $numOfWeeks as the number of weeks between $startDate and $endDate" in {
          numberOfWeeks(startDate,endDate) shouldBe numOfWeeks
        }
      }
    }
  }
}
