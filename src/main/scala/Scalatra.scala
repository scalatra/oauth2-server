import akka.actor.ActorSystem
import org.scalatra.oauth2._
import javax.servlet.ServletContext
import org.scalatra.LifeCycle
import akka.util.duration._

class Scalatra extends LifeCycle {

  implicit var system: ActorSystem = null
  override def init(context: ServletContext) {

    system = context.getOrElseUpdate(org.scalatra.oauth2.ActorSystemContextKey, ActorSystem(org.scalatra.oauth2.ActorSystemName)).asInstanceOf[ActorSystem]
    val oauth = OAuth2Extension(system)

    context mount (new HomeServlet, "/")
    context mount (new ClientsCrudApp, "/clients")
    context mount (new PermissionsCrudApp, "/permissions")
    context mount (new OAuthAuthentication, "/auth")
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
