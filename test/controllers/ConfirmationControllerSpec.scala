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

package controllers

import java.time.LocalDate
import base.SpecBase
import connectors.PaperlessPreferenceConnector
import models.paperless.{PaperlessStatus, PaperlessStatusResponse, Url}
import org.mockito.Matchers.{any, eq => eqm}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import views.html.{Confirmation2019_2020View, Confirmation2019_2020_2021View, Confirmation2021View}
import PaperlessAuditConst._
import models.SelectTaxYearsToClaimFor.{Option1, Option2}
import models.UserAnswers
import pages.{ClaimedForTaxYear2020, HasSelfAssessmentEnrolment, SelectTaxYearsToClaimForPage, WhenDidYouFirstStartWorkingFromHomePage}
import play.api.libs.json.Json
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// scalastyle:off magic.number
class ConfirmationControllerSpec extends SpecBase with MockitoSugar {

  val Confirmation2021ViewConst = 1
  val Confirmation20192020ViewConst = 2
  val Confirmation201920202021ViewConst = 3

  val somePreferencesUrl = "/change/preferences"

  "Confirmation Controller for when ClaimedForTaxYear2020 is false and confirmation year is 2021" must {
    "return OK and the correct view with paper preferences available" in {
      paperlessControllerTest(true, false, Confirmation2021ViewConst)
    }
    "return OK and the correct view with paper preferences unavailable" in {
      paperlessControllerTest(false, false, Confirmation2021ViewConst)
    }
    "return OK and the correct view with a 2019 tax year start date" in {
      paperlessControllerTest(true, false, Confirmation2021ViewConst)
    }
    "return OK and the correct view with a 2020 tax year start date" in {
      paperlessControllerTest(true, false, Confirmation2021ViewConst)
    }
  }

  "Confirmation Controller for when ClaimedForTaxYear2020 is false and confirmation year is 2019 & 2020" must {
    "return OK and the correct view with paper preferences available" in {
      paperlessControllerTest(true, false, Confirmation20192020ViewConst)
    }
    "return OK and the correct view with paper preferences unavailable" in {
      paperlessControllerTest(false, false, Confirmation20192020ViewConst)
    }
    "return OK and the correct view with a 2019 tax year start date" in {
      paperlessControllerTest(true, false, Confirmation20192020ViewConst)
    }
    "return OK and the correct view with a 2020 tax year start date" in {
      paperlessControllerTest(true, false, Confirmation20192020ViewConst)
    }
  }

  "Confirmation Controller for when ClaimedForTaxYear2020 is false and confirmation year is 2019 & 2020 & 2021" must {
    "return OK and the correct view with paper preferences available" in {
      paperlessControllerTest(true, false, Confirmation201920202021ViewConst)
    }
    "return OK and the correct view with paper preferences unavailable" in {
      paperlessControllerTest(false, false, Confirmation201920202021ViewConst)
    }
    "return OK and the correct view with a 2019 tax year start date" in {
      paperlessControllerTest(true, false, Confirmation201920202021ViewConst)
    }
    "return OK and the correct view with a 2020 tax year start date" in {
      paperlessControllerTest(true, false, Confirmation201920202021ViewConst)
    }
  }

  private def paperlessControllerTest(paperlessAvailable: Boolean,
                                      claimedForTaxYear2020: Boolean,
                                      expectedView: Int): Future[_] = {

    val optionJsonList = expectedView match {
      case 1 => Json.arr(Option1.toString)
      case 2 => Json.arr(Option2.toString)
      case 3 => Json.arr(Option1.toString, Option2.toString)
      case _ => ???
    }

    val paperlessPreferenceConnector = mock[PaperlessPreferenceConnector]
    val auditConnector = mock[AuditConnector]

    val application = applicationBuilder(userAnswers = Some(
      UserAnswers(userAnswersId, Json.obj(
        ClaimedForTaxYear2020.toString -> claimedForTaxYear2020,
        HasSelfAssessmentEnrolment.toString -> false,
        SelectTaxYearsToClaimForPage.toString -> optionJsonList,
        WhenDidYouFirstStartWorkingFromHomePage.toString -> earliestWorkingFromHomeDate)))
    ).overrides(bind[PaperlessPreferenceConnector].toInstance(paperlessPreferenceConnector))
      .overrides(bind[AuditConnector].toInstance(auditConnector))
      .build()

    if (paperlessAvailable) {
      when(paperlessPreferenceConnector.getPaperlessStatus(any())(any(), any())) thenReturn
        Future(
          Right(PaperlessStatusResponse(PaperlessStatus("ALRIGHT", "", ""), Url("", "")))
        )
    } else {
      when(paperlessPreferenceConnector.getPaperlessStatus(any())(any(), any())) thenReturn
        Future(
          Right(PaperlessStatusResponse(PaperlessStatus("PAPER", "", ""), Url(somePreferencesUrl, "")))
        )
    }

    val request = FakeRequest(GET, routes.ConfirmationController.onPageLoad().url)

    val result = route(application, request).value

    status(result) mustEqual OK

    val paperlessUrl = paperlessAvailable match {
      case true => None
      case false => Some(somePreferencesUrl)
    }

    expectedView match {
      case 1 =>
        val view = application.injector.instanceOf[Confirmation2021View]
        contentAsString(result) mustEqual
          view(paperlessAvailable, paperlessUrl)(request, messages).toString
      case 2 =>
        val view = application.injector.instanceOf[Confirmation2019_2020View]
        contentAsString(result) mustEqual
          view(paperlessAvailable, paperlessUrl)(request, messages).toString
      case 3 =>
        val view = application.injector.instanceOf[Confirmation2019_2020_2021View]
        contentAsString(result) mustEqual
          view(paperlessAvailable, paperlessUrl)(request, messages).toString
    }

    val dataToAudit = Map(NinoReference -> fakeNino, Enabled -> paperlessAvailable.toString)

    verify(auditConnector, times(1))
      .sendExplicitAudit(eqm("PaperlessPreferenceCheckSuccess"), eqm(dataToAudit))(any(), any())

    application.stop()
  }

}