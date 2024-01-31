/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers

import base.SpecBase
import org.mockito.ArgumentMatchers.{any, eq => eqs}
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfter
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.IABDService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ClaimedAllYearsStatusControllerSpec extends SpecBase with BeforeAndAfter {

  val mockIABDService: IABDService = mock[IABDService]

  "claimedAllYearsStatus" must {
    "return OK for a GET and provide value of true when claimed all years" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[IABDService].toInstance(mockIABDService))
        .build()

      when(mockIABDService.claimedAllYearsStatus(eqs(fakeNino))(any())).thenReturn(Future(true))

      val request = FakeRequest(GET, routes.ClaimedAllYearsStatusController.claimedAllYearsStatus().url)
      val result = route(application, request).value
      val resultValue = (contentAsJson(result) \ "claimedAllYearsStatus").as[Boolean]

      status(result) mustBe OK
      resultValue mustBe true

      application.stop()
    }

    "return OK for a GET and provide value of false when NOT claimed all years" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[IABDService].toInstance(mockIABDService))
        .build()

      when(mockIABDService.claimedAllYearsStatus(eqs(fakeNino))(any())).thenReturn(Future(false))

      val request = FakeRequest(GET, routes.ClaimedAllYearsStatusController.claimedAllYearsStatus().url)
      val result = route(application, request).value
      val resultValue = (contentAsJson(result) \ "claimedAllYearsStatus").as[Boolean]

      status(result) mustBe OK
      resultValue mustBe false

      application.stop()
    }
  }

}
