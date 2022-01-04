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

package connectors

import config.FrontendAppConfig
import models.{ETag, IABDExpense}
import play.api.http.Status.isSuccessful
import play.api.libs.json._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaiConnector @Inject()(appConfig: FrontendAppConfig, httpClient: HttpClient) {

  def getOtherExpensesData(nino: String, year: Int)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[IABDExpense]] = {
    getIabdData(nino, year, appConfig.otherExpensesId)
  }

  def getJobExpensesData(nino: String, year: Int)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[IABDExpense]] = {
    getIabdData(nino, year, appConfig.jobExpenseId)
  }

  private def getIabdData(nino: String, year: Int, iabd: Int)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[IABDExpense]] = {
    val taiUrl = s"${appConfig.taiHost}/tai/$nino/tax-account/$year/expenses/employee-expenses/$iabd"
    httpClient.GET[Seq[IABDExpense]](taiUrl)
  }

  def postIabdData(nino: String, year: Int, grossAmount: Int, eTag: ETag)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    val taiUrl = s"${appConfig.taiHost}/tai/$nino/tax-account/$year/expenses/working-from-home-employee-expenses/${appConfig.otherExpensesId}"
    val body = Json.obj("version" -> eTag.version, "grossAmount" -> grossAmount)
    httpClient.POST[JsValue, HttpResponse](taiUrl, body) map {
      response => response.status match {
        case code if isSuccessful(code) => ()
        case code => throw UpstreamErrorResponse.apply(response.body, code)
      }
    }
  }
}
