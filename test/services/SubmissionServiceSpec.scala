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

package services

import base.SpecBase
import connectors.{CitizenDetailsConnector, TaiConnector}
import models.TaxYearSelection.{CurrentYear, CurrentYearMinus1, CurrentYearMinus2, CurrentYearMinus3, CurrentYearMinus4}
import models.auditing.AuditEventType.{UpdateWorkingFromHomeFlatRateFailure, UpdateWorkingFromHomeFlatRateSuccess}
import models.requests.DataRequest
import models.{AuditData, ETag, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq => eqm}
import org.mockito.Mockito.{times, verify, when}
import org.mockito.{InOrder, Mockito}
import org.scalatest.BeforeAndAfter
import org.scalatestplus.mockito.MockitoSugar
import pages.SubmittedClaim
import play.api.mvc.AnyContent
import play.api.test.Helpers._
import uk.gov.hmrc.http.{TooManyRequestException, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.RateLimiting

import scala.collection.immutable.ListMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// scalastyle:off magic.number
class SubmissionServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfter {

  val mockCitizenDetailsConnector: CitizenDetailsConnector = mock[CitizenDetailsConnector]
  val mockTaiConnector: TaiConnector = mock[TaiConnector]
  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val mockThrottler: RateLimiting = mock[RateLimiting]
  val mockSessionService: SessionService = mock[SessionService]

  val testNino: String = "AA112233A"

  import org.mockito.ArgumentCaptor

  val userAnswersArgumentCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

  class Setup {
    val serviceUnderTest = new SubmissionService(
      mockCitizenDetailsConnector,
      mockTaiConnector,
      mockAuditConnector,
      mockSessionService,
      frontendAppConfig,
      mockThrottler
    )
  }

  before {
    Mockito.reset(mockCitizenDetailsConnector, mockTaiConnector, mockAuditConnector, mockThrottler, mockSessionService)
  }

  "submit" when {

    implicit val dataRequest: DataRequest[AnyContent] = DataRequest(fakeRequest, "internalId", UserAnswers("id"), testNino)

    val wholeYearClaimAmount = 312
    val perWeekAmount = 6

    val etag1 = ETag(100)
    val etag2 = ETag(101)
    val etag3 = ETag(102)
    val etag4 = ETag(103)
    val etag5 = ETag(104)
    val claimingFor2024 = Seq(CurrentYear)
    val claimingFor2023 = Seq(CurrentYearMinus1)
    val claimingFor2022 = Seq(CurrentYearMinus2)
    val claimingFor2021 = Seq(CurrentYearMinus3)
    val claimingFor2020 = Seq(CurrentYearMinus4)
    val claimingForAll = Seq(CurrentYear, CurrentYearMinus1, CurrentYearMinus2, CurrentYearMinus3, CurrentYearMinus4)
    val claimingFor2022And2021And2020 = Seq(CurrentYearMinus2, CurrentYearMinus3, CurrentYearMinus4)
    val claimingFor2022And2021 = Seq(CurrentYearMinus2, CurrentYearMinus3)
    val claimingFor2021And2020 = Seq(CurrentYearMinus3, CurrentYearMinus4)
    val claimingFor2022And2020 = Seq(CurrentYearMinus2, CurrentYearMinus4)
    val claimingFor2023And2020 = Seq(CurrentYearMinus1, CurrentYearMinus4)

    def verifySessionGotSubmittedState = {
      verify(mockSessionService).set(userAnswersArgumentCaptor.capture())(any())
      userAnswersArgumentCaptor.getValue.get(SubmittedClaim).isDefined mustBe true
    }

    def verifyNoSubmittedStateUpdate = {
      verify(mockSessionService, times(0)).set(any())(any())
    }

    "only claiming for 2022/23 tax year" should {
      "upsert 1 IABD 59, audit success and set submitted status in userAnswers" in new Setup {
        when(mockThrottler.enabled).thenReturn(false)
        when(mockThrottler.withToken(any())).thenCallRealMethod()

        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(Future {
            etag4
          })

        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2022), eqm(wholeYearClaimAmount), eqm(etag4))(any(), any())).thenReturn(Future.successful(()))

        await(serviceUnderTest.submitExpenses(claimingFor2022, ListMap())).isRight mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateSuccess.toString), any[AuditData]())(any(), any(), any())

        verifySessionGotSubmittedState
      }

      "report errors when ETAG call fails and audit failure" in new Setup {
        when(mockThrottler.enabled).thenReturn(false)
        when(mockThrottler.withToken(any())).thenCallRealMethod()
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any())).thenReturn(Future.failed(new RuntimeException))

        await(serviceUnderTest.submitExpenses(claimingFor2022, ListMap())).isLeft mustBe true

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

        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2021), eqm(wholeYearClaimAmount), eqm(etag3))(any(), any())).thenReturn(Future.successful(()))

        await(serviceUnderTest.submitExpenses(claimingFor2021, ListMap())).isRight mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateSuccess.toString), any[AuditData]())(any(), any(), any())

        verifySessionGotSubmittedState
      }

      "report errors when ETAG call fails and audit failure" in new Setup {
        when(mockThrottler.enabled).thenReturn(false)
        when(mockThrottler.withToken(any())).thenCallRealMethod()
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any())).thenReturn(Future.failed(new RuntimeException))

        await(serviceUnderTest.submitExpenses(claimingFor2021, ListMap())).isLeft mustBe true

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

        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2021), eqm(wholeYearClaimAmount), any())(any(), any())).thenReturn(Future.successful(()))
        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2022), eqm(wholeYearClaimAmount), any())(any(), any())).thenReturn(Future.successful(()))

        await(serviceUnderTest.submitExpenses(claimingFor2022And2021, ListMap())).isRight mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateSuccess.toString), any[AuditData]())(any(), any(), any())

        verifySessionGotSubmittedState
      }

      "report errors when ETAG call fails and audit failure" in new Setup {
        when(mockThrottler.enabled).thenReturn(false)
        when(mockThrottler.withToken(any())).thenCallRealMethod()
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any())).thenReturn(Future.failed(new RuntimeException))

        await(serviceUnderTest.submitExpenses(claimingFor2020, ListMap())).isLeft mustBe true

        val inOrder: InOrder = Mockito.inOrder(mockCitizenDetailsConnector, mockTaiConnector)
        inOrder.verify(mockCitizenDetailsConnector).getETag(eqm(testNino))(any(), any())
        inOrder.verify(mockTaiConnector, times(0)).postIabdData(any(), any(), any(), any())(any(), any())

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateFailure.toString), any[AuditData]())(any(), any(), any())

        verifyNoSubmittedStateUpdate
      }
    }

    s"only claiming for 2020 tax year" should {
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

        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2020), eqm(wholeYearClaimAmount), any())(any(), any())).thenReturn(Future.successful(()))

        await(serviceUnderTest.submitExpenses(claimingFor2020, ListMap())).isRight mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateSuccess.toString), any[AuditData]())(any(), any(), any())

        verifySessionGotSubmittedState
      }

      "report errors when ETAG call fails and audit failure" in new Setup {
        when(mockThrottler.enabled).thenReturn(false)
        when(mockThrottler.withToken(any())).thenCallRealMethod()
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any())).thenReturn(Future.failed(new RuntimeException))

        await(serviceUnderTest.submitExpenses(claimingFor2020, ListMap())).isLeft mustBe true

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

        await(serviceUnderTest.submitExpenses(claimingFor2020, ListMap())).isLeft mustBe true

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

    s"claiming for all available tax years, including 3 weeks of 2023 and 3 weeks of 2024" should {
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
          .thenReturn(Future.successful(
            etag5
          ))

        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2020), eqm(wholeYearClaimAmount), any())(any(), any())).thenReturn(Future.successful(()))
        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2021), eqm(wholeYearClaimAmount), any())(any(), any())).thenReturn(Future.successful(()))
        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2022), eqm(wholeYearClaimAmount), any())(any(), any())).thenReturn(Future.successful(()))
        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2023), eqm(3 * perWeekAmount), any())(any(), any())).thenReturn(Future.successful(()))
        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2024), eqm(3 * perWeekAmount), any())(any(), any())).thenReturn(Future.successful(()))

        await(serviceUnderTest.submitExpenses(claimingForAll, ListMap(CurrentYearMinus1 -> 3, CurrentYear -> 3))).isRight mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateSuccess.toString), any[AuditData]())(any(), any(), any())

        verifySessionGotSubmittedState
      }

      "report errors when ETAG call fails and audit failure" in new Setup {
        when(mockThrottler.enabled).thenReturn(false)
        when(mockThrottler.withToken(any())).thenCallRealMethod()
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any())).thenReturn(Future.failed(new RuntimeException))

        await(serviceUnderTest.submitExpenses(claimingFor2020, ListMap())).isLeft mustBe true

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

        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2020), eqm(wholeYearClaimAmount), any())(any(), any()))
          .thenReturn(Future.successful(()))

        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2021), eqm(wholeYearClaimAmount), any())(any(), any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("Not found", 404)))

        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2022), eqm(wholeYearClaimAmount), any())(any(), any()))
          .thenReturn(Future.successful(()))

        await(serviceUnderTest.submitExpenses(claimingFor2022And2021And2020, ListMap())).isLeft mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateFailure.toString), any[AuditData]())(any(), any(), any())

        verifyNoSubmittedStateUpdate
      }
    }

    s"claiming for 2021 and 2020" should {
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

        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2020), any(), any())(any(), any())).thenReturn(Future.successful(()))
        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2021), any(), any())(any(), any())).thenReturn(Future.successful(()))

        await(serviceUnderTest.submitExpenses(claimingFor2021And2020, ListMap())).isRight mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateSuccess.toString), any[AuditData]())(any(), any(), any())

        verifySessionGotSubmittedState
      }

      "report errors when ETAG call fails and audit failure" in new Setup {
        when(mockThrottler.enabled).thenReturn(false)
        when(mockThrottler.withToken(any())).thenCallRealMethod()
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any())).thenReturn(Future.failed(new RuntimeException))

        await(serviceUnderTest.submitExpenses(claimingFor2021And2020, ListMap())).isLeft mustBe true

        val inOrder: InOrder = Mockito.inOrder(mockCitizenDetailsConnector, mockTaiConnector)
        inOrder.verify(mockCitizenDetailsConnector).getETag(eqm(testNino))(any(), any())
        inOrder.verify(mockTaiConnector, times(0)).postIabdData(any(), any(), any(), any())(any(), any())

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateFailure.toString), any[AuditData]())(any(), any(), any())

        verifyNoSubmittedStateUpdate
      }
    }

    s"claiming for 2022 and 2020" should {
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

        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2020), any(), any())(any(), any())).thenReturn(Future.successful(()))
        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2022), any(), any())(any(), any())).thenReturn(Future.successful(()))

        await(serviceUnderTest.submitExpenses(claimingFor2022And2020, ListMap())).isRight mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateSuccess.toString), any[AuditData]())(any(), any(), any())

        verifySessionGotSubmittedState
      }

      "report errors when ETAG call fails and audit failure" in new Setup {
        when(mockThrottler.enabled).thenReturn(false)
        when(mockThrottler.withToken(any())).thenCallRealMethod()
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any())).thenReturn(Future.failed(new RuntimeException))

        await(serviceUnderTest.submitExpenses(claimingFor2022And2020, ListMap())).isLeft mustBe true

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

        await(serviceUnderTest.submitExpenses(claimingFor2021And2020, ListMap())).isLeft mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateFailure.toString), any[AuditData]())(any(), any(), any())

        verifyNoSubmittedStateUpdate
      }

    }

    s"only claiming for 3 weeks of the 2023/24 tax year" should {
      "upsert 1 IABD 59, audit success and set submitted status in userAnswers" in new Setup {
        when(mockThrottler.enabled).thenReturn(false)
        when(mockThrottler.withToken(any())).thenCallRealMethod()

        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(Future.successful(
            etag1
          ))

        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2023), any(), any())(any(), any())).thenReturn(Future.successful(()))

        await(serviceUnderTest.submitExpenses(claimingFor2023, ListMap(CurrentYearMinus1 -> 3))).isRight mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateSuccess.toString), any[AuditData]())(any(), any(), any())

        verifySessionGotSubmittedState
      }
    }

    s"only claiming for 3 weeks of the 2024/25 tax year" should {
      "upsert 1 IABD 59, audit success and set submitted status in userAnswers" in new Setup {
        when(mockThrottler.enabled).thenReturn(false)
        when(mockThrottler.withToken(any())).thenCallRealMethod()

        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(Future.successful(
            etag1
          ))

        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2024), any(), any())(any(), any())).thenReturn(Future.successful(()))

        await(serviceUnderTest.submitExpenses(claimingFor2024, ListMap(CurrentYear -> 3))).isRight mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateSuccess.toString), any[AuditData]())(any(), any(), any())

        verifySessionGotSubmittedState
      }
    }

      s"claiming for 3 weeks of 2023 and 2020 year" should {
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
            etag5
          ))

        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2020), eqm(wholeYearClaimAmount), any())(any(), any())).thenReturn(Future.successful(()))
        when(mockTaiConnector.postIabdData(eqm(testNino), eqm(2023), any(), any())(any(), any())).thenReturn(Future.successful(()))

        await(serviceUnderTest.submitExpenses(claimingFor2023And2020, ListMap(CurrentYearMinus1 -> 3))).isRight mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateSuccess.toString), any[AuditData]())(any(), any(), any())

        verifySessionGotSubmittedState
      }

      "report errors when ETAG call fails and audit failure" in new Setup {
        when(mockThrottler.enabled).thenReturn(false)
        when(mockThrottler.withToken(any())).thenCallRealMethod()
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any())).thenReturn(Future.failed(new RuntimeException))

        await(serviceUnderTest.submitExpenses(claimingFor2023And2020, ListMap(CurrentYearMinus1 -> 3))).isLeft mustBe true

        val inOrder: InOrder = Mockito.inOrder(mockCitizenDetailsConnector, mockTaiConnector)
        inOrder.verify(mockCitizenDetailsConnector).getETag(eqm(testNino))(any(), any())
        inOrder.verify(mockTaiConnector, times(0)).postIabdData(any(), any(), any(), any())(any(), any())

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
          await(serviceUnderTest.submitExpenses(claimingForAll, ListMap(CurrentYear -> 5, CurrentYearMinus1 -> 5)))
        }
      }

    }
  }
}
