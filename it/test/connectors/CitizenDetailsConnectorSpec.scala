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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import helpers.IntegrationSpec
import models.ETag
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status._
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.ExecutionContext.Implicits.global

class CitizenDetailsConnectorSpec
    extends IntegrationSpec
    with MockitoSugar
    with IntegrationPatience {

  private lazy val citizenDetailsConnector: CitizenDetailsConnector = app.injector.instanceOf[CitizenDetailsConnector]

  "getETag" must {
    "return an etag on success" in {
      stubFor(
        get(urlEqualTo(s"/citizen-details/$fakeNino/etag"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(validETagJson.toString)
          )
      )

      whenReady(citizenDetailsConnector.getETag(fakeNino)) {
        _ mustBe ETag(eTag)
      }
    }

    "return 500 on failure" in {
      stubFor(
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
      stubFor(
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
      stubFor(
        get(urlEqualTo(s"/citizen-details/$fakeNino/etag"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      whenReady(citizenDetailsConnector.getETag(fakeNino).failed) { ex =>
        ex mustBe an[UpstreamErrorResponse]
        ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe NOT_FOUND
      }
    }
  }

}
