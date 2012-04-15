package io.backchat.oauth2
package model

import OAuth2Imports._
import scala.util.control.Exception.ignoring
import com.mongodb.casbah.commons.conversions.scala.{ DeregisterJodaTimeConversionHelpers, RegisterJodaTimeConversionHelpers }
import com.mongodb.casbah.MongoURI

//case class MongoConfiguration(
//    host: String,
//    port: Int,
//    database: String,
//    user: Option[String] = None,
//    password: Option[String] = None) {
object MongoConfiguration {
  def apply(uri: String): MongoConfiguration = MongoConfiguration(MongoURI(uri))
}
case class MongoConfiguration(uri: MongoURI) {

  logger.info("Connecting to mongodb with: %s" format uri.toString())
  def isAuthenticated = uri.username.blankOption.isDefined
  var _db: MongoDB = null
  var _conn: MongoConnection = null
  def connection = synchronized {
    if (_conn == null) {
      _conn = uri.connect
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
      val db = uri.connectDB
      uri.username.blankOption foreach { u ⇒ db.authenticate(u, uri.password.toString) }
      _db = db
    }
    _db
  }

}
