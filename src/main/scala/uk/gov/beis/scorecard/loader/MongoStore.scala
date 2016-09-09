package uk.gov.beis.scorecard.loader

import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.MultiBulkWriteResult
import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}
import reactivemongo.bson.{BSONDocumentWriter, BSONDouble, BSONWriter, Macros}
import uk.gov.beis.scorecard.loader.models._

import scala.concurrent.{ExecutionContext, Future}

object MongoStore {
  val mongoUri = Option(System.getenv("MONGODB_URL")).getOrElse("mongodb://localhost:27017/apprenticeship-scorecard")

  println(mongoUri)

  import ExecutionContext.Implicits.global

  // Connect to the database: Must be done only once per application
  val driver: MongoDriver = MongoDriver()
  val uriF = Future.fromTry(MongoConnection.parseURI(mongoUri))
  val connF = uriF.map(driver.connection)

  lazy val db = for {
    uri <- uriF
    con <- connF
    dn <- Future(uri.db.getOrElse("apprenticeship-scorecard"))
    db <- con.database(dn)
  } yield db


  def providers: Future[BSONCollection] = db.map(_.collection("provider"))

  def apprenticeships: Future[BSONCollection] = db.map(_.collection("apprenticeship"))

  implicit def ukprnWriter = Macros.writer[UKPRN]

  implicit val bdWriter: BSONWriter[BigDecimal, BSONDouble] = new BSONWriter[BigDecimal, BSONDouble] {
    override def write(t: BigDecimal): BSONDouble = BSONDouble(t.doubleValue())
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

  def writeProviders(ps: List[Provider]): Future[MultiBulkWriteResult] = providers.flatMap { pColl =>
    val bulkDocs = ps.map(implicitly[pColl.ImplicitlyDocumentProducer](_))
    pColl.bulkInsert(ordered = true)(bulkDocs: _*)
  }

  def writeApprenticeships(as: List[Apprenticeship]) = apprenticeships.flatMap { aColl =>
    val bulkDocs = as.map(implicitly[aColl.ImplicitlyDocumentProducer](_))
    aColl.bulkInsert(ordered = true)(bulkDocs: _*)
  }

  def shutdown(): Future[Unit] = {
    connF.map { c =>
      c.close()
      c.actorSystem.shutdown()
    }
  }
}