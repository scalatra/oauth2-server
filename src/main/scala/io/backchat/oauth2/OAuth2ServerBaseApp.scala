package io.backchat.oauth2

import io.backchat.oauth2.auth.{ ForgotPasswordAuthSupport, PasswordAuthSupport, AuthenticationSupport }
import model.ResourceOwner
import org.scalatra.scalate.ScalateSupport
import akka.actor.ActorSystem
import org.scalatra.servlet.ServletBase
import javax.servlet.http.{ HttpServletResponse, HttpServletRequest }
import org.scalatra._
import liftjson.LiftJsonRequestBody
import scalaz._
import Scalaz._
import net.liftweb.json._
import OAuth2Imports._
import java.io.PrintWriter

trait AuthenticationApp[UserClass >: Null <: AppUser[_]]
    extends PasswordAuthSupport[UserClass]
    with ForgotPasswordAuthSupport[UserClass] {
  self: ServletBase with ApiFormats with FlashMapSupport with CookieSupport with ScalateSupport with AuthenticationSupport[UserClass] ⇒

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
    with OAuth2ResponseSupport
    with OAuth2MethodOverride
    with LiftJsonRequestBody
    with FlashMapSupport
    with CookieSupport
    with ScalateSupport
    with CORSSupport
    with LoadBalancedSslRequirement
    with AuthenticationSupport[ResourceOwner] {

  implicit protected def system: ActorSystem
  override protected implicit def jsonFormats: Formats = new OAuth2Formats

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
      "http%s://%s%s%s".format(
        if (oauth.web.sslRequired || this.isHttps) "s" else "",
        oauth.web.domainWithPort,
        request.getContextPath.blankOption.map("/" + _) | "/",
        if (path.startsWith("/")) path.substring(1) else path)
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

  protected def inferFromJValue: ContentTypeInferrer = {
    case _: JValue ⇒ formats('json.name)
  }

  override protected def transformRequestBody(body: JValue) = body.camelizeKeys

  override protected def contentTypeInferrer = inferFromFormats orElse inferFromJValue orElse super.contentTypeInferrer

  override protected def renderPipeline = renderBackchatResponse orElse super.renderPipeline

  /**
   * Redirect to full URL build from the given relative path.
   *
   * @param path a relative path
   */
  override def redirect(path: String) = {
    val url = buildFullUrl(path)
    logger debug ("redirecting to [%s]" format url)
    super.redirect(url)
  }

}

