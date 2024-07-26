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

import config.FrontendAppConfig
import connectors.{CitizenDetailsConnector, TaiConnector}
import models.auditing.AuditEventType._
import models.requests.DataRequest
import models.{AuditData, FlatRateItem, TaxYearSelection}
import pages.SubmittedClaim
import play.api.Logging
import play.api.mvc.AnyContent
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import javax.inject.{Inject, Singleton}
import scala.collection.immutable.ListMap
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionService @Inject()(citizenDetailsConnector: CitizenDetailsConnector,
                                  taiConnector: TaiConnector,
                                  auditConnector: AuditConnector,
                                  sessionService: SessionService,
                                  appConfig: FrontendAppConfig,
                                 ) extends Logging {

  def submitExpenses(selectedTaxYears: Seq[TaxYearSelection], weeksForTaxYears: ListMap[TaxYearSelection, Int])
                    (implicit dataRequest: DataRequest[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Future[Either[String, Unit]] = {

    submit(selectedTaxYears, weeksForTaxYears) map {
      case Right(submittedDetails) =>
        logger.info(s"[SubmissionService][submitExpenses] Submission successful")
        auditSubmissionSuccess(submittedDetails)
        dataRequest.userAnswers.set(SubmittedClaim, value = true).map(sessionService.set)
        Right(())

      case Left(error) =>
        logger.error(s"[SubmissionService][submitExpenses] Submission failed : $error")
        auditSubmissionFailure(error)
        Left(error)
    }
  }

  // scalastyle:off cyclomatic.complexity
  private def submit(selectedTaxYears: Seq[TaxYearSelection], weeksForTaxYears: ListMap[TaxYearSelection, Int])
                    (implicit dataRequest: DataRequest[AnyContent],
                     hc: HeaderCarrier,
                     ec: ExecutionContext) = {

    val (wholeYearSelections, perWeekSelections) = selectedTaxYears.partition(TaxYearSelection.wholeYearClaims.contains)

    val wholeYearItems: Seq[FlatRateItem] = wholeYearSelections.map { taxYearSelection =>
      FlatRateItem(taxYearSelection.toTaxYear.startYear, appConfig.taxReliefMaxPerYear(taxYearSelection))
    }

    val perWeekItems: Seq[FlatRateItem] = perWeekSelections.map { taxYearSelection =>
      val claimWeekAmount = weeksForTaxYears.getOrElse(
        taxYearSelection,
        throw new InternalServerException(s"[SubmissionService][submit] Week count for ${taxYearSelection.toString} is missing")
      )
      FlatRateItem(
        taxYearSelection.toTaxYear.startYear,
        (claimWeekAmount * appConfig.taxReliefPerWeek(taxYearSelection)).min(appConfig.taxReliefMaxPerYear(taxYearSelection))
      )
    }

    val flatRateItems = wholeYearItems ++ perWeekItems

    if (flatRateItems.isEmpty) {
      Future.successful(Left("Flat Rate Items sequence is empty, unable to submit"))
    } else {
      logger.info("[SubmissionService][submit] Submitting")
      futureSequence(flatRateItems) {
        item: FlatRateItem =>
          for {
            etag <- citizenDetailsConnector.getETag(dataRequest.nino)
            _ <- taiConnector.postIabdData(dataRequest.nino, item.year, item.amount, etag)
          } yield item
      } map {
        submittedDetails => Right(submittedDetails)
      } recover {
        case e => Left(e.getMessage)
      }
    }
  }

  private def futureSequence[I, O](inputs: Seq[I])(flatMapFunction: I => Future[O])(implicit ec: ExecutionContext): Future[Seq[O]] = {
    inputs.foldLeft(Future.successful(Seq.empty[O]))(
      (previousFutureResult, nextInput) =>
        for {
          futureSeq <- previousFutureResult
          future <- flatMapFunction(nextInput)
        } yield futureSeq :+ future
    )
  }

  private def auditSubmissionSuccess(submittedDetails: Seq[FlatRateItem])
                                    (implicit dataRequest: DataRequest[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Unit =
    auditConnector.sendExplicitAudit(
      UpdateWorkingFromHomeFlatRateSuccess.toString,
      AuditData(
        nino = dataRequest.nino,
        userAnswers = dataRequest.userAnswers.data,
        flatRateItems = Some(submittedDetails)
      )
    )


  private def auditSubmissionFailure(error: String)
                                    (implicit dataRequest: DataRequest[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Unit =
    auditConnector.sendExplicitAudit(
      UpdateWorkingFromHomeFlatRateFailure.toString,
      AuditData(
        nino = dataRequest.nino,
        userAnswers = dataRequest.userAnswers.data,
        submissionError = Some(error)
      )
    )

}
