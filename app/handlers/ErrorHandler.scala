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

package handlers

import controllers.routes
import play.api.Logging
import play.api.http.{HttpEntity, Writeable}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.Redirect
import play.api.mvc.{RequestHeader, ResponseHeader, Result}
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import views.html.ErrorTemplate

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ErrorHandler @Inject() (
    val messagesApi: MessagesApi,
    view: ErrorTemplate
)(implicit val ec: ExecutionContext)
    extends FrontendErrorHandler
    with I18nSupport
    with Logging {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(
      implicit rh: RequestHeader
  ): Future[Html] =
    Future.successful(view(pageTitle, heading, message))

  override def resolveError(rh: RequestHeader, ex: Throwable): Future[Result] =
    ex match {
      case e: Exception =>
        logger.warn(s"[ErrorHandler][resolveError] failed with: $e")
        Future.successful(Redirect(routes.TechnicalDifficultiesController.onPageLoad.url))
      case _ =>
        logger.error(s"[ErrorHandler][resolveError] Internal Server Error, (${rh.method})(${rh.uri})", ex)
        super.resolveError(rh, ex)
    }

  class Status(status: Int) extends Result(header = ResponseHeader(status), body = HttpEntity.NoEntity) {

    def apply[C](content: C)(implicit writeable: Writeable[C]): Result =
      Result(
        header,
        writeable.toEntity(content)
      )

  }

}
