package uk.gov.beis.scorecard.loader

import cats.data.Validated.Invalid
import cats.data.{NonEmptyList, ValidatedNel}
import cats.syntax.cartesian._
import cats.syntax.validated._
import play.api.libs.json._
import uk.gov.beis.scorecard.loader.models.{Apprenticeship, Provider}

case class ExtractErrors(errs: List[String], json: JsValue)

object RecordExtractor2 {


  def extract(fields: Map[String, String]): ValidatedNel[ExtractErrors, (Provider, Apprenticeship)] = {
    val providerJson = extract(fields, FieldMappings.providerMappings)
    val apprenticeshipJson = extract(fields, FieldMappings.apprenticeshipMappings)

    val provider = extractResult[Provider](providerJson)
    val apprenticeship = extractResult[Apprenticeship](apprenticeshipJson)

    (provider |@| apprenticeship).tupled
  }

  def extractResult[A: Reads](js: JsValue): ValidatedNel[ExtractErrors, A] = {
    js.validate[A] match {
      case JsSuccess(a, _) => a.valid
      case JsError(errs) =>
        val errStrings: List[String] = errs.flatMap {
          case (path, es) => es.map(e => s"${path.toString()}: ${e.message}")
        }.toList
        val errNel: NonEmptyList[String] = NonEmptyList.fromList(errStrings).getOrElse(NonEmptyList.of("no errors!"))
        Invalid(NonEmptyList.of(ExtractErrors(errNel.toList, js)))
    }
  }

  def extract(fields: Map[String, String], mappings: Seq[(Binding, String)]): JsObject = {
    val fs: Seq[Option[(String, JsValue)]] = mappings.map {
      case (StringBinding(fieldName), jsonFieldName) =>
        fields.get(fieldName).filter(nonBlank).map { value =>
          (jsonFieldName, JsString(value))
        }
      case (NumberBinding(fieldName), jsonFieldName) =>
        fields.get(fieldName).filter(nonBlank).map { value =>
          (jsonFieldName, JsNumber(BigDecimal(value)))
        }
      case (StructureBinding(ms), jsonFieldName) => Some((jsonFieldName, extract(fields, ms)))
    }

    JsObject(fs.flatten)
  }

  def nonBlank(s: String): Boolean = s.trim != ""
}
