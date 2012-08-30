package org.scalatra.oauth2.tests

import org.json4s._
import native.Serialization

trait RegistrationSpecPart { self: HomeServletSpec ⇒

  def registrationFragments =
    "when getting /register" ^
      "redirect to home if authenticated" ! redirectAuthenticated("/register") ^
      "render a register form if not authenticated" ! renderAngular("/register") ^
      "when posting to /register" ^
      "when the data is invalid" ^
      "return status 422" ! registerInvalid(Map("login" -> "blah"), verifyStatus = true) ^
      "response contains error messages" ! registerInvalid(Map("login" -> "blah")) ^ bt ^
      "when the data is valid" ^
      "redirects to login for a html request" ! registersSuccessfully(json = false) ^
      "returns the user json for a json request" ! registersSuccessfully(json = true) ^ bt ^ bt ^ p

  def registersSuccessfully(json: Boolean) = {
    clearDB
    val (login, email, name) = ("timmy", "timmy@unicorn.com", "Timmy the unicorn whisperer")
    val params = Map("login" -> login, "email" -> email, "name" -> name, "password" -> "password", "passwordConfirmation" -> "password")
    if (json) {
      post("/register", body = Serialization.write(params), headers = h.json) {
        (status must_== 200) and (verifyJsonAccount(parse(body), oauth.userProvider.findByLoginOrEmail(login).get))
      }
    } else {
      post("/register", params) {
        (status must_== 302) and (header("Location") must startWith("http://test.local:8080;jsessionid="))
      }
    }
  }

  def registerInvalid(data: Map[String, String], verifyStatus: Boolean = false) = {
    post("/register", body = Serialization.write(data), headers = h.json) {
      if (verifyStatus)
        status must_== 422
      else {
        val JArray(errs) = (parse(body) \ "errors")
        errs must not(beEmpty)
      }
    }
  }
}
