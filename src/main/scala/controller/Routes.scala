package controller

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContextExecutor

/**
  * Created by canoztokmak on 27/05/2017.
  */
trait Routes {
  implicit val sys: ActorSystem
  implicit val mat: ActorMaterializer
  // needed for the future map/flatmap in the end
  implicit def ec: ExecutionContextExecutor

  val movieRoutes = new MovieRoutes {
    override implicit def executor: ExecutionContextExecutor = ec
    override implicit val system: ActorSystem = sys
    override implicit val materializer: ActorMaterializer = mat
  }

  val routes: Route = movieRoutes.routes
}
