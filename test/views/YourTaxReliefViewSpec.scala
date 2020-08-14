/*
 * Copyright 2020 HM Revenue & Customs
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
import views.behaviours.ViewBehaviours
import views.html.YourTaxReliefView

class YourTaxReliefViewSpec extends ViewBehaviours {

  val workingFromHomeDate = LocalDate.of(2019,4,6)

  "YourTaxRelief view" must {

    val view = viewFor[YourTaxReliefView](Some(emptyUserAnswers))

    val applyView = view.apply(workingFromHomeDate)(fakeRequest, messages)

    behave like normalPage(applyView, "yourTaxRelief")

    behave like pageWithBackLink(applyView)
  }
}
