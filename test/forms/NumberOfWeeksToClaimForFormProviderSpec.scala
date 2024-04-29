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

class NumberOfWeeksToClaimForFormProviderSpec extends IntFieldBehaviours with GuiceFakeApplicationFactory {

  implicit val messages: Messages = fakeApplication().injector.instanceOf[MessagesApi].preferred(Seq(Lang.defaultLang))
  val formProvider: NumberOfWeeksToClaimForFormProvider = new NumberOfWeeksToClaimForFormProvider

  "form values when claiming for current tax year" when {
    val form = formProvider(Seq(CurrentYear))
    val field = CurrentYear.toTaxYear.startYear.toString
    behave like intField(
      form,
      field,
      FormError(field, "numberOfWeeksToClaimFor.error.nonNumeric", CurrentYear.formattedTaxYearArgs),
      FormError(field, "numberOfWeeksToClaimFor.error.wholeNumber", CurrentYear.formattedTaxYearArgs)
    )
    behave like intFieldWithMinimum(
      form,
      field,
      formProvider.ONE_WEEK,
      FormError(field, "numberOfWeeksToClaimFor.error.minimum", 1 +: CurrentYear.formattedTaxYearArgs)
    )
    behave like intFieldWithMaximum(
      form,
      field,
      formProvider.MAXIMUM_WEEKS_IN_A_YEAR,
      FormError(field, "numberOfWeeksToClaimFor.error.maximum", 52 +: CurrentYear.formattedTaxYearArgs)
    )
  }
  "form values claiming for previous tax year" when {
    val form = formProvider(Seq(CurrentYearMinus1))
    val field = CurrentYearMinus1.toTaxYear.startYear.toString
    behave like intField(
      form,
      field,
      FormError(field, "numberOfWeeksToClaimFor.previous.error.nonNumeric", CurrentYearMinus1.formattedTaxYearArgs),
      FormError(field, "numberOfWeeksToClaimFor.previous.error.wholeNumber", CurrentYearMinus1.formattedTaxYearArgs)
    )
    behave like intFieldWithMinimum(
      form,
      field,
      formProvider.ONE_WEEK,
      FormError(field, "numberOfWeeksToClaimFor.previous.error.minimum", 1 +: CurrentYearMinus1.formattedTaxYearArgs)
    )
    behave like intFieldWithMaximum(
      form,
      field,
      formProvider.MAXIMUM_WEEKS_IN_A_YEAR,
      FormError(field, "numberOfWeeksToClaimFor.previous.error.maximum", 52 +: CurrentYearMinus1.formattedTaxYearArgs)
    )
  }
}
