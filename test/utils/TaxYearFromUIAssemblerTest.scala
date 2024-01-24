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

import junit.framework.TestCase.{assertFalse, assertTrue}
import models.TaxYearFromUIAssembler
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.time.TaxYear
import utils.TaxYearDates.{YEAR_2020, YEAR_2021, YEAR_2022, YEAR_2023}

class TaxYearFromUIAssemblerTest extends PlaySpec with MockitoSugar {

  "Converting UI input components" should {
    "be translated (checkbox input marshalling)" when {
      "when ALL possible years are selected" in {
        val expectedYearsTupleList = List(
          (TaxYear(YEAR_2023).starts, TaxYear(YEAR_2023).finishes),
          (TaxYear(YEAR_2022).starts, TaxYear(YEAR_2022).finishes),
          (TaxYear(YEAR_2021).starts, TaxYear(YEAR_2021).finishes),
          (TaxYear(YEAR_2020).starts, TaxYear(YEAR_2020).finishes)
        )

        val selectedYearsOptions = List("option1", "option2", "option3", "option4")

        val assembledResult = TaxYearFromUIAssembler(selectedYearsOptions).assemble
        assert(assembledResult == expectedYearsTupleList)
      }

      "when 2021 & 2022 only years are selected" in {
        val expectedYearsTupleList = List(
          (TaxYear(YEAR_2022).starts, TaxYear(YEAR_2022).finishes),
          (TaxYear(YEAR_2021).starts, TaxYear(YEAR_2021).finishes)
        )
        val selectedYearsOptions = List("option2", "option3")

        val assembledResult = TaxYearFromUIAssembler(selectedYearsOptions).assemble
        assert(assembledResult == expectedYearsTupleList)
      }

      "when 2022 only year is selected" in {
        val expectedYearsTupleList = List((TaxYear(YEAR_2022).starts, TaxYear(YEAR_2022).finishes))
        val assembledResult = TaxYearFromUIAssembler(List("option2")).assemble
        assert(assembledResult == expectedYearsTupleList)
      }

      "when 2022 and previous year is selected" in {
        val expectedYearsTupleList = List(
          (TaxYear(YEAR_2022).starts, TaxYear(YEAR_2022).finishes),
          (TaxYear(YEAR_2020).starts, TaxYear(YEAR_2020).finishes)
        )

        val assembledResult = TaxYearFromUIAssembler(List("option2", "option4")).assemble
        assert(assembledResult == expectedYearsTupleList)
      }

      "when 2021 only year is selected" in {
        val expectedYearsTupleList = List((TaxYear(YEAR_2021).starts, TaxYear(YEAR_2021).finishes))
        val assembledResult = TaxYearFromUIAssembler(List("option3")).assemble
        assert(assembledResult == expectedYearsTupleList)
      }

      "when 2021 and previous year is selected" in {
        val expectedYearsTupleList = List(
          (TaxYear(YEAR_2021).starts, TaxYear(YEAR_2021).finishes),
          (TaxYear(YEAR_2020).starts, TaxYear(YEAR_2020).finishes)
        )
        val assembledResult = TaxYearFromUIAssembler(List("option3", "option4")).assemble
        assert(assembledResult == expectedYearsTupleList)
      }

      "when previous year only is selected" in {
        val expectedYearsTupleList = List(
          (TaxYear(YEAR_2020).starts, TaxYear(YEAR_2020).finishes)
        )
        val assembledResult = TaxYearFromUIAssembler(List("option4")).assemble
        assert(assembledResult == expectedYearsTupleList)
      }

      "when something completely unknown is selected" in {
        val assembledResult = TaxYearFromUIAssembler(List("XXXXXXX")).assemble
        assert(assembledResult == Nil)
      }

      "when empty list is selected (should never really happen)" in {
        assertThrows[IllegalArgumentException] {
          TaxYearFromUIAssembler(Nil).assemble
        }
      }

      "when null list is supplied (should never really happen)" in {
        assertThrows[IllegalArgumentException] {
          TaxYearFromUIAssembler(Nil).assemble
        }
      }
    }
    "contain correct message block selector on tax relief page" when {
      "when options option1" in {
        val assembledResult = TaxYearFromUIAssembler(List("option1"))
        assertTrue(assembledResult.containsCurrent)
      }
      "when options option2" in {
        val assembledResult = TaxYearFromUIAssembler(List("option2"))
        assertFalse(assembledResult.containsCurrent)
      }
      "when options option1 & 2 selected" in {
        val assembledResult = TaxYearFromUIAssembler(List("option1", "option2"))
        assertTrue(assembledResult.containsCurrent)
      }
      "when all years selected" in {
        val assembledResult = TaxYearFromUIAssembler(List("option1", "option2", "option3"))
        assertTrue(assembledResult.containsCurrent)
      }
    }
  }
}
