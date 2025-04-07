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

import models.TaxYearSelection
import models.TaxYearSelection.{
  CurrentYear,
  CurrentYearMinus1,
  CurrentYearMinus2,
  CurrentYearMinus3,
  CurrentYearMinus4,
  wholeYearClaims
}
import play.api.test.FakeRequest
import views.behaviours.ViewBehaviours
import views.html.HowWeWillCalculateTaxReliefView

class HowWeWillCalculateTaxReliefPageViewSpec extends ViewBehaviours {

  "Disclaimer view" must {

    val view = viewFor[HowWeWillCalculateTaxReliefView](Some(emptyUserAnswers))

    val taxYearList = Seq(CurrentYearMinus2)

    val applyView = view.apply(taxYearList)(fakeRequest, messages)

    object ExpectedContent {
      val title                   = "How we will calculate tax relief for the years you have selected"
      val insetText               = "To claim, you must meet the eligibility rules for each of the listed years."
      val weeklyText              = "Claims are calculated in weeks"
      val yearlyText              = "Claims are given for the entire tax year"
      def amountText(amount: Int) = s"Tax relief is £$amount per week"
      val enterNumberOfWeeksText  = "You need to enter the number of weeks you work from home for this tax year"
    }

    def checkContent(taxYear: TaxYearSelection, weekly: Boolean): Unit = {
      import ExpectedContent._
      val view    = viewFor[HowWeWillCalculateTaxReliefView](Some(emptyUserAnswers))
      val request = FakeRequest()

      val taxYearList = Seq(taxYear)

      val doc = asDocument(view.apply(taxYearList)(request, messages))
      assert(doc.toString.contains(title))
      assert(doc.toString.contains(insetText))
      assert(doc.toString.contains(s"${taxYear.formattedTaxYearArgs.head} to ${taxYear.formattedTaxYearArgs.last}"))
      assert(doc.toString.contains(if (weekly) weeklyText else yearlyText))
      assert(doc.toString.contains(amountText(frontendAppConfig.taxReliefPerWeek(taxYear))))
      if (weekly) assert(doc.toString.contains(enterNumberOfWeeksText))
    }

    behave.like(normalPage(applyView, "howWeWillCalculateTaxRelief", args = Nil))

    "show content" when {
      "when all howWeWillCalculateTaxRelief weekly content is displayed for CTY" in
        checkContent(CurrentYear, weekly = true)

      "when all howWeWillCalculateTaxRelief weekly content is displayed for CTY-1" in
        checkContent(CurrentYearMinus1, weekly = true)

      "when all howWeWillCalculateTaxRelief content is displayed for CTY-2" in
        checkContent(CurrentYearMinus2, weekly = !wholeYearClaims.contains(CurrentYearMinus2))

      "when all howWeWillCalculateTaxRelief content is displayed for CTY-3" in
        checkContent(CurrentYearMinus3, weekly = !wholeYearClaims.contains(CurrentYearMinus3))

      "when all howWeWillCalculateTaxRelief content is displayed for CTY-4" in
        checkContent(CurrentYearMinus4, weekly = !wholeYearClaims.contains(CurrentYearMinus4))
    }
  }

}
