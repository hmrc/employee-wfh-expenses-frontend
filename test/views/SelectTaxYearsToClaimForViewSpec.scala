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

import forms.SelectTaxYearsToClaimForFormProvider
import models.TaxYearSelection
import models.TaxYearSelection._
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.behaviours.CheckboxViewBehaviours
import views.html.SelectTaxYearsToClaimForView

class SelectTaxYearsToClaimForViewSpec extends CheckboxViewBehaviours[TaxYearSelection] {

  val messageKeyPrefix = "selectTaxYearsToClaimFor"

  val form = new SelectTaxYearsToClaimForFormProvider()()

  "SelectTaxYearsToClaimForView" must {

    val view = viewFor[SelectTaxYearsToClaimForView](Some(emptyUserAnswers))

    def applyView(form: Form[Seq[TaxYearSelection]]): HtmlFormat.Appendable = view.apply(form, valuesAll)(fakeRequest, messages)

    behave like normalPage(applyView(form), messageKeyPrefix, args = Nil)

    behave like checkboxPage(form, applyView, messageKeyPrefix, TaxYearSelection.options(form, valuesAll))
  }
}
