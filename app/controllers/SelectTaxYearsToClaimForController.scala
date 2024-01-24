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

package controllers

import config.FrontendAppConfig
import controllers.actions._
import forms.SelectTaxYearsToClaimForFormProvider
import javax.inject.Inject
import models.SelectTaxYearsToClaimFor
import navigation.Navigator
import pages._
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
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
                                                    appConfig: FrontendAppConfig
                                                  )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  val form = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      request.userAnswers.get(HasSelfAssessmentEnrolment) match {
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

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val availableYearsUserCanClaim = SelectTaxYearsToClaimFor.getValuesFromClaimedBooleans(
        request.userAnswers.get(ClaimedForTaxYear2020).getOrElse(false),
        request.userAnswers.get(ClaimedForTaxYear2021).getOrElse(false),
        request.userAnswers.get(ClaimedForTaxYear2022).getOrElse(false),
        request.userAnswers.get(ClaimedForTaxYear2023).getOrElse(false)
      )

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

  def hasSingleUnclaimedYear(claimed2020: Boolean, claimed2021: Boolean, claimed2022: Boolean, claimed2023: Boolean): Boolean = {
    Seq(claimed2020, claimed2021, claimed2022, claimed2023).count(_ == false) == 1
  }

}
