/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.time.TaxYear
import views.behaviours.ViewBehaviours
import views.html.ConfirmationView

// scalastyle:off magic.number
class ConfirmationViewSpec extends ViewBehaviours {

  private val Confirmation = "confirmation"

  "Confirmation view" should {
    val view = viewFor[ConfirmationView](Some(emptyUserAnswers))

    val request = FakeRequest()

    "show 2019 content" when {
      "when a started to work from home date is in the 2019 tax year" in {
        val doc = asDocument(view.apply(true, None, Some(TaxYear(2019)))(request, messages))
        assert(doc.toString.contains(messages("confirmation.whatHappensNext.paragraph.1.for.2019")))
      }
    }

    "not show 2019 content" when {
      "when a started to work from home date is in the 2020 tax year" in {
        val doc = asDocument(view.apply(true, None, Some(TaxYear(2020)))(request, messages))
        assert(!doc.toString.contains(messages("confirmation.whatHappensNext.paragraph.1.for.2019")))
      }
    }

    "show go paperless content" when {
      "when the user is not already paperless" in {
        val doc = asDocument(view.apply(false, Some("url-string"), Some(TaxYear(2020)))(request, messages))
        assert(doc.toString.contains(messages("confirmation.paperless.header")))
        assert(doc.toString.contains(messages("confirmation.paperless.button.text")))
      }
    }

    "not show go paperless content" when {
      "when the user is already paperless" in {
        val doc = asDocument(view.apply(true, None, Some(TaxYear(2020)))(request, messages))
        assert(!doc.toString.contains(messages("confirmation.paperless.header")))
        assert(!doc.toString.contains(messages("confirmation.paperless.button.text")))
      }
    }

    "behave like a normal page" when {
      behave like normalPage(view.apply(true, None, None)(request, messages), Confirmation)
    }

  }
  
}
