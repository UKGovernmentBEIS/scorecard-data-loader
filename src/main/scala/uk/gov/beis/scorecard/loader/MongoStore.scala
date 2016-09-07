package uk.gov.beis.scorecard.loader

import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}
import reactivemongo.bson.{BSONDocument, BSONDocumentWriter, BSONString, BSONWriter, Macros}
import uk.gov.beis.scorecard.loader.models._

import scala.concurrent.{ExecutionContext, Future}

object MongoStore {
  val mongoUri = "mongodb://localhost:27017/apprenticeship-scorecard"

  import ExecutionContext.Implicits.global

  // Connect to the database: Must be done only once per application
  val driver = MongoDriver()
  val parsedUri = MongoConnection.parseURI(mongoUri)
  val connection = parsedUri.map(driver.connection)

  // Database and collections: Get references
  val futureConnection = Future.fromTry(connection)

  def db: Future[DefaultDB] = futureConnection.flatMap(_.database("apprenticeship-scorecard"))

  def providers: Future[BSONCollection] = db.map(_.collection("provider"))

  def apprenticeships: Future[BSONCollection] = db.map(_.collection("apprenticeship"))

  implicit def ukprnWriter = Macros.writer[UKPRN]

  implicit val bdWriter: BSONWriter[BigDecimal, BSONString] = new BSONWriter[BigDecimal, BSONString] {
    override def write(t: BigDecimal): BSONString = BSONString(t.toString)
  }

  implicit def addressWriter = Macros.writer[Address]

  implicit def scWriter = Macros.writer[SubjectCode]

  implicit def lstatsWriter = Macros.writer[LearnerStats]

  implicit def qstatsWriter = Macros.writer[QualificationStats]

  implicit def earningsWriter = Macros.writer[Earnings]

  implicit def providerWriter: BSONDocumentWriter[Provider] = Macros.writer[Provider]

  implicit def apprenticeshipWriter: BSONDocumentWriter[Apprenticeship] = Macros.writer[Apprenticeship]

  def dropProviders(): Future[Boolean] = providers.flatMap(_.drop(failIfNotFound = false))

  def dropApprenticeships(): Future[Boolean] = apprenticeships.flatMap(_.drop(failIfNotFound = false))

  def writeProvider(p: Provider): Future[Unit] = providers.flatMap(_.insert(p).map(_ => ()))

  def writeApprenticeship(a: Apprenticeship): Future[Unit] = apprenticeships.flatMap(_.insert(a).map(_ => ()))

  def writeProviders(ps: List[Provider]) = ps.foldLeft(Future.successful(())) { (f, p) =>
    f.flatMap(_ => MongoStore.writeProvider(p))
  }

  def writeApprenticeships(as: List[Apprenticeship]) = as.foldLeft(Future.successful(())) { (f, a) =>
    f.flatMap(_ => MongoStore.writeApprenticeship(a))
  }


  def shutdown(): Future[Unit] = {
    futureConnection.map { c =>
      c.close()
      c.actorSystem.shutdown()
    }
  }

}
