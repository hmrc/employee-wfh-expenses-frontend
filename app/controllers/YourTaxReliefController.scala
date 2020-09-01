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

import controllers.actions._
import javax.inject.Inject
import pages.WhenDidYouFirstStartWorkingFromHomePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import views.html.YourTaxReliefView

import scala.concurrent.ExecutionContext

class YourTaxReliefController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       identify: IdentifierAction,
                                       checkAlreadyClaimed: CheckAlreadyClaimedAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: YourTaxReliefView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen checkAlreadyClaimed andThen getData andThen requireData) {
    implicit request =>
      request.userAnswers.get(WhenDidYouFirstStartWorkingFromHomePage) match {
        case None =>
          Redirect(routes.WhenDidYouFirstStartWorkingFromHomeController.onPageLoad())
        case Some(startedWorkingFromHomeDate) =>
          Ok(view(startedWorkingFromHomeDate))
      }
  }

  def onSubmit(): Action[AnyContent] = Action {
    implicit request =>
      Redirect(routes.ConfirmationController.onPageLoad())
  }

  }
