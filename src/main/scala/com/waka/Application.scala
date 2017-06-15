package com.waka

import akka.event.Logging
import akka.http.scaladsl.Http
import com.waka.controller.Routes

/**
  * Created by canoztokmak on 24/05/2017.
  */

object Application extends App with Routes {
  val logger = Logging(sys, getClass)

  Http().bindAndHandle(routes, Config.getHttpInterface, Config.getHttpPort)

  logger.debug("Movie Reservation app is started!!")
}
