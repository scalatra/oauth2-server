package org.scalatra
package oauth2
package model

import com.novus.salat._
import com.novus.salat.global._
import com.novus.salat.annotations._
import com.novus.salat.dao._
import scalaz._
import Scalaz._
import OAuth2Imports._
import akka.actor.ActorSystem
import databinding.FieldValidation
import org.scalatra.validation.ValidationError

case class Client(
    secret: String,
    profile: String,
    displayName: String,
    authorizationType: AuthorizationType.Value,
    @Key("_id") id: ObjectId = new ObjectId(),
    scope: List[String] = Nil,
    redirectUri: Option[String],
    link: Option[String] = None,
    revoked: DateTime = MinDate,
    tokensGranted: Int = 0,
    tokensRevoked: Int = 0) {
  def isRevoked = revoked > MinDate
  def isConfidential = profile equalsIgnoreCase "Web Application"
}

class ClientDao(collection: MongoCollection)(implicit system: ActorSystem)
    extends SalatDAO[Client, ObjectId](collection = collection) {

  object validate {
    import org.scalatra.validation.Validation._
    import Validations._

    def name(displayName: String) = nonEmptyString(fieldNames.displayName, displayName)

    def scopes(scope: List[String]) = nonEmptyCollection(fieldNames.scope, scope.filter(_.nonBlank))

    def clientProfile(profile: String): FieldValidation[String] =
      for {
        nem ← nonEmptyString(fieldNames.profile, profile)
        oo ← oneOf(fieldNames.profile, nem, "Web Application", "User-Agent", "Native Application")
      } yield oo

    def clientSecret(secret: String) = nonEmptyString(fieldNames.secret, secret)

    def validRedirectUri(uri: Option[String]): FieldValidation[Option[String]] = {
      (uri map { u ⇒
        for {
          nem ← nonEmptyString(fieldNames.redirectUri, u)
          valid ← validAbsoluteUrl(fieldNames.redirectUri, nem, !OAuth2Extension.isProduction, "http", "https")
        } yield valid.some
      }) | none[String].success
    }

    def validLink(uri: Option[String]): FieldValidation[Option[String]] =
      ((uri >>= (_.blankOption)) ∘ (s ⇒ validAbsoluteUrl(fieldNames.link, s, !OAuth2Extension.isProduction, "http", "https") ∘ (_.some))) | none[String].success

    def authorizationType(authType: String) =
      enumValue(fieldNames.authorizationType, authType, AuthorizationType).map(AuthorizationType.withName)

    def apply(client: Client) = {
      val factory: Factory = client.copy(client.secret, _, _, _, client.id, _, _, _)
      buildClient(client.profile, client.displayName, client.authorizationType.toString, client.scope, client.redirectUri, client.link)(factory)
    }

    /*_*/
    private def buildClient(
      profile: String,
      displayName: String,
      authType: String,
      scopes: List[String],
      redirectUri: Option[String],
      link: Option[String])(factory: Factory): ValidationNEL[ValidationError, Client] = {
      ((validate.clientProfile(profile).liftFailNel)
        |@| (validate.name(displayName).liftFailNel)
        |@| (validate.authorizationType(authType).liftFailNel)
        |@| (validate.scopes(scopes).liftFailNel)
        |@| (validate.validRedirectUri(redirectUri).liftFailNel)
        |@| (validate.validLink(link).liftFailNel))(factory)
    }
    /*_*/

  }
  private type Factory = (String, String, AuthorizationType.Value, List[String], Option[String], Option[String]) ⇒ Client

  def authorize(clientId: String, clientSecret: String) = { null }
}
