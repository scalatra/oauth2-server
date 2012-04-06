package io.backchat.oauth2
package auth

import model.ResourceOwner
import org.scalatra.{CookieSupport, FlashMapSupport, ScalatraServlet}
import org.scalatra.scalate.ScalateSupport
import akka.actor.ActorSystem

trait OAuth2ServerBaseApp extends ScalatraServlet
    with FlashMapSupport
    with CookieSupport
    with ScalateSupport
    with AuthenticationSupport[ResourceOwner] {

  implicit protected def system: ActorSystem
  val oauth = OAuth2Extension(system)

  protected val userManifest = manifest[ResourceOwner]

  protected lazy val authProvider = oauth.userProvider


}