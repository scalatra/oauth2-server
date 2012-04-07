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
  @Key("_id") id: String,
  secret: String,
  displayName: String,
  link: Option[String],
  redirectUri: Option[String],
  urlWhitelist: List[String],
  scope: String,
  authorizationType: AuthorizationType.Value,
  revoked: Option[DateTime],
  tokensGranted: Int,
  tokensRevoked: Int)

class ClientDao(collection: MongoCollection)(implicit system: ActorSystem)
    extends SalatDAO[Client, String](collection = collection) {

  def create()
}
