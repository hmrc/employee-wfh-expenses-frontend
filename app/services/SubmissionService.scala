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

import config.FrontendAppConfig
import connectors.{CitizenDetailsConnector, TaiConnector}
import models.auditing.AuditEventType._
import models.requests.DataRequest
import models.{AuditData, FlatRateItem, TaxYearFromUIAssembler}
import pages.SubmittedClaim
import play.api.Logging
import play.api.mvc.AnyContent
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.RateLimiting
import utils.TaxYearDates._

import java.time.LocalDate
import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps


@Singleton
class SubmissionService @Inject()
(
  citizenDetailsConnector:  CitizenDetailsConnector,
  taiConnector:             TaiConnector,
  auditConnector:           AuditConnector,
  sessionRepository:        SessionRepository,
  appConfig:                FrontendAppConfig,
  @Named("IABD POST") rateLimiter: RateLimiting
) extends Logging {

  val ZERO      = 0

  def submitExpenses(startDate: Option[LocalDate], selectedTaxYears: List[String])
                    (implicit dataRequest: DataRequest[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Future[Either[String, Unit]] = {


    rateLimiter.withToken(() => submit(startDate, selectedTaxYears) map {
      case Right(submittedDetails) =>
        logger.info(s"[SubmissionService][submitExpenses] Submission successful")
        auditSubmissionSuccess(submittedDetails)
        dataRequest.userAnswers.set(SubmittedClaim, value = true).map(sessionRepository.set)
        Right(())

      case Left(error) =>
        logger.error(s"[SubmissionService][submitExpenses] Submission failed : $error")
        auditSubmissionFailure(error)
        Left(error)
    })
  }

  private def submit(startDate: Option[LocalDate], selectedTaxYears: List[String])
                     (implicit dataRequest: DataRequest[AnyContent],
                      hc: HeaderCarrier,
                      ec: ExecutionContext) = {

    val assembler = TaxYearFromUIAssembler(selectedTaxYears)

    val flatRateItems: Seq[FlatRateItem] = startDate match {
      case None => selectedTaxYears match {
        case assembler.claiming2022Only =>
          Seq[FlatRateItem](
            FlatRateItem(year = YEAR_2022, amount = calculate2022FlatRate())
          )
        case assembler.claiming2022And2021 =>
          Seq[FlatRateItem](
            FlatRateItem(year = YEAR_2022, amount = calculate2022FlatRate()),
            FlatRateItem(year = YEAR_2021, amount = calculate2021FlatRate())
          )
        case assembler.claiming2021Only =>
          Seq[FlatRateItem](
            FlatRateItem(year = YEAR_2021, amount = calculate2021FlatRate())
          )
        case e =>
          logger.error(s"[SubmissionService][submit](no startDate found) Unexpected case match $e")
          Seq.empty[FlatRateItem]
      }
      case Some(date) => selectedTaxYears match {
        case assembler.claimingAllYears =>
          if(date.isAfter(TAX_YEAR_2019_END_DATE)) {
            Seq[FlatRateItem](
              FlatRateItem(year = YEAR_2022, amount = calculate2022FlatRate()),
              FlatRateItem(year = YEAR_2021, amount = calculate2021FlatRate()),
              FlatRateItem(year = YEAR_2020, amount = calculate2020FlatRate())
            )
          } else {
            Seq[FlatRateItem](
              FlatRateItem(year = YEAR_2022, amount = calculate2022FlatRate()),
              FlatRateItem(year = YEAR_2021, amount = calculate2021FlatRate()),
              FlatRateItem(year = YEAR_2020, amount = calculate2020FlatRate()),
              FlatRateItem(year = YEAR_2019, amount = calculate2019FlatRate(date))
            )
          }
        case assembler.claiming2022AndPrev =>
          if(date.isAfter(TAX_YEAR_2019_END_DATE)) {
            Seq[FlatRateItem](
              FlatRateItem(year = YEAR_2022, amount = calculate2022FlatRate()),
              FlatRateItem(year = YEAR_2020, amount = calculate2020FlatRate())
            )
          } else {
            Seq[FlatRateItem](
              FlatRateItem(year = YEAR_2022, amount = calculate2022FlatRate()),
              FlatRateItem(year = YEAR_2020, amount = calculate2020FlatRate()),
              FlatRateItem(year = YEAR_2019, amount = calculate2019FlatRate(date))
            )
          }
        case assembler.claiming2021AndPrev =>
          if(date.isAfter(TAX_YEAR_2019_END_DATE)) {
            Seq[FlatRateItem](
              FlatRateItem(year = YEAR_2021, amount = calculate2021FlatRate()),
              FlatRateItem(year = YEAR_2020, amount = calculate2020FlatRate())
            )
          } else {
            Seq[FlatRateItem](
              FlatRateItem(year = YEAR_2021, amount = calculate2021FlatRate()),
              FlatRateItem(year = YEAR_2020, amount = calculate2020FlatRate()),
              FlatRateItem(year = YEAR_2019, amount = calculate2019FlatRate(date))
            )
          }
        case assembler.claimingPrevOnly =>
          if(date.isAfter(TAX_YEAR_2019_END_DATE)) {
            Seq[FlatRateItem](
              FlatRateItem(year = YEAR_2020, amount = calculate2020FlatRate())
            )
          } else {
            Seq[FlatRateItem](
              FlatRateItem(year = YEAR_2020, amount = calculate2020FlatRate()),
              FlatRateItem(year = YEAR_2019, amount = calculate2019FlatRate(date))
            )
          }
        case e =>
          logger.error(s"[SubmissionService][submit](startDate found) Unexpected case match $e")
          Seq.empty[FlatRateItem]
      }
    }

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

  def futureSequence[I, O](inputs: Seq[I])(flatMapFunction: I => Future[O])(implicit ec: ExecutionContext): Future[Seq[O]] = {
    inputs.foldLeft(Future.successful(Seq.empty[O]))(
      (previousFutureResult, nextInput) =>
        for {
          futureSeq <- previousFutureResult
          future    <- flatMapFunction(nextInput)
        } yield futureSeq :+ future
    )
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

  def calculate2021FlatRate(): Int = {
      numberOfWeeks(TAX_YEAR_2021_START_DATE, TAX_YEAR_2021_END_DATE) * appConfig.taxReliefPerWeek2021 toInt match {
        case flatRateAmount: Int if flatRateAmount > appConfig.taxReliefMaxPerYear2021 => appConfig.taxReliefMaxPerYear2021
        case flatRateAmount: Int                                                       => flatRateAmount
      }
    }

  def calculate2022FlatRate(): Int = {
    numberOfWeeks(TAX_YEAR_2022_START_DATE, TAX_YEAR_2022_END_DATE) * appConfig.taxReliefPerWeek2022 toInt match {
      case flatRateAmount: Int if flatRateAmount > appConfig.taxReliefMaxPerYear2022 => appConfig.taxReliefMaxPerYear2022
      case flatRateAmount: Int                                                       => flatRateAmount
    }
  }

  private def auditSubmissionSuccess(submittedDetails: Seq[FlatRateItem])
                                    (implicit dataRequest: DataRequest[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Unit =
    auditConnector.sendExplicitAudit(
      UpdateWorkingFromHomeFlatRateSuccess.toString,
      AuditData(
        nino          = dataRequest.nino,
        userAnswers   = dataRequest.userAnswers.data,
        flatRateItems = Some(submittedDetails)
      )
    )


  private def auditSubmissionFailure(error: String)
                                    (implicit dataRequest: DataRequest[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Unit =
    auditConnector.sendExplicitAudit(
      UpdateWorkingFromHomeFlatRateFailure.toString,
      AuditData(
        nino            = dataRequest.nino,
        userAnswers     = dataRequest.userAnswers.data,
        submissionError = Some(error)
      )
    )

}
