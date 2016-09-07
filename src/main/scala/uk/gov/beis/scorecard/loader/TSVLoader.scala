package uk.gov.beis.scorecard.loader

import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyList, Validated, ValidatedNel}
import cats.syntax.validated._
import play.api.libs.json.JsObject
import uk.gov.beis.scorecard.loader.models.{Apprenticeship, Provider}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

case class ExtractionResult[T](json: JsObject, value: ValidatedNel[String, T])

case class ExtractionErrors[T](json: JsObject, errors: NonEmptyList[String])

case class LineResult(lineNumber: Int,
                      fields: Seq[(String, String)],
                      providerResult: ExtractionResult[Provider],
                      apprenticeshipResult: ExtractionResult[Apprenticeship])

case class LineErrors(lineNumber: Int,
                      fields: Seq[(String, String)],
                      providerErrors: Option[ExtractionErrors[Provider]],
                      apprenticeshipErrors: Option[ExtractionErrors[Apprenticeship]])

case class ProcessingResults(providers: List[Provider], apprenticeships: List[Apprenticeship], lineErrors: List[LineErrors], fileErrors: List[String])

object TSVLoader {

  val fileName = "data/Scorecard_Data_v5.tsv"

  def main(args: Array[String]): Unit = {
    val data: ProcessingResults = loadFromSource(Source.fromFile(fileName))

    println(s"Loaded ${data.providers.length} providers and ${data.apprenticeships.length} apprenticeships")

    data.lineErrors.foreach { le =>
      le.providerErrors.foreach { e =>
        e.errors.toList.foreach(s => println(s"line ${le.lineNumber}:      provider: $s"))
      }
      le.apprenticeshipErrors.foreach { e =>
        e.errors.toList.foreach(s => println(s"line ${le.lineNumber}:apprenticeship: $s"))
      }
    }

    for {
      _ <- MongoStore.dropApprenticeships()
      _ <- MongoStore.dropProviders()
      _ <- MongoStore.writeProviders(data.providers)
      _ <- MongoStore.writeApprenticeships(data.apprenticeships)
    } yield MongoStore.shutdown()
  }

  def loadFromSource(source: Source): ProcessingResults = {
    val lines = source.getLines.toList
    val results = lines.headOption.map(_.split("\t").toList) match {
      case None => List()
      case Some(colNames) => parseRecords(lines, colNames)
    }
    source.close()

    processResults(results)
  }

  /**
    * Now we have a list of results for each input line we want to identify the valid lines and
    * prepare the Providers and Apprenticeships for uploading to Mongo. We also need to identify
    * the lines with errors and prepare them for reporting.
    */
  def processResults(results: List[LineResult]): ProcessingResults = {
    val (providers, apprenticeships) = results.collect {
      case (LineResult(_, _, ExtractionResult(_, Valid(p)), ExtractionResult(_, Valid(a)))) => (p, a)
    }.unzip

    val uniqueProviders: List[Validated[String, Provider]] = providers.groupBy(_.ukprn).map {
      case (k, vs) if vs.distinct.length == 1 => vs.head.valid
      case (k, vs) => s"more than one provider was present with UKPRN of $k".invalid
    }.toList

    val validProviders = uniqueProviders.collect { case Valid(p) => p }
    val fileErrors = uniqueProviders.collect { case Invalid(s) => s }

    val lineErrors = results.flatMap { lr =>
      (errors(lr.providerResult), errors(lr.apprenticeshipResult)) match {
        case (None, None) => None
        case (p, a) => Some(LineErrors(lr.lineNumber, lr.fields, p, a))
      }
    }

    ProcessingResults(validProviders, apprenticeships, lineErrors, fileErrors)
  }

  def errors[A](result: ExtractionResult[A]): Option[ExtractionErrors[A]] = result.value match {
    case Invalid(es) => Some(ExtractionErrors[A](result.json, es))
    case _ => None
  }

  def parseRecords(lines: List[String], colNames: List[String]): List[LineResult] = {
    lines.tail.zipWithIndex.map {
      case (record, idx) =>
        val fieldValues = record.split("\t").toList
        val fields = colNames.zip(fieldValues)

        val providerResult = RecordExtractor.extract[Provider](fields.toMap, FieldMappings.providerMappings)
        val apprenticeshipResult = RecordExtractor.extract[Apprenticeship](fields.toMap, FieldMappings.apprenticeshipMappings)

        LineResult(idx, fields, providerResult, apprenticeshipResult)
    }
  }
}
