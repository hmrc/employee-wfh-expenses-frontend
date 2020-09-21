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

package controllers

import connectors.PaperlessPreferenceConnector
import controllers.actions._
import javax.inject.Inject
import models.UserAnswers
import pages.WhenDidYouFirstStartWorkingFromHomePage
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.SubmissionService
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import views.html.YourTaxReliefView
import scala.concurrent.{ExecutionContext, Future}

class YourTaxReliefController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         identify: IdentifierAction,
                                         checkAlreadyClaimed: CheckAlreadyClaimedAction,
                                         getData: DataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         submissionService: SubmissionService,
                                         sessionRepository: SessionRepository,
                                         val controllerComponents: MessagesControllerComponents,
                                         paperlessPreferenceConnector: PaperlessPreferenceConnector,
                                         view: YourTaxReliefView
                                       )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  case object SUBMITTED
  case object PAPERLESS

  def onPageLoad: Action[AnyContent] = (identify andThen checkAlreadyClaimed andThen getData andThen requireData) {
    implicit request =>
      request.userAnswers.get(WhenDidYouFirstStartWorkingFromHomePage) match {
        case None =>
          Redirect(routes.DisclaimerController.onPageLoad())
        case Some(startedWorkingFromHomeDate) =>
          Ok(view(startedWorkingFromHomeDate))
      }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen checkAlreadyClaimed andThen getData andThen requireData).async {
    implicit request =>

      request.userAnswers.get(WhenDidYouFirstStartWorkingFromHomePage) match {

        case None =>
          Logger.error(s"[SubmitYourClaimController][onSubmit] - Start date is missing")
          Future.successful(Redirect(routes.TechnicalDifficultiesController.onPageLoad()))

        case Some(startDate) =>

          val submissionFuture: Future[(Any, Boolean)] = submissionService.submitExpenses(startDate) map {
            case Right(_) =>
              sessionRepository.set(UserAnswers(request.internalId))
              (SUBMITTED, true)
            case Left(_) => {
              (SUBMITTED, false)
            }
          }

          val paperlessPreferenceFuture: Future[(Any, Boolean)] = paperlessPreferenceConnector.getPaperlessPreference().flatMap {
            case paperless => {
              paperless match {
                case Some(true) => Future.successful((PAPERLESS, true))
                case _ => Future.successful((PAPERLESS, false))
              }
            }
          }

          val futureList = List(submissionFuture, paperlessPreferenceFuture)
          val outcomeSequence = Future.sequence(futureList)

          outcomeSequence.map { outcome =>
            val submittedOk = outcome.filter(_._1 == SUBMITTED).head._2
            val hasPaperless = outcome.filter(_._1 == PAPERLESS).head._2

            submittedOk match {
              case true =>
                hasPaperless match {
                  case true => {
                    Redirect(routes.ConfirmationPaperlessController.onPageLoad())
                  }
                  case _ => {
                    Redirect(routes.ConfirmationController.onPageLoad())
                  }
                }
              case false => {
                Redirect(routes.TechnicalDifficultiesController.onPageLoad())
              }
            }
          }
      }
  }
}
