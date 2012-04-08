package io.backchat.oauth2
package model

import OAuth2Imports._

object Enums {

  object AuthorizationType extends Enumeration {
    val Code = Value("code")
    val Token = Value("token")
    val CodeAndToken = Value("code_and_token")
  }

  object ResponseType extends Enumeration {
    val Code = Value("code")
    val Token = Value("token")
    val CodeAndToken = Value("code_and_token")
  }

  object GrantCode extends Enumeration {
    val None = Value("none")
    val AuthorizationCode = Value("authorization_code")
    val Password = Value("password")
    val ClientCredentials = Value("client_credentials")
    val Implicit = Value("implicit")
    val RefreshToken = Value("refresh_token")

    def fromParam(code: Option[String]) = {
      val c = code.flatMap(_.blankOption) getOrElse "none"
      try {
        withName(c)
      } catch {
        case e: NoSuchElementException ⇒ GrantCode.None
      }
    }
  }

}