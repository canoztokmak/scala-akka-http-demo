package com.waka

import com.waka.model.Movie
import com.waka.repository.MovieRepository

import scala.concurrent.Future

/**
  * Created by canoztokmak on 15/06/2017.
  */
class MovieRepositoryMock extends MovieRepository {
  var movies: Map[(String, String), Movie] = Map.empty

  def addMovie(movie: Movie): Future[_] = {
    movies += (movie.imdbId, movie.screenId) -> movie
    Future.successful()
  }

  def reserveSeat(imdbId: String, screenId: String): Future[Long] = {
    movies.get((imdbId, screenId)) match {
      case Some(m) =>
        movies += (imdbId, screenId) -> m.copy(reservedSeats = m.reservedSeats + 1)
        Future.successful(1l)

      case None =>
        Future.successful(0l)
    }
  }

  def retrieveMovie(imdbId: String, screenId: String): Future[Option[Movie]] = {
    Future.successful(movies.get((imdbId, screenId)))
  }
}
