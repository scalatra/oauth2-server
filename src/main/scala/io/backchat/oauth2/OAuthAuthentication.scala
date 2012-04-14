package io.backchat.oauth2

import auth.ScribeAuthSupport
import model.ResourceOwner
import akka.actor.ActorSystem
import org.scalatra.{ SessionSupport, CookieSupport, FlashMapSupport, ScalatraServlet }
import scentry.ScentrySupport

class OAuthAuthentication(implicit system: ActorSystem)
    extends ScalatraServlet with FlashMapSupport with CookieSupport with ScribeAuthSupport[ResourceOwner] {
  protected def authProvider = null

}
