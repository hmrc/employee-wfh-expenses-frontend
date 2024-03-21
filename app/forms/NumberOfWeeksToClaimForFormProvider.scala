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

package forms

import forms.mappings.Mappings
import models.TaxYearSelection
import play.api.data.Form
import utils.TaxYearDates.{MAXIMUM_WEEKS_IN_A_YEAR, ONE_WEEK}

import javax.inject.Inject

class NumberOfWeeksToClaimForFormProvider @Inject() extends Mappings {
  def apply(taxYear: TaxYearSelection): Form[Int] = {
    val errorPrefix: String = if (taxYear.equals(TaxYearSelection.CurrentYear)) {
      "numberOfWeeksToClaimFor.error"
    } else {
      "numberOfWeeksToClaimFor.previous.error"
    }

    Form(
      "value" -> int(s"$errorPrefix.required",
        s"$errorPrefix.wholeNumber",
        s"$errorPrefix.nonNumeric"
      ).verifying(minimumValue(ONE_WEEK, s"$errorPrefix.minimum"))
        .verifying(maximumValue(MAXIMUM_WEEKS_IN_A_YEAR, s"$errorPrefix.maximum"))
    )
  }
}
