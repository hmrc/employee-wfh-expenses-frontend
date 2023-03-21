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
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, urlEqualTo}
import controllers.routes
import models.requests.IdentifierRequest
import org.scalatest.concurrent.ScalaFutures
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results.Ok
import play.api.mvc.{AnyContent, Result}
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import utils.WireMockHelper

import scala.concurrent.Future

class ManualCorrespondenceIndicatorActionSpec extends SpecBase with WireMockHelper with ScalaFutures {

  private lazy val fakeIdentifier = "fake-identifier"
  private lazy val mciAction: ManualCorrespondenceIndicatorAction = injector.instanceOf[ManualCorrespondenceIndicatorAction]
  private lazy val identifierRequest = IdentifierRequest[AnyContent](fakeRequest, fakeIdentifier, fakeNino)

  private def dummyBlockToExecute(identifierRequest: IdentifierRequest[AnyContent]): Future[Result] =
    Future.successful(Ok("dummy block executed"))

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .overrides(
      bind(classOf[CheckAlreadyClaimedAction]).to(classOf[CheckAlreadyClaimedActionImpl])
    )
    .configure(
      conf = "microservice.services.citizen-details.port" -> server.port
    ).build()

  "ManualCorrespondenceIndicatorAction" must {

    "execute the controller block" when {
      "citizen details returns a HTTP 200 response" in {
        server.stubFor(
          get(urlEqualTo(s"/citizen-details/$fakeNino/designatory-details"))
            .willReturn(
              aResponse()
                .withStatus(OK)
            )
        )

        val result = mciAction.invokeBlock(identifierRequest, dummyBlockToExecute)

        status(result) mustBe OK
        contentAsString(result) mustBe "dummy block executed"
      }
    }

    "redirect to the MCI controller and view" when {
      "citizen details returns a 423 LOCKED response" in {
        server.stubFor(
          get(urlEqualTo(s"/citizen-details/$fakeNino/designatory-details"))
            .willReturn(
              aResponse()
                .withStatus(LOCKED)
            )
        )

        val result = mciAction.invokeBlock(identifierRequest, dummyBlockToExecute)

        status(result) mustBe SEE_OTHER
        whenReady(result) {
          res =>
            res.header.headers(LOCATION) mustBe routes.ManualCorrespondenceIndicatorController.onPageLoad().url
        }
      }
    }

    "redirect to the Technical Error controller and view" when {
      "citizen details returns an unexpected error" in {
        server.stubFor(
          get(urlEqualTo(s"/citizen-details/$fakeNino/designatory-details"))
            .willReturn(
              aResponse()
                .withStatus(SERVICE_UNAVAILABLE)
            )
        )

        val result = mciAction.invokeBlock(identifierRequest, dummyBlockToExecute)

        status(result) mustBe SEE_OTHER
        whenReady(result) {
          res =>
            res.header.headers(LOCATION) mustBe routes.TechnicalDifficultiesController.onPageLoad.url
        }
      }
    }
  }


}
