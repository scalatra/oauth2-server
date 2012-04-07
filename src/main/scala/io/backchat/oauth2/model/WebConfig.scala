package io.backchat.oauth2
package model

import model.Enums.AuthorizationType

case class WebConfig(
    guiUrl: String,
    host: String,
    port: Int,
    sslRequired: Boolean,
    public: String,
    /** Supported authorization types. Defaults to AuthorizationType.CodeAndToken */
    authorizationType: AuthorizationType.Value,
    /** Authorization realm that will show up in 401 responses. Defaults to use the request host name. */
    realm: String,
    /** If true, supports authentication using query/form parameters. */
    allowParams: Boolean) {
  private[this] val DefaultPorts = Vector(80, 443)
  private def isWebDefault = DefaultPorts contains port

  val domainWithPort = if (isWebDefault) host else host + ":" + port
}
