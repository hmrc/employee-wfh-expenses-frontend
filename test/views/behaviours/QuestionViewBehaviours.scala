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

package views.behaviours

import play.api.data.{Form, FormError}
import play.twirl.api.HtmlFormat

trait QuestionViewBehaviours[A] extends ViewBehaviours {

  val errorKey     = "value"
  val errorMessage = "error.required"

  def error(messageKeyPrefix: String, args: Seq[String] = Nil) =
    FormError(errorKey, s"$messageKeyPrefix.$errorMessage", args)

  val form: Form[A]

  def pageWithTextFields(
      form: Form[A],
      createView: Form[A] => HtmlFormat.Appendable,
      messageKeyPrefix: String,
      expectedFormAction: String,
      args: Seq[String],
      fields: String*
  ) =

    "behave like a question page" when {

      "rendered" must {

        for (field <- fields)

          s"contain an input for $field" in {
            val doc = asDocument(createView(form))
            assertRenderedById(doc, field)
          }

        "not render an error summary" in {

          val doc = asDocument(createView(form))
          assertNotRenderedByClass(doc, "govuk-error-summary__title")
        }
      }

      "rendered with any error" must {

        "show an error prefix in the browser title" in {

          val doc   = asDocument(createView(form.withError(error(messageKeyPrefix))))
          val title = s"${messages(s"$messageKeyPrefix.title", args: _*)} - ${messages("service.name")} - GOV.UK"
          assertEqualsValue(doc, "title", s"""${messages("error.browser.title.prefix")} $title""")
        }
      }

      for (field <- fields)

        s"rendered with an error with field '$field'" must {

          "show an error summary" in {

            val doc = asDocument(createView(form.withError(FormError(field, "error"))))
            assertRenderedByClass(doc, "govuk-error-summary__title")
          }

          s"show an error associated with the field '$field'" in {

            val doc       = asDocument(createView(form.withError(FormError(field, "error"))))
            val errorSpan = doc.getElementsByClass("error-message").first
            doc.getElementById(field).attr("aria-describedby") contains errorSpan.attr("id")
            errorSpan.parent.attr("for") mustBe field
          }
        }
    }

}
