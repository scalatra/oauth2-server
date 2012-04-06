package io.backchat.oauth2
package model

case class WebConfig(guiUrl: String, host: String, port: Int, sslRequired: Boolean, public: String) {
  private[this] val DefaultPorts = Vector(80, 443)
  private def isWebDefault = DefaultPorts contains port

  val domainWithPort = if (isWebDefault) host else host + ":" + port
}
