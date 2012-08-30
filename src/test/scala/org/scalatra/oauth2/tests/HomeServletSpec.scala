package org.scalatra
package oauth2
package tests

import test.specs2.BaseScalatraSpec
import org.json4s._
import org.junit.runner._
import org.specs2.runner._

@RunWith(classOf[JUnitRunner])
class HomeServletSpec extends AkkaSpecification with BaseScalatraSpec
                                with AuthenticationSpecPart
                                with ActivationSpecPart
                                with ForgotPasswordSpecPart
                                with RegistrationSpecPart {

  implicit val jsonFormats: Formats = new OAuth2Formats

  def is = sequential ^
    "A HomeServlet should" ^ bt ^ p ^
      "when getting /" ^
        "get a html page for a html format request" ! renderAngular("/") ^
        "get the user json for an authenticated json request" ! rootAuthenticatedJson ^ bt ^ p ^
      loginFragments ^
      registrationFragments ^
      activationFragments ^
      forgotPasswordFragments ^
    end
}
