package utils

import junit.framework.TestCase.assertTrue
import org.junit.Assert.{assertEquals, assertFalse}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

import java.time.LocalDate

class StartDateValidatorTest extends PlaySpec with MockitoSugar {

  "Validate selected start date" should {
    "be translated (checkbox input marshalling)" when {
      "when start date is 2023" in {
        val startDate = Some(LocalDate.of(2023, 1, 1))
        assertFalse(StartDateValidator(List("option1", "option2", "option3"), startDate).isStartDateValid.get._1)
        assertFalse(StartDateValidator(List("option2", "option3"), startDate).isStartDateValid.get._1)
        assertFalse(StartDateValidator(List("option1", "option3"), startDate).isStartDateValid.get._1)
        assertFalse(StartDateValidator(List("option3"), startDate).isStartDateValid.get._1)
      }

      "when start date is 2022" in {
        val startDate = Some(LocalDate.of(2022, 4, 4))
        assertFalse(StartDateValidator(List("option1", "option2", "option3"), startDate).isStartDateValid.get._1)
        assertFalse(StartDateValidator(List("option2", "option3"), startDate).isStartDateValid.get._1)
        assertFalse(StartDateValidator(List("option1", "option3"), startDate).isStartDateValid.get._1)
        assertFalse(StartDateValidator(List("option3"), startDate).isStartDateValid.get._1)
      }

      "when start date is 2021" in {
        val startDate = Some(LocalDate.of(2021, 1, 4))
        assertTrue(StartDateValidator(List("option1", "option2", "option3"), startDate).isStartDateValid.get._1)
        assertTrue(StartDateValidator(List("option2", "option3"), startDate).isStartDateValid.get._1)
        assertTrue(StartDateValidator(List("option1", "option3"), startDate).isStartDateValid.get._1)
        assertTrue(StartDateValidator(List("option3"), startDate).isStartDateValid.get._1)
      }

      "when start date is 2021 - boundary check" in {
        val startDate = Some(LocalDate.of(2021, 4, 5))
        assertTrue(StartDateValidator(List("option1", "option2", "option3"), startDate).isStartDateValid.get._1)
        assertTrue(StartDateValidator(List("option2", "option3"), startDate).isStartDateValid.get._1)
        assertTrue(StartDateValidator(List("option1", "option3"), startDate).isStartDateValid.get._1)
        assertTrue(StartDateValidator(List("option3"), startDate).isStartDateValid.get._1)
      }

      "when start date is 2021 & in new tax year" in {
        val startDate = Some(LocalDate.of(2021, 4, 6))
        assertFalse(StartDateValidator(List("option1", "option2", "option3"), startDate).isStartDateValid.get._1)
        assertFalse(StartDateValidator(List("option2", "option3"), startDate).isStartDateValid.get._1)
        assertFalse(StartDateValidator(List("option1", "option3"), startDate).isStartDateValid.get._1)
        assertFalse(StartDateValidator(List("option3"), startDate).isStartDateValid.get._1)
      }

      "when start date is 2020" in {
        val startDate = Some(LocalDate.of(2020, 1, 1))
        assertTrue(StartDateValidator(List("option1", "option2", "option3"), startDate).isStartDateValid.get._1)
        assertTrue(StartDateValidator(List("option2", "option3"), startDate).isStartDateValid.get._1)
        assertTrue(StartDateValidator(List("option1", "option3"), startDate).isStartDateValid.get._1)
        assertTrue(StartDateValidator(List("option3"), startDate).isStartDateValid.get._1)
      }

  }}
}
