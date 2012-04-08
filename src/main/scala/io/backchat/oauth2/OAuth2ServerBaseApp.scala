package io.backchat.oauth2

import io.backchat.oauth2.auth.{ RememberMeAuthSupport, ForgotPasswordAuthSupport, PasswordAuthSupport, AuthenticationSupport }
import model.ResourceOwner
import org.scalatra.scalate.ScalateSupport
import akka.actor.ActorSystem
import org.scalatra.servlet.ServletBase
import org.scalatra.liftjson.LiftJsonRequestBody
import javax.servlet.http.{ HttpServletResponse, HttpServletRequest }
import java.io.PrintWriter
import org.scalatra._
import scalaz._
import Scalaz._

trait AuthenticationApp[UserClass >: Null <: AppUser[_]]
    extends PasswordAuthSupport[UserClass]
    with ForgotPasswordAuthSupport[UserClass]
    with RememberMeAuthSupport[UserClass] {
  self: ServletBase with FlashMapSupport with CookieSupport with ScalateSupport with AuthenticationSupport[UserClass] ⇒

}

/**
 * Mixin for clients that only support a limited set of HTTP verbs.  If the
 * request is a POST and the `_method` request parameter is set, the value of
 * the `_method` parameter is treated as the request's method.
 */
trait OAuth2MethodOverride extends Handler {
  abstract override def handle(req: RequestT, res: ResponseT) {
    val req2 = req.requestMethod match {
      case Post | Get ⇒ req.parameters.get(paramName) some (m ⇒ requestWithMethod(req, HttpMethod(m))) none req
      case _          ⇒ req
    }
    super.handle(req2, res)
  }

  /**
   * Returns a request identical to the current request, but with the
   * specified method.
   *
   * For backward compatibility, we need to transform the underlying request
   * type to pass to the super handler.
   */
  protected def requestWithMethod(req: RequestT, method: HttpMethod): RequestT

  private val paramName = "_method"
}

trait OAuth2ServerBaseApp extends ScalatraServlet
    with FlashMapSupport
    with CookieSupport
    with ScalateSupport
    with ApiFormats
    with OAuth2MethodOverride
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

  override protected def createRenderContext(req: HttpServletRequest, resp: HttpServletResponse, out: PrintWriter) = {
    val ctx = super.createRenderContext(req, resp, out)
    ctx.attributes("title") = "Backchat OAuth2"
    ctx
  }

  override protected def createTemplateEngine(config: ConfigT) = {
    val eng = super.createTemplateEngine(config)
    eng.importStatements :::=
      "import scalaz._" ::
      "import scalaz.Scalaz._" ::
      "import io.backchat.oauth2._" ::
      "import io.backchat.oauth2.OAuth2Imports._" ::
      "import io.backchat.oauth2.model._" ::
      Nil
    eng
  }
}

