import akka.actor.ActorRefFactory
import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import spray.routing.HttpService
import spray.http.StatusCodes._
import controllers.{WebActor, Web}
import services.Spitball


class FullTestKitExampleSpec extends Specification with Specs2RouteTest with HttpService {
  def actorRefFactory = system // connect the DSL to the test ActorSystem
  val routes = new WebActor

  "The service" should {

    "split multiple request ids" in {
      {


      }
    }

    "" in {
      Get("/ping") ~> smallRoute ~> check {
        entityAs[String] === "PONG!"
      }
    }

    "" in {
      Get("/kermit") ~> smallRoute ~> check {
        handled must beFalse
      }
    }

    "" in {
      Put() ~> sealRoute(smallRoute) ~> check {
        status === MethodNotAllowed
        entityAs[String] === "HTTP method not allowed, supported methods: GET"
      }
    }
  }
}