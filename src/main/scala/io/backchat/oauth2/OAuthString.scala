package io.backchat.oauth2

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
