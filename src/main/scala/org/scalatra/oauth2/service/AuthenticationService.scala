package org.scalatra
package oauth2
package service

import scala.util.control.Exception.allCatch
import commands._
import model._
import scalaz._
import Scalaz._
import akka.actor.ActorSystem

trait CommandHandler { self: Logging ⇒
  def execute[S: Manifest](cmd: OAuth2Command[S]): ModelValidation[S] = {
    logger.debug("Executing [%s].\n%s" format (cmd.getClass.getName, cmd.bindings.mkString(", ")))
    if (cmd.isValid) {
      val res = (allCatch withApply (serverError(cmd.getClass.getName, _))) {
        handle.lift(cmd).map(_.map(_.asInstanceOf[S])) | ServerError("Don't know how to handle: " + cmd.getClass.getName).failNel
      }
      val ftext = "with %d failures\n%s".format(~res.fail.toOption.map(_.list.size), ~res.fail.toOption.map(_.list))
      logger.debug("Command [%s] executed %s." format (cmd.getClass.getName, res.isSuccess ? "successfully." | ftext))
      res
    } else {
      val f = cmd.errors.map(_.validation) collect {
        case Failure(e) ⇒ e
      }
      logger.debug("Command [%s] executed with %d failures.\n%s" format (cmd.getClass.getName, f.size, f.toList))
      nel(f.head, f.tail: _*).fail
    }
  }

  private[this] def serverError[R](cmdName: String, ex: Throwable): ModelValidation[R] = {
    logger.error("There was an error while executing " + cmdName, ex)
    ServerError("An error occurred while handling: " + cmdName).failNel[R]
  }

  type Handler = PartialFunction[OAuth2Command[_], ModelValidation[_]]

  protected def handle: Handler
}

final class AuthenticationService(oauth: OAuth2Extension) extends Logging with CommandHandler {

  private val accounts = oauth.userProvider
  private val authSessions = oauth.authSessions

  def loginFromRemember(token: String): ModelValidation[AuthSession] = authSessions loginFromRemember token
  def remember(session: AppAuthSession[_]): command.FieldValidation[String] = authSessions remember session
  def validate(account: Account) = accounts.validate(account)
  def completedProfile(account: Account, ipAddress: String): ModelValidation[AuthSession] = {
    accounts.save(account)
    authSessions.newSession(ipAddress)(account).liftFailNel
  }
  def loggedIn(account: Account, ipAddress: String): command.FieldValidation[AuthSession] =
    authSessions.newSession(ipAddress)(accounts.loggedIn(account, ipAddress))

  protected val handle: Handler = {
    case c: LoginCommand               ⇒ accounts.login(c) flatMap (authSessions.newSession(c) _)
    case c: ActivateAccountCommand     ⇒ accounts.confirm(c) flatMap (authSessions.newSession(c) _)
    case c: ResetCommand               ⇒ accounts.resetPassword(c) flatMap (authSessions.newSession(c) _)
    case c: OAuthInfoIncompleteCommand ⇒ ServerError("Not Implemented").failNel[AuthSession] // TODO: Implement OAuthInfoIncompleteCommand handler
    case c: RegisterCommand            ⇒ accounts.register(c)
    case c: ForgotCommand              ⇒ accounts.forgot(c)
    case c: ChangePasswordCommand      ⇒ accounts.changePassword(c)
  }

}
