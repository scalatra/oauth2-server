package io.backchat.oauth2

import akka.actor._
import model.{ MailSender, SmtpConfig, ResourceOwnerDao, MongoConfiguration }
import net.liftweb.json.Formats
import service.SmtpTransport

object OAuth2Extension extends ExtensionId[OAuth2Extension] with ExtensionIdProvider {
  def lookup() = OAuth2Extension

  def createExtension(system: ExtendedActorSystem) = new OAuth2Extension(system)

  val Production = "production"
  val Development = "development"
  val Staging = "staging"
  val Test = "test"
}

class OAuth2Extension(system: ExtendedActorSystem) extends Extension {

  import JettyMain.confKey
  private[this] val cfg = system.settings.config
  private[this] def key(value: String) = confKey("mongo.%s" format value)

  val mongo = MongoConfiguration(
    cfg.getString(key("host")),
    cfg.getInt(key("port")),
    cfg.getString(key("database")),
    cfg.getString(key("user")).blankOption,
    cfg.getString(key("password")).blankOption)

  val defaultFormats: Formats = new OAuth2Formats

  lazy val userProvider = new ResourceOwnerDao(mongo.db("resource_owners"))(system)

  val guiUrl = cfg.getString(confKey("web.url"))

  val smtp = new SmtpTransport(SmtpConfig(
    cfg.getString("backchat.smtp.host"),
    cfg.getInt("backchat.smtp.port"),
    MailSender(cfg.getString("backchat.smtp.from.name"), cfg.getString("backchat.smtp.from.email")),
    cfg.getString("backchat.smtp.user").blankOption,
    cfg.getString("backchat.smtp.password").blankOption,
    cfg.getBoolean("backchat.smtp.sslRequired")))

}
