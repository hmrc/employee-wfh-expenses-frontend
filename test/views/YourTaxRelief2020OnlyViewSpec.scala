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

import utils.TaxYearDates._
import views.behaviours.ViewBehaviours
import views.html.YourTaxRelief2020OnlyView

// scalastyle:off magic.number
class YourTaxRelief2020OnlyViewSpec extends ViewBehaviours {

  val view = viewFor[YourTaxRelief2020OnlyView](Some(emptyUserAnswers))

  "YourTaxRelief2020OnlyView" must {
    val applyView = view.apply(TAX_YEAR_2020_START_DATE)(fakeRequest, messages)

    behave like normalPage(applyView, "yourTaxRelief")

    behave like pageWithBackLink(applyView)

    "must display the working from home start date in a easy readable fashion" in {
      val doc = asDocument(applyView)
      assertContainsText(doc, "6 April 2020")
    }
  }


}
