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

import controllers.UIAssembler
import models.{ClaimViewSettings, DisclaimerViewSettings, TaxYearFromUIAssembler}
import play.api.test.FakeRequest
import utils.DateLanguageTokenizer
import views.behaviours.ViewBehaviours
import views.html.HowWeWillCalculateTaxReliefView

import java.time.LocalDate

class HowWeWillCalculateTaxReliefPageViewSpec extends ViewBehaviours with UIAssembler {

  "Disclaimer view" must {

    val view = viewFor[HowWeWillCalculateTaxReliefView](Some(emptyUserAnswers))

    val assembler = TaxYearFromUIAssembler(List("option2"))

    val disclaimerViewSettings = DisclaimerViewSettings(Some(ClaimViewSettings(DateLanguageTokenizer.convertList(assembler.assemble),
      Some(DateLanguageTokenizer.convertList(assembler.assemble)))))

    val date = LocalDate.of(2020, 4, 1)

    val applyView = view.apply(true, disclaimerViewSettings, Some(date))(fakeRequest, messages)

    behave like normalPage(applyView, "howWeWillCalculateTaxRelief")

    "show content" when {
      "when all howWeWillCalculateTaxRelief content is displayed for tax year 2023 to 2024" in {
        val view = viewFor[HowWeWillCalculateTaxReliefView](Some(emptyUserAnswers))
        val request = FakeRequest()

        val assembler = TaxYearFromUIAssembler(List("option1"))

        val disclaimerViewSettings = DisclaimerViewSettings(Some(ClaimViewSettings(DateLanguageTokenizer.convertList(assembler.assemble),
          Some(DateLanguageTokenizer.convertList(assembler.assemble)))))

        val date = LocalDate.of(2022, 4, 1)
        val doc = asDocument(view.apply(true, disclaimerViewSettings, Some(date))(request, messages))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.heading")))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.eligibility.inset.text")))
        assert(doc.toString.contains(messages("6 April 2023 to 5 April 2024")))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.bullet.frequency.weekly.text")))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.bullet.tax.relief.amount.text", 6)))
      }

      "when all howWeWillCalculateTaxRelief content is displayed for tax year 2022 to 2023" in {
        val view = viewFor[HowWeWillCalculateTaxReliefView](Some(emptyUserAnswers))
        val request = FakeRequest()

        val assembler = TaxYearFromUIAssembler(List("option2"))

        val disclaimerViewSettings = DisclaimerViewSettings(Some(ClaimViewSettings(DateLanguageTokenizer.convertList(assembler.assemble),
          Some(DateLanguageTokenizer.convertList(assembler.assemble)))))

        val date = LocalDate.of(2022, 4, 1)
        val doc = asDocument(view.apply(true, disclaimerViewSettings, Some(date))(request, messages))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.heading")))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.eligibility.inset.text")))
        assert(doc.toString.contains(messages("6 April 2022 to 5 April 2023")))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.bullet.frequency.yearly.text")))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.bullet.tax.relief.amount.text", 6)))
      }

      "when all howWeWillCalculateTaxRelief content is displayed for tax year 2021 to 2022" in {
        val view = viewFor[HowWeWillCalculateTaxReliefView](Some(emptyUserAnswers))
        val request = FakeRequest()

        val assembler = TaxYearFromUIAssembler(List("option3"))

        val disclaimerViewSettings = DisclaimerViewSettings(Some(ClaimViewSettings(DateLanguageTokenizer.convertList(assembler.assemble),
          Some(DateLanguageTokenizer.convertList(assembler.assemble)))))

        val date = LocalDate.of(2022, 4, 1)
        val doc = asDocument(view.apply(true, disclaimerViewSettings, Some(date))(request, messages))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.heading")))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.eligibility.inset.text")))
        assert(doc.toString.contains(messages("6 April 2021 to 5 April 2022")))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.bullet.frequency.yearly.text")))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.bullet.tax.relief.amount.text", 6)))
      }

      "when all howWeWillCalculateTaxRelief content is displayed for tax year before 2021" in {
        val view = viewFor[HowWeWillCalculateTaxReliefView](Some(emptyUserAnswers))
        val request = FakeRequest()

        val assembler = TaxYearFromUIAssembler(List("option4"))

        val disclaimerViewSettings = DisclaimerViewSettings(Some(ClaimViewSettings(DateLanguageTokenizer.convertList(assembler.assemble),
          Some(DateLanguageTokenizer.convertList(assembler.assemble)))))

        val date = LocalDate.of(2022, 4, 1)
        val doc = asDocument(view.apply(true, disclaimerViewSettings, Some(date))(request, messages))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.heading")))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.eligibility.inset.text")))
        assert(doc.toString.contains(messages("Previous years before 5 April 2021")))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.end.date.inset.text")))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.bullet.frequency.weekly.text")))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.bullet.tax.relief.amount.text", 4)))
        assert(doc.toString.contains(messages("howWeWillCalculateTaxRelief.bullet.enter.start.date.text")))
      }
    }
  }
}
