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

package connectors

import com.google.inject.Inject
import config.FrontendAppConfig
import models.ETag
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CitizenDetailsConnector @Inject()(appConfig: FrontendAppConfig, httpClient: HttpClientV2) {

  def getAddress(nino: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val designatoryDetailsUrl: String = s"${appConfig.citizenDetailsHost}/citizen-details/$nino/designatory-details"
    httpClient
      .get(url"$designatoryDetailsUrl")
      .execute[HttpResponse]
      .flatMap{ response =>
        Future.successful(response)
      }
  }

  def getETag(nino: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ETag] = {
    val eTagUrl: String = s"${appConfig.citizenDetailsHost}/citizen-details/$nino/etag"
    httpClient
      .get(url"$eTagUrl")
      .execute[ETag]
      .flatMap { response =>
        Future.successful(response)
      }
  }

}
