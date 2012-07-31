//package org.scalatra
//package oauth2
//package auth
//
//import org.scalatra.liftjson.LiftJsonSupport
//import org.scalatra.auth.ScentryStrategy
//import scalaz._
//import Scalaz._
//import OAuth2Imports._
//
///**
//* Strategy for signing in a user, based on a token. This works for both params
//* and http headers. For the former, all you need to do is to pass the params in the URL:
//*
//*   http://myapp.example.com/?auth_token=SECRET
//*
//* For HTTP, you can pass the token as username and blank password. Since some clients may require
//* a password, you can pass "X" as password and it will simply be ignored.
//*/
//class AppTokenStrategy[UserClass <: AppUser[_]](
//    protected val app: ScalatraBase with LiftJsonSupport,
//    userProvider: UserProvider[UserClass],
//    tokenName: String = "auth_token",
//    headerModifier: String = "AuthToken") extends ScentryStrategy[UserClass] {
//
//
//
//  def authenticate(): Option[UserClass] = {
//
//  }
//
//
//  override def isValid: Boolean =
//    app.params.get(tokenName).isDefined ||
//    (app.request.providesAuth && app.request.authScheme == Some(headerModifier.toLowerCase))
//}