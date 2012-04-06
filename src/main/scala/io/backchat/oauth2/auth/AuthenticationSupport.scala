package io.backchat.oauth2
package auth

import org.scalatra.servlet.ServletBase
import org.scalatra.auth.{ ScentryStrategy, ScentryConfig, ScentrySupport }
import org.fusesource.scalate.Binding
import javax.servlet.http.{ HttpServletResponse, HttpServletRequest }
import java.io.PrintWriter
import model.{ AlreadyConfirmed, ResourceOwner }
import org.scalatra.{ FlashMapSupport, CookieSupport }
import org.scalatra.scalate.ScalateSupport
import scalaz._
import Scalaz._

trait AuthenticationSupport[UserClass >: Null <: AppUser[_]] extends ScentrySupport[UserClass] with ScalateSupport { self: ServletBase with CookieSupport with FlashMapSupport ⇒

  protected def fromSession = { case id: String ⇒ authProvider.findUserById(id).orNull }
  protected def toSession = { case usr: AppUser[_] ⇒ usr.idString }

  type ScentryConfiguration = ScentryConfig
  protected val scentryConfig = new ScentryConfig {}

  protected def userManifest: Manifest[UserClass]
  type AuthProvider = UserProvider[UserClass] with ForgotPasswordProvider[UserClass] with RememberMeProvider[UserClass]

  implicit override protected def user = scentry.user
  protected def authProvider: AuthProvider

  /**
   * Registers authentication strategies.
   */
  override protected def registerAuthStrategies = {
    Seq(
      new PasswordStrategy(self, authProvider),
      new ForgotPasswordStrategy(self, authProvider),
      new RememberMeStrategy(self, authProvider)) foreach { strategy ⇒
        scentry.registerStrategy(strategy.name, _ ⇒ strategy)
      }

  }

  before() {
    if (!isAuthenticated)
      scentry.authenticate('remember_me)
  }

  get("/login") {
    if (isAuthenticated) redirect("/")
    jade("login")
  }

  post("/login") {
    if (isAuthenticated) redirect("/")
    authenticate()
    if (isAnonymous) {
      flash.now("error") = "Username/password is incorrect"
      jade("login")
    } else {
      flash("success") = ("Welcome back, " + user.name)
      redirect("/")
    }
  }

  get("/register") {
    if (isAuthenticated) redirect("/")
    jade("register")
  }

  post("/register") {
    if (isAuthenticated) redirect("/")
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

  get("/forgot") {
    jade("forgot")
  }

  post("/forgot") {
    jade("forgot")
  }

  get("/logout") {
    scentry.logout()
    redirect("/login")
  }

  get("/activate/:token") {
    authProvider.confirm(params("token")).fold(
      {
        case m: AlreadyConfirmed ⇒
          flash("warn") = m.message + " Please log in."
          redirect("/login")
        case m ⇒
          jade("error", "errors" -> m.message)
      },
      owner ⇒ {
        flash("success") = "Account confirmed!"
        redirect("/login")
      })
  }

  def loggedIn(authenticated: UserClass, message: String) = {
    scentry.user = authenticated
    flash("success") = message
    redirect("/")
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
    val ctx = super.createRenderContext(req, resp, out)
    ctx.attributes.update("userOption", userOption)
    ctx.attributes.update("user", user)
    ctx.attributes.update("isAnonymous", isAnonymous)
    ctx.attributes.update("isAuthenticated", isAuthenticated)
    ctx
  }
}
