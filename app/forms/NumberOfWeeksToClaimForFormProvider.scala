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
import play.api.data.{Form, Forms}
import utils.TaxYearDates.{ONE_WEEK, MAXIMUM_WEEKS_IN_A_YEAR}

import javax.inject.Inject

class NumberOfWeeksToClaimForFormProvider @Inject() extends Mappings {

  def apply(): Form[Int] =
    Form(
      "value" -> int("numberOfWeeksToClaimFor.error.required",
        "numberOfWeeksToClaimFor.error.wholeNumber",
        "numberOfWeeksToClaimFor.error.nonNumeric"
      ).verifying(minimumValue(ONE_WEEK, "numberOfWeeksToClaimFor.error.minimum"))
          .verifying(maximumValue(MAXIMUM_WEEKS_IN_A_YEAR, "numberOfWeeksToClaimFor.error.maximum"))
    )

}
