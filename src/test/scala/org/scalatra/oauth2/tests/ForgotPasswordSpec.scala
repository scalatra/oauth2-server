package org.scalatra
package oauth2
package tests

import net.liftweb.json._

trait ForgotPasswordSpec { this: HomeServletSpec =>
  def forgotPasswordFragments =
    "when getting /forgot" ^
      "redirects to /home if authenticated" ! redirectAuthenticated("/forgot") ^
      "shows a reset form if unauthenticated" ! renderAngular("/forgot") ^
    "when posting to /forgot" ^
      "for invalid data" ^
        "returns status 422" ! invalidForgot(Map("login" -> ""), checkStatus = true) ^
        "return error messages in response" ! invalidForgot(Map("login" -> "bl"), checkStatus = false) ^ bt ^
      "for valid data" ^
        "changes the reset token" ! validForgot(json = true) ^
        "redirects to /login for a html request" ! validForgot(json = false) ^ bt ^ bt ^ p

  def validForgot(json: Boolean = false) = {
    clearDB
    val account = createAccount("tonythepony", "tony@mylittlepony.li", "Tony The Pony", "password", confirm = true)
    val orig = account.reset.token
    val params = Map("login" -> account.login)
    if (json) {
      post("/forgot", body = Serialization.write(params), headers = h.json) {
        (status must_== 200) and {
          oauth.userProvider.findByLoginOrEmail(account.login).get.reset.token must_!= orig
        }
      }

    } else {
      post("/forgot", params) {
        (status must_== 302) and (header("Location") must startWith("http://test.localhost:8080;jsessionid="))
      }
    }
  }

  def invalidForgot(data: Map[String, String], checkStatus: Boolean) = {
    post("/forgot", body = Serialization.write(data), headers = h.json) {
      if (checkStatus) {
        status must_== 422
      } else {
        val JArray(errors) = (parse(body) \ "errors")
        errors must not(beEmpty)
      }
    }
  }
}
