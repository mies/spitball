package controllers

import akka.actor._
import java.net.URL
import concurrent.Future
import spray.routing.HttpService
import spray.httpx.SprayJsonSupport._
import spray.routing.MalformedRequestContentRejection
import Formatters._
import services.Parser

class WebActor extends Actor with Web {
  def actorRefFactory = context
  def receive = runRoute(route)
}

trait Web extends HttpService with Handlers {

  val route = pathPrefix("v1") {
    post {
      path("drain") {
        complete("drained")
      }
    } ~
    get {
      path("requests" / Rest) { requestId =>
        complete(s"requests: $requestId")
      }
    }
  }
}
