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

import config.FrontendAppConfig
import javax.inject.{Inject, Singleton}
import utils.HttpResponseHelper
import models.{ETag, OtherExpense}
import play.api.Logger
import play.api.http.Status.{OK, isSuccessful}
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json, Reads}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

@Singleton
class TaiConnector @Inject()(appConfig: FrontendAppConfig, httpClient: HttpClient) extends HttpResponseHelper {

  private def withDefaultToEmptySeq[T: ClassTag](response: HttpResponse)
                                        (implicit reads: Reads[Seq[T]]): Seq[T] = {
    response.status match {
      case OK =>
        Json.parse(response.body).validate[Seq[T]] match {
          case JsSuccess(records, _) =>
            records
          case JsError(e) =>
            val typeName: String = implicitly[ClassTag[T]].runtimeClass.getCanonicalName
            Logger.error(s"[TaiConnector][$typeName][Json.parse] failed $e")
            Seq.empty
        }
      case _ =>
        Seq.empty
    }
  }

  def getIabdData(nino: String, year: Int)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[OtherExpense]] = {
    val taiUrl = s"${appConfig.taiHost}/tai/$nino/tax-account/$year/expenses/employee-expenses/${appConfig.otherExpensesId}"
    httpClient.GET(taiUrl).map(withDefaultToEmptySeq[OtherExpense])
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
