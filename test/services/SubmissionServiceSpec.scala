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

package services

import java.time.LocalDate
import java.time.temporal.ChronoUnit

import base.SpecBase
import connectors.{CitizenDetailsConnector, TaiConnector}
import models.auditing.AuditEventType.{UpdateWorkingFromHomeFlatRateFailure, UpdateWorkingFromHomeFlatRateSuccess}
import models.requests.DataRequest
import models.{AuditData, ETag, UserAnswers}
import org.mockito.Matchers.{any, eq => eqm}
import org.mockito.Mockito.{times, verify, when}
import org.mockito.{InOrder, Mockito}
import org.scalatest.BeforeAndAfter
import org.scalatest.Matchers._
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.AnyContent
import play.api.test.Helpers._
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// scalastyle:off magic.number
class SubmissionServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfter {

  val TAX_YEAR_1999_START_DATE: LocalDate = LocalDate.of(1999, 4, 6)
  val TAX_YEAR_2019_START_DATE: LocalDate = LocalDate.of(2019, 4, 6)
  val TAX_YEAR_2019_END_DATE: LocalDate = LocalDate.of(2020, 4, 5)
  val TAX_YEAR_2020_START_DATE: LocalDate = LocalDate.of(2020, 4, 6)
  val TAX_YEAR_2020_END_DATE: LocalDate = LocalDate.of(2021, 4, 5)

  val mockCitizenDetailsConnector: CitizenDetailsConnector = mock[CitizenDetailsConnector]
  val mockTaiConnector: TaiConnector = mock[TaiConnector]
  val mockAuditConnector: AuditConnector = mock[AuditConnector]

  val testNino:String = "AA112233A"

  class Setup {
    val serviceUnderTest = new SubmissionService(mockCitizenDetailsConnector, mockTaiConnector, mockAuditConnector, frontendAppConfig)
  }

  before {
    Mockito.reset(mockCitizenDetailsConnector, mockTaiConnector, mockAuditConnector)
  }

  "calculate2019FlatRate" should {

    "calculate the total rate as £4" when {
      val testDate = TAX_YEAR_2019_END_DATE
      s"given a start date of $testDate" in new Setup {
        serviceUnderTest.calculate2019FlatRate(testDate) shouldBe 4
      }
    }

    "calculate the total rate as £4" when {
      val testDate = TAX_YEAR_2019_END_DATE.minus(1, ChronoUnit.DAYS)
      s"given a start date of $testDate" in new Setup {
        serviceUnderTest.calculate2019FlatRate(testDate) shouldBe 4
      }
    }

    "calculate the total rate as £4" when {
      val testDate = TAX_YEAR_2019_END_DATE.minus(7, ChronoUnit.DAYS)
      s"given a start date of $testDate" in new Setup {
        serviceUnderTest.calculate2019FlatRate(testDate) shouldBe 4
      }
    }

    "calculate the total rate as £8" when {
      val testDate = TAX_YEAR_2019_END_DATE.minus(13, ChronoUnit.DAYS)
      s"given a start date of $testDate" in new Setup {
        serviceUnderTest.calculate2019FlatRate(testDate) shouldBe 8
      }
    }

    "calculate the total rate as £212" when {
      "given a start date of 6/4/2019 (the first day of the 2019 tax year)" in new Setup {
        serviceUnderTest.calculate2019FlatRate(TAX_YEAR_2019_START_DATE) shouldBe 212
      }
      "given a start date of 6/4/1999 (the first day of the 1999 tax year)" in new Setup {
        serviceUnderTest.calculate2019FlatRate(TAX_YEAR_1999_START_DATE) shouldBe 212
      }
    }

  }

  "calculate2020FlatRate" should {
    "calculate the total rate as £312" in new Setup {
      serviceUnderTest. calculate2020FlatRate() shouldBe 312
    }
  }


  "submit" when {
    implicit val dataRequest: DataRequest[AnyContent] = DataRequest(fakeRequest,"internalId", UserAnswers("id"), testNino)

    val tests = Seq(TAX_YEAR_2020_START_DATE, TAX_YEAR_2019_START_DATE)
    val etag1 = ETag(100)
    val etag2 = ETag(101)

    for (startDate <- tests) {
      s"a working from home date started on $startDate" should {
        "upsert 2 IABD 59's (happy flow) and audit success" in new Setup {

          when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
            .thenReturn(Future {etag1} )
            .thenReturn(Future {etag2} )

          when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2019), any(), eqm(etag1))(any(), any())).thenReturn(Future.successful(()))
          when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2020), any(), eqm(etag2))(any(), any())).thenReturn(Future.successful(()))

          await(serviceUnderTest.submitExpenses(startDate)).isRight shouldBe true

          val inOrder: InOrder = Mockito.inOrder(mockCitizenDetailsConnector, mockTaiConnector)
          inOrder.verify(mockCitizenDetailsConnector).getETag(eqm(testNino))(any(), any())
          inOrder.verify(mockTaiConnector).postIabdData(eqm(testNino), eqm(2019), any(), eqm(etag1))(any(), any())
          inOrder.verify(mockCitizenDetailsConnector).getETag(eqm(testNino))(any(), any())
          inOrder.verify(mockTaiConnector).postIabdData(eqm(testNino), eqm(2020), any(), eqm(etag2))(any(), any())

          verify(mockAuditConnector, times(1))
            .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateSuccess.toString),any[AuditData]())(any(),any(),any())
        }

        "report errors when 1st ETAG call fails and audit failure " in new Setup {
          when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any())).thenReturn(Future.failed(new RuntimeException))

          await(serviceUnderTest.submitExpenses(startDate)).isLeft shouldBe true

          val inOrder: InOrder = Mockito.inOrder(mockCitizenDetailsConnector, mockTaiConnector)
          inOrder.verify(mockCitizenDetailsConnector).getETag(eqm(testNino))(any(), any())
          inOrder.verify(mockTaiConnector, times(0)).postIabdData(any(), any(), any(), any())(any(), any())
          inOrder.verify(mockCitizenDetailsConnector, times(0)).getETag(any())(any(), any())
          inOrder.verify(mockTaiConnector, times(0)).postIabdData(any(), any(), any(), any())(any(), any())

          verify(mockAuditConnector, times(1))
            .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateFailure.toString),any[AuditData]())(any(),any(),any())
        }

        "report errors when 2nd ETAG call fails and audit failure" in new Setup {
          when(mockCitizenDetailsConnector.getETag(any())(any(), any()))
            .thenReturn(Future {etag1} )
            .thenReturn(Future.failed(new RuntimeException))

          when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2019), any(), eqm(etag1))(any(), any()))
            .thenReturn(Future.successful(()))

          await(serviceUnderTest.submitExpenses(startDate)).isLeft shouldBe true

          val inOrder: InOrder = Mockito.inOrder(mockCitizenDetailsConnector, mockTaiConnector)
          inOrder.verify(mockCitizenDetailsConnector).getETag(eqm(testNino))(any(), any())
          inOrder.verify(mockTaiConnector).postIabdData(eqm(testNino), eqm(2019), any(), eqm(etag1))(any(), any())
          inOrder.verify(mockCitizenDetailsConnector).getETag(eqm(testNino))(any(), any())
          inOrder.verify(mockTaiConnector, times(0)).postIabdData(any(), any(), any(), any())(any(), any())

          verify(mockAuditConnector, times(1))
            .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateFailure.toString),any[AuditData]())(any(),any(),any())
        }

        "report errors when 1st IABD 59 POST fails and audit failure" in new Setup {
          when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
            .thenReturn(Future {etag1})
            .thenReturn(Future {etag2})

          when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2019), any(), eqm(etag1))(any(), any()))
            .thenReturn(Future.failed(UpstreamErrorResponse("SERVICE IS UNAVAILABLE", SERVICE_UNAVAILABLE)))

          await(serviceUnderTest.submitExpenses(startDate)).isLeft shouldBe true

          val inOrder: InOrder = Mockito.inOrder(mockCitizenDetailsConnector, mockTaiConnector)
          inOrder.verify(mockCitizenDetailsConnector).getETag(eqm(testNino))(any(), any())
          inOrder.verify(mockTaiConnector).postIabdData(eqm(testNino), eqm(2019), any(), eqm(etag1))(any(), any())
          inOrder.verify(mockCitizenDetailsConnector, times(0)).getETag(any())(any(), any())
          inOrder.verify(mockTaiConnector, times(0)).postIabdData(any(), any(), any(), any())(any(), any())

          verify(mockAuditConnector, times(1))
            .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateFailure.toString),any[AuditData]())(any(),any(),any())
        }

        "report errors when 2nd IABD 59 POST fails and audit failure" in new Setup {
          when(mockCitizenDetailsConnector.getETag(any())(any(), any()))
            .thenReturn(Future {etag1})
            .thenReturn(Future {etag2})

          when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2019), any(), eqm(etag1))(any(), any()))
            .thenReturn(Future.successful(()))

          when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2020), any(), eqm(etag2))(any(), any()))
            .thenReturn(Future.failed(UpstreamErrorResponse("Not found", 404)))

          await(serviceUnderTest.submitExpenses(startDate)).isLeft shouldBe true

          val inOrder: InOrder = Mockito.inOrder(mockCitizenDetailsConnector, mockTaiConnector)
          inOrder.verify(mockCitizenDetailsConnector).getETag(eqm(testNino))(any(), any())
          inOrder.verify(mockTaiConnector).postIabdData(eqm(testNino), eqm(2019), any(), eqm(etag1))(any(), any())
          inOrder.verify(mockCitizenDetailsConnector).getETag(eqm(testNino))(any(), any())
          inOrder.verify(mockTaiConnector).postIabdData(eqm(testNino), eqm(2020), any(), eqm(etag2))(any(), any())

          verify(mockAuditConnector, times(1))
            .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateFailure.toString),any[AuditData]())(any(),any(),any())
        }
      }
    }
  }
}
