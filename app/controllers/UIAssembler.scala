package controllers

import models.requests.DataRequest
import navigation.TaxYearFromUIAssembler
import pages.SelectTaxYearsToClaimForPage
import play.api.mvc.AnyContent

trait UIAssembler {

  def taxYearFromUIAssemblerFromRequest()(implicit request: DataRequest[AnyContent]): TaxYearFromUIAssembler = {
    val selectedOptionsCheckBoxes = request.userAnswers.get(SelectTaxYearsToClaimForPage).getOrElse(Nil).map(_.toString).toList
    TaxYearFromUIAssembler(selectedOptionsCheckBoxes)
  }
}
