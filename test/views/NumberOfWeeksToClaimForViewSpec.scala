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

package views

import controllers.routes
import forms.NumberOfWeeksToClaimForFormProvider
import models.TaxYearSelection.{CurrentYear, CurrentYearMinus1}
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.behaviours.IntViewBehaviours
import views.html.NumberOfWeeksToClaimForView

class NumberOfWeeksToClaimForViewSpec extends IntViewBehaviours {

  val messageKeyPrefix = "numberOfWeeksToClaimFor"
  val formProvider = new NumberOfWeeksToClaimForFormProvider
  val form: Form[Int] = formProvider(CurrentYear)

  "NumberOfWeeksToClaimForFormView" when {
    "rendered for current year" must {
      val view = viewFor[NumberOfWeeksToClaimForView](Some(emptyUserAnswers))

      def applyView(form: Form[Int]): HtmlFormat.Appendable = view.apply(form, CurrentYear)(fakeRequest, messages)

      behave like normalPage(applyView(form), messageKeyPrefix = messageKeyPrefix, args = Nil)

      behave like intPage(form, applyView, messageKeyPrefix, routes.NumberOfWeeksToClaimForController.onSubmit().url)
    }
    "rendered for previous year" must {
      val messageKeyPrefix = "numberOfWeeksToClaimFor.previous"
      val view = viewFor[NumberOfWeeksToClaimForView](Some(emptyUserAnswers))
      val form: Form[Int] = formProvider(CurrentYearMinus1)

      def applyView(form: Form[Int]): HtmlFormat.Appendable = view.apply(form, CurrentYearMinus1)(fakeRequest, messages)

      behave like normalPage(applyView(form), messageKeyPrefix = messageKeyPrefix, args = CurrentYearMinus1.formattedTaxYearArgs)

      behave like intPage(form, applyView, messageKeyPrefix, routes.NumberOfWeeksToClaimForController.onSubmit().url, args = CurrentYearMinus1.formattedTaxYearArgs)
    }
  }
}
