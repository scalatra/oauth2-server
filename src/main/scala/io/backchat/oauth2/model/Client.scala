package io.backchat.oauth2
package model

import com.novus.salat._
import com.novus.salat.global._
import com.novus.salat.annotations._
import com.novus.salat.dao._
import Enums._
import scalaz._
import Scalaz._
import OAuth2Imports._
import akka.actor.ActorSystem

case class Client(
  secret: String,
  profile: String,
  displayName: String,
  authorizationType: AuthorizationType.Value,
  @Key("_id") id: ObjectId = new ObjectId,
  scope: List[String] = Nil,
  redirectUri: Option[String],
  link: Option[String] = None,
  revoked: DateTime = MinDate,
  tokensGranted: Int = 0,
  tokensRevoked: Int = 0)

class ClientDao(collection: MongoCollection)(implicit system: ActorSystem)
    extends SalatDAO[Client, ObjectId](collection = collection) {

  object validations {
    import Validations._

    def name(displayName: String) = nonEmptyString(fieldNames.displayName, displayName)

    def scopes(scope: List[String]) = {
      val r = nonEmptyCollection(fieldNames.scope, scope.filter(_.nonBlank))
      r
    }

    def clientProfile(profile: String): Validation[Error, String] =
      for {
        nem ← nonEmptyString(fieldNames.profile, profile)
        oo ← oneOf(fieldNames.profile, nem, "Web Application", "User-Agent", "Native Application")
      } yield oo

    def clientSecret(secret: String) = nonEmptyString(fieldNames.secret, secret)

    def validRedirectUri(uri: Option[String]): Validation[Error, Option[String]] = {
      (uri map { u ⇒
        for {
          nem ← nonEmptyString(fieldNames.redirectUri, u)
          valid ← validAbsoluteUrl(fieldNames.redirectUri, nem, "http", "https")
        } yield valid.some
      }) | none[String].success
    }

    def validLink(uri: Option[String]): Validation[Error, Option[String]] =
      ((uri >>= (_.blankOption)) ∘ (s ⇒ validAbsoluteUrl(fieldNames.link, s, "http", "https") ∘ (_.some))) | none[String].success

    def authorizationType(authType: String) =
      enumValue(fieldNames.authorizationType, authType, AuthorizationType).map(AuthorizationType.withName)
  }
  private type Factory = (String, String, AuthorizationType.Value, List[String], Option[String], Option[String]) ⇒ Client

  def create(
    profile: String,
    displayName: String,
    authType: String,
    scopes: List[String],
    redirectUri: Option[String],
    link: Option[String]) = {
    val factory = (p: String, n: String, at: AuthorizationType.Value, s: List[String], r: Option[String], l: Option[String]) ⇒
      Client(Token.generate.token, p, n, at, scope = s, redirectUri = r, link = l)
    val cl = buildClient(profile, displayName, authType, scopes, redirectUri, link)(factory)
    cl foreach save
    cl
  }

  def validateClient(client: Client) = {
    val factory: Factory = client.copy(client.secret, _, _, _, client.id, _, _, _)
    buildClient(client.profile, client.displayName, client.authorizationType.toString, client.scope, client.redirectUri, client.link)(factory)
  }

  private def buildClient(
    profile: String,
    displayName: String,
    authType: String,
    scopes: List[String],
    redirectUri: Option[String],
    link: Option[String])(factory: Factory): ValidationNEL[Error, Client] = {
    ((validations.clientProfile(profile).liftFailNel)
      |@| (validations.name(displayName).liftFailNel)
      |@| (validations.authorizationType(authType).liftFailNel)
      |@| (validations.scopes(scopes).liftFailNel)
      |@| (validations.validRedirectUri(redirectUri).liftFailNel)
      |@| (validations.validLink(link).liftFailNel))(factory)
  }

  def authorize(clientId: String, clientSecret: String) = { null }
}
