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

import config.FrontendAppConfig
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
                                                    eligibilityCheckerService: EligibilityCheckerService,
                                                    appConfig: FrontendAppConfig
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
      case Some(true) => handleSAFlow(WfhDueToCovidStatusWrapper(1))
      case _ =>
        (request.userAnswers.get(ClaimedForTaxYear2020),
          request.userAnswers.get(ClaimedForTaxYear2021),
          request.userAnswers.get(ClaimedForTaxYear2022),
          request.userAnswers.get(ClaimedForTaxYear2023)) match {
          case (Some(claimed2020), Some(claimed2021), Some(claimed2022), Some(claimed2023)) =>
            val availableYears = SelectTaxYearsToClaimFor.getValuesFromClaimedBooleans(claimed2020, claimed2021, claimed2022, claimed2023)

            val preparedForm = request.userAnswers.get(SelectTaxYearsToClaimForPage) match {
              case None => form
              case Some(value) => form.fill(value)
            }

            val showHintText = !hasSingleUnclaimedYear(claimed2020, claimed2021, claimed2022, claimed2023)

            Future.successful(Ok(view(preparedForm, availableYears, showHintText)))

          case (_, _, _, _) => Future.successful(Redirect(routes.IndexController.onPageLoad))
        }
    }
  }

  def handleSAFlow(wfhDueToCovidStatusWrapper: WfhDueToCovidStatusWrapper)
                  (implicit request: DataRequest[AnyContent]): Future[Result] = {

    if(request.userAnswers.get(ClaimedForTaxYear2022).getOrElse(false)) {
      Future.successful(Redirect(appConfig.p87DigitalFormUrl))
    } else {

      val optionList: Option[Set[SelectTaxYearsToClaimFor]] = wfhDueToCovidStatusWrapper.WfhDueToCovidStatus match {
        case 1 => Some(Set(SelectTaxYearsToClaimFor.Option1))
        case 2 => eligibilityCheckerValuesTaiOverride(request)
        case 3 => Some(Set(SelectTaxYearsToClaimFor.Option1))
        case _ => None
      }

      val availableYears = optionList.getOrElse(Set(SelectTaxYearsToClaimFor.Option1)).toSeq

      val preparedForm = request.userAnswers.get(SelectTaxYearsToClaimForPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Future.successful(Ok(view(preparedForm, availableYears, hintText = false)))
    }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val availableYearsUserCanClaim = SelectTaxYearsToClaimFor.getValuesFromClaimedBooleans(
        request.userAnswers.get(ClaimedForTaxYear2020).getOrElse(false),
        request.userAnswers.get(ClaimedForTaxYear2021).getOrElse(false),
        request.userAnswers.get(ClaimedForTaxYear2022).getOrElse(false),
        request.userAnswers.get(ClaimedForTaxYear2023).getOrElse(false)
      )

      request.userAnswers.eligibilityCheckerSessionIdOpt match {
        case Some(sessionId) => eligibilityCheckerService.wfhDueToCovidStatus(sessionId).flatMap { isSaUser =>
          val availableYears = if(isSaUser.isDefined) Seq(SelectTaxYearsToClaimFor.Option1) else availableYearsUserCanClaim

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
        case None =>
          form.bindFromRequest().fold(
            formWithErrors =>
              Future.successful(BadRequest(view(formWithErrors, availableYearsUserCanClaim))),

            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(SelectTaxYearsToClaimForPage, value))
                _ <- sessionRepository.set(updatedAnswers)
              } yield {
                Redirect(navigator.nextPage(SelectTaxYearsToClaimForPage, updatedAnswers))
              }
          )
      }
  }

  def eligibilityCheckerValuesTaiOverride(request: DataRequest[AnyContent]): Option[Set[SelectTaxYearsToClaimFor]] = {

    val overrideList: Set[SelectTaxYearsToClaimFor] = SelectTaxYearsToClaimFor
      .getValuesFromClaimedBooleans(request.userAnswers.get(ClaimedForTaxYear2020).getOrElse(false),
        request.userAnswers.get(ClaimedForTaxYear2021).getOrElse(false),
        request.userAnswers.get(ClaimedForTaxYear2022).getOrElse(false),
        request.userAnswers.get(ClaimedForTaxYear2023).getOrElse(false)).toSet

    Some(overrideList)
  }

  def hasSingleUnclaimedYear(claimed2020: Boolean, claimed2021: Boolean, claimed2022: Boolean, claimed2023: Boolean): Boolean = {
    Seq(claimed2020, claimed2021, claimed2022, claimed2023).count(_ == false) == 1
  }

}
