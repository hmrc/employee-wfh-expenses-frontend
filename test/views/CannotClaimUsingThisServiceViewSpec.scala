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
import views.html.CannotClaimUsingThisServiceView

class CannotClaimUsingThisServiceViewSpec extends ViewBehaviours {

  object MessagesHelper {
    val para1 = "You can only claim expenses for working from home on or after the 1 January 2020 for these tax years:"
    val bullet2024 = "6 April 2023 to 5 April 2024 (this tax year)"
    val bullet2023 = "6 April 2022 to 5 April 2023"
    val bullet2022 = "6 April 2021 to 5 April 2022"
    val bullet2021 = "6 April 2020 to 5 April 2021"
    val bullet2020 = "1 January 2020 to 5 April 2020"
  }

  "CannotClaimUsingThisService view" must {
    val view = viewFor[CannotClaimUsingThisServiceView](Some(emptyUserAnswers))

    val request = FakeRequest(GET, routes.CannotClaimUsingThisServiceController.onPageLoad().url)

    val applyView = view.apply()(request, messages)
    val doc = asDocument(applyView)

    behave like normalPage(applyView, "cannotClaimUsingThisService")

    "have valid content" in {
      assertContainsMessages(doc,
        MessagesHelper.para1,
        MessagesHelper.bullet2024,
        MessagesHelper.bullet2023,
        MessagesHelper.bullet2022,
        MessagesHelper.bullet2021,
        MessagesHelper.bullet2020
      )
    }
  }
}
