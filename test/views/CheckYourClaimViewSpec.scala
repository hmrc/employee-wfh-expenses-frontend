/*
 * Copyright 2022 HM Revenue & Customs
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

import models.{ClaimViewSettings, DisclaimerViewSettings, TaxYearFromUIAssembler}
import play.api.test.FakeRequest
import utils.DateLanguageTokenizer
import views.behaviours.ViewBehaviours
import views.html.CheckYourClaimView

import java.time.LocalDate

// scalastyle:off magic.number
class CheckYourClaimViewSpec extends ViewBehaviours {

  "Check your claim page" should {
    val view = viewFor[CheckYourClaimView](Some(emptyUserAnswers))

    val request = FakeRequest()

    "show content" when {
      "when all possible content is enabled" in {

        val assembler = TaxYearFromUIAssembler(List("option1"))
        val claimSettings = DisclaimerViewSettings(Some(ClaimViewSettings(DateLanguageTokenizer.convertList(assembler.assemble),
          Some(DateLanguageTokenizer.convertList(assembler.assemble)))))
        val date = LocalDate.of(2000, 4, 1)

        val doc = asDocument(view.apply(claimSettings.claimViewSettings.get, Some(date), 2)(request, messages))
        assert(doc.toString.contains(messages("checkYourClaimView.title")))
        assert(doc.toString.contains(messages("checkYourClaimView.header")))
        assert(doc.toString.contains(messages("checkYourClaimView.text.1")))
        assert(doc.toString.contains(messages("checkYourClaimView.text.2")))
        assert(doc.toString.contains(messages("checkYourClaimView.text.3")))
        assert(doc.toString.contains(messages("checkYourClaimView.text.4")))
        assert(doc.toString.contains(messages("checkYourClaimView.text.5")))
        assert(doc.toString.contains(messages("checkYourClaimView.button.label")))

        assert(doc.toString.contains(messages("checkYourClaimView.inset.text")))
        assert(doc.toString.contains(messages("the whole of tax year 6 April 2022 to 5 April 2023")))
        assert(doc.toString.contains(messages("2 weeks of tax year 6 April 2019 to 5 April 2020")))
      }

      "when content when start date is missing" in {

        val assembler = TaxYearFromUIAssembler(List("option1"))
        val claimSettings = DisclaimerViewSettings(Some(ClaimViewSettings(DateLanguageTokenizer.convertList(assembler.assemble),
          Some(DateLanguageTokenizer.convertList(assembler.assemble)))))

        val doc = asDocument(view.apply(claimSettings.claimViewSettings.get, None, 2)(request, messages))
        assert(!doc.toString.contains(messages("yourTaxRelief.inset.text")))
        assert(doc.toString.contains(messages("the whole of tax year 6 April 2022 to 5 April 2023")))
        assert(!doc.toString.contains(messages("2 weeks of tax year 6 April 2019 to 5 April 2020")))
      }

    }

    "behave like a normal page" when {
      val assembler = TaxYearFromUIAssembler(List("option1"))
      val claimSettings = DisclaimerViewSettings(Some(ClaimViewSettings(DateLanguageTokenizer.convertList(assembler.assemble),
        Some(DateLanguageTokenizer.convertList(assembler.assemble)))))
      val startDate = LocalDate.of(2020, 4, 1)
      behave like normalPage(view.apply(claimSettings.claimViewSettings.get, Some(startDate), 2)(request, messages), "check your claim")
    }
  }
}
