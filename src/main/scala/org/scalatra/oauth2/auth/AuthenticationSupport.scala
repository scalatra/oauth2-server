package org.scalatra
package oauth2
package auth

import org.fusesource.scalate.Binding
import javax.servlet.http.{ HttpServletResponse, HttpServletRequest }
import java.io.PrintWriter
import scalaz._
import Scalaz._
import org.scalatra.scalate.{ ScalatraRenderContext, ScalateSupport }
import org.scalatra.auth._
import ScentryAuthStore.CookieAuthStore
import databinding._
import model._
import org.json4s._
import OAuth2Imports._
import commands._
import service.AuthenticationService
import akka.actor.ActorSystem
import model.OAuth2Response
import model.ApiErrorList
import org.scalatra.validation.ValidationError
import org.scalatra.json.NativeJsonSupport

class OAuthScentryConfig extends ScentryConfig

trait PasswordAuthSupport[UserClass >: Null <: AppAuthSession[_ <: AppUser[_]]] {
  self: ScalatraBase with FlashMapSupport with CookieSupport with AuthenticationSupport[UserClass] with NativeJsonSupport with OAuth2CommandSupport ⇒

  protected def bindCommand[T <: OAuth2Command[_]](command: T)(implicit mf: Manifest[T]): T
  get("/login") {
    redirectIfAuthenticated()
    jade("angular")
  }

  post("/login") {
    redirectIfAuthenticated()
    authenticate()
    if (isAnonymous) {
      format match {
        case "json" | "xml" ⇒
          status = 401
          ApiError("Username or password is incorrect")
        case _ ⇒
          flash.now("error") = "Username/password is incorrect"
          jade("login")
      }

    } else {
      loggedIn(user, "Welcome back, " + user.account.name)
    }
  }

  protected def registerCommand: RegisterCommand

  get("/register") {
    redirectIfAuthenticated()
    jade("angular")
  }

  post("/register") {
    redirectIfAuthenticated()
    logger.debug("Registering user from " + format)
    val res = authService.execute(getCommand(registerCommand))
    format match {
      case "json" | "xml" ⇒ res
      case _ ⇒
        res.fold(
          errs ⇒ jade("angular", "errors" -> errs.list),
          sess ⇒ loggedIn(sess.asInstanceOf[UserClass], "Registered and logged in."))
    }
  }

  get("/logout") {
    logOut()
    format match {
      case "xml" | "json" ⇒ OAuth2Response(JNull)
      case _              ⇒ redirect(scentryConfig.login)
    }
  }

  protected def activateCommand: ActivateAccountCommand
  get("/activate/?:token?") {
    authService.execute(getCommand(activateCommand)).fold(
      m ⇒ {
        flash("error") = m.head.message
        redirect(scentryConfig.login)
      },
      owner ⇒ {
        flash("success") = "Account confirmed!"
        redirect(scentryConfig.login)
      })
  }

  get("/unauthenticated") {
    flash("warn") = "You must be logged in to view this page."
    redirect(scentryConfig.login)
  }
}

trait ForgotPasswordAuthSupport[UserClass >: Null <: AppAuthSession[_ <: AppUser[_]]] {
  self: ScalatraBase with FlashMapSupport with AuthenticationSupport[UserClass] with NativeJsonSupport with OAuth2CommandSupport ⇒
  get("/forgot") {
    redirectIfAuthenticated()
    jade("angular")
  }

  protected def forgotCommand: ForgotCommand
  post("/forgot") {
    redirectIfAuthenticated()
    val res = authService.execute(getCommand(forgotCommand))
    format match {
      case "json" | "xml" ⇒ res.map(_ ⇒ JNull)
      case _ ⇒ res.fold(
        errs ⇒ jade("angular", "errors" -> errs.list),
        owner ⇒ {
          flash("info") = "Password reset link has been sent to <a href=\"mailto:%s\">%s</a>.".format(owner.email, owner.email)
          redirect (session.get(scentryConfig.returnToKey).map(_.toString) | scentryConfig.returnTo)
        })
    }
  }

  get("/reset/?:token?") {
    redirectIfAuthenticated()
    requiresToken {
      jade("angular")
    }
  }

  private def requiresToken(res: ⇒ Any) = {
    params.get("token").flatMap(_.blankOption).flatMap(tok ⇒ oauth.userProvider.findOne(Map("reset.token" -> tok))) map { _ ⇒
      res
    } getOrElse OAuth2Error.InvalidToken.fail
  }

  protected def resetCommand: ResetCommand
  post("/reset/?:token?/?") {
    redirectIfAuthenticated()
    requiresToken {
      val res = authService.execute(getCommand(resetCommand))
      format match {
        case "json" | "xml" ⇒ res
        case _ ⇒ res.fold(
          errs ⇒ {
            (errs.list.filter {
              case ValidationError(_, Some(_), _, _) ⇒ false
              case _                                 ⇒ true
            }).headOption foreach { m ⇒ flash.now("error") = m.message }
            jade("angular", "errors" -> errs.list.collect { case a: ValidationError ⇒ a })

          },
          owner ⇒ loggedIn(owner.asInstanceOf[UserClass], "Password reset successfully"))

      }
    }
  }
}

trait AuthenticationSupport[UserClass >: Null <: AppAuthSession[_ <: AppUser[_]]] extends ScentrySupport[UserClass] with ScalateSupport {
  self: ScalatraBase with SessionSupport with FlashMapSupport with NativeJsonSupport ⇒

  implicit protected def jsonFormats: Formats
  protected def fromSession = { case id: String ⇒ authService.loginFromRemember(id).toOption.map(_.asInstanceOf[UserClass]).orNull }
  protected def toSession = { case usr: AppAuthSession[_] ⇒ usr.token.token }

  type ScentryConfiguration = OAuthScentryConfig
  protected val scentryConfig = new OAuthScentryConfig

  protected implicit def system: ActorSystem
  protected def authService: AuthenticationService
  def userManifest: Manifest[UserClass]
  implicit override protected def user = scentry.user

  def redirectIfAuthenticated() = if (isAuthenticated) redirectAuthenticated()

  def redirectAuthenticated() = {
    format match {
      case "json" | "xml" ⇒
        OAuth2Response(Extraction.decompose(user.account))
      case _ ⇒
        redirect(session.get(scentryConfig.returnToKey).map(_.toString) | scentryConfig.returnTo)
    }
  }

  def loggedIn(authenticated: UserClass, message: String) = {
    if (userOption.isEmpty) scentry.user = authenticated
    format match {
      case "json" | "xml" ⇒
        OAuth2Response(Extraction.decompose(authenticated.account))
      case _ ⇒
        if (format == "html") flash("success") = message
        redirectAuthenticated()
    }

  }

  override protected def createTemplateEngine(config: ConfigT) = {
    val engine = super.createTemplateEngine(config)
    engine.bindings :::= List(
      Binding("userOption", "Option[%s]".format(userManifest.erasure.getName), defaultValue = "None".some),
      Binding("user", userManifest.erasure.getName, defaultValue = "null".some),
      Binding("isAnonymous", "Boolean", defaultValue = "true".some),
      Binding("isAuthenticated", "Boolean", defaultValue = "false".some))

    engine
  }

  override protected def createRenderContext(req: HttpServletRequest, resp: HttpServletResponse, out: PrintWriter) = {
    val ctx = super.createRenderContext(req, resp, out).asInstanceOf[ScalatraRenderContext]
    ctx.attributes.update("userOption", userOption)
    ctx.attributes.update("user", user)
    ctx.attributes.update("accountOption", userOption.map(_.account))
    ctx.attributes.update("account", userOption.map(_.account).orNull)
    ctx.attributes.update("isAnonymous", isAnonymous)
    ctx.attributes.update("isAuthenticated", isAuthenticated)
    ctx.attributes.update("session", ctx.session)
    ctx.attributes.update("sessionOption", ctx.sessionOption)
    ctx.attributes.update("flash", ctx.flash)
    ctx.attributes.update("params", ctx.params)
    ctx.attributes.update("multiParams", ctx.multiParams)
    ctx.attributes.getOrUpdate("title", "OAuth2 Server")
    ctx.attributes.update("system", system)
    ctx
  }

  /**
   * Builds a full URL from the given relative path. Takes the port configuration, https, ... into account
   *
   * @param path a relative path
   *
   * @return the full URL
   */
  protected def buildFullUrl(path: String): String

  override protected def logOut() {
    authService logout scentry.store.get
    super.logOut()
  }
}

trait DefaultAuthenticationSupport[UserClass >: Null <: AppAuthSession[_ <: AppUser[_]]] extends AuthenticationSupport[UserClass] {
  self: ScalatraBase with SessionSupport with CookieSupport with FlashMapSupport with NativeJsonSupport with OAuth2CommandSupport ⇒

  protected def oauth: OAuth2Extension

  /**
   * Registers authentication strategies.
   */
  override protected def configureScentry {
    val authCookieOptions = cookieOptions.copy(
      domain = (if (oauth.web.domain == ".localhost" || oauth.isTest) "" else oauth.web.domain),
      secure = oauth.web.sslRequired,
      httpOnly = true)
    scentry.store = new CookieAuthStore(self)(authCookieOptions) {
      override def invalidate() {
        cookies.update(Scentry.scentryAuthKey, Token().token)(authCookieOptions.copy(maxAge = 0))
      }
    }
    scentry.unauthenticated { unauthenticated() }
  }

  protected def loginCommand: LoginCommand

  override protected def registerAuthStrategies = {
    scentry.register("user_password", _ ⇒ new PasswordStrategy(self, getCommand(loginCommand), authService))
    scentry.register("resource_owner_basic", _ ⇒ new AppUserBasicAuth(self, oauth.web.realm, getCommand(loginCommand), authService))
    scentry.register("remember_me", _ ⇒ new RememberMeStrategy(self, authService))

  }

  def unauthenticated() = {
    if (request.isBasicAuth && request.providesAuth) {
      scentry.strategies("resource_owner_basic").unauthenticated()
    } else {
      format match {
        case "json" | "xml" ⇒
          Unauthorized(OAuth2Response(JNull, ApiErrorList(List(ApiError("Unauthenticated"))).toJValue))
        case _ ⇒
          session(scentryConfig.returnToKey) = request.getRequestURI
          redirect(scentryConfig.failureUrl)
      }
    }
  }

}
