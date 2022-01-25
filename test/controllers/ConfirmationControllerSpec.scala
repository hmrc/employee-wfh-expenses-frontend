/*
 * Copyright 2022 HM Revenue & Customs
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
import controllers.PaperlessAuditConst._
import models.SelectTaxYearsToClaimFor.{Option1, Option2, Option3}
import models.UserAnswers
import models.paperless.{PaperlessStatus, PaperlessStatusResponse, Url}
import org.mockito.Matchers.{any, eq => eqm}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{ClaimedForTaxYear2020, HasSelfAssessmentEnrolment, SelectTaxYearsToClaimForPage, WhenDidYouFirstStartWorkingFromHomePage}
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import views.html.ConfirmationView

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// scalastyle:off magic.number
class ConfirmationControllerSpec extends SpecBase with MockitoSugar {

  val somePreferencesUrl = "/change/preferences"

  "Confirmation Controller is shown" must {
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

    val application = applicationBuilder(userAnswers = Some(
      UserAnswers(userAnswersId, Json.obj(
        ClaimedForTaxYear2020.toString -> true,
        HasSelfAssessmentEnrolment.toString -> false,
        SelectTaxYearsToClaimForPage.toString -> Json.arr(Option1.toString, Option2.toString, Option3.toString),
        WhenDidYouFirstStartWorkingFromHomePage.toString -> earliestWorkingFromHomeDate)))
    ).overrides(bind[PaperlessPreferenceConnector].toInstance(paperlessPreferenceConnector))
      .overrides(bind[AuditConnector].toInstance(auditConnector))
      .build()

    if (paperlessAvailable) {
      when(paperlessPreferenceConnector.getPaperlessStatus(any())(any(), any())) thenReturn
        Future(
          Right(PaperlessStatusResponse(PaperlessStatus("ALRIGHT"), Url("", "")))
        )
    } else {
      when(paperlessPreferenceConnector.getPaperlessStatus(any())(any(), any())) thenReturn
        Future(
          Right(PaperlessStatusResponse(PaperlessStatus("PAPER"), Url(somePreferencesUrl, "")))
        )
    }

    val request = FakeRequest(GET, routes.ConfirmationController.onPageLoad().url)

    val result = route(application, request).value

    status(result) mustEqual OK

    val paperlessUrl = paperlessAvailable match {
      case true => None
      case false => Some(somePreferencesUrl)
    }

    val view = application.injector.instanceOf[ConfirmationView]
    contentAsString(result) mustEqual
      view(paperlessAvailable, paperlessUrl, true, true, true)(request, messages).toString

    val dataToAudit = Map(NinoReference -> fakeNino, Enabled -> paperlessAvailable.toString)

    verify(auditConnector, times(1))
      .sendExplicitAudit(eqm("PaperlessPreferenceCheckSuccess"), eqm(dataToAudit))(any(), any())

    application.stop()
  }

}