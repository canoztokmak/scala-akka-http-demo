import java.io.IOException

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatchers, Route}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.{MongoClient, MongoCollection}

import scala.concurrent.{ExecutionContextExecutor, Future}
import spray.json.DefaultJsonProtocol

/**
  * Created by canoztokmak on 24/05/2017.
  */

case class Movie(imdbId: String, screenId: String, movieTitle: String, availableSeats: Int, reservedSeats: Int = 0)
case class ReserveSeatRequest(imdbId: String, screenId: String)
case class CreateMovieRequest(imdbId: String, screenId: String, availableSeats: Int)

case class OmdbResponse(Response: String, Error: Option[String], Title: String, imdbID: String)

trait Protocols extends DefaultJsonProtocol {
  implicit val movieFormat = jsonFormat5(Movie)
  implicit val createMovieRequestFormat = jsonFormat3(CreateMovieRequest)
  implicit val reserveSeatRequestFormat = jsonFormat2(ReserveSeatRequest)

  implicit val omdbResponseFormat = jsonFormat4(OmdbResponse)
}

trait MovieRepository {
  object DBMovie {
    def apply(imdbId: String, screenId: String, movieTitle: String, availableSeats: Int, reservedSeats: Int): DBMovie =
      DBMovie(new ObjectId, imdbId, screenId, movieTitle, availableSeats, reservedSeats)
  }
  case class DBMovie(_id: ObjectId, imdbId: String, screenId: String, movieTitle: String, availableSeats: Int, reservedSeats: Int = 0)

  import org.mongodb.scala.model.Filters._
  import org.mongodb.scala.model.Updates._

  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }

  implicit def executor: ExecutionContextExecutor

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

trait OmdbService extends Protocols {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  // needed for the future map/flatmap in the end
  implicit def executor: ExecutionContextExecutor

  private val config = ConfigFactory.load()
  private val apikey = config.getString("omdb.apikey")
  private val host = config.getString("omdb.host")

  private def outboundConnectionFlow = Http().outgoingConnection(host)

  def apiRequest(request: HttpRequest): Future[HttpResponse] = {
    Source.single(request).via(outboundConnectionFlow).runWith(Sink.head)
  }

  def fetchMovieTitle(imdbId: String): Future[String] = {
    val future = apiRequest(RequestBuilding.Get(s"/?apikey=$apikey&i=$imdbId")).flatMap { response =>
      response.status match {
        case OK => Unmarshal(response.entity).to[OmdbResponse]
        case _ =>
          println(s"received response from OmdbApi $response")
          Future.failed(new IOException("An error occurred.."))
      }
    }

    future.map { r =>
      if (r.Response.equalsIgnoreCase("true")) {
        r.Title
      } else {
        println(s"An error occurred while fetching movie title from OmdbService: ${r.Error}")
        ""
      }
    } recover {
      case e: Throwable =>
        println(s"Recovering from error while fetching movie title from OmdbService: $e")
        ""
    }
  }
}

trait Service extends Protocols with OmdbService with MovieRepository {
  // needed to run the route
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  // needed for the future map/flatmap in the end
  implicit def executor: ExecutionContextExecutor

  val config: Config
  val logger: LoggingAdapter

  val routes: Route = {
    logRequestResult("movie-reservation") {
      post {
        path("movies") {
          entity(as[CreateMovieRequest]) { request =>
            complete {
              fetchMovieTitle(request.imdbId).map[ToResponseMarshallable] { title =>
                addMovie(
                  Movie(request.imdbId, request.screenId, title, request.availableSeats)
                ).map(_ => Created)
              }
            }
          }
        }
      } ~
      post {
        pathPrefix("movies" / PathMatchers.Segment / "screens" / PathMatchers.Segment) { (imdbId, screenId) =>
          complete {
            reserveSeat(imdbId, screenId).map[ToResponseMarshallable] { updatedCount =>
              if (updatedCount == 0) {
                NotFound
              } else {
                NoContent
              }
            }
          }
        }
      } ~
      get {
        pathPrefix("movies" / PathMatchers.Segment / "screens" / PathMatchers.Segment) { (imdbId, screenId) =>
          complete {
            retrieveMovie(imdbId, screenId).map[ToResponseMarshallable] {
              case Some(movie) => OK -> movie
              case None => NotFound
            }
          }
        }
      }
    }
  }
}

object Application extends App with Service {
  override implicit val system = ActorSystem()
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()

  override val config = ConfigFactory.load()
  override val logger = Logging(system, getClass)

  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
}
