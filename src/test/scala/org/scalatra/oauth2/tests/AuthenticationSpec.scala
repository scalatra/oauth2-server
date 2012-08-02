package org.scalatra
package oauth2
package tests

import test.specs2.BaseScalatraSpec
import net.liftweb.json._
import org.scalatra.oauth2.{commands, OAuth2Imports, model}
import model._
import OAuth2Imports._
import commands.LoginCommand
import java.net.HttpCookie
import org.scalatra.auth.Scentry
import collection.JavaConverters._

trait AuthenticationSpec {
  this: AkkaSpecification with BaseScalatraSpec =>
  val oauth = OAuth2Extension(system)
  implicit val formats: Formats = new OAuth2Formats
  servletContextHandler.setResourceBase("src/main/webapp")

  addServlet(new HomeServlet, "/*")

  object h {
    val json = Map("Accept" -> "application/json", "Content-Type" -> "application/json")

    def cookie(pair: (String, String)) = Map("Cookie" -> "%s=%s".format(pair._1, pair._2))

  }

  def redirectAuthenticated(path: String) = {
    val (_, authSession) = authenticate("Johnny", "johnny@localhost.blah", "Johnny", "password")
    get(path, headers = Map(authenticatedHeader(authSession))) {
      (status must_== 302) and {
        header("Location") must startWith("http://test.localhost:8080;jsessionid=")
      }
    }
  }

  def authenticatedHeader(authSession: AuthSession) = "Cookie" -> ("scentry.auth.default.user=" + authSession.token.token)

  def clearDB = {
    oauth.userProvider.collection foreach {
      acc =>
        oauth.userProvider.remove(acc, WriteConcern.Safe)
    }
    oauth.authSessions.collection foreach {
      acc =>
        oauth.authSessions.remove(acc, WriteConcern.Safe)
    }
    oauth.userProvider.collection.dropIndexes()
    oauth.authSessions.collection.dropIndexes()
  }

  def createAccount(login: String, email: String, name: String, password: String, confirm: Boolean = true) = {
    val acct = Account(login, email, name, BCryptPassword(password).encrypted)
    val newAcc = if (confirm) acct.copy(confirmedAt = DateTime.now) else acct
    oauth.userProvider.save(newAcc, WriteConcern.Safe)
    newAcc
  }

  def createAuthSession(login: String, password: String) = {
    val loginCmd = new LoginCommand(oauth, "127.0.0.1")
    loginCmd.doBinding(Map("login" -> login, "password" -> password))
    oauth.authService.execute(loginCmd).toOption.get
  }

  def authenticate(login: String, email: String, name: String, password: String) = {
    clearDB
    (createAccount(login, email, name, password), createAuthSession(login, password))
  }

  def renderAngular(path: String) = get(path) {
    body must contain( """<div ng-view></div>""")
  }

  def rootAuthenticatedJson = {
    val (acct, authSession) = authenticate("Johnny", "johnny@localhost.blah", "Johnny", "password")
    get("/", headers = h.json ++ Map(authenticatedHeader(authSession))) {
      val jv = parse(body)
      verifyJsonAccount(jv, acct)
    }
  }

  def verifyJsonAccount(resp: JValue, account: Account) = {
    ((resp \ "data" \ "login").extract[String] must_== account.login) and
      ((resp \ "data" \ "email").extract[String] must_== account.email) and
      ((resp \ "data" \ "name").extract[String] must_== account.name)
  }

  def loginWith(params: Map[String, String], json: Boolean = true) = {
    clearDB
    val account = createAccount(params("login"), "thefrog@fff.feo", "Timmy The Frog", params("password"))
    if (json) {
      val js = Serialization.writePretty(params)
      post("/login", body = js, headers = h.json) {
        (status must_== 200) and {
          if (params.get("remember") != Some("true")) {
            (verifyJsonAccount(parse(body), account))
          } else {
            val token = oauth.authSessions.findOne(Map("userId" -> account.id)).get.token.token
            val cookies = Map((response.getHeaderValues("Set-Cookie").asScala
                                flatMap(HttpCookie.parse(_).asScala)
                                map(c => c.getName() -> c.getValue())).toSeq:_*)
            cookies(Scentry.scentryAuthKey) must_== token
          }
        }
      }
    } else {
      post("/login", params = params) {
        (status must_== 302) and (header("Location") must startWith("http://test.localhost:8080;jsessionid="))
      }
    }
  }

  def failLoginStatus(params: Map[String, String]) = {
    post("/login", body = Serialization.write(params), headers = Map("Accept" -> "application/json", "Content-Type" -> "application/json")) {
      status must_== 401
    }
  }

  def failLoginMessage(params: Map[String, String]) = {
    post("/login", body = Serialization.write(params), headers = Map("Accept" -> "application/json", "Content-Type" -> "application/json")) {
      body must /("errors") */ ("Username or password is incorrect")
    }
  }
  
//  def loggedIn(thunk: => Any) = {
//    post
//  }

  def loginFragments = sequential ^
    "when getting /login" ^
      "render a login form if not authenticated" ! renderAngular("/login") ^
      "redirect to home if authenticated" ! redirectAuthenticated("/login") ^ bt ^
    "when posting to /login" ^
      "if login is missing" ^
        "return status 401" ! failLoginStatus(Map("password" -> "blah")) ^
        "return login/password invalid error" ! failLoginMessage(Map("password" -> "blah")) ^ bt ^
      "if password is missing" ^
        "return status 401" ! failLoginStatus(Map("login" -> "blah")) ^
        "return login/password invalid error" ! failLoginMessage(Map("login" -> "blah")) ^ bt ^
      "when credentials are invalid" ^
        "return status 401" ! failLoginStatus(Map("login" -> "blah", "password" -> "blah")) ^
        "return login/password invalid error" ! failLoginMessage(Map("login" -> "blah", "password" -> "blah")) ^ bt ^
      "when credentials are valid" ^
        "return user json for a json request" ! loginWith(Map("login" -> "timmy", "password" -> "password")) ^
        "redirect to home on login" ! loginWith(Map("login" -> "timmy", "password" -> "password"), json = false) ^
        "set a cookie when remember me is ticked" ! loginWith(Map("login" -> "timmy", "password" -> "password", "remember" -> "true")) ^
    "when getting /logout" ^
      "changes to auth cookie to have an invalid token" ! pending ^
      "changes to auth cookie to have a date in the past" ! pending ^ 
      "returns the user as null in the response for a json request" ! pending ^ 
      "redirects to login for a html request" ! pending ^ bt ^ bt ^ p
      
}
