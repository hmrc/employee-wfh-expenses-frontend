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

import controllers.{UIAssembler, routes}
import models.{ClaimViewSettings, DisclaimerViewSettings, TaxYearFromUIAssembler}
import org.scalatest.Assertions.assert
import play.api.test.FakeRequest
import play.api.test.Helpers.GET
import utils.DateLanguageTokenizer
import views.behaviours.ViewBehaviours
import views.html.DisclaimerView
import play.api.test.FakeRequest

import java.time.LocalDate

class DisclaimerViewSpec extends ViewBehaviours with UIAssembler {

  "Disclaimer view" must {

    val view = viewFor[DisclaimerView](Some(emptyUserAnswers))

    val assembler = TaxYearFromUIAssembler(List("option1"))

    val disclaimerViewSettings = DisclaimerViewSettings(Some(ClaimViewSettings(DateLanguageTokenizer.convertList(assembler.assemble),
      Some(DateLanguageTokenizer.convertList(assembler.assemble)))))

    val date = LocalDate.of(2020, 4, 1)

    val applyView = view.apply(true, disclaimerViewSettings, Some(date))(fakeRequest, messages)

    behave like normalPage(applyView, "disclaimer")

    "show content" when {
      "when all disclaimer content is displayed before additional tax relief dates" in {
        val view = viewFor[DisclaimerView](Some(emptyUserAnswers))
        val request = FakeRequest()

        val assembler = TaxYearFromUIAssembler(List("option1"))

        val disclaimerViewSettings = DisclaimerViewSettings(Some(ClaimViewSettings(DateLanguageTokenizer.convertList(assembler.assemble),
          Some(DateLanguageTokenizer.convertList(assembler.assemble)))))

        val date = LocalDate.of(2020, 4, 1)
        val doc = asDocument(view.apply(true, disclaimerViewSettings, Some(date))(request, messages))
        assert(doc.toString.contains(messages("disclaimer.heading")))
        assert(doc.toString.contains(messages("disclaimer.your.claim.details.year.label")))
        assert(doc.toString.contains(messages("6 April 2022 to 5 April 2023")))
        assert(doc.toString.contains(messages("1 January 2020 to 5 April 2020")))
        assert(doc.toString.contains(messages("disclaimer.your.claim.text.2")))
        assert(doc.toString.contains(messages("disclaimer.your.claim.text.3")))
        assert(doc.toString.contains(messages("disclaimer.your.claim.text.4")))
        assert(doc.toString.contains(messages("disclaimer.your.claim.text.5")))
      }

      "when all disclaimer content is displayed after additional tax relief dates" in {
        val view = viewFor[DisclaimerView](Some(emptyUserAnswers))
        val request = FakeRequest()

        val assembler = TaxYearFromUIAssembler(List("option1"))

        val disclaimerViewSettings = DisclaimerViewSettings(Some(ClaimViewSettings(DateLanguageTokenizer.convertList(assembler.assemble),
          Some(DateLanguageTokenizer.convertList(assembler.assemble)))))

        val date = LocalDate.of(2020, 5, 1)
        val doc = asDocument(view.apply(true, disclaimerViewSettings, Some(date))(request, messages))
        assert(doc.toString.contains(messages("6 April 2022 to 5 April 2023")))
        assert(!doc.toString.contains(messages("1 January 2020 to 5 April 2020")))
      }
    }
  }
}
