package io.backchat.oauth2

import net.liftweb.json._

class OAuthString(s: String) {
  def blankOption = if (isBlank) None else Some(s)
  def isBlank = s == null || s.trim.isEmpty
  def nonBlank = !isBlank
  def asCheckboxBool = s.toUpperCase(ENGLISH) match {
    case "ON" | "TRUE" | "OK" | "1" | "CHECKED" ⇒ true
    case _                                      ⇒ false
  }
  def urlEncode: String = { // Encoding comforming to RFC 3986
    UrlCodingUtils.urlEncode(s)
  }
  def formEncode: String = { // This gives the same output as java.net.URLEncoder
    UrlCodingUtils.urlEncode(s, spaceIsPlus = true)
  }
  def urlDecode: String = {
    UrlCodingUtils.urlDecode(s, plusIsSpace = false)
  }

  def formDecode: String = { // This gives the same output as java.net.URLDecoder
    UrlCodingUtils.urlDecode(s, plusIsSpace = true)
  }
}

class OAuthJValue(json: JValue) {

  import OAuth2Imports.string2InflectorString

  def camelizeKeys = rewriteJsonAST(true)
  def snakizeKeys = rewriteJsonAST(false)

  private def rewriteJsonAST(camelize: Boolean): JValue = {
    json transform {
      case JField(nm, x) if !nm.startsWith("_") ⇒ JField(if (camelize) nm.camelize else nm.underscore, x)
      case x                                    ⇒ x
    }
  }
}