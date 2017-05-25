import java.io.IOException

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContextExecutor, Future}

/**
  * Created by canoztokmak on 25/05/2017.
  */
trait OmdbServiceComponent extends Protocols {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  // needed for the future map/flatmap in the end
  implicit def executor: ExecutionContextExecutor

  val omdbService: OmdbService
  val logger: LoggingAdapter

  class OmdbService {
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
            logger.debug(s"received response from OmdbApi $response")
            Future.failed(new IOException("An error occurred.."))
        }
      }

      future.map { r =>
        if (r.Response.equalsIgnoreCase("true")) {
          r.Title
        } else {
          logger.error(s"An error occurred while fetching movie title from OmdbService: ${r.Error}")
          ""
        }
      } recover {
        case e: Throwable =>
          logger.error(s"Recovering from error while fetching movie title from OmdbService: $e")
          ""
      }
    }
  }
}
