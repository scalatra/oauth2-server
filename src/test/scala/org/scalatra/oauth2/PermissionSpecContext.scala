package org.scalatra.oauth2

import model.PermissionDao
import org.specs2.specification.After
import org.scalatra.oauth2.OAuth2Imports._

trait PermissionSpecContextBase extends After {
  val conn = MongoConnection()
  val coll = conn("oauth_server_test")("permissions")
  coll.drop()
  def dao: PermissionDao

  def after = {
    conn.close()
  }

}