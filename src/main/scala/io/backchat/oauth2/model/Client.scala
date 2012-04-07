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
  @Key("_id") id: ObjectId,
  secret: String,
  profile: String,
  displayName: String,
  scope: String,
  authorizationType: AuthorizationType.Value,
  redirectUri: Option[String] = None,
  link: Option[String] = None,
  revoked: DateTime = MinDate,
  tokensGranted: Int = 0,
  tokensRevoked: Int = 0)

class ClientDao(collection: MongoCollection)(implicit system: ActorSystem)
    extends SalatDAO[Client, ObjectId](collection = collection) {

  object validations {
    import Validations._

    def clientProfile(profile: String) =
      for {
        nem ← nonEmptyString(fieldNames.profile).validate(profile)
        oo ← oneOf(fieldNames.profile, "Web Application", "User-Agent", "Native Application").validate(nem)
      } yield oo

    def validRedirectUri(uri: Option[String]): Validation[Error, String] =
      for {
        nem ← nonEmptyString(fieldNames.redirectUri).validate(~uri)
        uri ← validUrl(fieldNames.redirectUri, true, "http", "https").validate(nem)
      } yield uri

    def validLink(uri: Option[String]): Validation[Error, Option[String]] =
      ((uri >>= (_.blankOption)) ∘ (s ⇒ validUrl(fieldNames.link, true, "http", "https").validate(s) ∘ (_.some))) | none[String].success
  }

  def create(clientType: String, displayName: String) = { null }

  def authorize(clientId: String, clientSecret: String) = { null }
}
