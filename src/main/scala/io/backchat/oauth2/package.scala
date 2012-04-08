package io.backchat

import org.slf4j.LoggerFactory
import scalaz._
import Scalaz._
import java.util.Locale
import org.joda.time._
import format.ISODateTimeFormat
import java.nio.charset.Charset
import org.scalatra.{ Request, ScalatraBase }
import java.net.URI

package object oauth2 {

  import OAuth2Imports._
  val ENGLISH = Locale.ENGLISH
  private[oauth2] implicit val mongoDBObject2Zero: Zero[com.mongodb.DBObject] = zero(new BasicDBObject)

  private[oauth2] val logger = LoggerFactory.getLogger("OAuth2 Server")

  private[oauth2] implicit def uri2richerUri(uri: URI) = new OAuthUri(uri)

  private[oauth2] implicit def request2oauthRequest(req: Request) = new OAuthRequest(req)

  private[oauth2] implicit def servletBase2RicherServletBase(base: ScalatraBase) = new {
    def remoteAddress = base.request.remoteAddress

    def isHttps = { // also respect load balancer version of the protocol
      val h = base.request.headers.get("X-FORWARDED-PROTO").flatMap(_.blankOption)
      base.request.isSecure || (h.isDefined && h.forall(_.toUpperCase(ENGLISH) == "HTTPS"))
    }

  }

  val UTF_8 = "UTF-8"
  val Utf8 = Charset.forName(UTF_8)
  val UTC_STR = "UTC"
  val UTC = DateTimeZone.UTC

  val MinDate = new DateTime(0L)
  val minDateCal = MinDate.toCalendar(ENGLISH)
  val minDate = MinDate.toDate
  val Iso8601DateNoMillis = ISODateTimeFormat.dateTimeNoMillis.withZone(UTC)
  val Iso8601Date = ISODateTimeFormat.dateTime.withZone(UTC)

}