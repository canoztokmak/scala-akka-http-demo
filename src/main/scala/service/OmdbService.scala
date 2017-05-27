package service

import java.io.IOException

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.config.ConfigFactory
import model.{OmdbResponse, Protocols}

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

  class OmdbService {
    private lazy val config = ConfigFactory.load()
    private lazy val apikey = config.getString("omdb.apikey")
    private lazy val host = config.getString("omdb.host")

    private lazy val outboundConnectionFlow = Http().outgoingConnection(host)

    private def omdbRequest(request: HttpRequest): Future[HttpResponse] = {
      Source.single(request).via(outboundConnectionFlow).runWith(Sink.head)
    }

    def fetchMovieTitle(imdbId: String): Future[Option[String]] = {
      val future = omdbRequest(RequestBuilding.Get(s"/?apikey=$apikey&i=$imdbId")).flatMap { response =>
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
          None
        }
      } recover {
        case e: Throwable =>
          println(s"Recovering from error while fetching movie title from OmdbService: $e")
          None
      }
    }
  }
}
