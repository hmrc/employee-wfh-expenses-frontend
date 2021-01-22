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

package controllers

import controllers.actions._
import forms.WhenDidYouFirstStartWorkingFromHomeFormProvider
import javax.inject.Inject
import navigation.Navigator
import pages.WhenDidYouFirstStartWorkingFromHomePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.WhenDidYouFirstStartWorkingFromHomeView

import scala.concurrent.{ExecutionContext, Future}

class WhenDidYouFirstStartWorkingFromHomeController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: SessionRepository,
                                        navigator: Navigator,
                                        identify: IdentifierAction,
                                        checkAlreadyClaimed: CheckAlreadyClaimedAction,
                                        citizenDetailsCheck: ManualCorrespondenceIndicatorAction,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        formProvider: WhenDidYouFirstStartWorkingFromHomeFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: WhenDidYouFirstStartWorkingFromHomeView
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(WhenDidYouFirstStartWorkingFromHomePage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData andThen requireData).async {
    implicit request =>
      val messages = request2Messages
      form.bindFromRequest().fold(
        formWithErrors => {
          val errors = formWithErrors.errors.map(error => error.copy(args = error.args.map(arg => messages(s"date.$arg").toLowerCase)))
          Future.successful(BadRequest(view(formWithErrors.copy(errors = errors))))
        },
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(WhenDidYouFirstStartWorkingFromHomePage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(WhenDidYouFirstStartWorkingFromHomePage, updatedAnswers))
      )
  }
}
