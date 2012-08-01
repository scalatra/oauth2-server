package org.scalatra
package oauth2

import auth.{ OAuthToken, ScribeAuthSupport }
import akka.actor.ActorSystem
import dispatch._
import dispatch.oauth._
import model._
import model.Account
import model.LinkedOAuthAccount
import scalaz._
import Scalaz._
import OAuth2Imports._
import org.scribe.builder.api.{ TwitterApi, FacebookApi }
import net.liftweb.json._
import org.scalatra.{ CookieOptions, CookieSupport, FlashMapSupport, ScalatraServlet }
import annotation.tailrec
import command.FieldError
import org.scalatra.liftjson.LiftJsonSupport
import service.AuthenticationService
import com.ning.http.client.oauth.{ RequestToken, ConsumerKey }

class FacebookApiCalls(accessToken: OAuthToken)(implicit formats: Formats) {
  private val urlBase = :/("graph.facebook.com/").secure
  private val atParams = Map("access_token" -> accessToken.token)

  val http = dispatch.Http
  def getProfile(id: Option[Int] = None): JValue = {
    http(urlBase / (id some (_.toString) none "me") <<? atParams OK oauth2.as.JValue)()
  }
}

trait TwitterApiUrls extends oauth.SomeEndpoints {
  val requestToken: String = "https://api.twitter.com/oauth/request_token"

  val accessToken: String = "https://api.twitter.com/oauth/access_token"

  val authorize: String = "https://api.twitter.com/oauth/authorize"
}

class TwitterApiCalls(accessToken: OAuthToken, provider: OAuthProvider, val callback: String)(implicit formats: Formats)
    extends oauth.SomeHttp with oauth.SomeCallback with oauth.SomeConsumer with oauth.Exchange with TwitterApiUrls {

  val consumer: ConsumerKey = new ConsumerKey(provider.clientId, provider.clientSecret)

  val http: Executor = dispatch.Http

  private val token = new RequestToken(accessToken.token, accessToken.secret)

  private val urlBase = :/("api.twitter.com").secure / "1"

  def getProfile(id: Option[Int] = None): JValue = {
    val u = urlBase / "account" / "verify_credentials.json"
    http(u <@ (consumer, token) OK oauth2.as.JValue)()
  }
}

class OAuthAuthentication(implicit protected val system: ActorSystem)
    extends ScalatraServlet with XsrfTokenSupport with FlashMapSupport with CookieSupport with LiftJsonSupport with ScribeAuthSupport[AuthSession] {

  val oauth = OAuth2Extension(system)
  protected val authProvider: AccountDao = oauth.userProvider
  override protected lazy val jsonVulnerabilityGuard: Boolean = true
  override implicit val jsonFormats: Formats = new OAuth2Formats

  protected def authService: AuthenticationService = oauth.authService

  val userManifest = manifest[AuthSession]

  protected lazy val authCookieOptions = cookieOptions.copy(
    domain = (if (oauth.web.domain == ".localhost") "localhost" else oauth.web.domain),
    secure = oauth.web.sslRequired,
    httpOnly = true)

  before() {

    @tailrec
    def createName(nameVal: String, appendix: Int): String = {
      val proposal = if (appendix > 0) nameVal + "_" + appendix.toString else nameVal
      if (authProvider.count(Map("login" -> proposal)) > 0) createName(nameVal, (appendix + 1)) else proposal
    }

    val facebookProvider = oauth.providers("facebook") // requires at least scope email for facebook
    registerOAuthService(facebookProvider.name, facebookProvider.service[FacebookApi](callbackUrlFormat)) { token ⇒
      val fbUser = new FacebookApiCalls(token).getProfile()
      val fbEmail = (fbUser \ "email").extract[String]
      val fbUsername = (fbUser \ "username").extract[String]
      val foundUser = authProvider.findByLoginOrEmail(fbEmail) orElse authProvider.findByLinkedAccount(facebookProvider.name, fbUsername)
      val usr = (foundUser getOrElse {
        Account(
          login = createName(fbUsername, 0),
          email = fbEmail,
          name = (fbUser \ "name").extract[String],
          password = BCryptPassword.random,
          confirmedAt = DateTime.now)
      })
      val linkedAccounts = (LinkedOAuthAccount("facebook", (fbUser \ "username").extract[String]) :: usr.linkedOAuthAccounts).distinct
      authService.loggedIn(usr.copy(linkedOAuthAccounts = linkedAccounts), request.remoteAddress)
    }

    val twitterProvider = oauth.providers("twitter")
    registerOAuthService(twitterProvider.name, twitterProvider.service[TwitterApi](callbackUrlFormat)) { token ⇒
      val twitterUser = new TwitterApiCalls(token, twitterProvider, callbackUrlFormat.format(twitterProvider.name)).getProfile()
      val twLogin = (twitterUser \ "screen_name").extract[String]
      val owner = authProvider.findByLinkedAccount(twitterProvider.name, twLogin) getOrElse Account(
        login = createName(twLogin, 0),
        email = "",
        name = (twitterUser \ "name").extract[String],
        password = BCryptPassword.random,
        confirmedAt = DateTime.now)
      val linkedAccounts = (LinkedOAuthAccount("twitter", twLogin) :: owner.linkedOAuthAccounts).distinct
      authService.loggedIn(owner.copy(linkedOAuthAccounts = linkedAccounts), request.remoteAddress)
    }
  }

  protected def trySavingCompletedProfile() = {
    val usr = user.account.copy(login = ~params.get("login"), email = ~params.get("email"), name = ~params.get("name"), password = BCryptPassword.random)
    val validated = authService.validate(usr)
    validated flatMap (u ⇒ authService.completedProfile(u, this.remoteAddress))
  }

  private[this] def urlWithContextPath(path: String, params: Iterable[(String, Any)] = Iterable.empty): String = {
    val newPath = path match {
      case x if x.startsWith("/") ⇒ ensureSlash(contextPath) + ensureSlash(path)
      case _                      ⇒ ensureSlash(path)
    }
    val pairs = params map { case (key, value) ⇒ key.urlEncode + "=" + value.toString.urlEncode }
    val queryString = if (pairs.isEmpty) "" else pairs.mkString("?", "&", "")
    println("The url with context path: %s" format newPath)
    newPath + queryString
  }

  private def ensureSlash(candidate: String) = {
    (candidate.startsWith("/"), candidate.endsWith("/")) match {
      case (true, true)   ⇒ candidate.dropRight(1)
      case (true, false)  ⇒ candidate
      case (false, true)  ⇒ "/" + candidate.dropRight(1)
      case (false, false) ⇒ "/" + candidate
    }
  }

  private[this] def callbackUrlFormat = {
    oauth.web.appUrl + urlWithContextPath("auth/%s/callback")
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

}
