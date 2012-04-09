package io.backchat.oauth2
package auth

import org.scalatra.servlet.ServletBase
import org.scalatra.auth.{ ScentryStrategy, ScentryConfig, ScentrySupport }
import org.fusesource.scalate.Binding
import javax.servlet.http.{ HttpServletResponse, HttpServletRequest }
import java.io.PrintWriter
import org.scalatra.{ FlashMapSupport, CookieSupport }
import scalaz._
import Scalaz._
import model.{ ValidationError }
import org.scalatra.scalate.{ ScalatraRenderContext, ScalateSupport }

class OAuthScentryConfig extends ScentryConfig

trait PasswordAuthSupport[UserClass >: Null <: AppUser[_]] { self: ServletBase with FlashMapSupport with CookieSupport with AuthenticationSupport[UserClass] ⇒

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

trait ForgotPasswordAuthSupport[UserClass >: Null <: AppUser[_]] { self: ServletBase with FlashMapSupport with AuthenticationSupport[UserClass] ⇒
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

trait RememberMeAuthSupport[UserClass >: Null <: AppUser[_]] { self: AuthenticationSupport[UserClass] ⇒

  before() {
    if (isAnonymous) scentry.authenticate('remember_me)
  }

}

trait AuthenticationSupport[UserClass >: Null <: AppUser[_]] extends ScentrySupport[UserClass] with ScalateSupport { self: ServletBase with CookieSupport with FlashMapSupport ⇒

  protected def fromSession = { case id: String ⇒ authProvider.findUserById(id).orNull }
  protected def toSession = { case usr: AppUser[_] ⇒ usr.idString }

  type ScentryConfiguration = OAuthScentryConfig
  protected val scentryConfig = new OAuthScentryConfig

  protected def userManifest: Manifest[UserClass]
  type AuthProvider = UserProvider[UserClass] with ForgotPasswordProvider[UserClass] with RememberMeProvider[UserClass]

  implicit override protected def user = scentry.user
  protected def oauth: OAuth2Extension
  protected def authProvider: AuthProvider

  /**
   * Registers authentication strategies.
   */
  override protected def registerAuthStrategies = {
    Seq(
      new PasswordStrategy(self, authProvider),
      new ForgotPasswordStrategy(self, authProvider),
      new RememberMeStrategy(self, authProvider),
      new AppUserBasicAuth(self, oauth.web.realm, authProvider)) foreach { strategy ⇒
        scentry.registerStrategy(strategy.name, _ ⇒ strategy)
      }

  }

  def unauthenticated() = {
    session(scentryConfig.returnToKey) = request.getRequestURI
    redirect(scentryConfig.failureUrl)
  }

  def redirectIfAuthenticated() = if (isAuthenticated) redirectAuthenticated()

  def redirectAuthenticated() = redirect(session.get(scentryConfig.returnToKey).map(_.toString) | scentryConfig.returnTo)

  def loggedIn(authenticated: UserClass, message: String) = {
    scentry.user = authenticated
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
    ctx
  }

  /**
   * Builds a full URL from the given relative path. Takes into account the port configuration, https, ...
   *
   * @param path a relative path
   *
   * @return the full URL
   */
  protected def buildFullUrl(path: String): String

}