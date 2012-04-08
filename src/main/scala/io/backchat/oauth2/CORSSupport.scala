package io.backchat.oauth2

import org.scalatra._
import OAuth2Imports._
import akka.actor.ActorSystem
import scalaz._
import Scalaz._

object CORSSupport {
  val ORIGIN_HEADER: String = "Origin"
  val ACCESS_CONTROL_REQUEST_METHOD_HEADER: String = "Access-Control-Request-Method"
  val ACCESS_CONTROL_REQUEST_HEADERS_HEADER: String = "Access-Control-Request-Headers"
  val ACCESS_CONTROL_ALLOW_ORIGIN_HEADER: String = "Access-Control-Allow-Origin"
  val ACCESS_CONTROL_ALLOW_METHODS_HEADER: String = "Access-Control-Allow-Methods"
  val ACCESS_CONTROL_ALLOW_HEADERS_HEADER: String = "Access-Control-Allow-Headers"
  val ACCESS_CONTROL_MAX_AGE_HEADER: String = "Access-Control-Max-Age"
  val ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER: String = "Access-Control-Allow-Credentials"

  private val ANY_ORIGIN: String = "*"
  private val SIMPLE_HEADERS = List(ORIGIN_HEADER.toUpperCase(ENGLISH), "ACCEPT", "ACCEPT-LANGUAGE", "CONTENT-LANGUAGE")
  private val SIMPLE_CONTENT_TYPES = List("APPLICATION/X-WWW-FORM-URLENCODED", "MULTIPART/FORM-DATA", "TEXT/PLAIN")
  val CORS_HEADERS = List(
    ORIGIN_HEADER,
    ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER,
    ACCESS_CONTROL_ALLOW_HEADERS_HEADER,
    ACCESS_CONTROL_ALLOW_METHODS_HEADER,
    ACCESS_CONTROL_ALLOW_ORIGIN_HEADER,
    ACCESS_CONTROL_MAX_AGE_HEADER,
    ACCESS_CONTROL_REQUEST_HEADERS_HEADER,
    ACCESS_CONTROL_REQUEST_METHOD_HEADER)
}
trait CORSSupport extends Handler { self: ScalatraBase ⇒

  import CORSSupport._

  implicit protected def system: ActorSystem
  protected lazy val corsConfig = OAuth2Extension(system).web.cors
  private lazy val anyOriginAllowed: Boolean = corsConfig.allowedOrigins.contains(ANY_ORIGIN)
  import corsConfig._

  logger debug "Enabled CORS Support with:\nallowedOrigins:\n\t%s\nallowedMethods:\n\t%s\nallowedHeaders:\n\t%s".format(
    allowedOrigins mkString ", ",
    allowedMethods mkString ", ",
    allowedHeaders mkString ", ")

  protected def handlePreflightRequest() {
    logger debug "handling preflight request"
    // 5.2.7
    augmentSimpleRequest()
    // 5.2.8
    if (preflightMaxAge > 0) response.headers(ACCESS_CONTROL_MAX_AGE_HEADER) = preflightMaxAge.toString
    // 5.2.9
    response.headers(ACCESS_CONTROL_ALLOW_METHODS_HEADER) = allowedMethods mkString ","
    // 5.2.10
    response.headers(ACCESS_CONTROL_ALLOW_HEADERS_HEADER) = allowedHeaders mkString ","
    response.end()

  }

  protected def augmentSimpleRequest() {
    val hdr = if (anyOriginAllowed && !allowCredentials) ANY_ORIGIN else ~request.headers.get(ORIGIN_HEADER)
    response.headers(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER) = hdr
    if (allowCredentials) response.headers(ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER) = "true"
    /*
    if (allowedHeaders.nonEmpty) {
      val hdrs = allowedHeaders.filterNot(hn => SIMPLE_RESPONSE_HEADERS.contains(hn.toUpperCase(ENGLISH))).mkString(",")
      response.addHeader(ACCESS_CONTROL_ALLOW_HEADERS_HEADER, hdrs)
    }
*/
  }

  private def originMatches = // 6.2.2
    anyOriginAllowed || (allowedOrigins contains ~request.headers.get(ORIGIN_HEADER))

  private def isEnabled =
    !("Upgrade".equalsIgnoreCase(~request.headers.get("Connection")) &&
      "WebSocket".equalsIgnoreCase(~request.headers.get("Upgrade"))) &&
      !requestPath.contains("eb_ping") // don't do anything for the ping endpoint

  private def isValidRoute: Boolean = routes.matchingMethods.nonEmpty
  private def isPreflightRequest = {
    val isCors = isCORSRequest
    val validRoute = isValidRoute
    val isPreflight = request.headers.get(ACCESS_CONTROL_REQUEST_METHOD_HEADER).flatMap(_.blankOption).isDefined
    val enabled = isEnabled
    val matchesOrigin = originMatches
    val methodAllowed = allowsMethod
    val allowsHeaders = headersAreAllowed
    val result = isCors && validRoute && isPreflight && enabled && matchesOrigin && methodAllowed && allowsHeaders
    //    logger debug "This is a preflight validation check. valid? %s".format(result)
    //    logger debug "cors? %s, route? %s, preflight? %s, enabled? %s, origin? %s, method? %s, header? %s".format(
    //      isCors, validRoute, isPreflight, enabled, matchesOrigin, methodAllowed, allowsHeaders)
    result
  }

  private def isCORSRequest = request.headers.get(ORIGIN_HEADER).flatMap(_.blankOption).isDefined // 6.x.1

  private def isSimpleHeader(header: String) = {
    val ho = header.blankOption
    ho.isDefined && (ho forall { h ⇒
      val hu = h.toUpperCase(ENGLISH)
      SIMPLE_HEADERS.contains(hu) || (hu == "CONTENT-TYPE" &&
        SIMPLE_CONTENT_TYPES.exists((~request.contentType).toUpperCase(ENGLISH).startsWith))
    })
  }

  private def allOriginsMatch = { // 6.1.2
    val h = request.headers.get(ORIGIN_HEADER).flatMap(_.blankOption)
    h.isDefined && h.get.split(" ").nonEmpty && h.get.split(" ").forall(allowedOrigins.contains)
  }

  private def isSimpleRequest = {
    val isCors = isCORSRequest
    val enabled = isEnabled
    val allOrigins = allOriginsMatch
    val res = isCors && enabled && allOrigins && request.headers.keys.forall(isSimpleHeader)
    //    logger debug "This is a simple request: %s, because: %s, %s, %s".format(res, isCors, enabled, allOrigins)
    res
  }

  private def allowsMethod = { // 5.2.3 and 5.2.5
    val accessControlRequestMethod = ~request.headers.get(ACCESS_CONTROL_REQUEST_METHOD_HEADER).flatMap(_.blankOption)
    //    logger.debug("%s is %s" format (ACCESS_CONTROL_REQUEST_METHOD_HEADER, accessControlRequestMethod))
    val result = accessControlRequestMethod.nonBlank && allowedMethods.contains(accessControlRequestMethod.toUpperCase(ENGLISH))
    //    logger.debug("Method %s is %s among allowed methods %s".format(accessControlRequestMethod, if (result) "" else " not", allowedMethods))
    result
  }

  private def headersAreAllowed = { // 5.2.4 and 5.2.6
    val accessControlRequestHeaders = request.headers.get(ACCESS_CONTROL_REQUEST_HEADERS_HEADER).flatMap(_.blankOption)
    //    logger.debug("%s is %s".format(ACCESS_CONTROL_REQUEST_HEADERS_HEADER, accessControlRequestHeaders))
    val ah = (allowedHeaders ++ CORS_HEADERS).map(_.trim.toUpperCase(ENGLISH))
    val result = allowedHeaders.contains(ANY_ORIGIN) && (accessControlRequestHeaders forall { hdr ⇒
      val hdrs = hdr.split(",").map(_.trim.toUpperCase(ENGLISH))
      //      logger.debug("Headers [%s]".format(hdrs))
      (hdrs.nonEmpty && hdrs.forall { h ⇒ ah.contains(h) }) || isSimpleHeader(hdr)
    })
    //    logger.debug("Headers [%s] are %s among allowed headers %s".format(
    //      accessControlRequestHeaders getOrElse "No headers", if (result) "" else " not", ah))
    result
  }

  abstract override def handle(req: this.RequestT, res: this.ResponseT) {
    withRequestResponse(req, res) {
      //      logger debug "the headers are: %s".format(req.getHeaderNames.mkString(", "))
      request.requestMethod match {
        case Options if isPreflightRequest ⇒ {
          handlePreflightRequest()
        }
        case Get | Post | Head if isSimpleRequest ⇒ {
          augmentSimpleRequest()
          super.handle(req, res)
        }
        case _ if isCORSRequest ⇒ {
          augmentSimpleRequest()
          super.handle(req, res)
        }
        case _ ⇒ super.handle(req, res)
      }
    }
  }

}
