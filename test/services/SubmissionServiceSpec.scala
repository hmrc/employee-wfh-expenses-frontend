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
import models.TaxYearSelection.{
  CurrentYear,
  CurrentYearMinus1,
  CurrentYearMinus2,
  CurrentYearMinus3,
  CurrentYearMinus4,
  wholeYearClaims
}
import models.auditing.AuditEventType.{UpdateWorkingFromHomeFlatRateFailure, UpdateWorkingFromHomeFlatRateSuccess}
import models.requests.DataRequest
import models.{AuditData, ETag, TaxYearSelection, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq => eqm}
import org.mockito.Mockito.{times, verify, when}
import org.mockito.{InOrder, Mockito}
import org.scalatest.BeforeAndAfter
import org.scalatestplus.mockito.MockitoSugar
import pages.SubmittedClaim
import play.api.mvc.AnyContent
import play.api.test.Helpers._
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.collection.immutable.ListMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// scalastyle:off magic.number
class SubmissionServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfter {

  val mockCitizenDetailsConnector: CitizenDetailsConnector = mock[CitizenDetailsConnector]
  val mockTaiConnector: TaiConnector                       = mock[TaiConnector]
  val mockAuditConnector: AuditConnector                   = mock[AuditConnector]
  val mockSessionService: SessionService                   = mock[SessionService]

  val testNino: String = "AA112233A"

  import org.mockito.ArgumentCaptor

  val userAnswersArgumentCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

  class Setup {

    val serviceUnderTest = new SubmissionService(
      mockCitizenDetailsConnector,
      mockTaiConnector,
      mockAuditConnector,
      mockSessionService,
      frontendAppConfig
    )

  }

  before {
    Mockito.reset(mockCitizenDetailsConnector, mockTaiConnector, mockAuditConnector, mockSessionService)
  }

  "submit" when {

    implicit val dataRequest: DataRequest[AnyContent] =
      DataRequest(fakeRequest, "internalId", UserAnswers("id"), testNino)

    val wholeYearClaimAmount                  = 312
    def perWeekAmount(year: TaxYearSelection) = frontendAppConfig.taxReliefPerWeek(year)

    val etag1                    = ETag(100)
    val etag2                    = ETag(101)
    val etag3                    = ETag(102)
    val etag4                    = ETag(103)
    val etag5                    = ETag(104)
    val claimingForCurrent       = Seq(CurrentYear)
    val claimingForCurrentMinus1 = Seq(CurrentYearMinus1)
    val claimingForCurrentMinus2 = Seq(CurrentYearMinus2)
    val claimingForCurrentMinus3 = Seq(CurrentYearMinus3)
    val claimingForCurrentMinus4 = Seq(CurrentYearMinus4)
    val claimingForAll = Seq(CurrentYear, CurrentYearMinus1, CurrentYearMinus2, CurrentYearMinus3, CurrentYearMinus4)

    def verifySessionGotSubmittedState = {
      verify(mockSessionService).set(userAnswersArgumentCaptor.capture())(any())
      userAnswersArgumentCaptor.getValue.get(SubmittedClaim).isDefined mustBe true
    }

    def verifyNoSubmittedStateUpdate =
      verify(mockSessionService, times(0)).set(any())(any())

    def createMockTaiConnector(taxYearSelection: TaxYearSelection, etag: Option[ETag] = None) =
      when(
        mockTaiConnector.postIabdData(
          eqm(testNino),
          eqm(taxYearSelection.toTaxYear.startYear),
          if (wholeYearClaims.contains(taxYearSelection)) eqm(wholeYearClaimAmount)
          else eqm(3 * perWeekAmount(taxYearSelection)),
          if (etag.nonEmpty) eqm(etag.get) else any()
        )(any(), any())
      ).thenReturn(Future.successful(()))

    def createListMap(taxYearSelection: TaxYearSelection) =
      if (wholeYearClaims.contains(taxYearSelection)) ListMap().asInstanceOf[ListMap[TaxYearSelection, Int]]
      else ListMap(taxYearSelection -> 3)

    "only claiming for CTY" should {
      "upsert 1 IABD 59, audit success and set submitted status in userAnswers" in new Setup {
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(Future {
            etag4
          })

        createMockTaiConnector(CurrentYear, Some(etag4))

        await(serviceUnderTest.submitExpenses(claimingForCurrent, createListMap(CurrentYear))).isRight mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateSuccess.toString), any[AuditData]())(any(), any(), any())

        verifySessionGotSubmittedState
      }

      "report errors when ETAG call fails and audit failure" in new Setup {
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(Future.failed(new RuntimeException))

        await(serviceUnderTest.submitExpenses(claimingForCurrent, createListMap(CurrentYear))).isLeft mustBe true

        val inOrder: InOrder = Mockito.inOrder(mockCitizenDetailsConnector, mockTaiConnector)
        inOrder.verify(mockCitizenDetailsConnector).getETag(eqm(testNino))(any(), any())
        inOrder.verify(mockTaiConnector, times(0)).postIabdData(any(), any(), any(), any())(any(), any())

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateFailure.toString), any[AuditData]())(any(), any(), any())

        verifyNoSubmittedStateUpdate
      }
    }

    "only claiming for CTY-1" should {
      "upsert 1 IABD 59, audit success and set submitted status in userAnswers" in new Setup {
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(Future {
            etag3
          })

        createMockTaiConnector(CurrentYearMinus1, Some(etag3))
        await(
          serviceUnderTest.submitExpenses(claimingForCurrentMinus1, createListMap(CurrentYearMinus1))
        ).isRight mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateSuccess.toString), any[AuditData]())(any(), any(), any())

        verifySessionGotSubmittedState
      }

      "report errors when ETAG call fails and audit failure" in new Setup {
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(Future.failed(new RuntimeException))

        await(
          serviceUnderTest.submitExpenses(claimingForCurrentMinus1, createListMap(CurrentYearMinus1))
        ).isLeft mustBe true

        val inOrder: InOrder = Mockito.inOrder(mockCitizenDetailsConnector, mockTaiConnector)
        inOrder.verify(mockCitizenDetailsConnector).getETag(eqm(testNino))(any(), any())
        inOrder.verify(mockTaiConnector, times(0)).postIabdData(any(), any(), any(), any())(any(), any())

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateFailure.toString), any[AuditData]())(any(), any(), any())

        verifyNoSubmittedStateUpdate
      }
    }

    "only claiming claiming for CTY-2" should {
      "upsert 1 IABD 59, audit success and set submitted status in userAnswers" in new Setup {
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(Future {
            etag3
          })

        createMockTaiConnector(CurrentYearMinus2, Some(etag3))
        await(
          serviceUnderTest.submitExpenses(claimingForCurrentMinus2, createListMap(CurrentYearMinus2))
        ).isRight mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateSuccess.toString), any[AuditData]())(any(), any(), any())

        verifySessionGotSubmittedState
      }

      "report errors when ETAG call fails and audit failure" in new Setup {
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(Future.failed(new RuntimeException))

        await(
          serviceUnderTest.submitExpenses(claimingForCurrentMinus2, createListMap(CurrentYearMinus2))
        ).isLeft mustBe true

        val inOrder: InOrder = Mockito.inOrder(mockCitizenDetailsConnector, mockTaiConnector)
        inOrder.verify(mockCitizenDetailsConnector).getETag(eqm(testNino))(any(), any())
        inOrder.verify(mockTaiConnector, times(0)).postIabdData(any(), any(), any(), any())(any(), any())

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateFailure.toString), any[AuditData]())(any(), any(), any())

        verifyNoSubmittedStateUpdate
      }
    }

    s"only claiming for CTY-3" should {
      "upsert 1 IABD 59, audit success and set submitted status in userAnswers" in new Setup {
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(
            Future.successful(
              etag1
            )
          )
          .thenReturn(
            Future.successful(
              etag2
            )
          )

        createMockTaiConnector(CurrentYearMinus3)

        await(
          serviceUnderTest.submitExpenses(claimingForCurrentMinus3, createListMap(CurrentYearMinus3))
        ).isRight mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateSuccess.toString), any[AuditData]())(any(), any(), any())

        verifySessionGotSubmittedState
      }

      "report errors when ETAG call fails and audit failure" in new Setup {
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(Future.failed(new RuntimeException))

        await(
          serviceUnderTest.submitExpenses(claimingForCurrentMinus3, createListMap(CurrentYearMinus3))
        ).isLeft mustBe true

        val inOrder: InOrder = Mockito.inOrder(mockCitizenDetailsConnector, mockTaiConnector)
        inOrder.verify(mockCitizenDetailsConnector).getETag(eqm(testNino))(any(), any())
        inOrder.verify(mockTaiConnector, times(0)).postIabdData(any(), any(), any(), any())(any(), any())

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateFailure.toString), any[AuditData]())(any(), any(), any())

        verifyNoSubmittedStateUpdate
      }

      "report errors when 1st IABD 59 POST fails and audit failure" in new Setup {
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(Future {
            etag2
          })
          .thenReturn(Future {
            etag3
          })

        when(
          mockTaiConnector.postIabdData(eqm(testNino), eqm(CurrentYearMinus3.toTaxYear.startYear), any(), eqm(etag2))(
            any(),
            any()
          )
        )
          .thenReturn(Future.failed(UpstreamErrorResponse("SERVICE IS UNAVAILABLE", SERVICE_UNAVAILABLE)))

        await(
          serviceUnderTest.submitExpenses(claimingForCurrentMinus3, createListMap(CurrentYearMinus3))
        ).isLeft mustBe true

        val inOrder: InOrder = Mockito.inOrder(mockCitizenDetailsConnector, mockTaiConnector)
        inOrder.verify(mockCitizenDetailsConnector).getETag(eqm(testNino))(any(), any())
        inOrder
          .verify(mockTaiConnector)
          .postIabdData(eqm(testNino), eqm(CurrentYearMinus3.toTaxYear.startYear), any(), eqm(etag2))(any(), any())
        inOrder.verify(mockCitizenDetailsConnector, times(0)).getETag(any())(any(), any())
        inOrder.verify(mockTaiConnector, times(0)).postIabdData(any(), any(), any(), any())(any(), any())

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateFailure.toString), any[AuditData]())(any(), any(), any())

        verifyNoSubmittedStateUpdate
      }
    }

    s"only claiming for CTY-4" should {
      "upsert 1 IABD 59, audit success and set submitted status in userAnswers" in new Setup {
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(
            Future.successful(
              etag1
            )
          )
          .thenReturn(
            Future.successful(
              etag2
            )
          )

        createMockTaiConnector(CurrentYearMinus4)

        await(
          serviceUnderTest.submitExpenses(claimingForCurrentMinus4, createListMap(CurrentYearMinus4))
        ).isRight mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateSuccess.toString), any[AuditData]())(any(), any(), any())

        verifySessionGotSubmittedState
      }

      "report errors when ETAG call fails and audit failure" in new Setup {
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(Future.failed(new RuntimeException))

        await(
          serviceUnderTest.submitExpenses(claimingForCurrentMinus4, createListMap(CurrentYearMinus4))
        ).isLeft mustBe true

        val inOrder: InOrder = Mockito.inOrder(mockCitizenDetailsConnector, mockTaiConnector)
        inOrder.verify(mockCitizenDetailsConnector).getETag(eqm(testNino))(any(), any())
        inOrder.verify(mockTaiConnector, times(0)).postIabdData(any(), any(), any(), any())(any(), any())

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateFailure.toString), any[AuditData]())(any(), any(), any())

        verifyNoSubmittedStateUpdate
      }

      "report errors when 1st IABD 59 POST fails and audit failure" in new Setup {
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(Future {
            etag2
          })
          .thenReturn(Future {
            etag3
          })

        when(
          mockTaiConnector.postIabdData(eqm(testNino), eqm(CurrentYearMinus4.toTaxYear.startYear), any(), eqm(etag2))(
            any(),
            any()
          )
        )
          .thenReturn(Future.failed(UpstreamErrorResponse("SERVICE IS UNAVAILABLE", SERVICE_UNAVAILABLE)))

        await(
          serviceUnderTest.submitExpenses(claimingForCurrentMinus4, createListMap(CurrentYearMinus4))
        ).isLeft mustBe true

        val inOrder: InOrder = Mockito.inOrder(mockCitizenDetailsConnector, mockTaiConnector)
        inOrder.verify(mockCitizenDetailsConnector).getETag(eqm(testNino))(any(), any())
        inOrder
          .verify(mockTaiConnector)
          .postIabdData(eqm(testNino), eqm(CurrentYearMinus4.toTaxYear.startYear), any(), eqm(etag2))(any(), any())
        inOrder.verify(mockCitizenDetailsConnector, times(0)).getETag(any())(any(), any())
        inOrder.verify(mockTaiConnector, times(0)).postIabdData(any(), any(), any(), any())(any(), any())

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateFailure.toString), any[AuditData]())(any(), any(), any())

        verifyNoSubmittedStateUpdate
      }
    }

    s"claiming for all available tax years" should {
      "upsert 4 IABD 59, audit success and set submitted status in userAnswers" in new Setup {
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(
            Future.successful(
              etag1
            )
          )
          .thenReturn(
            Future.successful(
              etag2
            )
          )
          .thenReturn(
            Future.successful(
              etag3
            )
          )
          .thenReturn(
            Future.successful(
              etag4
            )
          )
          .thenReturn(
            Future.successful(
              etag5
            )
          )

        createMockTaiConnector(CurrentYearMinus4)
        createMockTaiConnector(CurrentYearMinus3)
        createMockTaiConnector(CurrentYearMinus2)
        createMockTaiConnector(CurrentYearMinus1)
        createMockTaiConnector(CurrentYear)

        await(
          serviceUnderTest.submitExpenses(
            claimingForAll,
            createListMap(CurrentYearMinus4) ++ createListMap(CurrentYearMinus3) ++ createListMap(
              CurrentYearMinus2
            ) ++ createListMap(CurrentYearMinus1) ++ createListMap(CurrentYear)
          )
        ).isRight mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateSuccess.toString), any[AuditData]())(any(), any(), any())

        verifySessionGotSubmittedState
      }

      "report errors when ETAG call fails and audit failure" in new Setup {
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(Future.failed(new RuntimeException))

        await(
          serviceUnderTest.submitExpenses(claimingForCurrentMinus4, createListMap(CurrentYearMinus4))
        ).isLeft mustBe true

        val inOrder: InOrder = Mockito.inOrder(mockCitizenDetailsConnector, mockTaiConnector)
        inOrder.verify(mockCitizenDetailsConnector).getETag(eqm(testNino))(any(), any())
        inOrder.verify(mockTaiConnector, times(0)).postIabdData(any(), any(), any(), any())(any(), any())

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateFailure.toString), any[AuditData]())(any(), any(), any())

        verifyNoSubmittedStateUpdate
      }

      "report errors when 2nd IABD 59 POST fails and audit failure" in new Setup {
        when(mockCitizenDetailsConnector.getETag(any())(any(), any()))
          .thenReturn(
            Future.successful(
              etag1
            )
          )
          .thenReturn(
            Future.successful(
              etag2
            )
          )
          .thenReturn(
            Future.successful(
              etag3
            )
          )
          .thenReturn(
            Future.successful(
              etag4
            )
          )

        createMockTaiConnector(CurrentYearMinus4)

        when(
          mockTaiConnector.postIabdData(eqm(testNino), eqm(CurrentYearMinus3.toTaxYear.startYear), any(), any())(
            any(),
            any()
          )
        )
          .thenReturn(Future.failed(UpstreamErrorResponse("Not found", 404)))

        createMockTaiConnector(CurrentYearMinus2)

        await(
          serviceUnderTest.submitExpenses(
            Seq(CurrentYearMinus2, CurrentYearMinus3, CurrentYearMinus4),
            createListMap(CurrentYearMinus2) ++ createListMap(CurrentYearMinus3) ++ createListMap(CurrentYearMinus4)
          )
        ).isLeft mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateFailure.toString), any[AuditData]())(any(), any(), any())

        verifyNoSubmittedStateUpdate
      }
    }

    s"claiming for CTY-3 and CTY-4" should {
      "upsert 3 IABD 59, audit success and set submitted status in userAnswers" in new Setup {
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(
            Future.successful(
              etag1
            )
          )
          .thenReturn(
            Future.successful(
              etag2
            )
          )
          .thenReturn(
            Future.successful(
              etag3
            )
          )

        createMockTaiConnector(CurrentYearMinus4)
        createMockTaiConnector(CurrentYearMinus3)

        await(
          serviceUnderTest.submitExpenses(
            Seq(CurrentYearMinus3, CurrentYearMinus4),
            createListMap(CurrentYearMinus4) ++ createListMap(CurrentYearMinus3)
          )
        ).isRight mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateSuccess.toString), any[AuditData]())(any(), any(), any())

        verifySessionGotSubmittedState
      }

      "report errors when ETAG call fails and audit failure" in new Setup {
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(Future.failed(new RuntimeException))

        await(
          serviceUnderTest.submitExpenses(
            Seq(CurrentYearMinus3, CurrentYearMinus4),
            createListMap(CurrentYearMinus4) ++ createListMap(CurrentYearMinus3)
          )
        ).isLeft mustBe true

        val inOrder: InOrder = Mockito.inOrder(mockCitizenDetailsConnector, mockTaiConnector)
        inOrder.verify(mockCitizenDetailsConnector).getETag(eqm(testNino))(any(), any())
        inOrder.verify(mockTaiConnector, times(0)).postIabdData(any(), any(), any(), any())(any(), any())

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateFailure.toString), any[AuditData]())(any(), any(), any())

        verifyNoSubmittedStateUpdate
      }
    }

    s"claiming for CTY-2 and CTY-4" should {
      "upsert 3 IABD 59, audit success and set submitted status in userAnswers" in new Setup {
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(
            Future.successful(
              etag1
            )
          )
          .thenReturn(
            Future.successful(
              etag2
            )
          )
          .thenReturn(
            Future.successful(
              etag4
            )
          )

        createMockTaiConnector(CurrentYearMinus4)
        createMockTaiConnector(CurrentYearMinus2)

        await(
          serviceUnderTest.submitExpenses(
            Seq(CurrentYearMinus2, CurrentYearMinus4),
            createListMap(CurrentYearMinus2) ++ createListMap(CurrentYearMinus4)
          )
        ).isRight mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateSuccess.toString), any[AuditData]())(any(), any(), any())

        verifySessionGotSubmittedState
      }

      "report errors when ETAG call fails and audit failure" in new Setup {
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(Future.failed(new RuntimeException))

        await(
          serviceUnderTest.submitExpenses(
            Seq(CurrentYearMinus2, CurrentYearMinus4),
            createListMap(CurrentYearMinus2) ++ createListMap(CurrentYearMinus4)
          )
        ).isLeft mustBe true

        val inOrder: InOrder = Mockito.inOrder(mockCitizenDetailsConnector, mockTaiConnector)
        inOrder.verify(mockCitizenDetailsConnector).getETag(eqm(testNino))(any(), any())
        inOrder.verify(mockTaiConnector, times(0)).postIabdData(any(), any(), any(), any())(any(), any())

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateFailure.toString), any[AuditData]())(any(), any(), any())

        verifyNoSubmittedStateUpdate
      }

      "report errors when 3rd ETAG call fails and audit failure" in new Setup {
        when(mockCitizenDetailsConnector.getETag(any())(any(), any()))
          .thenReturn(Future {
            etag2
          })
          .thenReturn(Future.failed(new RuntimeException))

        createMockTaiConnector(CurrentYearMinus4)

        await(
          serviceUnderTest.submitExpenses(
            Seq(CurrentYearMinus2, CurrentYearMinus4),
            createListMap(CurrentYearMinus2) ++ createListMap(CurrentYearMinus4)
          )
        ).isLeft mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateFailure.toString), any[AuditData]())(any(), any(), any())

        verifyNoSubmittedStateUpdate
      }
    }

    s"only claiming for 3 weeks of the CTY-1" should {
      "upsert 1 IABD 59, audit success and set submitted status in userAnswers" in new Setup {
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(
            Future.successful(
              etag1
            )
          )

        createMockTaiConnector(CurrentYearMinus1)

        await(
          serviceUnderTest.submitExpenses(claimingForCurrentMinus1, createListMap(CurrentYearMinus1))
        ).isRight mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateSuccess.toString), any[AuditData]())(any(), any(), any())

        verifySessionGotSubmittedState
      }
    }

    s"only claiming for 3 weeks of the CTY" should {
      "upsert 1 IABD 59, audit success and set submitted status in userAnswers" in new Setup {
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(
            Future.successful(
              etag1
            )
          )

        createMockTaiConnector(CurrentYear)

        await(serviceUnderTest.submitExpenses(claimingForCurrent, createListMap(CurrentYear))).isRight mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateSuccess.toString), any[AuditData]())(any(), any(), any())

        verifySessionGotSubmittedState
      }
    }

    s"claiming for  CTY-1 and CTY-4 year" should {
      "upsert 3 IABD 59, audit success and set submitted status in userAnswers" in new Setup {
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(
            Future.successful(
              etag1
            )
          )
          .thenReturn(
            Future.successful(
              etag2
            )
          )
          .thenReturn(
            Future.successful(
              etag5
            )
          )

        createMockTaiConnector(CurrentYearMinus4)
        createMockTaiConnector(CurrentYearMinus1)

        await(
          serviceUnderTest.submitExpenses(
            Seq(CurrentYearMinus1, CurrentYearMinus4),
            createListMap(CurrentYearMinus1) ++ createListMap(CurrentYearMinus4)
          )
        ).isRight mustBe true

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateSuccess.toString), any[AuditData]())(any(), any(), any())

        verifySessionGotSubmittedState
      }

      "report errors when ETAG call fails and audit failure" in new Setup {
        when(mockCitizenDetailsConnector.getETag(eqm(testNino))(any(), any()))
          .thenReturn(Future.failed(new RuntimeException))

        await(
          serviceUnderTest.submitExpenses(
            Seq(CurrentYearMinus1, CurrentYearMinus4),
            createListMap(CurrentYearMinus1) ++ createListMap(CurrentYearMinus4)
          )
        ).isLeft mustBe true

        val inOrder: InOrder = Mockito.inOrder(mockCitizenDetailsConnector, mockTaiConnector)
        inOrder.verify(mockCitizenDetailsConnector).getETag(eqm(testNino))(any(), any())
        inOrder.verify(mockTaiConnector, times(0)).postIabdData(any(), any(), any(), any())(any(), any())

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(eqm(UpdateWorkingFromHomeFlatRateFailure.toString), any[AuditData]())(any(), any(), any())

        verifyNoSubmittedStateUpdate
      }
    }
  }

}
