package org.scalatra
package oauth2
package model

import java.security.SecureRandom
import OAuth2Imports._

case class Token(token: String, createdAt: DateTime = DateTime.now) extends AppToken {
  def refreshed = Token()
}
object Token {

  def apply(): Token = generate()
  def generate(size: Int = 16, algo: String = "SHA1PRNG"): Token = {
    val random = SecureRandom.getInstance(algo)
    val pw = new Array[Byte](size)
    random.nextBytes(pw)
    Token(pw.hexEncode())
  }

  def isMatch(candidate: String, toMatch: Token) = candidate == toMatch.token
}
