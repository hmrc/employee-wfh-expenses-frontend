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

import models.TaxYearSelection.{CurrentYear, CurrentYearMinus1, CurrentYearMinus2, CurrentYearMinus3}
import play.api.test.FakeRequest
import views.behaviours.ViewBehaviours
import views.html.HowWeWillCalculateTaxReliefView

class HowWeWillCalculateTaxReliefPageViewSpec extends ViewBehaviours {

  "Disclaimer view" must {

    val view = viewFor[HowWeWillCalculateTaxReliefView](Some(emptyUserAnswers))

    val taxYearList = Seq(CurrentYearMinus2)

    val applyView = view.apply(taxYearList)(fakeRequest, messages)

    behave like normalPage(applyView, "howWeWillCalculateTaxRelief", args = Nil)

    "show content" when {
      "when all howWeWillCalculateTaxRelief content is displayed for tax year 2024 to 2025" in {
        val view = viewFor[HowWeWillCalculateTaxReliefView](Some(emptyUserAnswers))
        val request = FakeRequest()

        val taxYearList = Seq(CurrentYear)

        val doc = asDocument(view.apply(taxYearList)(request, messages))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.heading")))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.eligibility.inset.text")))
        assert(doc.toString.contains(messages("6 April 2024 to 5 April 2025")))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.bullet.frequency.weekly.text")))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.bullet.tax.relief.amount.text", 6)))
      }

      "when all howWeWillCalculateTaxRelief content is displayed for tax year 2023 to 2024" in {
        val view = viewFor[HowWeWillCalculateTaxReliefView](Some(emptyUserAnswers))
        val request = FakeRequest()

        val taxYearList = Seq(CurrentYearMinus1)

        val doc = asDocument(view.apply(taxYearList)(request, messages))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.heading")))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.eligibility.inset.text")))
        assert(doc.toString.contains(messages("6 April 2023 to 5 April 2024")))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.bullet.frequency.weekly.text")))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.bullet.tax.relief.amount.text", 6)))
      }

      "when all howWeWillCalculateTaxRelief content is displayed for tax year 2022 to 2023" in {
        val view = viewFor[HowWeWillCalculateTaxReliefView](Some(emptyUserAnswers))
        val request = FakeRequest()

        val taxYearList = Seq(CurrentYearMinus2)

        val doc = asDocument(view.apply(taxYearList)(request, messages))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.heading")))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.eligibility.inset.text")))
        assert(doc.toString.contains(messages("6 April 2022 to 5 April 2023")))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.bullet.frequency.yearly.text")))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.bullet.tax.relief.amount.text", 6)))
      }

      "when all howWeWillCalculateTaxRelief content is displayed for tax year 2021 to 2022" in {
        val view = viewFor[HowWeWillCalculateTaxReliefView](Some(emptyUserAnswers))
        val request = FakeRequest()

        val taxYearList = Seq(CurrentYearMinus3)

        val doc = asDocument(view.apply(taxYearList)(request, messages))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.heading")))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.eligibility.inset.text")))
        assert(doc.toString.contains(messages("6 April 2021 to 5 April 2022")))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.bullet.frequency.yearly.text")))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.bullet.tax.relief.amount.text", 6)))
      }
    }
  }
}
