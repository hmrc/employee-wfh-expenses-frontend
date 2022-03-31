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

package controllers

import controllers.actions._
import forms.SelectTaxYearsToClaimForFormProvider
import models.requests.DataRequest
import models.{SelectTaxYearsToClaimFor, WfhDueToCovidStatusWrapper}
import navigation.Navigator
import pages._
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.EligibilityCheckerService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.SelectTaxYearsToClaimForView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SelectTaxYearsToClaimForController @Inject()(
                                                    override val messagesApi: MessagesApi,
                                                    sessionRepository: SessionRepository,
                                                    navigator: Navigator,
                                                    identify: IdentifierAction,
                                                    getData: DataRetrievalAction,
                                                    requireData: DataRequiredAction,
                                                    formProvider: SelectTaxYearsToClaimForFormProvider,
                                                    val controllerComponents: MessagesControllerComponents,
                                                    view: SelectTaxYearsToClaimForView,
                                                    eligibilityCheckerService: EligibilityCheckerService
                                                  )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  val form = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      request.userAnswers.eligibilityCheckerSessionIdOpt match {
        case Some(sessionId) => eligibilityCheckerService.wfhDueToCovidStatus(sessionId).flatMap {
          case Some(wrapper) => handleSAFlow(wrapper)
          case None => handleDefaultSAFlow()
        }
        case None =>
          logger.info("Eligibility Checker SessionId parameter is missing from the request")
          handleDefaultSAFlow()
      }

  }

  def handleDefaultSAFlow()(implicit request: DataRequest[AnyContent]): Future[Result] = {
    request.userAnswers.get(HasSelfAssessmentEnrolment) match {
      case Some(true) => Future.successful(Redirect(routes.DisclaimerController.onPageLoad()))
      case _ =>
        (request.userAnswers.get(ClaimedForTaxYear2020), request.userAnswers.get(ClaimedForTaxYear2021), request.userAnswers.get(ClaimedForTaxYear2022)) match {
          case (Some(claimed2020), Some(claimed2021), Some(claimed2022)) =>
            if (hasSingleUnclaimedYear(claimed2020, claimed2021, claimed2022)) {
              val setOfUnclaimedYears: Set[SelectTaxYearsToClaimFor] = SelectTaxYearsToClaimFor.getValuesFromClaimedBooleans(claimed2020, claimed2021, claimed2022).toSet

              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(SelectTaxYearsToClaimForPage, setOfUnclaimedYears))
                _ <- sessionRepository.set(updatedAnswers)
              } yield Redirect(navigator.nextPage(SelectTaxYearsToClaimForPage, updatedAnswers))

            } else {
              val availableYears = SelectTaxYearsToClaimFor.getValuesFromClaimedBooleans(claimed2020, claimed2021, claimed2022)

              val preparedForm = request.userAnswers.get(SelectTaxYearsToClaimForPage) match {
                case None => form
                case Some(value) => form.fill(value)
              }

              Future.successful(Ok(view(preparedForm, availableYears)))
            }

          case (_, _, _) => Future.successful(Redirect(routes.IndexController.onPageLoad()))
        }
    }
  }

  def handleSAFlow(wfhDueToCovidStatusWrapper: WfhDueToCovidStatusWrapper)
                  (implicit request: DataRequest[AnyContent]): Future[Result] = {

    val optionList: Option[Set[SelectTaxYearsToClaimFor]] = if(wfhDueToCovidStatusWrapper.registeredForSA) {
      wfhDueToCovidStatusWrapper.WfhDueToCovidStatus match {
        case 1 => Some(Set(SelectTaxYearsToClaimFor.Option1))
        case 2 => eligibilityCheckerValuesTaiOverride(request)
        case 3 => Some(Set(SelectTaxYearsToClaimFor.Option1))
        case _ => None
    }} else {
      Some(Set.empty)
    }

    optionList match {
      case Some(listOfOptions) => if(listOfOptions.isEmpty) handleDefaultSAFlow() else {
        for {
          updatedAnswers <- Future.fromTry(request.userAnswers.set(SelectTaxYearsToClaimForPage, listOfOptions))
          _ <- sessionRepository.set(updatedAnswers)
        } yield Redirect(navigator.nextPage(SelectTaxYearsToClaimForPage, updatedAnswers))
      }
      case None =>
        logger.error(s"Eligibility Checker returned Covid Status value: [${wfhDueToCovidStatusWrapper.WfhDueToCovidStatus}], which is undefined.]")
        Future.successful(Redirect(routes.TechnicalDifficultiesController.onPageLoad()))
    }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val availableYears = SelectTaxYearsToClaimFor.getValuesFromClaimedBooleans(
        request.userAnswers.get(ClaimedForTaxYear2020).getOrElse(false),
        request.userAnswers.get(ClaimedForTaxYear2021).getOrElse(false),
        request.userAnswers.get(ClaimedForTaxYear2022).getOrElse(false)
      )

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, availableYears))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(SelectTaxYearsToClaimForPage, value))
            _ <- sessionRepository.set(updatedAnswers)
          } yield {
            Redirect(navigator.nextPage(SelectTaxYearsToClaimForPage, updatedAnswers))
          }
      )
  }

  def eligibilityCheckerValuesTaiOverride(request: DataRequest[AnyContent]): Option[Set[SelectTaxYearsToClaimFor]] = {

    val overrideList: Set[SelectTaxYearsToClaimFor] = SelectTaxYearsToClaimFor
      .getValuesFromClaimedBooleans(request.userAnswers.get(ClaimedForTaxYear2020).getOrElse(false),
        request.userAnswers.get(ClaimedForTaxYear2021).getOrElse(false),
        request.userAnswers.get(ClaimedForTaxYear2022).getOrElse(false)).toSet

    Some(overrideList)
  }

  def hasSingleUnclaimedYear(claimed2020: Boolean, claimed2021: Boolean, claimed2022: Boolean): Boolean = {
    (claimed2020 && !claimed2021 && claimed2022) || (claimed2020 && claimed2021 && !claimed2022) || (!claimed2020 && claimed2021 && claimed2022)
  }

}
