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

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import helpers.IntegrationSpec
import org.scalatest.concurrent.IntegrationPatience
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest

class EmployeeExpensesConnectorSpec
    extends IntegrationSpec
    with MockitoSugar
    with IntegrationPatience {

  implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  private lazy val employeeExpensesConnector = app.injector.instanceOf[EmployeeExpensesConnector]

  def stubForSessionRefresh(response: ResponseDefinitionBuilder): StubMapping =
    stubFor(
      get(urlPathMatching(s"/employee-expenses/merged-journey-refresh-session"))
        .willReturn(
          response
        )
    )

  "updateMergedJourneySession" must {

    "handle http 200 response as success" in {
      stubForSessionRefresh(response = aResponse().withStatus(OK))

      whenReady(employeeExpensesConnector.updateMergedJourneySession(hc))(result => result mustBe true)
    }

    "handle http 303 as a fail" in {
      stubForSessionRefresh(response = aResponse().withStatus(SEE_OTHER))

      whenReady(employeeExpensesConnector.updateMergedJourneySession(hc))(result => result mustBe false)
    }

    "handle exception as a fail" in
      whenReady(employeeExpensesConnector.updateMergedJourneySession(hc))(result => result mustBe false)
  }

}
