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

case class AccessGrant(
  code: String,
  identity: String,
  clientId: String,
  redirectUri: String,
  accessToken: String,
  scope: List[String],
  @Key("_id") id: ObjectId = new ObjectId,
  grantedAt: DateTime = MinDate,
  expiresAt: DateTime = MinDate,
  revoked: DateTime = MinDate)
