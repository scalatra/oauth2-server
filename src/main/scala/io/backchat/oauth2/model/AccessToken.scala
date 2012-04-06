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

case class AccessToken(
  @Key("_id") token: String,
  identity: String,
  clientId: String,
  scope: String,
  expiresAt: DateTime = MinDate,
  revoked: DateTime = MinDate,
  lastAccess: DateTime = MinDate,
  previousAccess: DateTime = MinDate)
