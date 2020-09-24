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
import config.FrontendAppConfig
import connectors.PaperlessPreferenceConnector
import controllers.PaperlessAuditConst.{Enabled, NinoReference}
import org.mockito.Matchers.{any, eq => eqm}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import views.html.ConfirmationView

import scala.concurrent.Future

class ConfirmationControllerSpec extends SpecBase with MockitoSugar {

  "Confirmation Controller" must {
    "return OK and the correct view with paper preferences available" in {
      paperlessControllerTest(true)
    }
    "return OK and the correct view with paper preferences unavailable" in {
      paperlessControllerTest(false)
    }
}

  private def paperlessControllerTest(paperlessAvailable: Boolean): Future[_] = {

    val paperlessPreferenceConnector = mock[PaperlessPreferenceConnector]
    val auditConnector = mock[AuditConnector]

    val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
      .overrides(bind[PaperlessPreferenceConnector].toInstance(paperlessPreferenceConnector))
      .overrides(bind[AuditConnector].toInstance(auditConnector))
      .build()

    when(paperlessPreferenceConnector.getPaperlessPreference()(any(), any())) thenReturn
      Future.successful(Some(paperlessAvailable))

    val request = FakeRequest(GET, routes.ConfirmationController.onPageLoad().url)

    val result = route(application, request).value

    val view = application.injector.instanceOf[ConfirmationView]
    val appConfig = application.injector.instanceOf[FrontendAppConfig]

    status(result) mustEqual OK

    val paperlessUrl = paperlessAvailable match {
      case true => None
      case false => Some(s"${appConfig.pertaxFrontendHost}/personal-account")
    }

    contentAsString(result) mustEqual
      view(paperlessAvailable, paperlessUrl)(fakeRequest, messages).toString

    val dataToAudit = Map(NinoReference -> fakeNino, Enabled -> paperlessAvailable.toString)
    verify(auditConnector, times(1))
    .sendExplicitAudit(eqm("PaperlessPreferenceAudit"), eqm(dataToAudit))(any(), any())

    application.stop()
  }
}