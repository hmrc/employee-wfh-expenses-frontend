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

package controllers.actions

import base.SpecBase
import com.github.tomakehurst.wiremock.client.WireMock.{status => _, _}
import config.FrontendAppConfig
import models.requests.IdentifierRequest
import org.scalatest.concurrent.ScalaFutures
import play.api.Application
import play.api.http.Status.OK
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, Result}
import play.api.test.Helpers._
import utils.WireMockHelper

import scala.concurrent.Future

class CheckAlreadyClaimedActionSpec extends SpecBase with WireMockHelper with ScalaFutures {

  private lazy val fakeIdentifier = "fake-identifier"

  private def checkIdentifierRequest(identifierRequest: IdentifierRequest[AnyContent]): Future[Result] = {
    identifierRequest match {
      case IdentifierRequest(_, identifier, nino) if identifier == fakeIdentifier
        && nino == fakeNino => Future.successful(Ok)
      case _ => Future.successful(InternalServerError)
    }
  }

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .overrides(
      bind(classOf[CheckAlreadyClaimedAction]).to(classOf[CheckAlreadyClaimedActionImpl])
    )
    .configure(
      conf = "microservice.services.tai.port" -> server.port,
      "otherExpensesId" -> 59,
      "jobExpenseId" -> 55,
      "urls.p87DigitalForm" -> p87RedirectUrl,
      "auditing.enabled" -> false
    ).build()

  private lazy val appConfig: FrontendAppConfig = injector.instanceOf[FrontendAppConfig]
  private lazy val checkAlreadyClaimedAction: CheckAlreadyClaimedAction = injector.instanceOf[CheckAlreadyClaimedAction]
  private lazy val identifierRequest = IdentifierRequest[AnyContent](fakeRequest, fakeIdentifier, fakeNino)
  private lazy val grossAmount = 312

  "checkAlreadyClaimedAction" must {
    "must allow access for claimants that have not already claimed for expenses" in {
      server.stubFor(
        get(urlEqualTo(s"/tai/$fakeNino/tax-account/2020/expenses/employee-expenses/${appConfig.otherExpensesId}"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(validIabdJson(Some(0)).toString)
          )
      )

      server.stubFor(
        get(urlEqualTo(s"/tai/$fakeNino/tax-account/2020/expenses/employee-expenses/${appConfig.jobExpenseId}"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(validIabdJson(Some(0)).toString)
          )
      )

      val result = checkAlreadyClaimedAction.invokeBlock(identifierRequest, checkIdentifierRequest)
      status(result) mustBe OK

      server.verify(1, getRequestedFor(urlEqualTo(s"/tai/$fakeNino/tax-account/2020/expenses/employee-expenses/${appConfig.otherExpensesId}")))
      server.verify(1, getRequestedFor(urlEqualTo(s"/tai/$fakeNino/tax-account/2020/expenses/employee-expenses/${appConfig.jobExpenseId}")))
    }

    "redirect claimants that have already claimed for expenses to p87" when {
      "when IABD 59 (other expenses) is greater than zero" in {
        server.stubFor(
          get(urlEqualTo(s"/tai/$fakeNino/tax-account/2020/expenses/employee-expenses/${appConfig.otherExpensesId}"))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(validIabdJson(Some(grossAmount)).toString)
            )
        )

        server.stubFor(
          get(urlEqualTo(s"/tai/$fakeNino/tax-account/2020/expenses/employee-expenses/${appConfig.jobExpenseId}"))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(validIabdJson(Some(0)).toString)
            )
        )

        val result = checkAlreadyClaimedAction.invokeBlock(identifierRequest, checkIdentifierRequest)
        status(result) mustBe SEE_OTHER

        whenReady(result) {
          res =>
            res.header.headers(LOCATION) mustBe p87RedirectUrl
        }
      }

      "when IABD 55 (job expenses) is greater than zero" in {
        server.stubFor(
          get(urlEqualTo(s"/tai/$fakeNino/tax-account/2020/expenses/employee-expenses/${appConfig.otherExpensesId}"))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(validIabdJson(Some(0)).toString)
            )
        )

        server.stubFor(
          get(urlEqualTo(s"/tai/$fakeNino/tax-account/2020/expenses/employee-expenses/${appConfig.jobExpenseId}"))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(validIabdJson(Some(grossAmount)).toString)
            )
        )

        val result = checkAlreadyClaimedAction.invokeBlock(identifierRequest, checkIdentifierRequest)
        status(result) mustBe SEE_OTHER

        whenReady(result) {
          res =>
            res.header.headers(LOCATION) mustBe p87RedirectUrl
        }
      }

      "when both IABD 55 and IABD 59 is greater than zero" in {
        server.stubFor(
          get(urlEqualTo(s"/tai/$fakeNino/tax-account/2020/expenses/employee-expenses/${appConfig.otherExpensesId}"))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(validIabdJson(Some(grossAmount)).toString)
            )
        )

        server.stubFor(
          get(urlEqualTo(s"/tai/$fakeNino/tax-account/2020/expenses/employee-expenses/${appConfig.jobExpenseId}"))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(validIabdJson(Some(grossAmount)).toString)
            )
        )

        val result = checkAlreadyClaimedAction.invokeBlock(identifierRequest, checkIdentifierRequest)
        status(result) mustBe SEE_OTHER

        whenReady(result) {
          res =>
            res.header.headers(LOCATION) mustBe p87RedirectUrl
        }

      }
    }

    "not call TAI to fetch 2nd IABD (job rate expenses)" when {
      "when 1st IABD check (other expenses) returns > 0" in {
        server.stubFor(
          get(urlEqualTo(s"/tai/$fakeNino/tax-account/2020/expenses/employee-expenses/${appConfig.otherExpensesId}"))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(validIabdJson(Some(grossAmount)).toString)
            )
        )

        server.stubFor(
          get(urlEqualTo(s"/tai/$fakeNino/tax-account/2020/expenses/employee-expenses/${appConfig.jobExpenseId}"))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(validIabdJson(Some(grossAmount)).toString)
            )
        )

        val result = checkAlreadyClaimedAction.invokeBlock(identifierRequest, checkIdentifierRequest)
        status(result) mustBe SEE_OTHER

        server.verify(1, getRequestedFor(urlEqualTo(s"/tai/$fakeNino/tax-account/2020/expenses/employee-expenses/${appConfig.otherExpensesId}")))
        server.verify(0, getRequestedFor(urlEqualTo(s"/tai/$fakeNino/tax-account/2020/expenses/employee-expenses/${appConfig.jobExpenseId}")))
      }
    }

    "call TAI twice" when {
      "when 1st IABD check (other expenses) returns 0" in {
        server.stubFor(
          get(urlEqualTo(s"/tai/$fakeNino/tax-account/2020/expenses/employee-expenses/${appConfig.otherExpensesId}"))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(validIabdJson(Some(0)).toString)
            )
        )

        server.stubFor(
          get(urlEqualTo(s"/tai/$fakeNino/tax-account/2020/expenses/employee-expenses/${appConfig.jobExpenseId}"))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(validIabdJson(Some(0)).toString)
            )
        )

        val result = checkAlreadyClaimedAction.invokeBlock(identifierRequest, checkIdentifierRequest)

        whenReady(result) {
          _ =>
            server.verify(1, getRequestedFor(urlEqualTo(s"/tai/$fakeNino/tax-account/2020/expenses/employee-expenses/${appConfig.otherExpensesId}")))
            server.verify(1, getRequestedFor(urlEqualTo(s"/tai/$fakeNino/tax-account/2020/expenses/employee-expenses/${appConfig.jobExpenseId}")))
        }
      }

    }
  }


}
