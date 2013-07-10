import spray.can.server.SprayCanHttpServerApp
import akka.actor.Props


object Main extends App with SprayCanHttpServerApp {
  val web  = system.actorOf(Props[controllers.WebActor])
  val port = sys.env.get("PORT").map(_.toInt).getOrElse(9000)
  newHttpServer(web) ! Bind(interface = "0.0.0.0", port = port)
}
