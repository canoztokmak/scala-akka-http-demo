import spray.json.DefaultJsonProtocol

/**
  * Created by canoztokmak on 25/05/2017.
  */

case class Movie(imdbId: String, screenId: String, movieTitle: String, availableSeats: Int, reservedSeats: Int = 0)

case class CreateMovieRequest(imdbId: String, screenId: String, availableSeats: Int)

case class OmdbResponse(Response: String, Error: Option[String], Title: String, imdbID: String)

trait Protocols extends DefaultJsonProtocol {
  implicit val movieFormat = jsonFormat5(Movie)
  implicit val createMovieRequestFormat = jsonFormat3(CreateMovieRequest)

  implicit val omdbResponseFormat = jsonFormat4(OmdbResponse)
}
