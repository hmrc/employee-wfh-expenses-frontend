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

package controllers.actions

import com.google.inject.Inject
import connectors.CitizenDetailsConnector
import controllers.routes
import models.requests.IdentifierRequest
import play.api.Logging
import play.api.http.Status
import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class ManualCorrespondenceIndicatorActionImpl @Inject()(
                                               citizenDetailsConnector: CitizenDetailsConnector,
                                               val parser: BodyParsers.Default
                                             )(implicit val executionContext: ExecutionContext) extends ManualCorrespondenceIndicatorAction with Logging {
  override protected def filter[A](request: IdentifierRequest[A]): Future[Option[Result]] = {
    implicit val hc = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    citizenDetailsConnector.getAddress(request.nino).map {
      response =>
        response.status match {
          case Status.OK =>
            None
          case Status.LOCKED =>
            logger.info(s"[ManualCorrespondenceIndicatorAction][filter] - Locked status code")
            Some(Redirect(routes.ManualCorrespondenceIndicatorController.onPageLoad().url))
          case statusCode =>
            logger.warn(s"[ManualCorrespondenceIndicatorAction][filter] - Unexpected status code: $statusCode ")
            Some(Redirect(routes.TechnicalDifficultiesController.onPageLoad.url))
        }
    }.recoverWith {
      case e =>
        logger.error(s"[ManualCorrespondenceIndicatorAction][filter] failed: $e")
        Future{Some(Redirect(routes.TechnicalDifficultiesController.onPageLoad.url))}
    }

  }
}

trait ManualCorrespondenceIndicatorAction extends ActionFilter[IdentifierRequest]
