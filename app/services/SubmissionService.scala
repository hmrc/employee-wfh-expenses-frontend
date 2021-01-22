/*
 * Copyright 2021 HM Revenue & Customs
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
import config.FrontendAppConfig
import connectors.{CitizenDetailsConnector, TaiConnector}

import javax.inject.{Inject, Singleton}
import models.auditing.AuditEventType._
import models.requests.DataRequest
import models.{AuditData, FlatRateItem}
import play.api.{Logger, Logging}
import play.api.mvc.AnyContent
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.TaxYearDates._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps


@Singleton
class SubmissionService @Inject()
(
  citizenDetailsConnector:  CitizenDetailsConnector,
  taiConnector:             TaiConnector,
  auditConnector:           AuditConnector,
  appConfig:                FrontendAppConfig
) extends Logging {

  val ZERO      = 0


  def submitExpenses(startDate: LocalDate)
                    (implicit dataRequest: DataRequest[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Future[Either[String, Unit]] = {

    submit(startDate) map {

      case Right(submittedDetails) =>
        logger.info(s"[SubmissionService][submitExpenses] Submission successful")
        auditSubmissionSuccess(submittedDetails)
        Right(())

      case Left(error) =>
        logger.error(s"[SubmissionService][submitExpenses] Submission failed : $error")
        auditSubmissionFailure(error)
        Left(error)
    }
  }


  private def submit(startDate: LocalDate)
            (implicit dataRequest: DataRequest[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Future[Either[String, Seq[FlatRateItem]]] = {

    val flatRateItems: Seq[FlatRateItem] = Seq[FlatRateItem](
      FlatRateItem(year = TAX_YEAR_2019_START_DATE.getYear, amount = calculate2019FlatRate(startDate)),
      FlatRateItem(year = TAX_YEAR_2020_START_DATE.getYear, amount = calculate2020FlatRate())
    )

    def futureSequence[I, O](inputs: Seq[I])(flatMapFunction: I => Future[O])(implicit ec: ExecutionContext): Future[Seq[O]] =
      inputs.foldLeft(Future.successful(Seq.empty[O]))(
        (previousFutureResult, nextInput) =>
          for {
            futureSeq <- previousFutureResult
            future    <- flatMapFunction(nextInput)
          } yield futureSeq :+ future
      )

    logger.info("[SubmissionService][submit] Submitting")

    futureSequence(flatRateItems) {
      item: FlatRateItem =>
        for {
          etag <- citizenDetailsConnector.getETag(dataRequest.nino)
          _    <- taiConnector.postIabdData(dataRequest.nino, item.year, item.amount, etag)
        } yield item
    } map {
      submittedDetails => Right(submittedDetails)
    } recover {
      case e => Left(e.getMessage)
    }
  }


  def calculate2019FlatRate(startDate: LocalDate): Int =
    if (startDate.isBefore(TAX_YEAR_2020_START_DATE)) {
       numberOfWeeks(startDate, TAX_YEAR_2019_END_DATE) * appConfig.taxReliefPerWeek2019 toInt match {
        case flatRateAmount: Int if flatRateAmount > appConfig.taxReliefMaxPerYear2019  => appConfig.taxReliefMaxPerYear2019
        case flatRateAmount: Int                                                        => flatRateAmount
      }
    } else {
      ZERO
    }


  def calculate2020FlatRate(): Int =
    numberOfWeeks(TAX_YEAR_2020_START_DATE, TAX_YEAR_2020_END_DATE ) * appConfig.taxReliefPerWeek2020 toInt match {
      case flatRateAmount: Int if flatRateAmount > appConfig.taxReliefMaxPerYear2020  => appConfig.taxReliefMaxPerYear2020
      case flatRateAmount: Int                                                        => flatRateAmount
    }


  private def auditSubmissionSuccess(submittedDetails: Seq[FlatRateItem])
                                    (implicit dataRequest: DataRequest[AnyContent], hc: HeaderCarrier, ec: ExecutionContext) =
    auditConnector.sendExplicitAudit(
      UpdateWorkingFromHomeFlatRateSuccess.toString,
      AuditData(
        nino          = dataRequest.nino,
        userAnswers   = dataRequest.userAnswers.data,
        flatRateItems = Some(submittedDetails)
      )
    )


  private def auditSubmissionFailure(error: String)
                                    (implicit dataRequest: DataRequest[AnyContent], hc: HeaderCarrier, ec: ExecutionContext) =
    auditConnector.sendExplicitAudit(
      UpdateWorkingFromHomeFlatRateFailure.toString,
      AuditData(
        nino            = dataRequest.nino,
        userAnswers     = dataRequest.userAnswers.data,
        submissionError = Some(error)
      )
    )

}
