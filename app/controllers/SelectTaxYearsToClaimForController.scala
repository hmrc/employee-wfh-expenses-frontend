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
import models.SelectTaxYearsToClaimFor._
import models.{SelectTaxYearsToClaimFor, WfhDueToCovidStatusWrapper}
import models.requests.DataRequest

import javax.inject.Inject
import navigation.Navigator
import pages.{ClaimedForTaxYear2020, ClaimedForTaxYear2021, ClaimedForTaxYear2022, HasSelfAssessmentEnrolment, SelectTaxYearsToClaimForPage}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.EligibilityCheckerService
import uk.gov.hmrc.govukfrontend.views.viewmodels.checkboxes.CheckboxItem
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.SelectTaxYearsToClaimForView

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

  val form: Form[Set[SelectTaxYearsToClaimFor]] = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      val checkboxes: Seq[CheckboxItem] = (
        request.userAnswers.get(ClaimedForTaxYear2020).getOrElse(true),
        request.userAnswers.get(ClaimedForTaxYear2021).getOrElse(true),
        request.userAnswers.get(ClaimedForTaxYear2022).getOrElse(true)
      ) match {
        case (true, true, true) => options(form, valuesClaimingAll)
        case (true, true, false) => options(form, valuesWithoutPrev)
        case (true, false, true) => options(form, valuesWithout2021)
        case (false, true, true) => options(form, valuesWithout2022)
      }

      request.userAnswers.eligibilityCheckerSessionIdOpt match {
        case Some(sessionId) => eligibilityCheckerService.wfhDueToCovidStatus(sessionId).flatMap {
          case Some(wrapper) => handleSAFlow(wrapper, checkboxes)
          case None => Future.successful(handleDefaultSAFlow(checkboxes))
        }
        case None =>
          logger.info("Eligibility Checker SessionId parameter is missing from the request")
          Future.successful(handleDefaultSAFlow(checkboxes))
      }

  }

  def handleDefaultSAFlow(checkboxes: Seq[CheckboxItem])(implicit request: DataRequest[AnyContent]): Result = {
    request.userAnswers.get(HasSelfAssessmentEnrolment) match {
      case Some(true) => Redirect(routes.DisclaimerController.onPageLoad())
      case _ =>
        request.userAnswers.get(ClaimedForTaxYear2020) match {
          case Some(claimedAlready) if claimedAlready =>
            Redirect(routes.DisclaimerController.onPageLoad())

          case Some(claimedAlready) if !claimedAlready =>
            val preparedForm = request.userAnswers.get(SelectTaxYearsToClaimForPage) match {
              case None => form
              case Some(value) => form.fill(value)
            }
            Ok(view(preparedForm, checkboxes))

          case None => Redirect(routes.IndexController.onPageLoad())
        }
    }

  }

  def handleSAFlow(wfhDueToCovidStatusWrapper: WfhDueToCovidStatusWrapper, checkboxes: Seq[CheckboxItem])
                  (implicit request: DataRequest[AnyContent]): Future[Result] = {

    val optionList: Option[Set[SelectTaxYearsToClaimFor]] = wfhDueToCovidStatusWrapper.WfhDueToCovidStatus match {
      case 1 => Some(Set(SelectTaxYearsToClaimFor.Option1))
      case 2 => Some(Set(SelectTaxYearsToClaimFor.Option2))
      case 3 => Some(Set(SelectTaxYearsToClaimFor.Option1, SelectTaxYearsToClaimFor.Option2))
      case _ => None
    }

    optionList match {
      case Some(listOfOptions) =>
        for {
          updatedAnswers <- Future.fromTry(request.userAnswers.set(SelectTaxYearsToClaimForPage, listOfOptions))
          _ <- sessionRepository.set(updatedAnswers)
        } yield Redirect(navigator.nextPage(Seq(SelectTaxYearsToClaimForPage), updatedAnswers))
      case None =>
        logger.error(s"Eligibility Checker return Covid Status value: [${wfhDueToCovidStatusWrapper.WfhDueToCovidStatus}], which is undefined.]")
        Future.successful(Redirect(routes.IndexController.onPageLoad()))
    }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      val checkboxes: Seq[CheckboxItem] = (
        request.userAnswers.get(ClaimedForTaxYear2020).getOrElse(true),
        request.userAnswers.get(ClaimedForTaxYear2021).getOrElse(true),
        request.userAnswers.get(ClaimedForTaxYear2022).getOrElse(true)
      ) match {
        case (true, true, true) => options(form, valuesClaimingAll)
        case (true, true, false) => options(form, valuesWithoutPrev)
        case (true, false, true) => options(form, valuesWithout2021)
        case (false, true, true) => options(form, valuesWithout2022)
      }

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, checkboxes))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(SelectTaxYearsToClaimForPage, value))
            _ <- sessionRepository.set(updatedAnswers)
          } yield {
            Redirect(navigator.nextPage(Seq(SelectTaxYearsToClaimForPage), updatedAnswers))
          }
      )
  }
}
