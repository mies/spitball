package controllers

import akka.actor._
import java.net.URL
import concurrent.Future
import spray.routing.HttpService
import spray.httpx.SprayJsonSupport._
import spray.routing.MalformedRequestContentRejection
import Formatters._
import services.Drainer
import scala.concurrent.ExecutionContext.Implicits.global
import spray.http.StatusCodes

class WebActor extends Actor with Web {
  def actorRefFactory = context
  def receive = runRoute(route)
}

trait Web extends HttpService with Handlers {

  val route = pathPrefix("v1") {
    post {
      path("drain") {
        entity(as[String]) { body =>
          Future(Drainer.drain(body))
          complete(StatusCodes.Accepted)
        }
      }
    } ~
    get {
      path("requests" / Rest) { requestId =>
        complete(Drainer.fromRedis(requestId))
      }
    }
  }
}
