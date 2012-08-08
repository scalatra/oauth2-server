package org.scalatra

import org.slf4j.LoggerFactory
import _root_.scalaz._
import Scalaz._
import java.util.Locale
import org.joda.time._
import format.ISODateTimeFormat
import java.net.URI
import scala.io.Codec
import javax.servlet.http.HttpServletRequest

package object oauth2 extends _root_.org.scalatra.servlet.ServletApiImplicits {

  import OAuth2Imports._
  val ENGLISH = Locale.ENGLISH
  private[oauth2] implicit val mongoDBObject2Zero: Zero[com.mongodb.DBObject] = zero(new BasicDBObject)

  private[oauth2] val logger = LoggerFactory.getLogger("OAuth2 Server")

  private[oauth2] implicit def uri2richerUri(uri: URI) = new OAuthUri(uri)

  private[oauth2] implicit def request2oauthRequest(req: HttpServletRequest) = new OAuthRequest(req)

  private[oauth2] implicit def servletBase2RicherServletBase(base: ScalatraBase) = new {
    def remoteAddress = base.request.remoteAddress

    def isHttps = { // also respect load balancer version of the protocol
      def h = base.request.headers.get("X-FORWARDED-PROTO").flatMap(_.blankOption).map(_.toUpperCase(ENGLISH))
      base.request.isSecure || h == Some("HTTPS")
    }

  }

  val UTF_8 = Codec.UTF8.name()
  val Utf8 = Codec.UTF8
  val UTC_STR = "UTC"
  val UTC = DateTimeZone.UTC

  val MinDate = new DateTime(0L)
  val minDateCal = MinDate.toCalendar(ENGLISH)
  val minDate = MinDate.toDate
  val Iso8601DateNoMillis = ISODateTimeFormat.dateTimeNoMillis.withZone(UTC)
  val Iso8601Date = ISODateTimeFormat.dateTime.withZone(UTC)

  val ActorSystemContextKey = "org.scalatra.oauth2.ServerActorSystem"
  val ActorSystemName = "oauth2server"

  private[oauth2] def confKey(path: String) = "scalatra.oauth2." + path

  object as {
    val JValue = dispatch.as.String.andThen(net.liftweb.json.parse)
  }
}