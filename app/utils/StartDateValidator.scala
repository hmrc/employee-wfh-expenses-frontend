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

package utils

import navigation.TaxYearOptionValues
import java.time.LocalDate

case class StartDateValidator(selectedUIOptions: List[String],
                              startDateToValidate: Option[LocalDate]) extends TaxYearOptionValues {

  val DefaultValidResponse: (Boolean, Option[String]) = (true, None)
  val InValidResponse2021Response = (false, Some("2021"))

  val isStartDateValid: Option[(Boolean, Option[String])] = {

    startDateToValidate match {
      case Some(startDate) =>
        (selectedUIOptions == claimingPrevOnly) match {
          case true =>
            if ((startDate.getDayOfMonth <= 5 && startDate.getMonthValue <= 4) && startDate.getYear <= 2021) {
              Some(DefaultValidResponse)
            }else {
              Some(InValidResponse2021Response)
            }
          case false =>
            Some(validateDates(startDate))
        }
      case _ => None
    }
}

  private def validateDates(startDate: LocalDate): (Boolean, Option[String])  = {
        if (startDate.getYear == 2023) {
          if ((selectedUIOptions == claimingAllYears) || (selectedUIOptions == claiming2021AndPrev)
            || (selectedUIOptions == claiming2022AndPrev) || (selectedUIOptions == claimingPrevOnly)) {
            InValidResponse2021Response
          } else {
            DefaultValidResponse
          }
        } else if (startDate.getYear == 2022) {
          if ((selectedUIOptions == claimingAllYears) || (selectedUIOptions == claiming2021AndPrev)
            || (selectedUIOptions == claiming2022AndPrev) || (selectedUIOptions == claimingPrevOnly)) {
            InValidResponse2021Response
          } else {
            DefaultValidResponse
          }
        }
        else if (startDate.getYear == 2021) {
          if ((startDate.getDayOfMonth <= 5 && startDate.getMonthValue <= 4)
            && ((selectedUIOptions == claimingAllYears) || (selectedUIOptions == claiming2021AndPrev)
            || (selectedUIOptions == claiming2022AndPrev) || (selectedUIOptions == claimingPrevOnly))) {
            DefaultValidResponse
          } else if ((selectedUIOptions == claimingAllYears) || (selectedUIOptions == claiming2021AndPrev)
            || (selectedUIOptions == claiming2022AndPrev) || (selectedUIOptions == claimingPrevOnly)) {
            InValidResponse2021Response
          } else DefaultValidResponse
        } else if (startDate.getYear == 2020) {
          DefaultValidResponse
        }
        else {
          DefaultValidResponse
        }
    }

}