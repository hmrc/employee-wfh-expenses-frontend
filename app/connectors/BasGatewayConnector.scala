/*
 * Copyright 2025 HM Revenue & Customs
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

  import play.api.Logging
  import sttp.model.HeaderNames.ContentType
  import sttp.model.MediaType.ApplicationJson
  import config.FrontendAppConfig
  import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
  import uk.gov.hmrc.http.client.HttpClientV2

  import javax.inject.Inject
  import scala.concurrent.{ExecutionContext, Future}
  import scala.util.Failure

  class BasGatewayConnector @Inject() (http: HttpClientV2, appConfig: FrontendAppConfig) extends Logging {

    private val EmptyJsonStr = "{}"

    def signOutUser()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
      val basGatewayFrontendBaseUrl = appConfig.basGatewayBaseUrl
      val signOutPath               = "/bas-gateway/logout-without-state"
      val fullSignOutUrl            = s"$basGatewayFrontendBaseUrl$signOutPath"

      http
        .post(url"$fullSignOutUrl")
        .setHeader(ContentType -> ApplicationJson.toString())
        .withBody(EmptyJsonStr)
        .execute[HttpResponse]
        .andThen { case Failure(error) => logger.warn(s"Received error response from bas-gateway: ${error.getMessage}") }
    }




}
