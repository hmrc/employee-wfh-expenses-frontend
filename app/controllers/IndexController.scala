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

import controllers.actions.{CheckAlreadyClaimedAction, DataRetrievalAction, IdentifierAction, ManualCorrespondenceIndicatorAction}
import models.UserAnswers
import navigation.Navigator
import pages.{ClaimedForTaxYear2020, EligibilityCheckerSessionId, HasSelfAssessmentEnrolment}
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.IABDService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.TaxYearDates.YEAR_2020

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global

class IndexController @Inject()(
                                 val controllerComponents: MessagesControllerComponents,
                                 sessionRepository: SessionRepository,
                                 identify: IdentifierAction,
                                 checkAlreadyClaimed: CheckAlreadyClaimedAction,
                                 citizenDetailsCheck: ManualCorrespondenceIndicatorAction,
                                 iabdService: IABDService,
                                 navigator: Navigator,
                                 getData: DataRetrievalAction
                               ) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen citizenDetailsCheck andThen checkAlreadyClaimed andThen getData).async {
    implicit request => {
      for {
        alreadyClaimed <- iabdService.alreadyClaimed(request.nino, YEAR_2020)
      } yield {

        val eligibilityCheckerSessionIdString = request.queryString.get("eligibilityCheckerSessionId") match {
          case Some(sessionIdSeq) => sessionIdSeq.head
          case None => ""
        }

        val answers = UserAnswers(
          request.internalId,
          Json.obj(
            ClaimedForTaxYear2020.toString -> alreadyClaimed.isDefined,
            HasSelfAssessmentEnrolment.toString -> request.saUtr.isDefined,
            EligibilityCheckerSessionId.toString() -> eligibilityCheckerSessionIdString
          )
        )

        sessionRepository.set(answers)

        Redirect(navigator.nextPage(ClaimedForTaxYear2020, answers))
      }
    }

  }
}