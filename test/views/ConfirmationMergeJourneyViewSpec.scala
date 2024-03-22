/*
 * Copyright 2024 HM Revenue & Customs
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
import views.html.ConfirmationMergeJourneyView

class ConfirmationMergeJourneyViewSpec extends ViewBehaviours {
  private val ConfirmationMergeJourney = "confirmation.mergeJourney"

  "ConfirmationMergeJourney view" should {

    val view = viewFor[ConfirmationMergeJourneyView](Some(emptyUserAnswers))

    val request = FakeRequest()

    "show content" when {

      val doc = asDocument(view.apply("url-string")(request, messages))

      "when all confirmation content is displayed" in {
        assert(doc.toString.contains(messages("confirmation.mergeJourney.whatHappensNext.heading")))
        assert(doc.toString.contains(messages("confirmation.mergeJourney.whatHappensNext.paragraph.1")))
        assert(doc.toString.contains(messages("confirmation.mergeJourney.whatHappensNext.paragraph.2")))
      }

      "when a continue button is present with the correct link" in {
        assert(doc.toString.contains(messages("site.continue")))
        assert(doc.toString.contains("href=\"url-string\""))
      }
    }

    "behave like a normal page" when {

      behave like normalPage(view.apply("url-string")(request, messages), ConfirmationMergeJourney, args = Nil)
    }
  }
}
