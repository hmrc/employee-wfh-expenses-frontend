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

import controllers.routes
import play.api.test.FakeRequest
import play.api.test.Helpers.GET
import views.behaviours.ViewBehaviours
import views.html.InformClaimNowInWeeksView

class InformClaimNowInWeeksViewSpec extends ViewBehaviours{

  val title = messages("informClaimNowInWeeks.title")
  val heading = messages("informClaimNowInWeeks.heading")

  "InformClaimNowInWeeks view" must {
    val view = viewFor[InformClaimNowInWeeksView](Some(emptyUserAnswers))
    val request = FakeRequest(GET, routes.CannotClaimUsingThisServiceController.onPageLoad().url)
    val applyView = view.apply()(request, messages)

    behave like normalPage(applyView, "informClaimNowInWeeks")
    behave like pageWithBackLink(applyView)
  }
}
