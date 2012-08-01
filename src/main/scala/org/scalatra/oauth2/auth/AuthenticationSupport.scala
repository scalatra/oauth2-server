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
import model.{ ApiErrorList, OAuth2Response, ApiError }
import net.liftweb.json.Extraction
import liftjson.{ LiftJsonSupport }
import OAuth2Imports._
import net.liftweb.json.JsonAST.JNull
import commands._
import service.AuthenticationService
import model.ApiErrorList
import model.OAuth2Response
import akka.actor.ActorSystem

class OAuthScentryConfig extends ScentryConfig

trait PasswordAuthSupport[UserClass >: Null <: AppAuthSession[_ <: AppUser[_]]] { self: ScalatraBase with FlashMapSupport with CookieSupport with AuthenticationSupport[UserClass] with LiftJsonSupport ⇒

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
    authService.execute(registerCommand)
    //    format match {
    //      case "json" | "xml" ⇒
    //        val json = parsedBody
    //        val regResult =
    //          authProvider.register(
    //            (json \ "login").extractOpt[String].flatMap(_.blankOption),
    //            (json \ "email").extractOpt[String].flatMap(_.blankOption),
    //            (json \ "name").extractOpt[String].flatMap(_.blankOption),
    //            (json \ "password").extractOpt[String].flatMap(_.blankOption),
    //            (json \ "passwordConfirmation").extractOpt[String].flatMap(_.blankOption))
    //        regResult.fold(
    //          errs ⇒ {
    //            val e = ApiErrorList((errs.list map {
    //              case er: ValidationError ⇒ ApiError(er.field, er.message)
    //              case er                  ⇒ ApiError(er.message)
    //            }).toList)
    //            BadRequest(OAuth2Response(parsedBody, e.toJValue))
    //          },
    //          loggedIn(_, "Registered and logged in."))
    //      case _ ⇒
    //        val regResult =
    //          authProvider.register(
    //            params.get("login"),
    //            params.get("email"),
    //            params.get("name"),
    //            params.get("password"),
    //            params.get("password_confirmation"))
    //        regResult.fold(
    //          errs ⇒ jade("register", "errors" -> errs.list),
    //          loggedIn(_, "Registered and logged in."))
    //    }
  }

  get("/logout") {
    logOut()
    redirect(scentryConfig.failureUrl)
  }

  get("/activate") {
    flash("error") = "The token is required"
    redirect(scentryConfig.login)
  }

  protected def activateCommand: ActivateAccountCommand
  get("/activate/:token") {
    authService.execute(activateCommand)
    //    authProvider.confirm(params("token")).fold(
    //      m ⇒ {
    //        flash("error") = m.message
    //        redirect(scentryConfig.login)
    //      },
    //      owner ⇒ {
    //        flash("success") = "Account confirmed!"
    //        redirect(scentryConfig.login)
    //      })
  }

  get("/unauthenticated") {
    flash("warn") = "You must be logged in to view this page."
    redirect(scentryConfig.login)
  }
}

trait ForgotPasswordAuthSupport[UserClass >: Null <: AppAuthSession[_ <: AppUser[_]]] { self: ScalatraBase with FlashMapSupport with AuthenticationSupport[UserClass] with LiftJsonSupport ⇒
  get("/forgot") {
    redirectIfAuthenticated()
    jade("angular")
  }

  protected def forgotCommand: ForgotCommand
  post("/forgot") {
    redirectIfAuthenticated()
    authService.execute(forgotCommand)
    //    format match {
    //      case "json" | "xml" ⇒
    //        authProvider.forgot(jsonParam[String]("login")).fold(
    //          err ⇒ ApiError(err.message),
    //          owner ⇒ OAuth2Response(Extraction.decompose(owner)))
    //      case _ ⇒
    //        authProvider.forgot(params.get("login")).fold(
    //          err ⇒ jade("forgot", "error" -> err.message),
    //          owner ⇒ {
    //            flash("info") = "Password reset link has been sent to <a href=\"mailto:%s\">%s</a>.".format(owner.email, owner.email)
    //            redirectAuthenticated()
    //          })
    //    }
  }

  get("/reset/:token") {
    redirectIfAuthenticated()
    jade("angular")
  }

  protected def resetCommand: ResetCommand
  post("/reset/:token") {
    redirectIfAuthenticated()
    authService.execute(resetCommand)
    //    format match {
    //      case "json" | "xml" ⇒
    //        authProvider.resetPassword(params("token"), ~jsonParam[String]("password"), ~jsonParam[String]("passwordConfirmation")).fold(
    //          err ⇒ {
    //            val e = ApiErrorList((err.list map {
    //              case er: ValidationError ⇒ ApiError(er.field, er.message)
    //              case er                  ⇒ ApiError(er.message)
    //            }).toList)
    //            BadRequest(OAuth2Response(parsedBody, e.toJValue))
    //          },
    //          loggedIn(_, "Password reset successfully"))
    //      case _ ⇒
    //        authProvider.resetPassword(params("token"), ~params.get("password"), ~params.get("password_confirmation")).fold(
    //          err ⇒ {
    //            (err.list.filter {
    //              case a: ValidationError ⇒ false
    //              case _                  ⇒ true
    //            }).headOption foreach { m ⇒ flash.now("error") = m.message }
    //            jade("reset", "errors" -> err.list.collect { case a: ValidationError ⇒ a })
    //          },
    //          loggedIn(_, "Password reset successfully"))
    //
    //    }

  }
  //
  //  private def jsonParam[TParam](key: String)(implicit mf: Manifest[TParam]): Option[TParam] = {
  //    val r = (parsedBody \ key).extractOpt[TParam]
  //    if (mf == manifest[String]) r.flatMap(_.asInstanceOf[String].blankOption).asInstanceOf[Option[TParam]]
  //    else r
  //  }

}

trait AuthenticationSupport[UserClass >: Null <: AppAuthSession[_ <: AppUser[_]]] extends ScentrySupport[UserClass] with ScalateSupport { self: ScalatraBase with SessionSupport with FlashMapSupport with LiftJsonSupport ⇒

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
    if (format == "html") flash("success") = message
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

trait DefaultAuthenticationSupport[UserClass >: Null <: AppAuthSession[_ <: AppUser[_]]] extends AuthenticationSupport[UserClass] { self: ScalatraBase with SessionSupport with CookieSupport with FlashMapSupport with LiftJsonSupport ⇒

  protected def oauth: OAuth2Extension

  /**
   * Registers authentication strategies.
   */
  override protected def configureScentry {
    val authCookieOptions = cookieOptions.copy(
      domain = (if (oauth.web.domain == ".localhost") "localhost" else oauth.web.domain),
      secure = oauth.web.sslRequired,
      httpOnly = true)
    scentry.store = new CookieAuthStore(self)(authCookieOptions)
    scentry.unauthenticated { unauthenticated() }
  }

  protected def loginCommand: LoginCommand

  override protected def registerAuthStrategies = {
    scentry.register("user_password", _ ⇒ new PasswordStrategy(self, loginCommand, authService))
    scentry.register("resource_owner_basic", _ ⇒ new AppUserBasicAuth(self, oauth.web.realm, loginCommand, authService))
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
