import akka.actor.ActorSystem
import org.scalatra.oauth2._
import akka.util.duration._
import javax.servlet.ServletContext
import org.scalatra.LifeCycle

class Scalatra extends LifeCycle {

  implicit var system: ActorSystem = null
  override def init(context: ServletContext) {

    system = context.getOrElseUpdate(ActorSystemContextKey, ActorSystem(ActorSystemName)).asInstanceOf[ActorSystem]
    OAuth2Extension(system)

    //    context mount (new HomeServlet, "/*")
    //    context mount (new ClientsCrudApp, "/clients")
    //    context mount (new PermissionsCrudApp, "/permissions")
    //    context mount (new OAuthAuthentication, "/auth")
  }

  override def destroy(context: ServletContext) {
    system.synchronized {
      if (system != null && !system.isTerminated) {
        system.shutdown()
        system.awaitTermination(30 seconds)
      }
    }
  }
}
