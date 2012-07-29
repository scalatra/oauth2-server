//package org.scalatra
//package oauth2
//package auth
//
//import org.scalatra.auth.ScentryStrategy
//import scalaz._
//import Scalaz._
//import OAuth2Imports._
//import org.scalatra.ScalatraBase
//import liftjson.LiftJsonSupport
//
//class PasswordStrategy[UserClass <: AppUser[_]](protected val app: ScalatraBase with LiftJsonSupport, userProvider: UserProvider[UserClass]) extends ScentryStrategy[UserClass] {
//
//  import app.jsonFormats
//  override val name = "user_password"
//
//  private def login = {
//    app.format match {
//      case "json" | "xml" ⇒
//        (app.parsedBody \ "login").extractOpt[String] flatMap (_.blankOption)
//      case _ ⇒
//        app.params.get("login") flatMap (_.blankOption)
//    }
//  }
//
//  private def password = {
//    app.format match {
//      case "json" | "xml" ⇒
//        (app.parsedBody \ "password").extractOpt[String] flatMap (_.blankOption)
//      case _ ⇒
//        app.params.get("password") flatMap (_.blankOption)
//    }
//  }
//
//  override def isValid = {
//    login.isDefined && password.isDefined
//  }
//
//  /**
//   * Authenticates a user by validating the username (or email) and password request params.
//   */
//  def authenticate: Option[UserClass] = {
//    logger debug "Authenticating in UserPasswordStrategy from %s with: %s, %s".format(app.format, ~login, app.remoteAddress)
//    val usr = userProvider.login(login.get, password.get, app.remoteAddress)
//    usr.toOption
//  }
//
//  /*
//  override def beforeSetUser(user: UserClass) {
//    println("before set user " + getClass.getName)
//  }
//
//  override def afterAuthenticate(winningStrategy: String, user: UserClass) {
//    if (winningStrategy == "user_password") app.user = userProvider.loggedIn(user, app.remoteAddress)
//    println("after authenticate " + getClass.getName)
//  }
//
//  override def beforeFetch[IdType](userId: IdType) {
//    println("before fetch user " + getClass.getName)
//  }
//
//  override def afterFetch(user: UserClass) {
//    println("before fetch user " + getClass.getName)
//  }
//
//  override def beforeLogout(user: UserClass) {
//    println("before logout " + getClass.getName)
//  }
//
//  override def afterSetUser(user: UserClass) {
//    println("after set user " + getClass.getName)
//  }
//
//  override def unauthenticated() {
//    println("unauthenticated " + getClass.getName)
//  }
//
//  override def afterLogout(user: UserClass) {
//    println("after logout " + getClass.getName)
//  }
//*/
//
//}
//
