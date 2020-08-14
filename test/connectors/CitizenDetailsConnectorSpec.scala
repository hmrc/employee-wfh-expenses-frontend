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
  }

}
