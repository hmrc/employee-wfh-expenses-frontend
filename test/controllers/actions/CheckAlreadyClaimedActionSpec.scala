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

package controllers.actions

import base.SpecBase
import com.github.tomakehurst.wiremock.client.WireMock.{status => _}
import config.FrontendAppConfig
import models.requests.IdentifierRequest
import models.{Expenses, IABDExpense}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status.OK
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, Result}
import play.api.test.Helpers._
import services.IABDServiceImpl
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// scalastyle:off magic.number
class CheckAlreadyClaimedActionSpec extends SpecBase with ScalaFutures with BeforeAndAfter {

  private lazy val fakeIdentifier = "fake-identifier"

  private def checkIdentifierRequest(identifierRequest: IdentifierRequest[AnyContent]): Future[Result] = {
    identifierRequest match {
      case IdentifierRequest(_, identifier, nino, _) if identifier == fakeIdentifier
        && nino == fakeNino => Future.successful(Ok)
      case _ => Future.successful(InternalServerError)
    }
  }

  val identifierRequest = IdentifierRequest[AnyContent](fakeRequest, fakeIdentifier, fakeNino)
  val testOtherExpensesAmount = 123
  val testJobExpensesAmount = 321

  val mockIabdService = mock[IABDServiceImpl]
  val mockAppConfig = mock[FrontendAppConfig]
  val mockAuditConnector = mock[AuditConnector]

  class Setup {
    val filterUnderTest = new CheckAlreadyClaimedActionImpl(mockIabdService,mockAppConfig,mockAuditConnector)
  }

  before {
    Mockito.reset(mockIabdService, mockAppConfig, mockAuditConnector)
  }

  "ActionFilter" must {
    "must allow access for claimants that have not already claimed for expenses" in new Setup {
      when(mockIabdService.alreadyClaimed(any(), any())(any())).thenReturn(Future(None))

      val result = filterUnderTest.invokeBlock(identifierRequest, checkIdentifierRequest)
      status(result) mustBe OK
    }

    "redirect claimants that have already claimed for expenses to p87" when {
      "IABD 59 (other expenses) is greater than zero" in new Setup {
        when(mockAppConfig.p87DigitalFormUrl).thenReturn(p87RedirectUrl)
        when(mockIabdService.alreadyClaimed(any(), any())(any()))
          .thenReturn(
            Future(
              Some(
                Expenses(2021,Seq(IABDExpense(123)),Seq.empty,wasJobRateExpensesChecked = false)
              )
            )
          )

        val result = filterUnderTest.invokeBlock(identifierRequest, checkIdentifierRequest)

        status(result) mustBe SEE_OTHER
        whenReady(result) {
          res =>
            res.header.headers(LOCATION) mustBe p87RedirectUrl
        }
      }

      "IABD 55 (job expenses) is greater than zero" in new Setup {
        when(mockAppConfig.p87DigitalFormUrl).thenReturn(p87RedirectUrl)
        when(mockIabdService.alreadyClaimed(any(), any())(any()))
          .thenReturn(
            Future(
              Some(
                Expenses(2021,Seq.empty,Seq(IABDExpense(123)),wasJobRateExpensesChecked = true)
              )
            )
          )

        val result = filterUnderTest.invokeBlock(identifierRequest, checkIdentifierRequest)

        status(result) mustBe SEE_OTHER
        whenReady(result) {
          res =>
            res.header.headers(LOCATION) mustBe p87RedirectUrl
        }
      }

      "Both IABD 55 and IABD 59 is greater than zero" in new Setup {
        when(mockAppConfig.p87DigitalFormUrl).thenReturn(p87RedirectUrl)
        when(mockIabdService.alreadyClaimed(any(), any())(any()))
          .thenReturn(
            Future(
              Some(
                Expenses(2021,Seq(IABDExpense(123)),Seq(IABDExpense(123)),wasJobRateExpensesChecked = true)
              )
            )
          )

        val result = filterUnderTest.invokeBlock(identifierRequest, checkIdentifierRequest)

        status(result) mustBe SEE_OTHER
        whenReady(result) {
          res =>
            res.header.headers(LOCATION) mustBe p87RedirectUrl
        }
      }
    }

  }

}
