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
import models.ETag
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.{HttpResponse, NotFoundException, UpstreamErrorResponse}
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

      whenReady(citizenDetailsConnector.getETag(fakeNino)) {
        _ mustBe ETag(etag)
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

      whenReady(citizenDetailsConnector.getETag(fakeNino).failed) { ex =>
        ex mustBe an[UpstreamErrorResponse]
        ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe INTERNAL_SERVER_ERROR
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

      whenReady(citizenDetailsConnector.getETag(fakeNino).failed) { ex =>
        ex mustBe an[UpstreamErrorResponse]
        ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe UNAUTHORIZED
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

      whenReady(citizenDetailsConnector.getETag(fakeNino).failed) { ex =>
        ex mustBe an[NotFoundException]
      }
    }
  }

}
