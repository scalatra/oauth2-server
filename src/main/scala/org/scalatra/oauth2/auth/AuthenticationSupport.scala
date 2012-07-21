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
import command._

class OAuthScentryConfig extends ScentryConfig

trait PasswordAuthSupport[UserClass >: Null <: AppUser[_]] { self: ScalatraBase with FlashMapSupport with CookieSupport with AuthenticationSupport[UserClass] ⇒

  get("/login") {
    redirectIfAuthenticated()
    jade("login")
  }

  post("/login") {
    redirectIfAuthenticated()
    authenticate()
    if (isAnonymous) {
      flash.now("error") = "Username/password is incorrect"
      jade("login")
    } else {
      flash("success") = ("Welcome back, " + user.name)
      redirectAuthenticated()
    }
  }

  get("/register") {
    redirectIfAuthenticated()
    jade("register")
  }

  post("/register") {
    redirectIfAuthenticated()
    val regResult =
      authProvider.register(
        params.get("login"),
        params.get("email"),
        params.get("name"),
        params.get("password"),
        params.get("password_confirmation"))
    regResult.fold(
      errs ⇒ jade("register", "errors" -> errs.list),
      loggedIn(_, "Registered and logged in."))
  }

  get("/logout") {
    logOut()
    redirect(scentryConfig.failureUrl)
  }

  get("/activate") {
    flash("error") = "The token is required"
    redirect(scentryConfig.login)
  }

  get("/activate/:token") {
    authProvider.confirm(params("token")).fold(
      m ⇒ {
        flash("error") = m.message
        redirect(scentryConfig.failureUrl)
      },
      owner ⇒ {
        flash("success") = "Account confirmed!"
        redirect(scentryConfig.failureUrl)
      })
  }

  get("/unauthenticated") {
    flash("warn") = "You must be logged in to view this page."
    redirect(scentryConfig.login)
  }
}

trait ForgotPasswordAuthSupport[UserClass >: Null <: AppUser[_]] { self: ScalatraBase with FlashMapSupport with AuthenticationSupport[UserClass] ⇒
  get("/forgot") {
    redirectIfAuthenticated()
    jade("forgot")
  }

  post("/forgot") {
    redirectIfAuthenticated()
    authProvider.forgot(params.get("login")).fold(
      err ⇒ jade("forgot", "error" -> err.message),
      owner ⇒ {
        flash("info") = "Password reset link has been sent to <a href=\"mailto:%s\">%s</a>.".format(owner.email, owner.email)
        redirectAuthenticated()
      })
  }

  get("/reset/:token") {
    redirectIfAuthenticated()
    jade("reset", "token" -> params("token"))
  }

  post("/reset/:token") {
    redirectIfAuthenticated()
    authProvider.resetPassword(params("token"), ~params.get("password"), ~params.get("password_confirmation")).fold(
      err ⇒ {
        (err.list.filter {
          case a: ValidationError ⇒ false
          case _                  ⇒ true
        }).headOption foreach { m ⇒ flash.now("error") = m.message }
        jade("reset", "errors" -> err.list.collect { case a: ValidationError ⇒ a })
      },
      loggedIn(_, "Password reset successfully"))
  }

}

trait AuthenticationSupport[UserClass >: Null <: AppUser[_]] extends ScentrySupport[UserClass] with ScalateSupport { self: ScalatraBase with SessionSupport with FlashMapSupport ⇒

  protected def fromSession = { case id: String ⇒ authProvider.findUserById(id).orNull }
  protected def toSession = { case usr: AppUser[_] ⇒ usr.idString }

  type ScentryConfiguration = OAuthScentryConfig
  protected val scentryConfig = new OAuthScentryConfig

  protected def userManifest: Manifest[UserClass]
  type AuthProvider = UserProvider[UserClass] with ForgotPasswordProvider[UserClass] with RememberMeProvider[UserClass]

  implicit override protected def user = scentry.user
  protected def authProvider: AuthProvider

  def redirectIfAuthenticated() = if (isAuthenticated) redirectAuthenticated()

  def redirectAuthenticated() = redirect(session.get(scentryConfig.returnToKey).map(_.toString) | scentryConfig.returnTo)

  def loggedIn(authenticated: UserClass, message: String) = {
    if (userOption.isEmpty) scentry.user = authenticated
    flash("success") = message
    redirectAuthenticated()
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
    ctx.attributes.update("isAnonymous", isAnonymous)
    ctx.attributes.update("isAuthenticated", isAuthenticated)
    ctx.attributes.update("session", ctx.session)
    ctx.attributes.update("sessionOption", ctx.sessionOption)
    ctx.attributes.update("flash", ctx.flash)
    ctx.attributes.update("params", ctx.params)
    ctx.attributes.update("multiParams", ctx.multiParams)
    ctx.attributes.getOrUpdate("title", "OAuth2 Server")
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

}

trait DefaultAuthenticationSupport[UserClass >: Null <: AppUser[_]] extends AuthenticationSupport[UserClass] { self: ScalatraBase with SessionSupport with CookieSupport with FlashMapSupport with ApiFormats ⇒

  protected def oauth: OAuth2Extension

  /**
   * Registers authentication strategies.
   */
  override protected def configureScentry {
    val authCookieOptions = CookieOptions(
      domain = (if (oauth.web.domain == ".localhost") "localhost" else oauth.web.domain),
      path = "/",
      secure = oauth.web.sslRequired,
      httpOnly = true)
    scentry.store = new CookieAuthStore(self, authCookieOptions)
    scentry.unauthenticated {
      unauthenticated()
    }
  }

  override protected def registerAuthStrategies = {
    Seq(
      new PasswordStrategy(self, authProvider),
      new ForgotPasswordStrategy(self, authProvider),
      new RememberMeStrategy(self, authProvider),
      new AppUserBasicAuth(self, oauth.web.realm, authProvider)) foreach { strategy ⇒
        scentry.register(strategy.name, _ ⇒ strategy)
      }

  }

  def unauthenticated() = {
    format match {
      case "json" ⇒ scentry.strategies("resource_owner_basic").unauthenticated()
      case _ ⇒
        session(scentryConfig.returnToKey) = request.getRequestURI
        redirect(scentryConfig.failureUrl)
    }
  }

}
