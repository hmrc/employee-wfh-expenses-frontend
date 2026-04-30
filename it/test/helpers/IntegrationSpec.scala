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

package helpers

import helpers.application.IntegrationApplication
import helpers.wiremock.WireMockSetup
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier

trait IntegrationSpec
    extends AnyWordSpecLike
    with Matchers
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with WireMockSetup
    with IntegrationApplication
    with ScalaFutures {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWmServer()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    resetWmServer()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    stopWmServer()
  }

  lazy val fakeNino = "AB123456A"
  lazy val eTag: Int = 123
  lazy val validETagJson: JsValue = Json.parse(
    s"""
       |{
       |  "etag":"$eTag"
       |}
     """.stripMargin
  )
  lazy val invalidJson: JsValue = Json.parse("""{ "invalid" : "json" }""")
  def validIabdJson(grossAmount: Option[Int]): JsValue = Json.parse(
    s"""
       |[{
       |    "nino": "$fakeNino",
       |    "sequenceNumber": 201600003,
       |    "taxYear": 2019,
       |    "type": 59,
       |    "source": 26,
       |    "grossAmount": ${grossAmount.getOrElse(0)},
       |    "receiptDate": null,
       |    "captureDate": null,
       |    "typeDescription": "Other Expenses",
       |    "netAmount": null
       |}]
       |""".stripMargin
  )

  lazy val paperlessCustomer: JsValue = Json.parse(
    s"""
       |{
       |  "status": {
       |    "name": "ALRIGHT",
       |    "category": "INFO",
       |    "text": "some text"
       |  },
       |  "url": {
       |    "link": "/go/here",
       |    "text": "some link text"
       |  }
       |}
     """.stripMargin
  )

  lazy val paperCustomer: JsValue = Json.parse(
    s"""
       |{
       |  "status": {
       |    "name": "PAPER",
       |    "category": "INFO",
       |    "text": "some text"
       |  },
       |  "url": {
       |    "link": "/go/here",
       |    "text": "some link text"
       |  }
       |}
     """.stripMargin
  )

}
