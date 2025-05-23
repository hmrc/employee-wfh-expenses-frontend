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

import play.twirl.api.HtmlFormat
import views.ViewSpecBase

trait ViewBehaviours extends ViewSpecBase {

  def normalPage(
      view: HtmlFormat.Appendable,
      messageKeyPrefix: String,
      args: Seq[String],
      guidanceKeysWithArgs: (String, Seq[String])*
  ): Unit =

    "behave like a normal page" when {

      "rendered" must {

        "display the correct browser title" in {

          val doc = asDocument(view)
          assertEqualsValue(
            doc,
            "title",
            s"${messages(s"$messageKeyPrefix.title", args: _*)} - ${messages("service.name")} - GOV.UK"
          )
        }

        "display the correct page title" in {

          val doc = asDocument(view)
          assertPageTitleEqualsMessage(doc, s"$messageKeyPrefix.heading", args: _*)
        }

        "display the correct guidance" in {

          val doc = asDocument(view)
          for (key <- guidanceKeysWithArgs)
            assertContainsText(doc, messages(s"$messageKeyPrefix.${key._1}", key._2: _*))
        }
      }
    }

  def pageWithBackLink(view: HtmlFormat.Appendable): Unit =

    "behave like a page with a back link" must {

      "have a back link" in {

        val doc = asDocument(view)
        assertRenderedByClass(doc, "govuk-back-link")
      }
    }

}
