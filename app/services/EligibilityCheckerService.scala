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

package services

import connectors.EligibilityCheckerConnector
import models.WfhDueToCovidStatusWrapper
import models.requests.DataRequest
import play.api.Logging
import play.api.mvc.AnyContent
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EligibilityCheckerService @Inject()(eligibilityCheckerConnector: EligibilityCheckerConnector) extends Logging {

  def wfhDueToCovidStatus(sessionId: String)(implicit hc: HeaderCarrier,
                                             ec: ExecutionContext): Future[Option[WfhDueToCovidStatusWrapper]] = {

    eligibilityCheckerConnector.wfhDueToCovidStatus(sessionId).map {
      response => Some(response)
    }.recoverWith {
      case e =>
        logger.error(s"[EligibilityCheckerService] failed: $e")
        Future(None)
    }
  }
}
