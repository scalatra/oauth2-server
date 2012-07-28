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
    updatedAt: DateTime = DateTime.now) {

  val isExpired = expiresAt > MinDate && expiresAt < DateTime.now
  val isRemembered = rememberedAt > MinDate

}
