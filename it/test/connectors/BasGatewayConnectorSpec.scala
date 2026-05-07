/*
 * Copyright 2026 HM Revenue & Customs
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
import org.scalacheck.Gen
import org.scalatest.concurrent.IntegrationPatience
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.http.Status._

import scala.concurrent.ExecutionContext.Implicits.global

class BasGatewayConnectorSpec
    extends IntegrationSpec
    with ScalaCheckDrivenPropertyChecks
    with IntegrationPatience {

  private lazy val basGatewayConnector: BasGatewayConnector = app.injector.instanceOf[BasGatewayConnector]

  private val signOutPath = "/bas-gateway/logout-without-state"

  "sign out" must {
    "return 200 on success" in {
      stubFor(
        post(urlEqualTo(signOutPath))
          .willReturn(
            aResponse()
              .withStatus(OK)
          )
      )

      whenReady(basGatewayConnector.signOutUser())(e => e.status mustBe OK)
    }

    "return error response code on failure" in {
      val statusGen: Gen[Int] = Gen.oneOf(401, 404, 500)
      forAll(statusGen) { statusCode =>
        stubFor(
          post(urlEqualTo(signOutPath))
            .willReturn(
              aResponse()
                .withStatus(statusCode)
            )
        )

        whenReady(basGatewayConnector.signOutUser())(e => e.status mustBe statusCode)

      }
    }

  }

}
