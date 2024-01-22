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

import base.SpecBase
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, urlEqualTo}
import config.FrontendAppConfig
import models.{ETag, IABDExpense}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.{JsValidationException, UpstreamErrorResponse}
import utils.WireMockHelper

import scala.concurrent.ExecutionContext.Implicits.global


class TaiConnectorSpec extends SpecBase with WireMockHelper with GuiceOneAppPerSuite with ScalaFutures with IntegrationPatience {
  override implicit lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure(
        conf = "microservice.services.tai.port" -> server.port,
        "otherExpensesId" -> 59,
        "jobExpenseId" -> 55
      )
      .build()

  private lazy val taiConnector: TaiConnector = app.injector.instanceOf[TaiConnector]
  private lazy val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  private val testTaxYear = 2019
  private val testGrossAmount = 120
  private val testETag = ETag(version = etag)

  "getOtherExpensesData [IABD 59]" must {
    "return valid data for a 200 response with a valid response body" in {

      server.stubFor(
        get(urlEqualTo(s"/tai/$fakeNino/tax-account/$testTaxYear/expenses/employee-expenses/${appConfig.otherExpensesId}"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(validIabdJson(Some(testGrossAmount)).toString)
          )
      )

      val result = taiConnector.getOtherExpensesData(fakeNino, testTaxYear)

      whenReady(result) {
        res =>
          res mustBe a[Seq[_]]
          res.headOption mustBe Some(IABDExpense(testGrossAmount))
      }
    }

    "return a JsValidationException for a 200 response with an unexpected response body" in {
      server.stubFor(
        get(urlEqualTo(s"/tai/$fakeNino/tax-account/$testTaxYear/expenses/employee-expenses/${appConfig.otherExpensesId}"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(invalidJson.toString)
          )
      )

      val result = taiConnector.getOtherExpensesData(fakeNino, testTaxYear)

      whenReady(result.failed) {ex =>
        ex mustBe an[JsValidationException]
      }
    }

    "return an UpstreamErrorResponse for a 5xx response" in {
      server.stubFor(
        get(urlEqualTo(s"/tai/$fakeNino/tax-account/$testTaxYear/expenses/employee-expenses/${appConfig.otherExpensesId}"))
          .willReturn(
            aResponse()
              .withStatus(SERVICE_UNAVAILABLE)
              .withBody("")
          )
      )

      val result = taiConnector.getOtherExpensesData(fakeNino, testTaxYear)

      whenReady(result.failed) {ex =>
        ex mustBe an[UpstreamErrorResponse]
        ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe SERVICE_UNAVAILABLE
      }
    }

    "return an NotFoundException for a 404 response" in {
      server.stubFor(
        get(urlEqualTo(s"/tai/$fakeNino/tax-account/$testTaxYear/expenses/employee-expenses/${appConfig.otherExpensesId}"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
              .withBody("")
          )
      )

      val result = taiConnector.getOtherExpensesData(fakeNino, testTaxYear)

      whenReady(result.failed) {ex =>
        ex mustBe an[UpstreamErrorResponse]
        ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe NOT_FOUND
      }
    }
  }

  "getJobExpensesData [IABD 55]" must {
    "return valid data for a 200 response with a valid response body" in {

      server.stubFor(
        get(urlEqualTo(s"/tai/$fakeNino/tax-account/$testTaxYear/expenses/employee-expenses/${appConfig.jobExpenseId}"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(validIabdJson(Some(testGrossAmount)).toString)
          )
      )

      val result = taiConnector.getJobExpensesData(fakeNino, testTaxYear)

      whenReady(result) {
        res =>
          res mustBe a[Seq[_]]
          res.headOption mustBe Some(IABDExpense(testGrossAmount))
      }
    }

    "return a JsValidationException for a 200 response with an unexpected response body" in {
      server.stubFor(
        get(urlEqualTo(s"/tai/$fakeNino/tax-account/$testTaxYear/expenses/employee-expenses/${appConfig.jobExpenseId}"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(invalidJson.toString)
          )
      )

      val result = taiConnector.getJobExpensesData(fakeNino, testTaxYear)

      whenReady(result.failed) {ex =>
        ex mustBe an[JsValidationException]
      }
    }

    "return an UpstreamErrorResponse for a 5xx response" in {
      server.stubFor(
        get(urlEqualTo(s"/tai/$fakeNino/tax-account/$testTaxYear/expenses/employee-expenses/${appConfig.jobExpenseId}"))
          .willReturn(
            aResponse()
              .withStatus(SERVICE_UNAVAILABLE)
              .withBody("")
          )
      )

      val result = taiConnector.getJobExpensesData(fakeNino, testTaxYear)

      whenReady(result.failed) {ex =>
        ex mustBe an[UpstreamErrorResponse]
        ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe SERVICE_UNAVAILABLE
      }
    }

    "return an NotFoundException for a 404 response" in {
      server.stubFor(
        get(urlEqualTo(s"/tai/$fakeNino/tax-account/$testTaxYear/expenses/employee-expenses/${appConfig.jobExpenseId}"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
              .withBody("")
          )
      )

      val result = taiConnector.getJobExpensesData(fakeNino, testTaxYear)

      whenReady(result.failed) {ex =>
        ex mustBe an[UpstreamErrorResponse]
        ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe NOT_FOUND
      }
    }
  }

  "postIabdData" must {
    "return a 200 response" in {
      server.stubFor(
        post(urlEqualTo(s"/tai/$fakeNino/tax-account/$testTaxYear/expenses/working-from-home-employee-expenses/${appConfig.otherExpensesId}"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody("")
          )
      )

      val result = taiConnector.postIabdData(fakeNino, testTaxYear, testGrossAmount, testETag)

      whenReady(result) {
        res =>
          whenReady(result) {_ => succeed}
      }
    }

    "return a 401 response" in {
      server.stubFor(
        post(urlEqualTo(s"/tai/$fakeNino/tax-account/$testTaxYear/expenses/working-from-home-employee-expenses/${appConfig.otherExpensesId}"))
          .willReturn(
            aResponse()
              .withStatus(UNAUTHORIZED)
          )
      )

      val result = taiConnector.postIabdData(fakeNino, testTaxYear, testGrossAmount, testETag)

      whenReady(result.failed) {ex =>
        ex mustBe an[UpstreamErrorResponse]
        ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe UNAUTHORIZED
      }
    }

    "return a 404 response" in {
      server.stubFor(
        post(urlEqualTo(s"/tai/$fakeNino/tax-account/$testTaxYear/expenses/working-from-home-employee-expenses/${appConfig.otherExpensesId}"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      val result = taiConnector.postIabdData(fakeNino, testTaxYear, testGrossAmount, testETag)

      whenReady(result.failed) {ex =>
        ex mustBe an[UpstreamErrorResponse]
        ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe NOT_FOUND
      }
    }
  }

}
