/*
 * Copyright 2022 HM Revenue & Customs
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

package services


import base.SpecBase
import connectors.EligibilityCheckerConnector
import models.requests.DataRequest
import models.{UserAnswers, WfhDueToCovidStatusWrapper}
import org.mockito.Matchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfter
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.AnyContent
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EligibilityCheckerServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfter {

  val mockEligibilityCheckerConnector: EligibilityCheckerConnector = mock[EligibilityCheckerConnector]

  class Setup {
    val serviceUnderTest = new EligibilityCheckerService(mockEligibilityCheckerConnector)
  }

  before {
    Mockito.reset(mockEligibilityCheckerConnector)
  }

  implicit val dataRequest: DataRequest[AnyContent] = DataRequest(fakeRequest, "internalId", UserAnswers("id"), "testNino", None)

  "Eligibility Checker Service" should {

    "return wfhDueToCovidStatus value from repo" in new Setup {

      when(mockEligibilityCheckerConnector.wfhDueToCovidStatus(any())(any(), any())) thenReturn Future.successful(WfhDueToCovidStatusWrapper(1, false))

      val result = await(serviceUnderTest.wfhDueToCovidStatus("session-4968a9e4-0fa1-445f-a947-a828c69ef96b"))

      result.get.WfhDueToCovidStatus mustBe 1

    }

  }
}
