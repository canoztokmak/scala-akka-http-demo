package com.waka.controller

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContextExecutor

/**
  * Created by canoztokmak on 27/05/2017.
  */
trait Routes {
  implicit val sys: ActorSystem = ActorSystem()
  implicit val mat: ActorMaterializer = ActorMaterializer()
  // needed for the future map/flatmap in the end
  implicit def ec: ExecutionContextExecutor = sys.dispatcher

  val movieRoutes = new MovieRoutes {
    override implicit def executor: ExecutionContextExecutor = ec
    override implicit val system: ActorSystem = sys
    override implicit val materializer: ActorMaterializer = mat
  }

  val routes: Route = movieRoutes.routes
}
