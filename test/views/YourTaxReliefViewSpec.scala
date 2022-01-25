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
import views.html.YourTaxReliefView

// scalastyle:off magic.number
class YourTaxReliefViewSpec extends ViewBehaviours {

  private val YourTaxRelief = "yourTaxRelief"

  "YourTaxReliefView" should {

    val view = viewFor[YourTaxReliefView](Some(emptyUserAnswers))

    val request = FakeRequest()

    "show content" when {
      "when all both sections are required" in {

        val doc = asDocument(view.apply("April", "2020",
          Some("May"), Some("2022"), true, true)(request, messages))
        assert(doc.toString.contains(messages("Check to ensure you can claim tax relief after April 2020")))
        assert(doc.toString.contains(messages("Check to ensure you can claim tax relief before May 2022")))

        assert(doc.toString.contains(messages("yourTaxRelief.heading.after")))
        assert(doc.toString.contains(messages("yourTaxRelief.info.text.1")))
        assert(doc.toString.contains(messages("yourTaxRelief.bullet.heading")))
        assert(doc.toString.contains(messages("yourTaxRelief.bullet.text.1")))
        assert(doc.toString.contains(messages("yourTaxRelief.bullet.text.2")))
        assert(doc.toString.contains(messages("yourTaxRelief.heading.before")))
        assert(doc.toString.contains(messages("yourTaxRelief.bullet.heading.1")))
        assert(doc.toString.contains(messages("yourTaxRelief.bullet.text.3")))
        assert(doc.toString.contains(messages("yourTaxRelief.bullet.text.4")))
        assert(doc.toString.contains(messages("yourTaxRelief.bullet.text.5")))
        assert(doc.toString.contains(messages("yourTaxRelief.warning.text")))
        assert(doc.toString.contains(messages("yourTaxRelief.button.text")))

      }
      "when all both after only is required" in {
        val doc = asDocument(view.apply("April", "2020",
          None, None, true, false)(request, messages))
        assert(doc.toString.contains(messages("Check to ensure you can claim tax relief after April 2020")))
        assert(!doc.toString.contains(messages("Check to ensure you can claim tax relief before")))
      }
    }

    "behave like a normal page" when {
      behave like normalPage(view.apply("April", "2020",
        Some("May"), Some("2022"), true, true)(request, messages), YourTaxRelief)
    }
  }
}
