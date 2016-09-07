package uk.gov.beis.scorecard.loader

import cats.data.NonEmptyList
import cats.data.Validated.Invalid
import cats.syntax.validated._
import play.api.libs.json._
import uk.gov.beis.scorecard.loader.FieldMappings._

object RecordExtractor {

  def extract[A: Reads](fields: Map[String, String], mappings: FieldMappings): ExtractionResult[A] = extract[A](mapToJson(fields, mappings))

  def extract[A: Reads](js: JsObject): ExtractionResult[A] = {
    js.validate[A] match {
      case JsSuccess(a, _) => ExtractionResult(js, a.valid)
      case JsError(errs) =>
        val errStrings: List[String] = errs.flatMap {
          case (path, es) => es.map(e => s"${path.toString()}: ${e.message}")
        }.toList
        val errNel: NonEmptyList[String] = NonEmptyList.fromList(errStrings).getOrElse(NonEmptyList.of("no errors!"))
        ExtractionResult(js, Invalid(errNel))
    }
  }

  def mapToJson(fields: Map[String, String], mappings: Seq[(Binding, String)]): JsObject = {
    val fs = mappings.flatMap {
      case (StringBinding(fieldName), jsonFieldName) =>
        fields.get(fieldName).filter(nonBlank).map(s => (jsonFieldName, JsString(s)))
      case (NumberBinding(fieldName), jsonFieldName) =>
        fields.get(fieldName).filter(nonBlank).map(s => (jsonFieldName, JsNumber(BigDecimal(s))))
      case (StructureBinding(ms), jsonFieldName) => Some((jsonFieldName, mapToJson(fields, ms)))
    }

    JsObject(fs)
  }

  def nonBlank(s: String): Boolean = s.trim != ""
}
