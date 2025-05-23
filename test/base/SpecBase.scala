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

package base

import config.FrontendAppConfig
import controllers.actions._
import models.UserAnswers
import org.jsoup.Jsoup
import org.scalatest.TryValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice._
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{Injector, bind}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.Future

trait SpecBase
    extends PlaySpec
    with GuiceOneAppPerSuite
    with TryValues
    with ScalaFutures
    with IntegrationPatience
    with MockitoSugar {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val userAnswersId = "id"

  val p87RedirectUrl = "p87-redirect-url"

  val earliestWorkingFromHomeDate = LocalDate.of(2020, 1, 1)

  def emptyUserAnswers = UserAnswers(userAnswersId, Json.obj())

  def injector: Injector = app.injector

  def frontendAppConfig: FrontendAppConfig = injector.instanceOf[FrontendAppConfig]

  def messagesApi: MessagesApi = injector.instanceOf[MessagesApi]

  def fakeRequest = FakeRequest("", "")

  implicit def messages: Messages = messagesApi.preferred(fakeRequest)

  protected def applicationBuilder(userAnswers: Option[UserAnswers] = None): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers)),
        bind[ManualCorrespondenceIndicatorAction].to[FakeManualCorrespondenceIndicatorAction]
      )

  lazy val fakeNino = "AB123456A"

  lazy val fakeInternalId = "111"

  lazy val fakeSaUtr = "222"

  lazy val etag: Int = 123

  lazy val validETagJson: JsValue = Json.parse(
    s"""
       |{
       |  "etag":"$etag"
       |}
     """.stripMargin
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

  def prepopulatedValue(result: Future[Result], id: String): String =
    Jsoup.parse(contentAsString(result)).getElementById(id).attr("value")

}
