package uk.gov.beis.scorecard.loader

import com.ning.http.client.Response
import dispatch._
import play.api.libs.json.{JsNumber, JsValue, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

case class GeoPoint(lat: BigDecimal, lon: BigDecimal)


object Postcodes {

  val baseUrl = "http://localhost:8000"

  implicit class StringSyntax(s: String) {
    def stripSpaces = s.replaceAll("\\s", "")
  }

  def location(postcode: String): Future[Option[GeoPoint]] = {
    val req: Req = url(s"$baseUrl/postcodes/${postcode.stripSpaces}").GET

    val geoExtractor: Response => Option[GeoPoint] = { response =>
      response.getStatusCode match {
        case 200 =>

          Try(Json.parse(response.getResponseBody)).toOption.flatMap { js =>
            for {
              lat <- extractNumber(js, "latitude")
              lon <- extractNumber(js, "longitude")
            } yield GeoPoint(lat, lon)
          }
        case s => println(s"got status $s for $postcode"); None
      }
    }

    Http(req > geoExtractor)
  }

  def extractNumber(js: JsValue, fieldName: String): Option[BigDecimal] = {
    (js \\ fieldName).headOption.flatMap {
      case JsNumber(n) => Some(n)
      case _ => None
    }
  }
}
