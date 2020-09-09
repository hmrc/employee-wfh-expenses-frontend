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
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.HttpResponse
import utils.WireMockHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CitizenDetailsConnectorSpec extends SpecBase with MockitoSugar with WireMockHelper with GuiceOneAppPerSuite with ScalaFutures with IntegrationPatience {

  override implicit lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure(
        conf = "microservice.services.citizen-details.port" -> server.port
      )
      .build()

  private lazy val citizenDetailsConnector: CitizenDetailsConnector = app.injector.instanceOf[CitizenDetailsConnector]

  "getAddress" must {
    "return an address on success" in {
      server.stubFor(
        get(urlEqualTo(s"/citizen-details/$fakeNino/designatory-details"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(validAddressJson.toString)
          )
      )

      val result = citizenDetailsConnector.getAddress(fakeNino)

      whenReady(result) {
        res =>
          res mustBe a[HttpResponse]
          res.status mustBe 200
          res.body mustBe validAddressJson.toString
      }

    }

    "return a 400 on failure" in {
      server.stubFor(
        get(urlEqualTo(s"/citizen-details/$fakeNino/designatory-details"))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
              .withBody(validAddressJson.toString)
          )
      )

      val result = citizenDetailsConnector.getAddress(fakeNino)

      whenReady(result) {
        res =>
          res mustBe a[HttpResponse]
          res.status mustBe 400
          res.body mustBe validAddressJson.toString
      }

    }

    "return a 404 on failure" in {
      server.stubFor(
        get(urlEqualTo(s"/citizen-details/$fakeNino/designatory-details"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
              .withBody(validAddressJson.toString)
          )
      )

      val result = citizenDetailsConnector.getAddress(fakeNino)

      whenReady(result) {
        res =>
          res mustBe a[HttpResponse]
          res.status mustBe 404
          res.body mustBe validAddressJson.toString
      }

    }

    "return a 500 on failure" in {
      server.stubFor(
        get(urlEqualTo(s"/citizen-details/$fakeNino/designatory-details"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody(validAddressJson.toString)
          )
      )

      val result = citizenDetailsConnector.getAddress(fakeNino)

      whenReady(result) {
        res =>
          res mustBe a[HttpResponse]
          res.status mustBe 500
          res.body mustBe validAddressJson.toString
      }

    }
  }

  "getETag" must {
    "return an etag on success" in {
      server.stubFor(
        get(urlEqualTo(s"/citizen-details/$fakeNino/etag"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(validETagJson.toString)
          )
      )

      val result = citizenDetailsConnector.getETag(fakeNino)

      whenReady(result) {
        res =>
          res mustBe a[HttpResponse]
          res.status mustBe 200
          res.body mustBe validETagJson.toString
      }
    }

    "return 500 on failure" in {
      server.stubFor(
        get(urlEqualTo(s"/citizen-details/$fakeNino/etag"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      val result: Future[HttpResponse] = citizenDetailsConnector.getETag(fakeNino)

      whenReady(result) {
        res =>
          res mustBe a[HttpResponse]
          res.status mustBe 500
          res.body mustBe ""
      }
    }

    "return 401 on failure" in {
      server.stubFor(
        get(urlEqualTo(s"/citizen-details/$fakeNino/etag"))
          .willReturn(
            aResponse()
              .withStatus(UNAUTHORIZED)
          )
      )

      val result: Future[HttpResponse] = citizenDetailsConnector.getETag(fakeNino)

      whenReady(result) {
        res =>
          res mustBe a[HttpResponse]
          res.status mustBe 401
          res.body mustBe ""
      }
    }

    "return 404 on failure" in {
      server.stubFor(
        get(urlEqualTo(s"/citizen-details/$fakeNino/etag"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      val result: Future[HttpResponse] = citizenDetailsConnector.getETag(fakeNino)

      whenReady(result) {
        res =>
          res mustBe a[HttpResponse]
          res.status mustBe 404
          res.body mustBe ""
      }
    }
  }

}
