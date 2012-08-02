package org.scalatra
package oauth2
package tests

import test.specs2.BaseScalatraSpec
import net.liftweb.json._



class HomeServletSpec extends AkkaSpecification with BaseScalatraSpec
                                with AuthenticationSpec
                                with ActivationSpec
                                with ForgotPasswordSpec
                                with RegistrationSpec {


  def is = sequential ^
    "A HomeServlet should" ^ bt ^ p ^
//      "when getting /" ^
//        "get a html page for a html format request" ! renderAngular("/") ^
//        "get the user json for an authenticated json request" ! rootAuthenticatedJson ^ bt ^ p ^
//      loginFragments ^
//      registrationFragments ^
//      activationFragments ^
      forgotPasswordFragments ^
    end
}
