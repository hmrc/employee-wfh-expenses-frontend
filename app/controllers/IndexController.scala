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
import controllers.actions.{DataRetrievalAction, IdentifierAction, ManualCorrespondenceIndicatorAction}
import models.UserAnswers
import navigation.Navigator
import pages._
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import services.{IABDService, SessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IndexController @Inject()(val controllerComponents: MessagesControllerComponents,
                                val sessionService: SessionService,
                                val navigator: Navigator,
                                val iabdService: IABDService,
                                val appConfig: FrontendAppConfig,
                                getData: DataRetrievalAction,
                                identify: IdentifierAction,
                                citizenDetailsCheck: ManualCorrespondenceIndicatorAction
                               )(implicit executionContext: ExecutionContext)
  extends FrontendBaseController with I18nSupport {

  def onPageLoad(isMergedJourney: Boolean = false): Action[AnyContent] = (identify andThen citizenDetailsCheck andThen getData).async {
    implicit request => {
      iabdService.getAlreadyClaimedStatusForAllYears(request.nino).map { claimedYears =>
        if(iabdService.allYearsClaimed(request.nino, claimedYears)) {
          Redirect(appConfig.p87DigitalFormUrl)
        } else {
          val answers = UserAnswers(
            request.internalId,
            Json.obj(
              MergedJourneyFlag.toString -> (isMergedJourney && appConfig.mergedJourneyEnabled),
              ClaimedForTaxYear2020.toString -> claimedYears._1.isDefined,
              ClaimedForTaxYear2021.toString -> claimedYears._2.isDefined,
              ClaimedForTaxYear2022.toString -> claimedYears._3.isDefined,
              ClaimedForTaxYear2023.toString -> claimedYears._4.isDefined
            )
          )

          sessionService.set(answers)

          val navigatorPageResult: Call = navigator.nextPage(ClaimedForTaxYear2020, answers)

          Redirect(navigatorPageResult)
        }
      }
    }
  }
}