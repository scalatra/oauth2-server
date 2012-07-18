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

case class Permission(@Key("_id") code: String, name: String, description: String, isSystem: Boolean = false)

class PermissionDao(collection: MongoCollection)(implicit system: ActorSystem)
    extends SalatDAO[Permission, String](collection = collection) {
  private val oauth = OAuth2Extension(system)

  oauth.permissions foreach save

}