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

import forms.WhenDidYouFirstStartWorkingFromHomeFormProvider
import play.api.Application
import play.api.data.Form
import play.twirl.api.Html
import views.behaviours.ViewBehaviours
import views.html.WhenDidYouFirstStartWorkingFromHomeView

class WhenDidYouFirstStartWorkingFromHomeViewSpec extends ViewBehaviours {


  val messageKeyPrefix = s"whenDidYouFirstStartWorkingFromHome"

  val application: Application = applicationBuilder().build

  val view: WhenDidYouFirstStartWorkingFromHomeView = application.injector.instanceOf[WhenDidYouFirstStartWorkingFromHomeView]

  val form = new WhenDidYouFirstStartWorkingFromHomeFormProvider()()

  def createView(form: Form[_]): Html = view.apply(form, true)(fakeRequest, messages)

  val title = "When did you start working from home during 2020 to 2021?"
  val hintText = "For example, 23 3 2020"

  "When did you first start working from home view" should {
    "have the correct banner title" in {
      val doc = asDocument(createView(form))
      val banner = doc.select(".hmrc-header__service-name")

      banner.text() mustEqual messages("service.name")
    }

    "display the correct heading" in {
      val doc = asDocument(createView(form))
      assertPageTitleEqualsMessage(doc, s"$messageKeyPrefix.heading")
    }

    "show content" when {
      "when all WhenDidYouFirstStartWorkingFromHomeView content is displayed" in {
        val doc = asDocument(createView(form))
        assertContainsMessages(doc, title)
        assertContainsMessages(doc, hintText)
      }
    }

    behave like pageWithBackLink(createView(form))

  }
}
