package io.backchat.oauth2
package model

import com.novus.salat._
import com.novus.salat.global._
import com.novus.salat.annotations._
import com.novus.salat.dao._
import scalaz._
import Scalaz._
import OAuth2Imports._

case class AuthRequest(
  clientId: String,
  accessToken: String,
  redirectUri: String,
  responseType: Option[ResponseType.Value],
  grantCode: GrantCode.Value,
  scope: List[String] = Nil,
  state: Option[String] = None,
  revoked: DateTime = MinDate,
  @Key("_id") id: ObjectId = new ObjectId)
