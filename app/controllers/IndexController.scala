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

import controllers.actions.{CheckAlreadyClaimedAction, DataRetrievalAction, IdentifierAction, ManualCorrespondenceIndicatorAction}
import models.UserAnswers
import models.requests.OptionalDataRequest
import navigation.Navigator
import pages._
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.Results.Redirect
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.IABDService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.TaxYearDates.{YEAR_2020, YEAR_2021, YEAR_2022}

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

trait TaiLookupHandler extends Logging {

  val iabdService: IABDService
  val navigator: Navigator
  val sessionRepository: SessionRepository

  def handlePageRequest(successHandler: (Boolean, Boolean, Boolean) => Result)
                       (implicit dataRequest: OptionalDataRequest[AnyContent], hc: HeaderCarrier): Future[Result] = {


    val alreadyClaimed2020Future = iabdService.alreadyClaimed(dataRequest.nino, YEAR_2020)
    val alreadyClaimed2021Future = iabdService.alreadyClaimed(dataRequest.nino, YEAR_2021)
    val alreadyClaimed2022Future = iabdService.alreadyClaimed(dataRequest.nino, YEAR_2022)

    for {
      alreadyClaimed2020 <- alreadyClaimed2020Future
      alreadyClaimed2021 <- alreadyClaimed2021Future
      alreadyClaimed2022 <- alreadyClaimed2022Future
    } yield {
      successHandler(alreadyClaimed2020.isDefined,
        alreadyClaimed2021.isDefined, alreadyClaimed2022.isDefined)
    }
  }.recoverWith {
    case ex: Exception =>
      val message = s"TaiIndexLookupService lookup failed with: ${ex.getMessage}"
      logger.error(message)
      Future.failed(new RuntimeException(message))
  }

  def taiLookupSuccessHandler(alreadyClaimed2020: Boolean,
                              alreadyClaimed2021: Boolean,
                              alreadyClaimed2022: Boolean)
                             (implicit request: OptionalDataRequest[AnyContent]): Result = {

    val eligibilityCheckerSessionIdString = request.queryString.get("eligibilityCheckerSessionId") match {
      case Some(sessionIdSeq) => sessionIdSeq.head
      case None => ""
    }

    val answers = UserAnswers(
      request.internalId,
      Json.obj(
        ClaimedForTaxYear2020.toString -> alreadyClaimed2020,
        ClaimedForTaxYear2021.toString -> alreadyClaimed2021,
        ClaimedForTaxYear2022.toString -> alreadyClaimed2022,
        HasSelfAssessmentEnrolment.toString -> request.saUtr.isDefined,
        EligibilityCheckerSessionId.toString() -> eligibilityCheckerSessionIdString
      )
    )

    sessionRepository.set(answers)

    val navigatorPageResult: Call = navigator.nextPage(ClaimedForTaxYear2020, answers)

    Redirect(navigatorPageResult)
  }
}

class IndexController @Inject()(
                                 val controllerComponents: MessagesControllerComponents,
                                 sessionRepositoryInput: SessionRepository,
                                 identify: IdentifierAction,
                                 checkAlreadyClaimed: CheckAlreadyClaimedAction,
                                 citizenDetailsCheck: ManualCorrespondenceIndicatorAction,
                                 navigatorInput: Navigator,
                                 getData: DataRetrievalAction,
                                 iabdServiceInput: IABDService)(implicit ec: ExecutionContext) extends FrontendBaseController
  with TaiLookupHandler with I18nSupport {

  val iabdService = iabdServiceInput
  val navigator = navigatorInput
  val sessionRepository = sessionRepositoryInput

  def onPageLoad(): Action[AnyContent] = (identify andThen citizenDetailsCheck andThen checkAlreadyClaimed andThen getData).async {
    implicit request => {
      handlePageRequest(taiLookupSuccessHandler)
    }
  }

}