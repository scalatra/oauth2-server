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

object fieldNames {
  val login = "login"
  val id = "id"
  val _id = "_id"
  val email = "email"
  val password = "password"
  val createdAt = "createdAt"
  val updatedAt = "updatedAt"
  val secret = "secret"
  val displayName = "displayName"
  val rememberedAt = "rememberedAt"
  val link = "link"
  val redirectUri = "redirectUri"
  val urlWhitelist = "urlWhitelist"
  val scope = "scope"
  val authorizationType = "authorizationType"
  val revoked = "revoked"
  val tokensGranted = "tokensGranted"
  val tokensRevoked = "tokensRevoked"
  val clientId = "clientId"
  val state = "state"
  val responseType = "responseType"
  val grantCode = "grantCode"
  val accessToken = "accessToken"
  val identity = "identity"
  val expiresAt = "expiresAt"
  val lastAccess = "lastAccess"
  val previousAccess = "previousAccess"
  val name = "name"
  val grantedAt = "grantedAt"
  val token = "token"
  val pwd = "pwd"
  val salt = "salt"
  val currentSignInIp = "currentSignInIp"
  val previousSignInIp = "previousSignInIp"
  val currentSignInAt = "currentSignInAt"
  val previousSignInAt = "previousSignInAt"
  val remembered = "remembered"
  val confirmation = "confirmation"
  val reset = "reset"
  val stats = "stats"
  val clientType = "clientType"
  val profile = "profile"
}

case class Token(token: String, createdAt: DateTime = DateTime.now) extends AppToken {
  def refreshed = Token()
}
object Token {

  def apply(): Token = generate
  def generate: Token = {
    val random = SecureRandom.getInstance("SHA1PRNG")
    val pw = new Array[Byte](16)
    random.nextBytes(pw)
    Token(Hex.encodeHexString(pw))
  }

  def isMatch(candidate: String, toMatch: Token) = candidate == toMatch.token
}

case class BCryptPassword(pwd: String, salted: Boolean, stretches: Int) {
  def encrypted = salted ? this | BCryptPassword.hash(this)
  def isMatch(candidate: String) = BCryptPassword.isMatch(candidate, this)
}
object BCryptPassword {
  def apply(in: String, stretches: Int = 10): BCryptPassword = BCryptPassword(in, false, stretches)

  def hash(pwd: BCryptPassword): BCryptPassword =
    createHashed(pwd.pwd, BCrypt.gensalt(pwd.stretches), pwd.stretches)

  def hash(pwd: String, stretches: Int = 10): BCryptPassword =
    createHashed(pwd, BCrypt.gensalt(stretches), stretches)

  private def createHashed(pwd: String, salt: String, stretches: Int) = {
    BCryptPassword(BCrypt.hashpw(pwd, salt), true, stretches)
  }

  def isMatch(candidate: String, toMatch: BCryptPassword): Boolean =
    toMatch.salted ? BCrypt.checkpw(candidate, toMatch.pwd) | isMatch(candidate, toMatch.pwd)

  def isMatch(candidate: String, toMatch: String): Boolean = { candidate == toMatch }

  def random = {
    val rand = new SecureRandom
    val pwdBytes = Array.ofDim[Byte](8)
    rand.nextBytes(pwdBytes)
    BCryptPassword.hash(Hex.encodeHexString(pwdBytes))
  }
}

case class LinkedOAuthAccount(provider: String, id: String)

case class AuthStats(
    currentSignInIp: String = "",
    previousSignInIp: String = "",
    currentSignInAt: DateTime = MinDate,
    lastFailureAt: DateTime = MinDate,
    previousSignInAt: DateTime = MinDate,
    loginFailures: Int = 0,
    loginSuccess: Int = 0) {
  def tick(ip: String) =
    copy(
      currentSignInIp = ip,
      previousSignInIp = currentSignInIp,
      currentSignInAt = DateTime.now,
      previousSignInAt = currentSignInAt,
      loginSuccess = (loginSuccess + 1),
      loginFailures = 0,
      lastFailureAt = MinDate)

  def tickFailures =
    copy(
      loginFailures = (loginFailures + 1),
      lastFailureAt = DateTime.now)
}

case class Account(
    login: String,
    email: String,
    name: String,
    password: BCryptPassword,
    @Key("_id") id: ObjectId = new ObjectId,
    remembered: Token = Token(),
    confirmation: Token = Token(),
    reset: Token = Token(),
    stats: AuthStats = AuthStats(),
    linkedOAuthAccounts: List[LinkedOAuthAccount] = Nil,
    confirmedAt: DateTime = MinDate,
    resetAt: DateTime = MinDate,
    createdAt: DateTime = DateTime.now,
    updatedAt: DateTime = DateTime.now) extends AppUser[BCryptPassword] {
  val isConfirmed = confirmedAt > MinDate
  val isReset = resetAt > MinDate
  val idString = id.toString
}

class AccountDao(collection: MongoCollection)(implicit system: ActorSystem)
    extends SalatDAO[Account, ObjectId](collection = collection)
    with UserProvider[Account]
    with RememberMeProvider[Account]
    with ForgotPasswordProvider[Account]
    with AuthenticatedChangePasswordProvider[Account] {

  private[this] val oauth = OAuth2Extension(system)
  collection.ensureIndex(Map("login" -> 1, "email" -> 1), "login_email_idx", true)
  collection.ensureIndex(Map("login" -> 1), "login_idx", true)
  collection.ensureIndex(Map("email" -> 1), "email_idx", true)
  collection.ensureIndex(Map("confirmation.token" -> 1), "confirmation_token_idx")
  collection.ensureIndex(Map("reset.token" -> 1), "reset_token_idx")
  collection.ensureIndex(Map("remembered.token" -> 1), "remembered_token_idx")
  collection.ensureIndex(Map("linkedOAuthAccounts.provider" -> 1, "linkedOAuthAccounts.id" -> 1), "linked_oauth_accounts_idx", true)

  def login(loginOrEmail: String, password: String, ipAddress: String): Validation[Error, Account] = {
    val usrOpt = findByLoginOrEmail(loginOrEmail)
    val verifiedPass = usrOpt filter (_.password.isMatch(password))
    if (verifiedPass.isEmpty && usrOpt.isDefined)
      usrOpt foreach (u ⇒ save(u.copy(stats = u.stats.tickFailures)))
    (verifiedPass
      map (loggedIn(_, ipAddress).success)).getOrElse(SimpleError("Login/password don't match.").fail)
  }

  def loggedIn(owner: Account, ipAddress: String): Account = {
    val ticked = owner.copy(stats = owner.stats.tick(ipAddress))
    save(ticked)
    ticked
  }

  def findByLoginOrEmail(loginOrEmail: String): Option[Account] =
    findOne($or(fieldNames.login -> loginOrEmail, fieldNames.email -> loginOrEmail))

  def findUserById(id: String) = findOneById(new ObjectId(id))

  def findByLinkedAccount(provider: String, id: String) = findOne(Map("linkedOAuthAccounts.provider" -> provider, "linkedOAuthAccounts.id" -> id))

  def loginFromRemember(token: String): Validation[Error, Account] = {
    val key = fieldNames.remembered + "." + fieldNames.token
    (findOne(Map(key -> token))
      some (_.success[Error])
      none InvalidToken().fail[Account])
  }

  def remember(owner: Account): Validation[Error, String] =
    allCatch.withApply(e ⇒ SimpleError(e.getMessage).fail) {
      val token = Token()
      save(owner.copy(remembered = token))
      token.token.success
    }

  def rememberedPassword(owner: Account, ipAddress: String): Account = {
    loggedIn(owner.copy(reset = Token()), ipAddress)
  }

  object validations {
    import Validations._

    def name(name: String): Validation[Error, String] = nonEmptyString(fieldNames.name, name)

    def login(login: String, id: Option[ObjectId] = None): Validation[Error, String] = {
      for {
        a ← nonEmptyString(fieldNames.login, login)
        b ← validFormat(fieldNames.login, a, """^\w+([\.\w]*)*$""".r, "%s can only contain letters, numbers, underscores and dots.")
        c ← uniqueField[String](fieldNames.login, b, collection, id)
      } yield c
    }

    def email(email: String, id: Option[ObjectId] = None): Validation[Error, String] =
      for {
        a ← nonEmptyString(fieldNames.email, email)
        b ← validEmail(fieldNames.email, a)
        c ← uniqueField[String](fieldNames.email, b, collection, id)
      } yield c

    def password(password: String): Validation[Error, String] =
      for {
        a ← nonEmptyString(fieldNames.password, password)
        b ← minLength(fieldNames.password, a, 6)
      } yield b

    def passwordWithConfirmation(password: String, passwordConfirmation: String): Validation[Error, String] =
      for {
        a ← this.password(password)
        b ← validConfirmation(fieldNames.password, a, fieldNames.password + "Confirmation", passwordConfirmation)
      } yield b

    def tokenRequired(tokenType: String, token: String): Validation[Error, String] =
      nonEmptyString(tokenType.toLowerCase + "." + fieldNames.token, token)

    def validPassword(owner: Account, password: String): Validation[Error, Account] =
      if (owner.password.isMatch(password)) owner.success
      else SimpleError("The username/password combination doesn not match").fail

  }

  def register(
    login: Option[String],
    email: Option[String],
    name: Option[String],
    password: Option[String],
    passwordConfirmation: Option[String]): ValidationNEL[Error, Account] = {
    val newOwner: ValidationNEL[Error, Account] = (validations.login(~login).liftFailNel
      |@| validations.email(~email).liftFailNel
      |@| validations.name(~name).liftFailNel
      |@| (validations
        passwordWithConfirmation (~password, ~passwordConfirmation)
        map (BCryptPassword(_).encrypted)).liftFailNel) { Account(_, _, _, _) }

    newOwner foreach { o ⇒
      save(o)
      if (!oauth.isTest) oauth.smtp.send(MailMessage(ConfirmationMail(o.name, o.login, o.email, o.confirmation.token)))
    }
    newOwner
  }

  private type Factory = (String, String, String) ⇒ Account

  def validate(owner: Account): ValidationNEL[Error, Account] = {
    val factory: Factory = owner.copy(_, _, _)
    (validations.login(owner.login, owner.id.some).liftFailNel
      |@| validations.email(owner.email, owner.id.some).liftFailNel
      |@| validations.name(owner.name).liftFailNel)(factory)
  }

  def confirm(token: String): Validation[Error, Account] = {
    val key = fieldNames.confirmation + "." + fieldNames.token
    validations.tokenRequired("confirmation", token) flatMap { tok ⇒
      findOne(Map(key -> tok)) map { owner ⇒
        if (!owner.isConfirmed) {
          val upd = owner.copy(confirmedAt = DateTime.now)
          save(upd)
          upd.success[Error]
        } else AlreadyConfirmed().fail
      } getOrElse InvalidToken().fail
    }
  }

  def forgot(loginOrEmail: Option[String]): Validation[Error, Account] = {
    Validations.nonEmptyString(fieldNames.login, ~loginOrEmail) flatMap { loe ⇒
      findByLoginOrEmail(loe) map { owner ⇒
        val updated = owner.copy(reset = Token(), resetAt = MinDate)
        save(updated)
        if (!oauth.isTest) oauth.smtp.send(MailMessage(SendForgotPasswordMail(updated.name, updated.login, updated.email, updated.reset.token)))
        updated.success[Error]
      } getOrElse SimpleError("Account not found.").fail
    }
  }

  def resetPassword(token: String, password: String, passwordConfirmation: String): ValidationNEL[Error, Account] = {
    val r: ValidationNEL[Error, Validation[Error, Account]] = (validations.tokenRequired("reset", token).liftFailNel
      |@| validations.passwordWithConfirmation(password, passwordConfirmation).liftFailNel)(doReset _)
    (r fold (_.fail, _.liftFailNel))
  }

  def changePassword(owner: Account, oldPassword: String, password: String, passwordConfirmation: String): Validation[Error, Account] = {
    for {
      o ← validations.validPassword(owner, oldPassword)
      pwd ← validations.passwordWithConfirmation(password, passwordConfirmation)
    } yield {
      val upd = o.copy(password = BCryptPassword(password).encrypted, resetAt = DateTime.now, reset = Token())
      save(upd)
      upd
    }
  }

  private def doReset(token: String, password: String): Validation[Error, Account] = {
    val key = fieldNames.reset + "." + fieldNames.token
    findOne(Map(key -> token)) map { owner ⇒
      owner.isReset ? (InvalidToken(): Error).fail[Account] | {
        val upd = owner.copy(password = BCryptPassword(password).encrypted, resetAt = DateTime.now, reset = Token())
        save(upd)
        upd.success[Error]
      }
    } getOrElse InvalidToken().fail
  }

  override def save(t: Account, wc: WriteConcern) {
    super.save(t.copy(updatedAt = DateTime.now), wc)
  }

}

