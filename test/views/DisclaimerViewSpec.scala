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
import views.html.DisclaimerView

// scalastyle:off magic.number
class DisclaimerViewSpec extends ViewBehaviours {

  private val Disclaimer = "disclaimer"

  "DisclaimerView" should {

    val view = viewFor[DisclaimerView](Some(emptyUserAnswers))

    val request = FakeRequest()

    "show content" when {
      "when all both sections are required" in {

        val doc = asDocument(view.apply("April", "2020",
          Some("May"), Some("2022"), true, true)(request, messages))
        assert(doc.toString.contains(messages("Claiming tax relief on or after")))
        assert(doc.toString.contains(messages("Claiming tax relief on or before")))

        assert(doc.toString.contains(messages("disclaimer.heading.after")))
        assert(doc.toString.contains(messages("disclaimer.info.text.1")))
        assert(doc.toString.contains(messages("disclaimer.bullet.heading")))
        assert(doc.toString.contains(messages("disclaimer.bullet.text.1")))
        assert(doc.toString.contains(messages("disclaimer.bullet.text.2")))
        assert(doc.toString.contains(messages("disclaimer.heading.before")))
        assert(doc.toString.contains(messages("disclaimer.bullet.heading.1")))
        assert(doc.toString.contains(messages("disclaimer.bullet.text.3")))
        assert(doc.toString.contains(messages("disclaimer.bullet.text.4")))
        assert(doc.toString.contains(messages("disclaimer.bullet.text.5")))
        assert(doc.toString.contains(messages("disclaimer.bullet.text.6")))
        assert(doc.toString.contains(messages("disclaimer.warning.text.1")))
        assert(doc.toString.contains(messages("disclaimer.warning.text.2")))
        assert(doc.toString.contains(messages("disclaimer.button.text")))

      }
      "when all both after only is required" in {
        val doc = asDocument(view.apply("April", "2020",
          None, None, true, false)(request, messages))
        assert(doc.toString.contains(messages("Claiming tax relief on or after")))
        assert(!doc.toString.contains(messages("Claiming tax relief on or before")))
      }
    }

    "behave like a normal page" when {
      behave like normalPage(view.apply("April", "2020",
        Some("May"), Some("2022"), true, true)(request, messages), Disclaimer)
    }
  }
}
