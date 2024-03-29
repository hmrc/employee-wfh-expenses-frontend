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
import models.{AuditData, Date, FlatRateItem, TaxYearFromUIAssembler}
import pages.SubmittedClaim
import play.api.Logging
import play.api.mvc.AnyContent
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.RateLimiting
import utils.TaxYearDates._
import java.time.LocalDate

import javax.inject.{Inject, Named, Singleton}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps


@Singleton
class SubmissionService @Inject()(citizenDetailsConnector: CitizenDetailsConnector,
                                  taiConnector: TaiConnector,
                                  auditConnector: AuditConnector,
                                  sessionService: SessionService,
                                  appConfig: FrontendAppConfig,
                                  @Named("IABD POST") rateLimiter: RateLimiting
                                 ) extends Logging {

  val ZERO      = 0

  def submitExpenses(startDate: Option[Date], selectedTaxYears: List[String], numberOfWeeksOf2023: Option[Int])
                    (implicit dataRequest: DataRequest[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Future[Either[String, Unit]] = {


    rateLimiter.withToken(() => submit(startDate, selectedTaxYears, numberOfWeeksOf2023) map {
      case Right(submittedDetails) =>
        logger.info(s"[SubmissionService][submitExpenses] Submission successful")
        auditSubmissionSuccess(submittedDetails)
        dataRequest.userAnswers.set(SubmittedClaim, value = true).map(sessionService.set)
        Right(())

      case Left(error) =>
        logger.error(s"[SubmissionService][submitExpenses] Submission failed : $error")
        auditSubmissionFailure(error)
        Left(error)
    })
  }

  private def submit(startDate: Option[Date], selectedTaxYears: List[String], numberOfWeeksOf2023: Option[Int])
                     (implicit dataRequest: DataRequest[AnyContent],
                      hc: HeaderCarrier,
                      ec: ExecutionContext) = {

    val assembler = TaxYearFromUIAssembler(selectedTaxYears)

    val wholeYearItems: Seq[FlatRateItem] = Seq(
      if(assembler.contains2021) Some(FlatRateItem(year = YEAR_2021, amount = calculate2021FlatRate())) else None,
      if(assembler.contains2022) Some(FlatRateItem(year = YEAR_2022, amount = calculate2022FlatRate())) else None
    ).flatten

    val previousYearItems: Seq[FlatRateItem] = startDate match {
      case Some(date) if assembler.containsPrevious => Seq(
        Some(FlatRateItem(year = YEAR_2020, amount = calculate2020FlatRate())),
        if(date.date.isBefore(TAX_YEAR_2019_END_DATE)) {
          Some(FlatRateItem(year = YEAR_2019, amount = calculate2019FlatRate(date.date)))
      } else {
        None
      }
      ).flatten
      case _ => Seq.empty[FlatRateItem]
    }

    val perWeekItems: Seq[FlatRateItem] = numberOfWeeksOf2023 match {
      case Some(weeks) => Seq(FlatRateItem(year = YEAR_2023, amount = calculate2023FlatRate(weeks)))
      case _ => Seq.empty[FlatRateItem]
    }

    val flatRateItems = wholeYearItems ++ previousYearItems ++ perWeekItems

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

  def calculate2023FlatRate(numberOfWeeks: Int): Int =
    (numberOfWeeks * appConfig.taxReliefPerWeek2023).min(appConfig.taxReliefMaxPerYear2023)

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
