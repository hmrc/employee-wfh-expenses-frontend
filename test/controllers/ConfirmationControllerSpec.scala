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

package controllers

import base.SpecBase
import connectors.PaperlessPreferenceConnector
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.ConfirmationView
import scala.concurrent.Future

class ConfirmationControllerSpec extends SpecBase with MockitoSugar {

  private val paperlessPreferenceConnector = mock[PaperlessPreferenceConnector]

  "Confirmation Controller" must {
    "return OK and the correct view with paper preferences available" in {
      paperlessControllerTest(true)
    }
    "return OK and the correct view with paper preferences unavailable" in {
      paperlessControllerTest(false)
    }
  }

  private def paperlessControllerTest(paperlessAvailable: Boolean): Future[_] = {

    val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
      .overrides(bind[PaperlessPreferenceConnector].toInstance(paperlessPreferenceConnector))
      .build()

    when(paperlessPreferenceConnector.getPaperlessPreference()(any(), any())) thenReturn
      Future.successful(Some(paperlessAvailable))

    val request = FakeRequest(GET, routes.ConfirmationController.onPageLoad().url)

    val result = route(application, request).value

    val view = application.injector.instanceOf[ConfirmationView]

    status(result) mustEqual OK

    contentAsString(result) mustEqual
      view(paperlessAvailable, None)(fakeRequest, messages).toString

    application.stop()
  }
}