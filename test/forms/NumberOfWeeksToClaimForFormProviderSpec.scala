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

import forms.behaviours.IntFieldBehaviours
import models.TaxYearSelection.{CurrentYear, CurrentYearMinus1}
import org.scalatestplus.play.guice.GuiceFakeApplicationFactory
import play.api.data.FormError
import play.api.i18n.{Lang, Messages, MessagesApi}
import utils.TaxYearDates.{MAXIMUM_WEEKS_IN_A_YEAR, ONE_WEEK}

class NumberOfWeeksToClaimForFormProviderSpec extends IntFieldBehaviours with GuiceFakeApplicationFactory {

  implicit val messages: Messages = fakeApplication().injector.instanceOf[MessagesApi].preferred(Seq(Lang.defaultLang))
  val formProvider: NumberOfWeeksToClaimForFormProvider = new NumberOfWeeksToClaimForFormProvider

  "form values when claiming for current tax year" when {
    val form = formProvider(Seq(CurrentYear))
    behave like intField(
      form,
      "option1",
      FormError("option1", "numberOfWeeksToClaimFor.error.nonNumeric", CurrentYear.formattedTaxYearArgs),
      FormError("option1", "numberOfWeeksToClaimFor.error.wholeNumber", CurrentYear.formattedTaxYearArgs)
    )
    behave like intFieldWithMinimum(
        form,
    "option1",
        ONE_WEEK,
        FormError("option1", "numberOfWeeksToClaimFor.error.minimum", 1 +: CurrentYear.formattedTaxYearArgs)
    )
    behave like intFieldWithMaximum(
        form,
    "option1",
        MAXIMUM_WEEKS_IN_A_YEAR,
        FormError("option1", "numberOfWeeksToClaimFor.error.maximum", 52 +: CurrentYear.formattedTaxYearArgs)
    )
  }
  "form values claiming for previous tax year" when {
    val form = formProvider(Seq(CurrentYearMinus1))
    behave like intField(
      form,
      "option2",
      FormError("option2", "numberOfWeeksToClaimFor.previous.error.nonNumeric", CurrentYearMinus1.formattedTaxYearArgs),
      FormError("option2", "numberOfWeeksToClaimFor.previous.error.wholeNumber", CurrentYearMinus1.formattedTaxYearArgs)
    )
    behave like intFieldWithMinimum(
      form,
      "option2",
      ONE_WEEK,
      FormError("option2", "numberOfWeeksToClaimFor.previous.error.minimum", 1 +: CurrentYearMinus1.formattedTaxYearArgs)
    )
    behave like intFieldWithMaximum(
      form,
      "option2",
      MAXIMUM_WEEKS_IN_A_YEAR,
      FormError("option2", "numberOfWeeksToClaimFor.previous.error.maximum", 52 +: CurrentYearMinus1.formattedTaxYearArgs)
    )
  }
}
