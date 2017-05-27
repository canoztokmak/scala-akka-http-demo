package com.waka

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.waka.controller.Routes

/**
  * Created by canoztokmak on 24/05/2017.
  */

object Application extends App with Routes {
  override implicit val sys = ActorSystem()
  override implicit val ec = sys.dispatcher
  override implicit val mat = ActorMaterializer()
  val logger = Logging(sys, getClass)

  Http().bindAndHandle(routes, Config.getHttpInterface, Config.getHttpPort)

  logger.debug("Movie Reservation app is started!!")
}
