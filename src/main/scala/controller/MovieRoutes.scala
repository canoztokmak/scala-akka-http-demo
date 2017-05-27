package controller

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatchers, Route}
import akka.stream.ActorMaterializer
import model.{CreateMovieRequest, Movie, Protocols}
import repository.MovieRepositoryComponent
import service.OmdbServiceComponent

import scala.concurrent.ExecutionContextExecutor

/**
  * Created by canoztokmak on 25/05/2017.
  */
abstract class MovieRoutes extends Protocols with OmdbServiceComponent with MovieRepositoryComponent {
  // needed to run the route
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  // needed for the future map/flatmap in the end
  implicit def executor: ExecutionContextExecutor

  val omdbService = new OmdbService
  val movieRepository = new MovieRepository

  val routes: Route = {
    logRequestResult("movie-reservation") {
      path("movies") {
        post {
          entity(as[CreateMovieRequest]) { request =>
            complete {
              omdbService.fetchMovieTitle(request.imdbId).map[ToResponseMarshallable] {
                case Some(title) =>
                  movieRepository.addMovie(
                    Movie(request.imdbId, request.screenId, title, request.availableSeats)
                  ).map(_ => Created)

                case None =>
                  NotFound
              }
            }
          }
        }
      } ~
        pathPrefix("movies" / PathMatchers.Segment / "screens" / PathMatchers.Segment) { (imdbId, screenId) =>
          post {
            complete {
              movieRepository.reserveSeat(imdbId, screenId).map[ToResponseMarshallable] { updatedCount =>
                if (updatedCount == 0) {
                  NotFound
                } else {
                  NoContent
                }
              }
            }
          } ~
          get {
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
