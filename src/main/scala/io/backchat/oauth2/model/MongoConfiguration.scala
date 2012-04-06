package io.backchat.oauth2
package model

import OAuth2Imports._
import scala.util.control.Exception.ignoring
import com.mongodb.casbah.commons.conversions.scala.{ DeregisterJodaTimeConversionHelpers, RegisterJodaTimeConversionHelpers }

case class MongoConfiguration(
    host: String,
    port: Int,
    database: String,
    user: Option[String] = None,
    password: Option[String] = None) {

  def isAuthenticated = user.isDefined

  var _db: MongoDB = null
  var _conn: MongoConnection = null
  def connection = synchronized {
    if (_conn == null) {
      _conn = MongoConnection(host, port)
      RegisterJodaTimeConversionHelpers()
    }
    _conn
  }

  def disconnect = synchronized {
    ignoring(classOf[Throwable]) { Option(_conn) foreach { _.close() } }
    DeregisterJodaTimeConversionHelpers()
    _conn = null
    _db = null
  }

  def db = synchronized {
    if (_db == null) {
      val db = connection(database)
      user foreach { u ⇒ db.authenticate(u, password getOrElse "") }
      _db = db
    }
    _db
  }

}
