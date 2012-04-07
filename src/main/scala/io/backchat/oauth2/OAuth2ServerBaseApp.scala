package io.backchat.oauth2

import auth.{ RememberMeAuthSupport, ForgotPasswordAuthSupport, PasswordAuthSupport, AuthenticationSupport }
import model.ResourceOwner
import org.scalatra.scalate.ScalateSupport
import akka.actor.ActorSystem
import org.scalatra.servlet.ServletBase
import org.scalatra.liftjson.LiftJsonRequestBody
import org.scalatra.{ ApiFormats, ScalatraServlet, FlashMapSupport, CookieSupport }

trait AuthenticationApp[UserClass >: Null <: AppUser[_]]
    extends PasswordAuthSupport[UserClass]
    with ForgotPasswordAuthSupport[UserClass]
    with RememberMeAuthSupport[UserClass] {
  self: ServletBase with FlashMapSupport with CookieSupport with ScalateSupport with AuthenticationSupport[UserClass] ⇒

}

trait OAuth2ServerBaseApp extends ScalatraServlet
    with FlashMapSupport
    with CookieSupport
    with ScalateSupport
    with LiftJsonRequestBody
    with ApiFormats
    with AuthenticationSupport[ResourceOwner] {

  implicit protected def system: ActorSystem
  val oauth = OAuth2Extension(system)

  protected val userManifest = manifest[ResourceOwner]

  protected lazy val authProvider = oauth.userProvider

  /**
   * Builds a full URL from the given relative path. Takes into account the port configuration, https, ...
   *
   * @param path a relative path
   *
   * @return the full URL
   */
  protected def buildFullUrl(path: String) = {
    if (path.startsWith("http")) path else {
      "http%s://%s%s/%s".format(
        if (oauth.web.sslRequired || this.isHttps) "s" else "",
        oauth.web.domainWithPort,
        request.getContextPath,
        path)
    }
  }

  notFound {
    serveStaticResource() getOrElse resourceNotFound()
  }

  override def environment = oauth.environment
}

