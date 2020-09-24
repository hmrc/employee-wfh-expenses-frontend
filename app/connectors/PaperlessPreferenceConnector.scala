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

package connectors

import com.google.inject.Inject
import config.FrontendAppConfig
import javax.inject.Singleton
import play.api.Logger
import play.api.http.Status.OK
import play.api.http.Status.NO_CONTENT
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.partials.HeaderCarrierForPartialsConverter
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaperlessPreferenceConnector @Inject()(appConfig: FrontendAppConfig, httpClient: HttpClient)
  extends HeaderCarrierForPartialsConverter {

  override def crypto: String => String = cookie => cookie

  /**
   * A 200 http response expected if user/data found.
   */
  private def responseHandler(response: HttpResponse): Option[Boolean] = {
    response.status match {
      case OK => Some(true)
      case NO_CONTENT => Some(false)
      case _ => None
    }
  }

  def getPaperlessPreference()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Boolean]] = {

    val preferencesUrl = s"${appConfig.preferencesFrontendHost}/paperless/preferences"

    httpClient.GET[HttpResponse](preferencesUrl).map(responseHandler).recover {
      case _: BadRequestException =>
        // Expected from service if a Preference is not found. No need for logging.
        None
      case ex: Throwable =>
        Logger.error(s"PaperlessPreferenceConnector: Unexpected Error during REST call: ${ex.getCause}")
        None
    }
  }
}