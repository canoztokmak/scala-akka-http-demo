package com.waka

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.ActorMaterializer
import com.waka.controller.MovieRoutes
import com.waka.model._
import org.mongodb.scala.MongoClient
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

import scala.concurrent.ExecutionContextExecutor

/**
  * Created by canoztokmak on 27/05/2017.
  */
class MovieRoutesSpec extends WordSpec with Matchers with ScalatestRouteTest with Protocols with BeforeAndAfterAll {
  val movieRoutes = new MovieRoutes {
    override implicit val system: ActorSystem = ActorSystem()
    override implicit def executor: ExecutionContextExecutor = system.dispatcher
    override implicit val materializer: ActorMaterializer = ActorMaterializer()
  }

  val routes = movieRoutes.routes

  override protected def beforeAll(): Unit = {
    MongoClient().getDatabase("movie-reservation").drop()
  }

  "Create Movie" should {
    "create movie with valid a request" in {
      Post("/movies", TestHelper.validCreateMovieRequest) ~> routes ~> check {
        status shouldBe StatusCodes.Created
      }
    }

    "return not found when invalid imdbId is provided" in {
      Post("/movies", TestHelper.invalidCreateMovieRequest) ~> routes ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }
  }

  "Reserve Movie" should {
    "reserve movie with valid a request" in {
      Post(s"/movies/${TestHelper.imdbId}/screens/${TestHelper.screenId}") ~> routes ~> check {
        status shouldBe StatusCodes.NoContent
      }
    }

    "return not found if invalid imdbId is provided" in {
      Post(s"/movies/${TestHelper.invalidImdbId}/screens/${TestHelper.screenId}") ~> routes ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "return not found if invalid screenId is provided" in {
      Post(s"/movies/${TestHelper.imdbId}/screens/${TestHelper.invalidScreenId}") ~> routes ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "return forbidden when there is no available seat" in {
      Post("/movies", TestHelper.validCreateMovieRequestOneAvailableSeat) ~> routes ~> check {
        status shouldBe StatusCodes.Created

        Post(s"/movies/${TestHelper.imdbId}/screens/${TestHelper.screenId2}") ~> routes ~> check {
          status shouldBe StatusCodes.NoContent

          Post(s"/movies/${TestHelper.imdbId}/screens/${TestHelper.screenId2}") ~> routes ~> check {
            status shouldBe StatusCodes.Forbidden
          }
        }
      }
    }
  }

  "Retrieve Movie" should {
    "retrieve movie with a valid request" in {
      Get(s"/movies/${TestHelper.imdbId}/screens/${TestHelper.screenId}") ~> routes ~> check {
        status shouldBe StatusCodes.OK
        val movieResp = responseAs[Movie]
        movieResp.imdbId shouldBe TestHelper.imdbId
        movieResp.screenId shouldBe TestHelper.screenId
        movieResp.movieTitle shouldBe TestHelper.movieTitle
        movieResp.availableSeats shouldBe TestHelper.availableSeats
        movieResp.reservedSeats should be < TestHelper.availableSeats
      }
    }

    "return not found when invalid imdbId is provided" in {
      Get(s"/movies/${TestHelper.invalidImdbId}/screens/${TestHelper.screenId}") ~> routes ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "return not found when invalid screenId is provided" in {
      Get(s"/movies/${TestHelper.imdbId}/screens/${TestHelper.invalidScreenId}") ~> routes ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "return updated reservedSeat value after reserving a seat" in {
      Post(s"/movies/${TestHelper.imdbId}/screens/${TestHelper.screenId}") ~> routes ~> check {
        status shouldBe StatusCodes.NoContent

        Get(s"/movies/${TestHelper.imdbId}/screens/${TestHelper.screenId}") ~> routes ~> check {
          status shouldBe StatusCodes.OK
          val movieResp = responseAs[Movie]
          movieResp.imdbId shouldBe TestHelper.imdbId
          movieResp.screenId shouldBe TestHelper.screenId
          movieResp.movieTitle shouldBe TestHelper.movieTitle
          movieResp.availableSeats shouldBe TestHelper.availableSeats
          movieResp.reservedSeats should be > 0
        }
      }
    }
  }
}
