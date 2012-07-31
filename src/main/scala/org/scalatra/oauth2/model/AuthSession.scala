package org.scalatra
package oauth2
package model

import com.novus.salat._
import com.novus.salat.global._
import com.novus.salat.annotations._
import com.novus.salat.dao._
import org.scalatra.oauth2.UserProvider
import java.security.SecureRandom
import scala.util.control.Exception._
import org.apache.commons.codec.binary.Hex
import scalaz._
import Scalaz._
import OAuth2Imports._
import org.mindrot.jbcrypt.BCrypt
import akka.actor.ActorSystem
import command.{ FieldValidation, FieldError, SimpleError }
import commands.{ HasRequestIp, LoginCommand }

object AuthSession {
  val userId: Lens[AuthSession, ObjectId] = Lens(_.userId, (sess, uid) ⇒ sess copy (userId = uid))
  val ipAddress: Lens[AuthSession, String] = Lens(_.ipAddress, (sess, ip) ⇒ sess copy (ipAddress = ip))
  val token: Lens[AuthSession, Token] = Lens(_.token, (sess, tok) ⇒ sess copy (token = tok))
  val accessCount: Lens[AuthSession, Long] = Lens(_.accessCount, (sess, c) ⇒ sess copy (accessCount = c))
  val expiresAt: Lens[AuthSession, DateTime] = Lens(_.expiresAt, (sess, d) ⇒ sess copy (expiresAt = d))
  val rememberedAt: Lens[AuthSession, DateTime] = Lens(_.rememberedAt, (sess, d) ⇒ sess copy (rememberedAt = d))
  val updatedAt: Lens[AuthSession, DateTime] = Lens(_.updatedAt, (sess, d) ⇒ sess copy (updatedAt = d))
}
case class AuthSession(
    userId: ObjectId,
    ipAddress: String,
    @Key("_id") id: ObjectId = new ObjectId,
    token: Token = Token(),
    accessCount: Long = 0,
    rememberedAt: DateTime = MinDate,
    expiresAt: DateTime = MinDate,
    createdAt: DateTime = DateTime.now,
    updatedAt: DateTime = DateTime.now) extends AppAuthSession {

  val isExpired = expiresAt > MinDate && expiresAt <= DateTime.now
  val isActive = !isExpired
  val isRemembered = rememberedAt > MinDate

  val idString: String = id.toString
  val userIdString: String = userId.toString

  var account: Account = null
}

class AuthSessionDao(collection: MongoCollection)(implicit system: ActorSystem)
    extends SalatCommandableDao[AuthSession, ObjectId](collection = collection) with Logging {

  val oauth = OAuth2Extension(system)
  def newSession(cmd: HasRequestIp)(account: Account): ModelValidation[AuthSession] = {
    (allCatch withApply saveError) {
      val sess = AuthSession(account.id, cmd.ipAddress, expiresAt = DateTime.now + oauth.authSessionTimeout)
      save(sess)
      sess.account = account
      logger debug "Created auth session: %s".format(sess)
      sess.successNel
    }
  }

  private[this] def saveError(ex: Throwable): ModelValidation[AuthSession] = {
    logger.error("There was an error while saving an auth session.", ex)
    ServerError("An error occurred while saving an auth session").failNel
  }

  def loginFromRemember(token: String): ModelValidation[AuthSession] = {
    val key = fieldNames.token + "." + fieldNames.token
    findOne(Map(key -> token)).map(_.successNel).getOrElse(InvalidToken().failNel)
  }

  def remember(session: AppAuthSession): FieldValidation[String] = {
    allCatch.withApply(e ⇒ SimpleError(e.getMessage).fail) {
      findOneById(new ObjectId(session.idString)) map { sess ⇒
        val token = Token()
        save(sess.copy(token = token, rememberedAt = DateTime.now))
        token.token.success
      } getOrElse NotFound("Session " + session.idString + " could not be found.").fail

    }
  }

}
