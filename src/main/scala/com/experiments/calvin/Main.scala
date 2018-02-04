package com.experiments.calvin

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.experiments.calvin.web.Routes

import scala.concurrent.ExecutionContext
import scala.util._

object Main extends App with Routes {
  implicit val system: ActorSystem    = ActorSystem("ws-pub-sub-experiment-system")
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext   = system.dispatcher

  Http().bindAndHandle(routes, "127.0.0.1", 9001).onComplete {
    case Success(binding) =>
      println(s"Server online at http://${binding.localAddress.getHostName}:${binding.localAddress.getPort}\n")

    case Failure(ex) =>
      println(s"Failed to start server, shutting down actor system. Exception is: ${ex.getCause}: ${ex.getMessage}")
      system.terminate()
  }
}
