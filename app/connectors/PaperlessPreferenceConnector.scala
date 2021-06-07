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

package connectors

import com.google.inject.{ImplementedBy, Inject}
import config.FrontendAppConfig
import models.paperless.PaperlessStatusResponse
import play.api.Logging
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.play.partials.HeaderCarrierForPartialsConverter
import views.html.helper.urlEncode

import scala.concurrent.{ExecutionContext, Future}

class PaperlessPreferenceConnectorImpl @Inject()(
                                                  appConfig: FrontendAppConfig,
                                                  httpClient: HttpClient,
                                                  applicationCrypto:  ApplicationCrypto,
                                                )
  extends PaperlessPreferenceConnector with HeaderCarrierForPartialsConverter with Logging {

  def crypto: String => String = cookie => cookie

  def getPaperlessStatus(returnUrl: String)(implicit request: Request[AnyContent], ec: ExecutionContext): Future[Either[String, PaperlessStatusResponse]] = {
    val paperlessStatusUrl =
      s"${appConfig.preferencesFrontendHost}/paperless/status" +
        s"?returnUrl=${ encryptAndEncode( returnUrl ) }" +
        s"&returnLinkText=${ encryptAndEncode( "Continue" ) }"

    logger.debug(s"[PaperlessPreferenceConnector][getPaperlessPreference] $paperlessStatusUrl")

    httpClient.GET[PaperlessStatusResponse](paperlessStatusUrl) map { Right(_) } recoverWith {
      case ex: Exception =>
        logger.error(s"[PaperlessPreferenceConnector][getPaperlessStatus] Failed with: ${ex.getMessage}")
        Future.successful(Left(ex.getMessage))
    }
  }

  private def encryptAndEncode(s: String): String = urlEncode( applicationCrypto.QueryParameterCrypto.encrypt( PlainText(s) ).value )
}

@ImplementedBy(classOf[PaperlessPreferenceConnectorImpl])
trait PaperlessPreferenceConnector {
  def getPaperlessStatus(returnUrl: String)(implicit request: Request[AnyContent], ec: ExecutionContext): Future[Either[String, PaperlessStatusResponse]]
}




