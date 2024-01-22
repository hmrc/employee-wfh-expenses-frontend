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

import forms.ConfirmClaimInWeeksFormProvider
import play.api.Application
import play.api.data.Form
import play.twirl.api.Html
import views.behaviours.ViewBehaviours
import views.html.ConfirmClaimInWeeksView

// scalastyle:off magic.number
class ConfirmClaimInWeeksViewSpec extends ViewBehaviours {


  val application: Application = applicationBuilder().build()

  val view: ConfirmClaimInWeeksView = application.injector.instanceOf[ConfirmClaimInWeeksView]
  val form = new ConfirmClaimInWeeksFormProvider()(2)

  def createView(form: Form[Boolean]): Html = view.apply(form, 2)(fakeRequest, messages)

  val title = "Do you want to claim for 2 weeks of working from home in the current tax year?"
  val hint = "The claim of 2 weeks is for the current tax year from 6 April 2023 to 5 April 2024."

  "Confirm claim in weeks view" should {

    "have the correct banner title" in {
      val doc = asDocument(createView(form))
      val banner = doc.select(".hmrc-header__service-name")

      banner.text() mustEqual messages("service.name")
    }

    "show content" when {
      "when all confirmClaimInWeeks content is displayed" in {
        val doc = asDocument(createView(form))
        assertContainsMessages(doc, title)
        assertContainsMessages(doc, hint)
      }
    }

    behave like pageWithBackLink(createView(form))

  }
}
