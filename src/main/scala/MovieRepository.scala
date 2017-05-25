import akka.event.LoggingAdapter
import com.typesafe.config.ConfigFactory
import org.mongodb.scala.{MongoClient, MongoCollection}
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates._

import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.bson.codecs.configuration.CodecRegistries.{fromRegistries, fromProviders}

import scala.concurrent.{ExecutionContextExecutor, Future}

/**
  * Created by canoztokmak on 25/05/2017.
  */
trait MovieRepositoryComponent {
  implicit def executor: ExecutionContextExecutor

  val movieRepository: MovieRepository

  val logger: LoggingAdapter

  class MovieRepository {
    private object DBMovie {
      def apply(imdbId: String, screenId: String, movieTitle: String, availableSeats: Int, reservedSeats: Int): DBMovie =
        DBMovie(new ObjectId, imdbId, screenId, movieTitle, availableSeats, reservedSeats)
    }
    private case class DBMovie(_id: ObjectId, imdbId: String, screenId: String, movieTitle: String, availableSeats: Int, reservedSeats: Int = 0)

    private val config = ConfigFactory.load()
    private val host = config.getString("mongodb.host")
    private val port = config.getString("mongodb.port")

    private lazy val generateMongoConnectionString: String = s"mongodb://$host:$port"
    private lazy val mongoClient = MongoClient(generateMongoConnectionString)

    private lazy val codecRegistry = fromRegistries(fromProviders(classOf[DBMovie]), DEFAULT_CODEC_REGISTRY)
    private lazy val db = mongoClient.getDatabase("movie-reservation").withCodecRegistry(codecRegistry)

    private lazy val collection: MongoCollection[DBMovie] = db.getCollection("movies")

    def addMovie(movie: Movie): Future[_] = {
      collection.insertOne(
        DBMovie(movie.imdbId, movie.screenId, movie.movieTitle, movie.availableSeats, movie.reservedSeats)
      ).toFuture
    }

    def reserveSeat(imdbId: String, screenId: String): Future[Long] = {
      collection.updateOne(
        and(
          equal("imdbId", imdbId),
          equal("screenId", screenId)
        ),
        inc("reservedSeats", 1)
      ).toFuture map {
        s => s.getModifiedCount
      }
    }

    def retrieveMovie(imdbId: String, screenId: String): Future[Option[Movie]] = {
      collection.find(
        and(
          equal("imdbId", imdbId),
          equal("screenId", screenId)
        )
      ).first().toFuture.map(doc =>
        if (doc == null) {
          None
        } else {
          Some(Movie(doc.imdbId, doc.screenId, doc.movieTitle, doc.availableSeats, doc.reservedSeats))
        }
      )
    }
  }
}
