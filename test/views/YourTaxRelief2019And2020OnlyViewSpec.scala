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

import java.time.LocalDate

import utils.TaxYearDates._
import views.behaviours.ViewBehaviours
import views.html.YourTaxRelief2019And2020View

// scalastyle:off magic.number
class YourTaxRelief2019And2020OnlyViewSpec extends ViewBehaviours {

  val view = viewFor[YourTaxRelief2019And2020View](Some(emptyUserAnswers))

  "YourTaxRelief2019And2020View" must {
    val applyView = view.apply(TAX_YEAR_2019_START_DATE, 53)(fakeRequest, messages)

    behave like normalPage(applyView, "yourTaxRelief")

    behave like pageWithBackLink(applyView)

    "must display the number of weeks on the page (plural)" in {
      val workingFromHomeDate: LocalDate = TAX_YEAR_2019_START_DATE

      val applyView = view.apply(workingFromHomeDate, 53)(fakeRequest, messages)

      val doc = asDocument(applyView)
      assertContainsText(doc, messages("yourTaxRelief.2019And2020.paragraph.one", "53 weeks"))
    }

    "must display the number of weeks on the page (singular)" in {
      val applyView = view.apply(TAX_YEAR_2019_END_DATE, 1)(fakeRequest, messages)

      val doc = asDocument(applyView)
      assertContainsText(doc, messages("yourTaxRelief.2019And2020.paragraph.one", "1 week"))
    }

    "must display the working from home start date in a easy readable fashion" in {
      val applyView = view.apply(TAX_YEAR_2019_START_DATE, 53)(fakeRequest, messages)

      val doc = asDocument(applyView)
      assertContainsText(doc, "6 April 2019")
    }
  }

}