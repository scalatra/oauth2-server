package org.scalatra
package oauth2

import auth.{ DefaultAuthenticationSupport, ForgotPasswordAuthSupport, PasswordAuthSupport, AuthenticationSupport }
import model.{ OAuth2ModelCommand, Account }
import org.scalatra.scalate.ScalateSupport
import akka.actor.ActorSystem
import org.scalatra.servlet.ServletBase
import javax.servlet.http.{ HttpServletRequestWrapper, HttpServletResponse, HttpServletRequest }
import liftjson.{ LiftJsonSupport, LiftJsonRequestBody }
import scalaz._
import Scalaz._
import net.liftweb.json._
import OAuth2Imports._
import java.io.PrintWriter
import command.CommandSupport
import extension.TypedParamSupport

trait AuthenticationApp[UserClass >: Null <: AppUser[_]]
    extends PasswordAuthSupport[UserClass]
    with ForgotPasswordAuthSupport[UserClass] {
  self: ServletBase with LiftJsonSupport with FlashMapSupport with CookieSupport with ScalateSupport with DefaultAuthenticationSupport[UserClass] ⇒

}

/**
 * Mixin for clients that only support a limited set of HTTP verbs.  If the
 * request is a POST and the `_method` request parameter is set, the value of
 * the `_method` parameter is treated as the request's method.
 */
trait OAuth2MethodOverride extends Handler {
  abstract override def handle(req: HttpServletRequest, res: HttpServletResponse) {
    val req2 = req.requestMethod match {
      case Post | Get ⇒ requestWithMethod(req, methodOverride(req))
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
  protected def requestWithMethod(req: HttpServletRequest, method: Option[String]): HttpServletRequest =
    new HttpServletRequestWrapper(req) {
      override def getMethod(): String =
        method getOrElse req.getMethod
    }

  private def methodOverride(req: HttpServletRequest) = {
    import MethodOverride._
    (req.parameters.get(ParamName) orElse req.headers.get(HeaderName(0)))
  }
}

trait OAuth2ServerBaseApp extends ScalatraServlet
    with CookieSupport
    with XsrfTokenSupport
    with OAuth2ResponseSupport
    with OAuth2MethodOverride
    with FlashMapSupport
    with LiftJsonSupport
    with ScalateSupport
    with CorsSupport
    with LoadBalancedSslRequirement
    with DefaultAuthenticationSupport[Account]
    with CommandSupport
    with TypedParamSupport {

  implicit protected def system: ActorSystem
  override implicit val jsonFormats: Formats = new OAuth2Formats
  override protected lazy val jsonVulnerabilityGuard: Boolean = true

  val oauth = OAuth2Extension(system)

  protected val userManifest = manifest[Account]

  protected lazy val authProvider = oauth.userProvider

  before() {
    logger.info("Requesting path: " + requestPath)
  }

  /**
   * Builds a full URL from the given relative path. Takes into account the port configuration, https, ...
   *
   * @param path a relative path
   *
   * @return the full URL
   */
  protected def buildFullUrl(path: String) = {
    if (path.startsWith("http")) path
    else oauth.web.appUrl + url(path)
  }

  notFound {
    serveStaticResource() getOrElse resourceNotFound()
  }

  override def environment = oauth.environment

  override protected def createRenderContext(req: HttpServletRequest, resp: HttpServletResponse, out: PrintWriter) = {
    val ctx = super.createRenderContext(req, resp, out)
    ctx.attributes("title") = "Scalatra OAuth2"
    ctx
  }

  override protected def createTemplateEngine(config: ConfigT) = {
    val eng = super.createTemplateEngine(config)
    eng.importStatements :::=
      "import scalaz._" ::
      "import scalaz.Scalaz._" ::
      "import org.scalatra.oauth2._" ::
      "import org.scalatra.oauth2.OAuth2Imports._" ::
      "import org.scalatra.oauth2.model._" ::
      Nil
    eng
  }

  protected def inferFromJValue: ContentTypeInferrer = {
    case _: JValue if format == "xml" ⇒ formats("xml")
    case _: JValue                    ⇒ formats("json")
  }

  override protected def transformRequestBody(body: JValue) = body.camelizeKeys

  override protected def contentTypeInferrer = inferFromFormats orElse inferFromJValue orElse super.contentTypeInferrer

  override protected def renderPipeline = renderOAuth2Response orElse super.renderPipeline

  override protected def isScalateErrorPageEnabled = isDevelopmentMode

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

  override def url(path: String, params: Iterable[(String, Any)] = Iterable.empty): String = {
    val newPath = path match {
      case x if x.startsWith("/") ⇒ ensureSlash(contextPath) + ensureSlash(path)
      case _                      ⇒ ensureSlash(routeBasePath) + ensureSlash(path)
    }
    val pairs = params map { case (key, value) ⇒ key.urlEncode + "=" + value.toString.urlEncode }
    val queryString = if (pairs.isEmpty) "" else pairs.mkString("?", "&", "")
    addSessionId((newPath.startsWith("/") ? newPath.substring(1) | newPath) + queryString)
  }

  private def ensureSlash(candidate: String) = {
    (candidate.startsWith("/"), candidate.endsWith("/")) match {
      case (true, true)   ⇒ candidate.dropRight(1)
      case (true, false)  ⇒ candidate
      case (false, true)  ⇒ "/" + candidate.dropRight(1)
      case (false, false) ⇒ "/" + candidate
    }
  }

  /**
   * Create and bind a [[org.scalatra.command.Command]] of the given type with the current Scalatra params.
   *
   * For every command type, creation and binding is performed only once and then stored into
   * a request attribute.
   */
  def oauth2Command[T <: OAuth2ModelCommand[_]](implicit mf: Manifest[T], system: ActorSystem): T = {
    commandOption[T] getOrElse {
      val newCommand = mf.erasure.getConstructor(classOf[ActorSystem]).newInstance(system).asInstanceOf[T]
      format match {
        case "json" | "xml" ⇒
          logger.debug("Binding from json")
          newCommand.doBinding(json = parsedBody, params = params)
        case _ ⇒
          logger.debug("Binding from params")
          newCommand.doBinding(params)
      }
      request("_command_" + mf.erasure.getName) = newCommand
      newCommand
    }
  }

}

