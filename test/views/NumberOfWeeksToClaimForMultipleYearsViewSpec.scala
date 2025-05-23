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
import models.TaxYearSelection
import models.TaxYearSelection.{CurrentYear, CurrentYearMinus1}
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.behaviours.QuestionViewBehaviours
import views.html.NumberOfWeeksToClaimForMultipleYearsView

import scala.collection.immutable.ListMap

class NumberOfWeeksToClaimForMultipleYearsViewSpec extends QuestionViewBehaviours[ListMap[TaxYearSelection, Int]] {

  val messageKeyPrefix                           = "numberOfWeeksToClaimFor"
  val formProvider                               = new NumberOfWeeksToClaimForFormProvider
  val form: Form[ListMap[TaxYearSelection, Int]] = formProvider(Seq(CurrentYear))

  "NumberOfWeeksToClaimForMultipleView" when {
    "rendered for all years" must {
      val messageKeyPrefix = "numberOfWeeksToClaimFor.multiple"
      val view             = viewFor[NumberOfWeeksToClaimForMultipleYearsView](Some(emptyUserAnswers))
      val taxYears         = Seq(CurrentYear, CurrentYearMinus1)
      val form: Form[ListMap[TaxYearSelection, Int]] = formProvider(taxYears)

      def applyView(form: Form[ListMap[TaxYearSelection, Int]]): HtmlFormat.Appendable =
        view.apply(form, taxYears)(fakeRequest, messages)

      behave.like(normalPage(applyView(form), messageKeyPrefix = messageKeyPrefix, args = Nil))

      behave.like(
        pageWithTextFields(
          form,
          applyView,
          messageKeyPrefix,
          routes.NumberOfWeeksToClaimForController.onSubmit().url,
          args = Nil
        )
      )
    }
  }

}
