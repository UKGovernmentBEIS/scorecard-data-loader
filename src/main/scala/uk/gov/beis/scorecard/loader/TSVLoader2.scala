package uk.gov.beis.scorecard.loader

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import play.api.libs.json.Json
import uk.gov.beis.scorecard.loader.models.{Apprenticeship, Provider}

import scala.io.Source

case class DataSet()



object TSVLoader2 {

  val fileName = "data/Scorecard_Data_v5.tsv"


  def main(args: Array[String]): Unit = {
    val data = loadFromSource(Source.fromFile(fileName))

  }

  def loadFromSource(source: Source): DataSet = {
    val lines = source.getLines.toList
    val ds = lines.headOption.map(_.split("\t").toList) match {
      case None => DataSet()
      case Some(colNames) => processData(lines, colNames)
    }

    source.close()

    ds
  }

  def processData(lines: List[String], colNames: List[String]): DataSet = {
    val results = parseRecords(lines, colNames)

    val (providers, apprenticeships) = results.collect { case Valid(p) => p }.unzip

    val providerMap = providers.groupBy(_.ukprn).flatMap {
      case (k, v :: vs) => Some((k, v))
      case _ => None
    }


    val errs = results.collect { case Invalid(es) => es }.flatten

    ???
  }


  def parseRecords(lines: List[String], colNames: List[String]): List[Validated[List[String], (Provider, Apprenticeship)]] = {
    val os = lines.tail.zipWithIndex.map {
      case (record, idx) =>
        val fieldValues = record.split("\t").toList
        val fields = colNames.zip(fieldValues).toMap

        RecordExtractor2.extract(fields)
    }

    os.headOption.foreach {
      case Valid(_) => // do nothing
      case Invalid(errs) =>
        val es = errs.head
        println(Json.prettyPrint(es.json))
        es.errs.foreach(println)
    }

    List()

  }
}
