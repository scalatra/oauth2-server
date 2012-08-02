package org.scalatra
package oauth2
package tests

import test.specs2.BaseScalatraSpec
import net.liftweb.json._



class HomeServletSpec extends AkkaSpecification with BaseScalatraSpec with AuthenticationSpec with RegistrationSpec {


  def is = sequential ^
    "A HomeServlet should" ^
      "when getting /" ^
        "get a html page for a html format request" ! renderAngular("/") ^
        "get the user json for an authenticated json request" ! rootAuthenticatedJson ^ bt ^ p ^
      loginFragments ^
      registrationFragments ^
//      "when getting /activate/:token" ^
//        "fails for an invalid token" ^
//          "with status 400" ! pending ^
//          "with an invalid token error" ! pending ^ bt ^
//        "fails for a missing token" ^
//          "with status 400" ! pending ^
//          "with an invalid token error" ! pending ^ bt ^
//        "with a valid token" ^
//          "activates user" ! pending ^
//          "redirects authenticated to home for a html request" ! pending ^
//          "returns the user json for a json request" ! pending ^ bt ^ bt ^
//      "when getting /forgot" ^
//        "redirects to /home if authenticated" ! redirectAuthenticated("/forgot") ^
//        "shows a reset form if unauthenticated" ! renderAngular("/forgot") ^ bt ^
//      "when posting to /forgot" ^
//        "for invalid data" ^
//          "returns status 422" ! pending ^
//          "return error messages in response" ! pending ^ bt ^
//        "for valid data" ^
//          "changes the password" ! pending ^
//          "logs user in and returns the user json for a json request" ! pending ^
//          "logs user in and redirects to home for a html request" ! pending ^
    end
}
