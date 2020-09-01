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

package controllers.actions

import base.SpecBase
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, urlEqualTo}
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
      "urls.p87DigitalForm" -> p87RedirectUrl
    ).build()

  private lazy val appConfig: FrontendAppConfig = injector.instanceOf[FrontendAppConfig]
  private lazy val checkAlreadyClaimedAction: CheckAlreadyClaimedAction = injector.instanceOf[CheckAlreadyClaimedAction]
  private lazy val identifierRequest = IdentifierRequest[AnyContent](fakeRequest, fakeIdentifier, fakeNino)
  private lazy val grossAmount = 312

  "checkAlreadyClaimedAction" must {
    "allow access for claimants that have not already claimed for expenses" in {
      server.stubFor(
        get(urlEqualTo(s"/tai/$fakeNino/tax-account/2020/expenses/employee-expenses/${appConfig.otherExpensesId}"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(validIabdJson(None).toString)
          )
      )

      val result = checkAlreadyClaimedAction.invokeBlock(identifierRequest, checkIdentifierRequest)
      status(result) mustBe OK
    }

    "redirect claimants that have already claimed for expenses to p87" in {
      server.stubFor(
        get(urlEqualTo(s"/tai/$fakeNino/tax-account/2020/expenses/employee-expenses/${appConfig.otherExpensesId}"))
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


}
