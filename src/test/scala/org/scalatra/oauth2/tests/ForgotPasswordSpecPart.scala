package org.scalatra
package oauth2
package tests

import org.json4s._
import org.json4s.native.Serialization
import java.lang.Boolean

trait ForgotPasswordSpecPart { this: HomeServletSpec ⇒

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
      "redirects to /login for a html request" ! validForgot(json = false) ^ bt ^ bt ^ p ^
      "when getting /reset" ^
      "returns 404 when token is missing" ! resetWithMissingToken() ^
      "redirects to / if authenticated" ! redirectAuthenticated("/reset") ^
      "shows a reset form if unauthenticated" ! resetGetValidToken() ^
      "when posting to /reset" ^
      "fail for a missing token" ! resetWithMissingToken("post") ^
      "with invalid data" ^
      "return status 422" ! resetInvalidData(checkStatus = true) ^
      "return error messages in response" ! resetInvalidData(checkStatus = false) ^ bt ^
      "with valid data" ^
      "changes the password" ! resetChangesPassword() ^
      "redirects to authenticated for a html request" ! resetSucceeds(json = false) ^
      "returns the user json for a json request" ! resetSucceeds(json = true) ^ bt ^ bt ^ p

  def createResetAccount() = {
    clearDB
    createAccount("tonythepony", "tony@mylittlepony.li", "Tony The Pony", "password", confirm = true)
  }

  def resetChangesPassword() = {
    val account = createResetAccount()
    val params = Map("password" -> "password123", "passwordConfirmation" -> "password123")
    post("/reset/" + account.reset.token, body = Serialization.write(params), headers = h.json) {
      (status must_== 200) and {
        oauth.userProvider.findByLoginOrEmail(account.login).get.password.isMatch("password123") must beTrue
      }
    }
  }

  def resetSucceeds(json: Boolean = true) = {
    val account = createResetAccount()
    val params = Map("password" -> "password123", "passwordConfirmation" -> "password123")
    if (json) {
      post("/reset/" + account.reset.token, body = Serialization.write(params), headers = h.json) {
        (status must_== 200) and {
          verifyJsonAccount(parse(body), account)
        }
      }
    } else {
      post("/reset/" + account.reset.token, params = params) {
        (status must_== 302) and {
          header("Location") must startWith("http://test.local:8080;jsessionid=")
        }
      }
    }
  }

  def resetInvalidData(checkStatus: Boolean) = {
    val account = createResetAccount()
    val params = Map("password" -> "password2", "passwordConfirmation" -> "password")
    post("/reset/" + account.reset.token, body = Serialization.write(params), headers = h.json) {
      if (checkStatus) {
        status must_== 422
      } else {
        val JArray(errors) = (parse(body) \ "errors")
        errors must not(beEmpty)
      }
    }
  }

  def resetWithMissingToken(method: String = "get") = {
    submit(method.toUpperCase, "/reset") {
      status must_== 404
    }
  }

  def resetGetValidToken() = {
    val account = createResetAccount()
    renderAngular("/reset/" + account.reset.token)
  }

  def validForgot(json: Boolean = false) = {
    val account = createResetAccount()
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
        (status must_== 302) and (header("Location") must startWith("http://test.local:8080;jsessionid="))
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
