package uk.gov.beis.scorecard

import com.wellfactored.playbindings.ValueClassFormats
import play.api.libs.json._
import uk.gov.beis.scorecard.loader.models._

package object loader extends ValueClassFormats {

  implicit val addressFormats = Json.format[Address]

  implicit val learnerFormats = Json.format[LearnerStats]
  implicit val qsFormats = Json.format[QualificationStats]
  implicit val earningsFormats = Json.format[Earnings]
  implicit val apprenticeshipFormats = Json.format[Apprenticeship]

  implicit val providerFormats = Json.format[Provider]
}
