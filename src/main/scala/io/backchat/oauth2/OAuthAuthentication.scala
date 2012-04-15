package io.backchat.oauth2

import auth.{ OAuthToken, ScribeAuthSupport }
import akka.actor.ActorSystem
import model.{ BCryptPassword, ResourceOwner }
import org.scalatra.{ CookieSupport, FlashMapSupport, ScalatraServlet }
import dispatch._
import dispatch.oauth._
import dispatch.liftjson.Js._
import scalaz._
import Scalaz._
import java.security.SecureRandom
import org.apache.commons.codec.binary.Hex
import OAuth2Imports._
import org.scribe.builder.api.{ TwitterApi, FacebookApi }
import net.liftweb.json._

class FacebookApiCalls(accessToken: OAuthToken)(implicit formats: Formats) {
  private val urlBase = "https://graph.facebook.com/"
  private val atParams = Map("access_token" -> accessToken.token)

  def getProfile(id: Option[Int] = None): JValue = {
    Http(url(urlBase + "/" + (id some (_.toString) none "me")) <<? atParams ># identity)
  }
}

class TwitterApiCalls(accessToken: OAuthToken, provider: OAuthProvider)(implicit formats: Formats) {
  import OAuth._
  private val consumer = dispatch.oauth.Consumer(provider.clientId, provider.clientSecret)
  private val token = dispatch.oauth.Token(accessToken.token, accessToken.secret)

  private val urlBase = "https://api.twitter.com/1/"

  def getProfile(id: Option[Int] = None): JValue = {
    Http(url(urlBase + "/account/verify_credentials.json") <@ (consumer, token) ># identity)
  }
}

class OAuthAuthentication(implicit system: ActorSystem)
    extends ScalatraServlet with FlashMapSupport with CookieSupport with ScribeAuthSupport[ResourceOwner] {

  val oauth = OAuth2Extension(system)
  protected val authProvider = oauth.userProvider
  implicit val jsonFormats: Formats = new OAuth2Formats

  before() {
    val facebookProvider = oauth.providers("facebook") // requires scope email at least for facebook
    registerOAuthService(facebookProvider.name, facebookProvider.service[FacebookApi](callbackUrlFormat)) { token ⇒
      val fbUser = new FacebookApiCalls(token).getProfile()
      val fbEmail = (fbUser \ "email").extract[String]
      val foundUser = authProvider.findByLoginOrEmail(fbEmail)
      (foundUser getOrElse {
        val usr = ResourceOwner(
          login = (fbUser \ "username").extract[String],
          email = fbEmail,
          name = (fbUser \ "name").extract[String],
          password = BCryptPassword(randomPassword).encrypted,
          confirmedAt = DateTime.now)
        authProvider.loggedIn(usr, request.remoteAddress)
      }).success[model.Error]
    }

    val twitterProvider = oauth.providers("twitter")
    registerOAuthService(twitterProvider.name, twitterProvider.service[TwitterApi](callbackUrlFormat)) { token ⇒
      val twitterUser = new TwitterApiCalls(token, twitterProvider).getProfile()

      null
    }
  }

  private[this] def randomPassword = {
    val rand = new SecureRandom
    val pwdBytes = Array.ofDim[Byte](8)
    rand.nextBytes(pwdBytes)
    Hex.encodeHexString(pwdBytes)
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
    oauth.web.appUrl + urlWithContextPath("/auth/%s/callback")
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
