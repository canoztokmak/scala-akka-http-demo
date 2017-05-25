import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatchers, Route}
import akka.stream.ActorMaterializer
import com.typesafe.config.Config

import scala.concurrent.ExecutionContextExecutor

/**
  * Created by canoztokmak on 25/05/2017.
  */
trait MovieService extends Protocols with OmdbServiceComponent with MovieRepositoryComponent {
  // needed to run the route
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  // needed for the future map/flatmap in the end
  implicit def executor: ExecutionContextExecutor

  val omdbService = new OmdbService
  val movieRepository = new MovieRepository

  val config: Config
  val logger: LoggingAdapter

  val routes: Route = {
    logRequestResult("movie-reservation") {
      post {
        path("movies") {
          entity(as[CreateMovieRequest]) { request =>
            complete {
              omdbService.fetchMovieTitle(request.imdbId).map[ToResponseMarshallable] { title =>
                movieRepository.addMovie(
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
              movieRepository.reserveSeat(imdbId, screenId).map[ToResponseMarshallable] { updatedCount =>
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
              movieRepository.retrieveMovie(imdbId, screenId).map[ToResponseMarshallable] {
                case Some(movie) => OK -> movie
                case None => NotFound
              }
            }
          }
        }
    }
  }
}

