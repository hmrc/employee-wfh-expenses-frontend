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

package connectors

import base.SpecBase
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import utils.WireMockHelper

import scala.concurrent.ExecutionContext.Implicits.global


sealed trait ExpectedResults {
  val expectedValidPreference = Some(true)
}

class PaperlessConnectorSpec extends SpecBase with MockitoSugar with WireMockHelper with GuiceOneAppPerSuite
  with ScalaFutures with IntegrationPatience
  with ExpectedResults {

  override implicit val fakeRequest = FakeRequest()

  override implicit lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure(
        conf = "microservice.services.preferences-frontend.port" -> server.port
      )
      .build()

  private lazy val paperlessPreferenceConnector = app.injector.instanceOf[PaperlessPreferenceConnector]

  val someReturnUrl = "/go/somewhere"

  def stubForPaperlessStatus(response: ResponseDefinitionBuilder) = {
    server.stubFor(
      get(urlPathMatching(s"/paperless/status"))
        .withQueryParam("returnUrl", matching(".*"))
        .withQueryParam("returnLinkText", matching(".*"))
        .willReturn(
          response
        )
    )
  }

  "getPaperlessPreference" must {

    "handle http 200 as a paperless customer" in {
      stubForPaperlessStatus(response = aResponse().withStatus(OK).withBody(paperlessCustomer.toString))

      whenReady(paperlessPreferenceConnector.getPaperlessStatus(someReturnUrl)) {
        res =>
          res.isRight mustBe true
          res.right.get.isPaperlessCustomer mustBe true
      }
    }

    "handle http 200 as a paper/post customer" in {
      stubForPaperlessStatus(response = aResponse().withStatus(OK).withBody(paperCustomer.toString))

      whenReady(paperlessPreferenceConnector.getPaperlessStatus(someReturnUrl)) {
        res =>
          res.isRight mustBe true
          res.right.get.isPaperlessCustomer mustBe false
      }
    }

    "handle http 200 with invalid JSON" in {
      stubForPaperlessStatus(response = aResponse().withStatus(OK).withBody("""{"field":"not expected"}"""))

      whenReady(paperlessPreferenceConnector.getPaperlessStatus(someReturnUrl)) {
        res =>
          res.isLeft mustBe true
          res.left.get must include("returned invalid json")
      }
    }

    "handle http 400 correctly" in {
      stubForPaperlessStatus(response = aResponse().withStatus(BAD_REQUEST))

      whenReady(paperlessPreferenceConnector.getPaperlessStatus(someReturnUrl)) {
        response =>
          response.isLeft mustBe true
          response.left.get must include("returned 400")
      }
    }

    "handle http 500 correctly" in {
      stubForPaperlessStatus(response = aResponse().withStatus(INTERNAL_SERVER_ERROR))

      whenReady(paperlessPreferenceConnector.getPaperlessStatus(someReturnUrl)) {
        response =>
          response.isLeft mustBe true
          response.left.get must include("returned 500")
      }
    }

  }
}
