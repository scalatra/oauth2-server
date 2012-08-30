package org.scalatra
package oauth2
package service

import scala.util.control.Exception.allCatch
import commands._
import model._
import scalaz._
import Scalaz._
import akka.actor.ActorSystem
import org.scalatra.validation.{ ValidationError, UnknownError, NotImplemented }

trait CommandHandler { self: Logging ⇒
  def execute[S: Manifest](cmd: OAuth2Command[S]): ModelValidation[S] = {
    logger.debug("Executing [%s].\n%s" format (cmd.getClass.getName, cmd))
    if (cmd.isValid) {
      val res = (allCatch withApply (serverError(cmd.getClass.getName, _))) {
        handle.lift(cmd).map(_.map(_.asInstanceOf[S])) | ValidationError("Don't know how to handle: " + cmd.getClass.getName, UnknownError).failNel
      }
      val ftext = "with %d failures\n%s".format(~res.fail.toOption.map(_.list.size), ~res.fail.toOption.map(_.list))
      logger.debug("Command [%s] executed %s." format (cmd.getClass.getName, res.isSuccess ? "successfully." | ftext))
      res
    } else {
      val f = cmd.errors.map(_.value) collect {
        case Failure(e) ⇒ e
      }
      logger.debug("Command [%s] executed with %d failures.\n%s" format (cmd.getClass.getName, f.size, f.toList))
      nel(f.head, f.tail: _*).fail
    }
  }

  private[this] def serverError[R](cmdName: String, ex: Throwable): ModelValidation[R] = {
    logger.error("There was an error while executing " + cmdName, ex)
    ValidationError("An error occurred while handling: " + cmdName, UnknownError).failNel[R]
  }

  type Handler = PartialFunction[OAuth2Command[_], ModelValidation[_]]

  protected def handle: Handler
}

final class AuthenticationService(oauth: OAuth2Extension) extends Logging with CommandHandler {

  private val accounts = oauth.userProvider
  private val authSessions = oauth.authSessions

  def loginFromRemember(token: String): ModelValidation[AuthSession] = authSessions loginFromRemember token
  def remember(session: AppAuthSession[_]): databinding.FieldValidation[String] = authSessions remember session
  def validate(account: Account) = accounts.validate(account)
  def completedProfile(account: Account, ipAddress: String): ModelValidation[AuthSession] = {
    accounts.save(account)
    authSessions.newSession(ipAddress)(account).liftFailNel
  }
  def loggedIn(account: Account, ipAddress: String): databinding.FieldValidation[AuthSession] =
    authSessions.newSession(ipAddress)(accounts.loggedIn(account, ipAddress))

  def logout(token: String) = authSessions.logout(token)

  protected val handle: Handler = {
    case c: LoginCommand               ⇒ accounts.login(c) flatMap (authSessions.newSession(c) _)
    case c: ActivateAccountCommand     ⇒ accounts.confirm(c) flatMap (authSessions.newSession(c) _)
    case c: ResetCommand               ⇒ accounts.resetPassword(c) flatMap (authSessions.newSession(c) _)
    case c: OAuthInfoIncompleteCommand ⇒ ValidationError("Not Implemented", NotImplemented).failNel[AuthSession] // TODO: Implement OAuthInfoIncompleteCommand handler
    case c: RegisterCommand            ⇒ accounts.register(c) flatMap (authSessions.newSession(c) _)
    case c: ForgotCommand              ⇒ accounts.forgot(c)
    case c: ChangePasswordCommand      ⇒ accounts.changePassword(c)
  }

}
