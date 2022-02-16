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

import play.api.test.FakeRequest
import views.behaviours.ViewBehaviours
import views.html.ConfirmationView

// scalastyle:off magic.number
class ConfirmationViewSpec extends ViewBehaviours {

  private val Confirmation = "confirmation"

  "Confirmation view" should {
    val view = viewFor[ConfirmationView](Some(emptyUserAnswers))

    val request = FakeRequest()

    "show content" when {
      "when all confirmation content is displayed" in {
        val doc = asDocument(view.apply(true, None, true, true)(request, messages))
        assert(doc.toString.contains(messages("confirmation.whatHappensNext.currentTaxYear.text.1")))
        assert(doc.toString.contains(messages("confirmation.whatHappensNext.currentTaxYear.text.2")))
        assert(doc.toString.contains(messages("confirmation.whatHappensNext.currentTaxYear.text.3")))
        assert(doc.toString.contains(messages("confirmation.whatHappensNext.previousTaxYears.text")))
      }
    }

    "show go paperless content" when {
      "when the user is not already paperless" in {
        val doc = asDocument(view.apply(false, Some("url-string"), false, false)(request, messages))
        assert(doc.toString.contains(messages("confirmation.paperless.header")))
        assert(doc.toString.contains(messages("confirmation.paperless.paragraph.1")))
        assert(doc.toString.contains(messages("confirmation.paperless.button.text")))
      }
    }

    "not show go paperless content" when {
      "when the user is already paperless" in {
        val doc = asDocument(view.apply(true, None, false, false)(request, messages))
        assert(!doc.toString.contains(messages("confirmation.paperless.header")))
        assert(!doc.toString.contains(messages("confirmation.paperless.paragraph.1")))
        assert(!doc.toString.contains(messages("confirmation.paperless.button.text")))
      }
    }

    "behave like a normal page" when {
      behave like normalPage(view.apply(true, None, true, true)(request, messages), Confirmation)
    }
  }
}
