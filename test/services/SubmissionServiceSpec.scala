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
import org.scalatestplus.mockito.MockitoSugar
import pages.SubmittedClaim
import play.api.mvc.AnyContent
import play.api.test.Helpers._
import repositories.SessionRepository
import uk.gov.hmrc.http.{TooManyRequestException, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.RateLimiting

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// scalastyle:off magic.number
class SubmissionServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfter {

  val TAX_YEAR_1999_START_DATE: LocalDate = LocalDate.of(1999, 4, 6)
  val TAX_YEAR_2019_START_DATE: LocalDate = LocalDate.of(2019, 4, 6)
  val TAX_YEAR_2019_END_DATE: LocalDate = LocalDate.of(2020, 4, 5)
  val YEAR_2020_START_DATE: LocalDate = LocalDate.of(2020, 1, 1)
  val TAX_YEAR_2020_START_DATE: LocalDate = LocalDate.of(2020, 4, 6)
  val TAX_YEAR_2020_END_DATE: LocalDate = LocalDate.of(2021, 4, 5)
  val TAX_YEAR_2021_START_DATE: LocalDate = LocalDate.of(2021, 4, 6)

  val mockCitizenDetailsConnector: CitizenDetailsConnector = mock[CitizenDetailsConnector]
  val mockTaiConnector: TaiConnector = mock[TaiConnector]
  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val mockThrottler: RateLimiting = mock[RateLimiting]
  val mockSessionRepository: SessionRepository = mock[SessionRepository]

  val testNino: String = "AA112233A"

  import org.mockito.ArgumentCaptor

  val userAnswersArgumentCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

  class Setup {
    val serviceUnderTest = new SubmissionService(mockCitizenDetailsConnector, mockTaiConnector, mockAuditConnector, mockSessionRepository, frontendAppConfig, mockThrottler)
  }

  before {
    Mockito.reset(mockCitizenDetailsConnector, mockTaiConnector, mockAuditConnector, mockThrottler, mockSessionRepository)
  }

  "calculate2019FlatRate" should {

    "calculate the total rate as £4" when {
      val testDate = TAX_YEAR_2019_END_DATE
      s"given a start date of $testDate" in new Setup {
        serviceUnderTest.calculate2019FlatRate(testDate) mustBe 4
      }
    }

    "calculate the total rate as £4" when {
      val testDate = TAX_YEAR_2019_END_DATE.minus(1, ChronoUnit.DAYS)
      s"given a start date of $testDate" in new Setup {
        serviceUnderTest.calculate2019FlatRate(testDate) mustBe 4
      }
    }

    "calculate the total rate as £4" when {
      val testDate = TAX_YEAR_2019_END_DATE.minus(7, ChronoUnit.DAYS)
      s"given a start date of $testDate" in new Setup {
        serviceUnderTest.calculate2019FlatRate(testDate) mustBe 4
      }
    }

    "calculate the total rate as £8" when {
      val testDate = TAX_YEAR_2019_END_DATE.minus(13, ChronoUnit.DAYS)
      s"given a start date of $testDate" in new Setup {
        serviceUnderTest.calculate2019FlatRate(testDate) mustBe 8
      }
    }

    "calculate the total rate as £56" when {
      "given a start date of 1/1/2020 (latest 2019 claiming date)" in new Setup {
        serviceUnderTest.calculate2019FlatRate(YEAR_2020_START_DATE) mustBe 56
      }

      "given a start date of 6/4/2019 (the start of the 2019 tax year)" in new Setup {
        serviceUnderTest.calculate2019FlatRate(TAX_YEAR_2019_START_DATE) mustBe 56
      }

      "given a start date of 6/4/1999 (the first day of the 1999 tax year)" in new Setup {
        serviceUnderTest.calculate2019FlatRate(TAX_YEAR_1999_START_DATE) mustBe 56
      }
    }

  }

  "calculate2020FlatRate" should {
    "calculate the total rate as £312" in new Setup {
      serviceUnderTest.calculate2020FlatRate() mustBe 312
    }
  }

  "calculate2021FlatRate" should {
    "calculate the total rate as £312 for a 2021 claim" in new Setup {
      serviceUnderTest.calculate2021FlatRate() mustBe 312
    }
  }

  "calculate2022FlatRate" should {
    "calculate the total rate as £312 for the 2022 claim" in new Setup {
      serviceUnderTest.calculate2022FlatRate() mustBe 312
    }
  }

  "submit" when {

    implicit val dataRequest: DataRequest[AnyContent] = DataRequest(fakeRequest, "internalId", UserAnswers("id"), testNino, None)

    val etag1 = ETag(100)
    val etag2 = ETag(101)
    val etag3 = ETag(102)
    val etag4 = ETag(103)
    val claimingFor2022 = List("option2")
    val claimingFor2021 = List("option3")
    val claimingForPrev = List("option4")
    val claimingForAll = List("option1", "option2", "option3", "option4")
    val claimingFor2022And2021 = List("option2", "option3")
    val claimingFor2021AndPrev = List("option3", "option4")
    val claimingFor2022AndPrev = List("option2", "option4")

    def verifySessionGotSubmittedState = {
      verify(mockSessionRepository).set(userAnswersArgumentCaptor.capture())
      userAnswersArgumentCaptor.getValue.get(SubmittedClaim).isDefined mustBe true
    }

    def verifyNoSubmittedStateUpdate = {
      verify(mockSessionRepository, times(0)).set(any())
    }

    "only claiming for 2022/23 tax year" should {
      "upsert 1 IABD 59, audit success and set submitted status in userAnswers" in new Setup {
        when(mockThrottler.enabled).thenReturn(false)
        when(mockThrottler.withToken(any())).thenCallRealMethod()

        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(Future {
            etag4
          })

        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2022), any(), eqm(etag4))(any(), any())).thenReturn(Future.successful(()))

        await(serviceUnderTest.submitExpenses(None, claimingFor2022, None)).isRight mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateSuccess.toString), any[AuditData]())(any(), any(), any())

        verifySessionGotSubmittedState
      }

      "report errors when ETAG call fails and audit failure" in new Setup {
        when(mockThrottler.enabled).thenReturn(false)
        when(mockThrottler.withToken(any())).thenCallRealMethod()
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any())).thenReturn(Future.failed(new RuntimeException))

        await(serviceUnderTest.submitExpenses(None, claimingFor2022, None)).isLeft mustBe true

        val inOrder: InOrder = Mockito.inOrder(mockCitizenDetailsConnector, mockTaiConnector)
        inOrder.verify(mockCitizenDetailsConnector).getETag(eqm(testNino))(any(), any())
        inOrder.verify(mockTaiConnector, times(0)).postIabdData(any(), any(), any(), any())(any(), any())

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateFailure.toString), any[AuditData]())(any(), any(), any())

        verifyNoSubmittedStateUpdate
      }
    }

    "only claiming for 2021/22 tax year" should {
      "upsert 1 IABD 59, audit success and set submitted status in userAnswers" in new Setup {
        when(mockThrottler.enabled).thenReturn(false)
        when(mockThrottler.withToken(any())).thenCallRealMethod()

        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(Future {
            etag3
          })

        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2021), any(), eqm(etag3))(any(), any())).thenReturn(Future.successful(()))

        await(serviceUnderTest.submitExpenses(None, claimingFor2021, None)).isRight mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateSuccess.toString), any[AuditData]())(any(), any(), any())

        verifySessionGotSubmittedState
      }

      "report errors when ETAG call fails and audit failure" in new Setup {
        when(mockThrottler.enabled).thenReturn(false)
        when(mockThrottler.withToken(any())).thenCallRealMethod()
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any())).thenReturn(Future.failed(new RuntimeException))

        await(serviceUnderTest.submitExpenses(None, claimingFor2021, None)).isLeft mustBe true

        val inOrder: InOrder = Mockito.inOrder(mockCitizenDetailsConnector, mockTaiConnector)
        inOrder.verify(mockCitizenDetailsConnector).getETag(eqm(testNino))(any(), any())
        inOrder.verify(mockTaiConnector, times(0)).postIabdData(any(), any(), any(), any())(any(), any())

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateFailure.toString), any[AuditData]())(any(), any(), any())

        verifyNoSubmittedStateUpdate
      }
    }



    "claiming for 2022 and 2021" should {
      "upsert 2 IABD 59, audit success and set submitted status in userAnswers" in new Setup {
        when(mockThrottler.enabled).thenReturn(false)
        when(mockThrottler.withToken(any())).thenCallRealMethod()

        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(Future.successful(
            etag3
          ))
          .thenReturn(Future.successful(
            etag4
          ))

        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2021), any(), any())(any(), any())).thenReturn(Future.successful(()))
        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2022), any(), any())(any(), any())).thenReturn(Future.successful(()))

        await(serviceUnderTest.submitExpenses(None, claimingFor2022And2021, None)).isRight mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateSuccess.toString), any[AuditData]())(any(), any(), any())

        verifySessionGotSubmittedState
      }

      "report errors when ETAG call fails and audit failure" in new Setup {
        when(mockThrottler.enabled).thenReturn(false)
        when(mockThrottler.withToken(any())).thenCallRealMethod()
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any())).thenReturn(Future.failed(new RuntimeException))

        await(serviceUnderTest.submitExpenses(Some(YEAR_2020_START_DATE), claimingForPrev, None)).isLeft mustBe true

        val inOrder: InOrder = Mockito.inOrder(mockCitizenDetailsConnector, mockTaiConnector)
        inOrder.verify(mockCitizenDetailsConnector).getETag(eqm(testNino))(any(), any())
        inOrder.verify(mockTaiConnector, times(0)).postIabdData(any(), any(), any(), any())(any(), any())

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateFailure.toString), any[AuditData]())(any(), any(), any())

        verifyNoSubmittedStateUpdate
      }
    }

    s"only claiming for previous tax years with start date of $YEAR_2020_START_DATE" should {
      "upsert 2 IABD 59, audit success and set submitted status in userAnswers" in new Setup {
        when(mockThrottler.enabled).thenReturn(false)
        when(mockThrottler.withToken(any())).thenCallRealMethod()

        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(Future.successful(
            etag1
            ))
          .thenReturn(Future.successful(
            etag2
          ))

        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2019), any(), any())(any(), any())).thenReturn(Future.successful(()))
        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2020), any(), any())(any(), any())).thenReturn(Future.successful(()))

        await(serviceUnderTest.submitExpenses(Some(YEAR_2020_START_DATE), claimingForPrev, None)).isRight mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateSuccess.toString), any[AuditData]())(any(), any(), any())

        verifySessionGotSubmittedState
      }

      "report errors when ETAG call fails and audit failure" in new Setup {
        when(mockThrottler.enabled).thenReturn(false)
        when(mockThrottler.withToken(any())).thenCallRealMethod()
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any())).thenReturn(Future.failed(new RuntimeException))

        await(serviceUnderTest.submitExpenses(Some(YEAR_2020_START_DATE), claimingForPrev, None)).isLeft mustBe true

        val inOrder: InOrder = Mockito.inOrder(mockCitizenDetailsConnector, mockTaiConnector)
        inOrder.verify(mockCitizenDetailsConnector).getETag(eqm(testNino))(any(), any())
        inOrder.verify(mockTaiConnector, times(0)).postIabdData(any(), any(), any(), any())(any(), any())

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateFailure.toString), any[AuditData]())(any(), any(), any())

        verifyNoSubmittedStateUpdate
      }

      "report errors when 1st IABD 59 POST fails and audit failure" in new Setup {
        when(mockThrottler.enabled).thenReturn(false)
        when(mockThrottler.withToken(any())).thenCallRealMethod()

        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(Future {
            etag2
          })
          .thenReturn(Future {
            etag3
          })

        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2020), any(), eqm(etag2))(any(), any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("SERVICE IS UNAVAILABLE", SERVICE_UNAVAILABLE)))

        await(serviceUnderTest.submitExpenses(Some(TAX_YEAR_2020_START_DATE), claimingForPrev, None)).isLeft mustBe true

        val inOrder: InOrder = Mockito.inOrder(mockCitizenDetailsConnector, mockTaiConnector)
        inOrder.verify(mockCitizenDetailsConnector).getETag(eqm(testNino))(any(), any())
        inOrder.verify(mockTaiConnector).postIabdData(eqm(testNino), eqm(2020), any(), eqm(etag2))(any(), any())
        inOrder.verify(mockCitizenDetailsConnector, times(0)).getETag(any())(any(), any())
        inOrder.verify(mockTaiConnector, times(0)).postIabdData(any(), any(), any(), any())(any(), any())

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateFailure.toString), any[AuditData]())(any(), any(), any())

        verifyNoSubmittedStateUpdate
      }
    }

    s"claiming for all available tax years with a start date of $YEAR_2020_START_DATE" should {
      "upsert 4 IABD 59, audit success and set submitted status in userAnswers" in new Setup {
        when(mockThrottler.enabled).thenReturn(false)
        when(mockThrottler.withToken(any())).thenCallRealMethod()

        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(Future.successful(
            etag1
          ))
          .thenReturn(Future.successful(
            etag2
          ))
          .thenReturn(Future.successful(
            etag3
          ))
          .thenReturn(Future.successful(
            etag4
          ))

        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2019), any(), any())(any(), any())).thenReturn(Future.successful(()))
        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2020), any(), any())(any(), any())).thenReturn(Future.successful(()))
        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2021), any(), any())(any(), any())).thenReturn(Future.successful(()))
        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2022), any(), any())(any(), any())).thenReturn(Future.successful(()))

        await(serviceUnderTest.submitExpenses(Some(YEAR_2020_START_DATE), claimingForAll, None)).isRight mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateSuccess.toString), any[AuditData]())(any(), any(), any())

        verifySessionGotSubmittedState
      }

      "report errors when ETAG call fails and audit failure" in new Setup {
        when(mockThrottler.enabled).thenReturn(false)
        when(mockThrottler.withToken(any())).thenCallRealMethod()
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any())).thenReturn(Future.failed(new RuntimeException))

        await(serviceUnderTest.submitExpenses(Some(YEAR_2020_START_DATE), claimingForPrev, None)).isLeft mustBe true

        val inOrder: InOrder = Mockito.inOrder(mockCitizenDetailsConnector, mockTaiConnector)
        inOrder.verify(mockCitizenDetailsConnector).getETag(eqm(testNino))(any(), any())
        inOrder.verify(mockTaiConnector, times(0)).postIabdData(any(), any(), any(), any())(any(), any())

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateFailure.toString), any[AuditData]())(any(), any(), any())

        verifyNoSubmittedStateUpdate
      }

      "report errors when 2nd IABD 59 POST fails and audit failure" in new Setup {
        when(mockThrottler.enabled).thenReturn(false)
        when(mockThrottler.withToken(any())).thenCallRealMethod()
        when(mockCitizenDetailsConnector.getETag(any())(any(), any()))
          .thenReturn(Future.successful(
            etag1
          ))
          .thenReturn(Future.successful(
            etag2
            ))
          .thenReturn(Future.successful(
            etag3
          ))
          .thenReturn(Future.successful(
            etag4
          ))

        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2019), any(), any())(any(), any()))
          .thenReturn(Future.successful(()))

        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2020), any(), any())(any(), any()))
          .thenReturn(Future.successful(()))

        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2021), any(), any())(any(), any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("Not found", 404)))

        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2022), any(), any())(any(), any()))
          .thenReturn(Future.successful(()))

        await(serviceUnderTest.submitExpenses(Some(TAX_YEAR_2020_START_DATE), claimingForAll, None)).isLeft mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateFailure.toString), any[AuditData]())(any(), any(), any())

        verifyNoSubmittedStateUpdate
      }
    }

    s"claiming for 2021 and previous years with a start date of $YEAR_2020_START_DATE" should {
      "upsert 3 IABD 59, audit success and set submitted status in userAnswers" in new Setup {
        when(mockThrottler.enabled).thenReturn(false)
        when(mockThrottler.withToken(any())).thenCallRealMethod()

        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(Future.successful(
            etag1
          ))
          .thenReturn(Future.successful(
            etag2
          ))
          .thenReturn(Future.successful(
            etag3
          ))

        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2019), any(), any())(any(), any())).thenReturn(Future.successful(()))
        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2020), any(), any())(any(), any())).thenReturn(Future.successful(()))
        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2021), any(), any())(any(), any())).thenReturn(Future.successful(()))

        await(serviceUnderTest.submitExpenses(Some(YEAR_2020_START_DATE), claimingFor2021AndPrev, None)).isRight mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateSuccess.toString), any[AuditData]())(any(), any(), any())

        verifySessionGotSubmittedState
      }

      "report errors when ETAG call fails and audit failure" in new Setup {
        when(mockThrottler.enabled).thenReturn(false)
        when(mockThrottler.withToken(any())).thenCallRealMethod()
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any())).thenReturn(Future.failed(new RuntimeException))

        await(serviceUnderTest.submitExpenses(Some(YEAR_2020_START_DATE), claimingFor2021AndPrev, None)).isLeft mustBe true

        val inOrder: InOrder = Mockito.inOrder(mockCitizenDetailsConnector, mockTaiConnector)
        inOrder.verify(mockCitizenDetailsConnector).getETag(eqm(testNino))(any(), any())
        inOrder.verify(mockTaiConnector, times(0)).postIabdData(any(), any(), any(), any())(any(), any())

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateFailure.toString), any[AuditData]())(any(), any(), any())

        verifyNoSubmittedStateUpdate
      }
    }

    s"claiming for 2022 and previous years with a start date of $YEAR_2020_START_DATE" should {
      "upsert 3 IABD 59, audit success and set submitted status in userAnswers" in new Setup {
        when(mockThrottler.enabled).thenReturn(false)
        when(mockThrottler.withToken(any())).thenCallRealMethod()

        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(Future.successful(
            etag1
          ))
          .thenReturn(Future.successful(
            etag2
          ))
          .thenReturn(Future.successful(
            etag4
          ))

        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2019), any(), any())(any(), any())).thenReturn(Future.successful(()))
        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2020), any(), any())(any(), any())).thenReturn(Future.successful(()))
        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2022), any(), any())(any(), any())).thenReturn(Future.successful(()))

        await(serviceUnderTest.submitExpenses(Some(YEAR_2020_START_DATE), claimingFor2022AndPrev, None)).isRight mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateSuccess.toString), any[AuditData]())(any(), any(), any())

        verifySessionGotSubmittedState
      }

      "report errors when ETAG call fails and audit failure" in new Setup {
        when(mockThrottler.enabled).thenReturn(false)
        when(mockThrottler.withToken(any())).thenCallRealMethod()
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any())).thenReturn(Future.failed(new RuntimeException))

        await(serviceUnderTest.submitExpenses(Some(YEAR_2020_START_DATE), claimingFor2022AndPrev, None)).isLeft mustBe true

        val inOrder: InOrder = Mockito.inOrder(mockCitizenDetailsConnector, mockTaiConnector)
        inOrder.verify(mockCitizenDetailsConnector).getETag(eqm(testNino))(any(), any())
        inOrder.verify(mockTaiConnector, times(0)).postIabdData(any(), any(), any(), any())(any(), any())

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateFailure.toString), any[AuditData]())(any(), any(), any())

        verifyNoSubmittedStateUpdate
      }

      "report errors when 3rd ETAG call fails and audit failure" in new Setup {
        when(mockThrottler.enabled).thenReturn(false)
        when(mockThrottler.withToken(any())).thenCallRealMethod()
        when(mockCitizenDetailsConnector.getETag(any())(any(), any()))
          .thenReturn(Future {
            etag2
          })
          .thenReturn(Future.failed(new RuntimeException))

        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2020), any(), eqm(etag2))(any(), any()))
          .thenReturn(Future.successful(()))

        await(serviceUnderTest.submitExpenses(Some(TAX_YEAR_2020_START_DATE), claimingFor2021AndPrev, None)).isLeft mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateFailure.toString), any[AuditData]())(any(), any(), any())

        verifyNoSubmittedStateUpdate
      }

    }

    "rate limit has been reached (no tokens in bucket)" should {
      "throw a TooManyRequestException" in new Setup {

        when(mockThrottler.enabled).thenReturn(true)
        when(mockThrottler.hasAToken).thenReturn(false)
        when(mockThrottler.withToken(any())).thenCallRealMethod()

        intercept[TooManyRequestException] {
          await(serviceUnderTest.submitExpenses(Some(TAX_YEAR_2020_START_DATE), claimingForAll, Some(5)))
        }
      }

    }
  }
}
