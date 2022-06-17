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

package handlers

import play.api.mvc.Results.Redirect
import controllers.routes
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Request, RequestHeader, ResponseHeader, Result}
import play.twirl.api.Html
import uk.gov.hmrc.http.TooManyRequestException
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import views.html.ErrorTemplate
import javax.inject.{Inject, Singleton}
import play.api.http.{HttpEntity, Status, Writeable}

import scala.language.implicitConversions

@Singleton
class ErrorHandler @Inject()(
                              val messagesApi: MessagesApi,
                              view: ErrorTemplate
                            ) extends FrontendErrorHandler with I18nSupport with Logging {

  private implicit def rhToRequest(rh: RequestHeader): Request[_] = Request(rh, "")

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit rh: Request[_]): Html =
    view(pageTitle, heading, message)

  override def resolveError(rh: RequestHeader, ex: Throwable): Result = {
    ex match {
      case rateLimitEx: TooManyRequestException =>
        logger.warn(s"[ErrorHandler][resolveError] Rate limiting detected, (${rh.method})(${rh.uri})", rateLimitEx )
        new Status(Status.TOO_MANY_REQUESTS)( serviceTooBusyTemplate(rh) )
      case e: Exception =>
        logger.warn(s"[ErrorHandler][resolveError] failed with: $e")
        Redirect(routes.TechnicalDifficultiesController.onPageLoad.url)
      case _ =>
        logger.error(s"[ErrorHandler][resolveError] Internal Server Error, (${rh.method})(${rh.uri})", ex)
        super.resolveError(rh, ex)
    }
  }

  def serviceTooBusyTemplate(implicit request: Request[_]): Html =
    standardErrorTemplate(
      Messages("service.is.busy.title"),
      Messages("service.is.busy.heading"),
      Messages("service.is.busy.message")
    )

  class Status(status: Int) extends Result(header = ResponseHeader(status), body = HttpEntity.NoEntity) {

    def apply[C](content: C)(implicit writeable: Writeable[C]): Result = {
      Result(
        header,
        writeable.toEntity(content)
      )
    }
  }
}
