package org.scalatra
package oauth2
package tests

import OAuth2Imports._

trait ActivationSpec { this: HomeServletSpec =>

  def activationFragments =
    "when getting /activate/:token" ^
      "fails for an invalid token" ^
        "with status 302" ! invalidToken("i-am-invalid", checkStatus = true) ^
        "redirects to /login" ! invalidToken("i-am-invalid", checkStatus = false) ^ bt ^
      "fails for a missing token" ^
        "with status 302" ! invalidToken("", checkStatus = true) ^
        "redirects to /login" ! invalidToken("", checkStatus = false) ^ bt ^
      "with a valid token" ^
        "activates user" ! activatesUser() ^ bt ^ bt ^ p
        "with status 302" ! activatesUser(checkStatus = true) ^ bt ^ bt ^ p
        "redirects to /login" ! activatesUser(checkLocation = true) ^ bt ^ bt ^ p

  def invalidToken(token: String = "", checkStatus: Boolean = true) = {
    clearDB
    get("/activate/" + token, headers = h.json) {
      if (checkStatus)
        status must_== 302
      else {
        (header("Location") must startWith("http://test.local:8080/login;jsessionid="))
      }
    }
  }

  def activatesUser(checkStatus: Boolean = false, checkLocation: Boolean = false) = {
    clearDB
    val account = createAccount("activa", "activa@passiva.br", "Activa Passiva", "password", confirm = false)
    (account.isConfirmed must beFalse) and {
      get("/activate/" + account.confirmation.token, headers = h.json) {
        (checkStatus, checkLocation) match {
          case (_, true) => header("Location") must startWith("http://test.local:8080/login;jsessionid=")
          case (true, _) => status must_== 302
          case (false, false) => oauth.userProvider.findOneById(account.id).get.isConfirmed must beTrue
        }

      }
    }
  }
}
