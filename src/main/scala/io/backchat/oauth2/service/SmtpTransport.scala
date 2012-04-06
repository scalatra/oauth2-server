package io.backchat.oauth2
package service

import java.util.Properties
import javax.mail.{ Address, Session }
import javax.mail.internet.{ MimeBodyPart, MimeMessage, MimeMultipart }
import javax.activation.{ MailcapCommandMap, CommandMap, FileDataSource, DataHandler }
import model.{ MailSender, SmtpConfig, MailMessage }
import scalaz._
import Scalaz._
import javax.mail.Message.RecipientType

class SmtpTransport(val config: SmtpConfig) extends MailTransport {

  protected val props = new Properties
  props.setProperty("mail.transport.protocol", "smtp")
  props.setProperty("mail.smtp.host", config.host)
  props.setProperty("mail.smtp.port", config.port.toString)
  if (config.authRequired) {
    props.setProperty("mail.smtp.user", ~config.user)
    props.setProperty("mail.smtp.password", ~config.password)
  }

  protected def createMimeMessage(message: MailMessage, session: Session) = {
    val mailMsg = new MimeMessage(session)
    mailMsg.setSubject(message.subject, UTF_8)
    mailMsg.setFrom(message.from.asEmail)

    mailMsg.setRecipients(RecipientType.TO, message.to.map(_.asEmail.asInstanceOf[Address]).toArray)
    mailMsg.setRecipients(RecipientType.CC, message.cc.map(_.asEmail.asInstanceOf[Address]).toArray)
    mailMsg.setRecipients(RecipientType.BCC, message.bcc.map(_.asEmail.asInstanceOf[Address]).toArray)

    val body = new MimeBodyPart()
    body.setContent(message.body, "text/html")

    val attachments = message.attachments map { att ⇒
      val fds = new FileDataSource(att)
      val filePart = new MimeBodyPart
      filePart.setDataHandler(new DataHandler(fds))
      filePart.setFileName(fds.getName)
      filePart
    }

    val mp = new MimeMultipart
    mp.addBodyPart(body)
    attachments.foreach(att ⇒ mp.addBodyPart(att))
    mailMsg.setContent(mp)
    mailMsg
  }

  def send(message: MailMessage) {
    val mc = CommandMap.getDefaultCommandMap.asInstanceOf[MailcapCommandMap]
    mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html")
    mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml")
    mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain")
    mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed")
    mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822")
    CommandMap.setDefaultCommandMap(mc)

    val mailSession = Session.getInstance(props)
    val transport = mailSession.getTransport("smtp")

    val mailMsg = createMimeMessage(message, mailSession)
    transport.connect
    try {
      transport.sendMessage(mailMsg, mailMsg.getRecipients(RecipientType.TO))
    } catch {
      case e ⇒ logger error ("There was a problem sending an email: %s" format message, e)
    } finally {
      transport.close
    }
  }

  val noReplySender = MailSender(config.from.name, config.from.email)
}

trait MailTransport {

  def send(email: MailMessage): Unit
}