package io.backchat.oauth2
package model

case class SmtpConfig(
    host: String,
    port: Int,
    from: MailSender,
    user: Option[String],
    password: Option[String],
    sslRequired: Boolean) {
  val authRequired = user.isDefined
}