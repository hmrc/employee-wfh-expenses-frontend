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
import models.{Mode, NormalMode, UserAnswers}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import views.html.DisclaimerView

import scala.concurrent.{ExecutionContext, Future}

class DisclaimerController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       sessionRepository: SessionRepository,
                                       identify: IdentifierAction,
                                       checkAlreadyClaimed: CheckAlreadyClaimedAction,
                                       citizenDetailsCheck: ManualCorrespondenceIndicatorAction,
                                       getData: DataRetrievalAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: DisclaimerView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen citizenDetailsCheck andThen checkAlreadyClaimed andThen getData) {
    implicit request =>

      if (request.userAnswers.isEmpty) {
        sessionRepository.set(UserAnswers(request.internalId))
      }

      Ok(view())
  }

  def onSubmit(): Action[AnyContent] = (identify andThen citizenDetailsCheck andThen checkAlreadyClaimed andThen getData) {
    implicit request =>
      Redirect(routes.WhenDidYouFirstStartWorkingFromHomeController.onPageLoad())
  }
}
