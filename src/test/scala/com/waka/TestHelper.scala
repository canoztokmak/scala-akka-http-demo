package com.waka

import com.waka.model.{CreateMovieRequest, Movie}

/**
  * Created by canoztokmak on 27/05/2017.
  */
object TestHelper {
  def imdbId = "tt1790800"
  def screenId = "screenId"
  def movieTitle = "Paper Shoes"
  def availableSeats = 100

  def invalidImdbId = "invalidImdbId"
  def invalidScreenId = "invalidScreenId"

  val validCreateMovieRequest = CreateMovieRequest(imdbId, screenId, availableSeats)
  val movie = Movie(imdbId, screenId, movieTitle, availableSeats, 0)

  val invalidCreateMovieRequest = CreateMovieRequest("tt", "screenId", 100)
}
