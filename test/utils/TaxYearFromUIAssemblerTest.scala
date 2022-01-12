package utils

import junit.framework.TestCase.{assertFalse, assertTrue}
import navigation.TaxYearFromUIAssembler
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.time.TaxYear
import utils.TaxYearDates.{YEAR_2020, YEAR_2021, YEAR_2022}

class TaxYearFromUIAssemblerTest extends PlaySpec with MockitoSugar {

  "Converting UI input components" should {
    "be translated (checkbox input marshalling)" when {
      "when ALL possible years are selected" in {
        val expectedYearsTupleList = List(
          (TaxYear(YEAR_2022).starts, TaxYear(YEAR_2022).finishes),
          (TaxYear(YEAR_2021).starts, TaxYear(YEAR_2021).finishes),
          (TaxYear(YEAR_2020).starts, TaxYear(YEAR_2020).finishes)
        )

        val selectedYearsOptions = List("option1", "option2", "option3")

        val assembledResult = TaxYearFromUIAssembler(selectedYearsOptions).assemble
        assert(assembledResult == expectedYearsTupleList)
      }

      "when 2021 & 2022 only years are selected" in {
        val expectedYearsTupleList = List(
          (TaxYear(YEAR_2022).starts, TaxYear(YEAR_2022).finishes),
          (TaxYear(YEAR_2021).starts, TaxYear(YEAR_2021).finishes)
        )
        val selectedYearsOptions = List("option1", "option2")

        val assembledResult = TaxYearFromUIAssembler(selectedYearsOptions).assemble
        assert(assembledResult == expectedYearsTupleList)
      }

      "when 2022 only year is selected" in {
        val expectedYearsTupleList = List((TaxYear(YEAR_2022).starts, TaxYear(YEAR_2022).finishes))
        val assembledResult = TaxYearFromUIAssembler(List("option1")).assemble
        assert(assembledResult == expectedYearsTupleList)
      }

      "when 2022 and previous year is selected" in {
        val expectedYearsTupleList = List((TaxYear(YEAR_2022).starts, TaxYear(YEAR_2022).finishes))
        val assembledResult = TaxYearFromUIAssembler(List("option1", "option3")).assemble
        assert(assembledResult == expectedYearsTupleList)
      }

      "when 2021 only year is selected" in {
        val expectedYearsTupleList = List((TaxYear(YEAR_2021).starts, TaxYear(YEAR_2021).finishes))
        val assembledResult = TaxYearFromUIAssembler(List("option2")).assemble
        assert(assembledResult == expectedYearsTupleList)
      }

      "when 2021 and previous year is selected" in {
        val expectedYearsTupleList = List((TaxYear(YEAR_2021).starts, TaxYear(YEAR_2021).finishes))
        val assembledResult = TaxYearFromUIAssembler(List("option2", "option3")).assemble
        assert(assembledResult == expectedYearsTupleList)
      }

      "when previous year only is selected" in {
        val assembledResult = TaxYearFromUIAssembler(List("option3")).assemble
        assert(assembledResult == Nil)
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
        assertFalse(assembledResult.showBothMessageBlocksOnTaxReliefPage)
      }
      "when options option2" in {
        val assembledResult = TaxYearFromUIAssembler(List("option2"))
        assertTrue(assembledResult.showBothMessageBlocksOnTaxReliefPage)
      }
      "when options option1 & 2 selected" in {
        val assembledResult = TaxYearFromUIAssembler(List("option1", "option2"))
        assertTrue(assembledResult.showBothMessageBlocksOnTaxReliefPage)
      }
      "when all years selected" in {
        val assembledResult = TaxYearFromUIAssembler(List("option1", "option2", "option3"))
        assertTrue(assembledResult.showBothMessageBlocksOnTaxReliefPage)
      }
    }
  }
}
