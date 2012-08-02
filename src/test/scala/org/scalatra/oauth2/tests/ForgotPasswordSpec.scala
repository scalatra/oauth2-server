package org.scalatra
package oauth2
package tests

trait ForgotPasswordSpec { this: HomeServletSpec =>
  def forgotPasswordFragments = 
    "when getting /forgot" ^
      "redirects to /home if authenticated" ! redirectAuthenticated("/forgot") ^
      "shows a reset form if unauthenticated" ! renderAngular("/forgot") ^ bt ^
    "when posting to /forgot" ^
      "for invalid data" ^
        "returns status 422" ! pending ^
        "return error messages in response" ! pending ^ bt ^
      "for valid data" ^
        "changes the password" ! pending ^
        "logs user in and returns the user json for a json request" ! pending ^
        "logs user in and redirects to home for a html request" ! pending ^ bt ^ bt ^ p

}
