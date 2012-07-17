package io.backchat.oauth2

import org.eclipse.jetty.http.HttpHeaders
import net.iharder.Base64
import scalaz._
import Scalaz._
import OAuth2Imports._
import javax.servlet.http.HttpServletRequest

class OAuthRequest(req: HttpServletRequest) {

  private[this] def authorizationKey = req.headers.get(HttpHeaders.AUTHORIZATION).flatMap(_.blankOption)
  /**
   * A flag to indicate this request has the OAuth2 Authorization header
   */
  def isOAuth2 = authScheme.isDefined && authScheme.forall(_.startsWith("oauth"))
  /**
   * A flag to indicate this request has the Basic Authentication Authorization header
   */
  def isBasicAuth = authScheme.isDefined && authScheme.forall(_.startsWith("basic"))

  /**
   * A flag to indicate whether this request provides authentication information
   */
  def providesAuth = authorizationKey.isDefined

  /**
   * Returns the username for this request
   */
  def username = credentials map { _._1 } getOrElse null

  /**
   * Returns the password for this request
   */
  def password = credentials map { _._2 } getOrElse null

  /**
   * The authentication scheme for this request
   */
  def authScheme = parts.headOption.map(sch ⇒ sch.toLowerCase(ENGLISH))

  /**
   * The elements contained in the header value
   */
  def parts: Seq[String] = authorizationKey.map(_.split(" ", 2).toSeq) | Nil

  /**
   * The user provided parts contained in the header value
   */
  def params = parts.tail.headOption

  private var _credentials: Option[(String, String)] = None
  /**
   * The credentials for this request
   */
  def credentials = {
    if (_credentials.isEmpty)
      _credentials = params map { p ⇒
        if (isBasicAuth) {
          (null.asInstanceOf[(String, String)] /: new String(Base64.decode(p), Utf8).split(":", 2)) { (t, l) ⇒
            if (t == null) (l, null) else (t._1, l)
          }
        } else {
          (p, null)
        }
      }
    _credentials
  }
}
