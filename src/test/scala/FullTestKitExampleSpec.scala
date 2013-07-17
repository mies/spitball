import akka.actor.ActorRefFactory
import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import spray.routing.HttpService
import spray.http.StatusCodes._
import controllers.{WebActor, Web}


class FullTestKitExampleSpec extends Specification with Specs2RouteTest with HttpService {
  def actorRefFactory = system // connect the DSL to the test ActorSystem
  val routes = new WebActor

  "The service" should {

    "return a greeting for GET requests to the root path" in {
      Get() ~> smallRoute ~> check {
        entityAs[String] must contain("Say goodbye")
      }
    }

    "return a 'PONG!' response for GET requests to /ping" in {
      Get("/ping") ~> smallRoute ~> check {
        entityAs[String] === "PONG!"
      }
    }

    "leave GET requests to other paths unhandled" in {
      Get("/kermit") ~> smallRoute ~> check {
        handled must beFalse
      }
    }

    "return a MethodNotAllowed error for PUT requests to the root path" in {
      Put() ~> sealRoute(smallRoute) ~> check {
        status === MethodNotAllowed
        entityAs[String] === "HTTP method not allowed, supported methods: GET"
      }
    }
  }
}