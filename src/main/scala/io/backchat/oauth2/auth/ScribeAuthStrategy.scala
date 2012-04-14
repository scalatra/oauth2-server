package io.backchat.oauth2
package auth

import scala.util.control.Exception._
import scalaz._
import Scalaz._
import org.scribe.model.Verifier
import org.scribe.oauth.{ OAuth20ServiceImpl, OAuth10aServiceImpl, OAuthService }
import OAuth2Imports._
import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConverters._
import org.scalatra._
import scentry.{ ScentrySupport, ScentryStrategy }
import scentry.ScentryAuthStore.HttpOnlyCookieAuthStore

object OAuthToken {
  def apply(scribeToken: org.scribe.model.Token): OAuthToken = OAuthToken(scribeToken.getToken, scribeToken.getSecret)
}
case class OAuthToken(token: String, secret: String)

trait ScribeAuthStrategyContext[UserClass >: Null <: AppUser[_]] {
  def oauthService: OAuthService
  def name: String

  def app: ScalatraBase with FlashMapSupport with ScribeAuthSupport[UserClass]
  def createOrUpdateFromOAuthService(accessToken: OAuthToken): UserClass
}

trait ScribeAuthSupport[UserClass >: Null <: AppUser[_]] extends ScentrySupport[UserClass] { self: ScalatraBase with SessionSupport with FlashMapSupport ⇒

  private[this] val oauthServicesRegistry = new ConcurrentHashMap[String, OAuthService].asScala

  protected def fromSession = { case id: String ⇒ authProvider.findUserById(id).orNull }
  protected def toSession = { case usr: AppUser[_] ⇒ usr.idString }

  type ScentryConfiguration = OAuthScentryConfig
  protected val scentryConfig = new OAuthScentryConfig

  type AuthProvider = UserProvider[UserClass]
  protected def authProvider: AuthProvider

  private[this] val thisApp = this

  protected def sslRequired: Boolean = true

  def registerOAuthService(name: String, service: OAuthService)(createOrUpdateUser: OAuthToken ⇒ UserClass) = {
    val nm = name
    scentry.registerStrategy(name, _ ⇒ new ScribeAuthStrategy[UserClass](new ScribeAuthStrategyContext[UserClass] {
      val oauthService = service
      val name = nm
      val app = thisApp
      def createOrUpdateFromOAuthService(accessToken: OAuthToken) = createOrUpdateUser(accessToken)
    }))
    oauthServicesRegistry += name -> service
  }

  get("/:provider") {
    if (!oauthServicesRegistry.contains(params("provider"))) halt(404, "The provider [" + params("provider") + "] is not available.")

    oauthServicesRegistry get (params("provider")) flatMap {
      case svc: OAuth10aServiceImpl ⇒
        val tok = svc.getRequestToken
        if (tok == null) halt(502, "Couldn't obtain a request token for " + params("provider"))
        ScribeAuthStrategy.requestTokens(tok.getToken) = tok
        svc.getAuthorizationUrl(tok).blankOption

      case svc ⇒ svc.getAuthorizationUrl(null).blankOption
    } foreach redirect

    halt(400, "Couldn't get a authorization url for oauth provider: %s" format params("provider"))
  }

  get("/:provider/callback") {
    scentry.authenticate(params("provider"))
    userOption.fold(u ⇒ loggedIn(u.login + " logged in from " + params("provider") + "."), unauthenticated())
  }

  /**
   * Registers authentication strategies.
   */
  override protected def configureScentry {
    scentry.store = new HttpOnlyCookieAuthStore(this, sslRequired)
    scentry.unauthenticated {
      unauthenticated()
    }
  }

  def unauthenticated() {
    session(scentryConfig.returnToKey) = request.uri.toASCIIString
    redirect(scentryConfig.failureUrl)
  }

  protected def redirectIfAuthenticated() = if (isAuthenticated) redirectAuthenticated()

  protected def redirectAuthenticated() = redirect(session.get(scentryConfig.returnToKey).map(_.toString) | scentryConfig.returnTo)

  protected def loggedIn(message: String) {
    flash("success") = message
    redirectAuthenticated()
  }

}

object ScribeAuthStrategy {
  private[auth] val requestTokens = new ConcurrentHashMap[String, org.scribe.model.Token].asScala

}
class ScribeAuthStrategy[UserClass >: Null <: AppUser[_]](context: ScribeAuthStrategyContext[UserClass]) extends ScentryStrategy[UserClass] {

  import ScribeAuthStrategy._
  override val name = context.name
  protected val app = context.app

  override def isValid =
    app.request.requestMethod == Get &&
      app.params.contains("provider") &&
      app.params("provider") == name &&
      matchesForOAuthVersion

  private[this] def matchesForOAuthVersion = context.oauthService match {
    case _: OAuth20ServiceImpl  ⇒ hasKey("code")
    case _: OAuth10aServiceImpl ⇒ hasKey("oauth_token") && hasKey("oauth_verifier")
    case _                      ⇒ false
  }

  private[this] def hasKey(key: String) = app.params.get(key).flatMap(_.blankOption).isDefined
  private[this] def verifier: String = context.oauthService match {
    case _: OAuth20ServiceImpl  ⇒ app.params("code")
    case _: OAuth10aServiceImpl ⇒ app.params("oauth_verifier")
  }

  override def unauthenticated() {
    app.unauthenticated()
  }

  def authenticate(): Option[UserClass] =
    (allCatch withApply logError) {
      val reqToken = app.params.get("oauth_token").flatMap(requestTokens.get)
      reqToken foreach (requestTokens -= _.getToken)
      val accessToken = OAuthToken(context.oauthService.getAccessToken(reqToken.orNull, new Verifier(verifier)))
      Option(context.createOrUpdateFromOAuthService(accessToken))
    }

  private[this] def logError(ex: Throwable): Option[UserClass] = {
    logger.error("There was a problem authenticating with " + name, ex)
    none[UserClass]
  }

}

