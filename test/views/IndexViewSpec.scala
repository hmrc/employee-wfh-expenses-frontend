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
import views.html.IndexView

class IndexViewSpec extends ViewBehaviours {

  "Index view" must {

    val application = applicationBuilder().build()

    val view = application.injector.instanceOf[IndexView]

    val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)

    val applyView = view.apply()(request, messages)

    behave.like(normalPage(applyView, "index", args = Nil, ("heading", Nil)))
  }

}
