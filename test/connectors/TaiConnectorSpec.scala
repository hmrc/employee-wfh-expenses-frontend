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
import config.FrontendAppConfig
import models.OtherExpense
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.{OK, NO_CONTENT}
import play.api.inject.guice.GuiceApplicationBuilder
import utils.WireMockHelper

import scala.concurrent.ExecutionContext.Implicits.global


class TaiConnectorSpec extends SpecBase with WireMockHelper with GuiceOneAppPerSuite with ScalaFutures with IntegrationPatience {
  override implicit lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure(
        conf = "microservice.services.tai.port" -> server.port,
        "otherExpensesId" -> 59
      )
      .build()

  private lazy val taiConnector: TaiConnector = app.injector.instanceOf[TaiConnector]
  private lazy val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  private val testTaxYear = 2019
  private val grossAmount = 120

  "getIabdData" must {
    "return valid IABD data for a 200 response with a valid response body" in {
      server.stubFor(
        get(urlEqualTo(s"/tai/$fakeNino/tax-account/$testTaxYear/expenses/employee-expenses/${appConfig.otherExpensesId}"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(validIabdJson(Some(grossAmount)).toString)
          )
      )

      val result = taiConnector.getIabdData(fakeNino, testTaxYear)

      whenReady(result) {
        res =>
          res mustBe a[Seq[_]]
          res.headOption mustBe Some(OtherExpense(grossAmount))
      }
    }

    "return an empty list for a 200 response with an invalid response body" in {
      server.stubFor(
        get(urlEqualTo(s"/tai/$fakeNino/tax-account/$testTaxYear/expenses/employee-expenses/${appConfig.otherExpensesId}"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(invalidJson.toString)
          )
      )

      val result = taiConnector.getIabdData(fakeNino, testTaxYear)

      whenReady(result) {
        res =>
          res mustBe a[Seq[_]]
          res.headOption mustBe None
      }
    }

    "return an empty list for a non-200 response" in {
      server.stubFor(
        get(urlEqualTo(s"/tai/$fakeNino/tax-account/$testTaxYear/expenses/employee-expenses/${appConfig.otherExpensesId}"))
          .willReturn(
            aResponse()
              .withStatus(NO_CONTENT)
              .withBody("")
          )
      )

      val result = taiConnector.getIabdData(fakeNino, testTaxYear)

      whenReady(result) {
        res =>
          res mustBe a[Seq[_]]
          res.headOption mustBe None
      }
    }

  }
}
