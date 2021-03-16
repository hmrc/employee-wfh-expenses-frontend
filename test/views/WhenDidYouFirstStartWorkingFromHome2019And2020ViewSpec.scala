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

import forms.WhenDidYouFirstStartWorkingFromHomeFormProvider
import models.UserAnswers
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.behaviours.QuestionViewBehaviours
import views.html.WhenDidYouFirstStartWorkingFromHome2019_2020View

import java.time.LocalDate

class WhenDidYouFirstStartWorkingFromHome2019And2020ViewSpec extends QuestionViewBehaviours[LocalDate] {

  val messageKeyPrefix = "whenDidYouFirstStartWorkingFromHome"

  val form = new WhenDidYouFirstStartWorkingFromHomeFormProvider()()

  "WhenDidYouFirstStartWorkingFromHomeView view" must {

    val application = applicationBuilder(userAnswers = Some(UserAnswers(userAnswersId))).build()

    val view = application.injector.instanceOf[WhenDidYouFirstStartWorkingFromHome2019_2020View]

    def applyView(form: Form[_]): HtmlFormat.Appendable =
      view.apply(form)(fakeRequest, messages)

    behave like normalPage(applyView(form), messageKeyPrefix)

    behave like pageWithBackLink(applyView(form))
  }
}