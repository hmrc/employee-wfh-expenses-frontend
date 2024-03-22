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

import models.TaxYearFromUIAssembler
import play.api.test.FakeRequest
import utils.TaxYearFormatter
import views.behaviours.ViewBehaviours
import views.html.CheckYourClaimView

// scalastyle:off magic.number
class CheckYourClaimViewSpec extends ViewBehaviours {

  "Check your claim page" should {
    val view = viewFor[CheckYourClaimView](Some(emptyUserAnswers))

    val request = FakeRequest()
    val optionList = List("option3")

    "show content" when {
      "when all possible content is enabled" in {

        val assembler = TaxYearFromUIAssembler(optionList)
        val taxYears = TaxYearFormatter(assembler.assembleWholeYears).formattedTaxYears

        val doc = asDocument(view.apply(taxYears, Some(3), optionList)(request, messages))
        assert(doc.toString.contains(messages("checkYourClaimView.title")))
        assert(doc.toString.contains(messages("checkYourClaimView.heading")))
        assert(doc.toString.contains(messages("checkYourClaimView.text.1.1")))
        assert(doc.toString.contains(messages("checkYourClaimView.text.1.2")))
        assert(doc.toString.contains(messages("checkYourClaimView.text.2")))
        assert(doc.toString.contains(messages("checkYourClaimView.text.4")))
        assert(doc.toString.contains(messages("checkYourClaimView.text.5")))
        assert(doc.toString.contains(messages("checkYourClaimView.button.label")))

        assert(doc.toString.contains(messages("the whole of tax year 6 April 2022 to 5 April 2023")))
      }
    }

    "behave like a normal page" when {
      val assembler = TaxYearFromUIAssembler(List("option3"))
      val taxYears = TaxYearFormatter(assembler.assembleWholeYears).formattedTaxYears

      behave like normalPage(view.apply(taxYears, None, optionList)(request, messages), "checkYourClaimView", args = Nil)
    }
  }
}
