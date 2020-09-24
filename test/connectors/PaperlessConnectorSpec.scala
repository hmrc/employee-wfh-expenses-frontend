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

package connectors

import base.SpecBase
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, urlEqualTo}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.OK
import play.api.http.Status.BAD_REQUEST
import play.api.http.Status.INTERNAL_SERVER_ERROR
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

  "getPaperlessPreference" must {
    "return an preferences on success" in {
      server.stubFor(
        get(urlEqualTo(s"/paperless/preferences"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(validPaperlessPreferences.toString)
          )
      )

      val result = paperlessPreferenceConnector.getPaperlessPreference()

      whenReady(result) {
        res =>
          res mustBe a[Option[_]]
          res mustBe expectedValidPreference
      }
    }

    "return an preferences on success but missing preference" in {
      server.stubFor(
        get(urlEqualTo(s"/paperless/preferences"))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
          )
      )

      val errorResponse = paperlessPreferenceConnector.getPaperlessPreference()

      whenReady(errorResponse)(_ mustBe None)
    }

    "handle http 500 correctly" in {
      server.stubFor(
        get(urlEqualTo(s"/paperless/preferences"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      val errorResponse = paperlessPreferenceConnector.getPaperlessPreference()

      whenReady(errorResponse)(_ mustBe None)
    }

    "handle http Exception correctly" in {
      server.stubFor(
        get(urlEqualTo(s"/paperless/preferences"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      val errorResponse = paperlessPreferenceConnector.getPaperlessPreference()

      whenReady(errorResponse)(_ mustBe None)
    }
  }
}
