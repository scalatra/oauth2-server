package io.backchat.oauth2
package model

import javax.mail.internet.InternetAddress
import org.clapper.scalasti.StringTemplateGroup
import OAuth2Imports._
import akka.actor.ActorSystem

sealed trait UserMailMessage
case class ConfirmationMail(name: String, login: String, email: String, token: String) extends UserMailMessage
case class SendForgotPasswordMail(name: String, login: String, email: String, token: String) extends UserMailMessage
case class SendInvitationMail(name: String, email: String, token: String) extends UserMailMessage

case class MailSender(name: String, email: String) {
  def asEmail = name match {
    case "" | null ⇒ new InternetAddress(email, "", UTF_8)
    case nm        ⇒ new InternetAddress(email, name, UTF_8)
  }
}
object MailSender {
  def apply(email: String): MailSender = MailSender(null, email)
}

case class MailMessage(from: MailSender, to: List[MailSender], cc: List[MailSender], bcc: List[MailSender], subject: String, body: String, attachments: List[String])
object MailMessage {

  private val mailTemplates = new StringTemplateGroup("mailTemplates")
  private def renderTemplate(group: StringTemplateGroup, name: String, context: Map[String, Any]): String = {
    val template = group.template("oauth2/mails/%s".format(name))
    template.setAttributes(context)
    template.toString
  }

  def apply(from: MailSender, to: MailSender, subject: String, body: String, attachments: List[String] = Nil): MailMessage =
    MailMessage(from, to :: Nil, Nil, Nil, subject, body, attachments)

  def apply(from: MailSender, to: List[MailSender], subject: String, body: String, attachments: List[String]): MailMessage =
    MailMessage(from, to, Nil, Nil, subject, body, attachments)

  def apply(mail: ConfirmationMail)(implicit system: ActorSystem): MailMessage = {
    val oauth = OAuth2Extension(system)
    val params = Map(
      "activation_url" -> "%s/activate/%s".format(oauth.web.guiUrl, mail.token),
      "apikey_url" -> oauth.web.guiUrl,
      "name" -> mail.name,
      "login" -> mail.login,
      "email" -> mail.email)
    MailMessage(
      oauth.smtp.noReplySender,
      MailSender(mail.name, mail.email),
      "Thank you for registering, please confirm your account.",
      renderTemplate(mailTemplates, "confirm", params))
  }

  def apply(mail: SendForgotPasswordMail)(implicit system: ActorSystem): MailMessage = {
    val oauth = OAuth2Extension(system)
    val params = Map(
      "url" -> (oauth.web.guiUrl + "/reset/%s".format(mail.token)),
      "name" -> mail.name,
      "login" -> mail.login,
      "email" -> mail.email)
    MailMessage(
      oauth.smtp.noReplySender,
      MailSender(mail.name, mail.email),
      "You requested a password reset.",
      renderTemplate(mailTemplates, "forgot", params))
  }

}
