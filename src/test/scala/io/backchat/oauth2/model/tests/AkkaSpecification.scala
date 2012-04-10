package io.backchat.oauth2
package model
package tests

import akka.testkit._
import org.specs2.specification._
import com.typesafe.config.{ ConfigFactory, Config }
import akka.actor.{ ActorSystemImpl, ActorSystem }
import akka.dispatch.Await
import java.util.concurrent.TimeoutException
import akka.util.duration._
import io.backchat.oauth2.Logging
import collection.JavaConverters._

object AkkaSpecification {
  val testConf: Config = ConfigFactory.parseString("""
      akka {
        event-handlers = ["akka.testkit.TestEventListener"]
        loglevel = WARNING
        stdout-loglevel = WARNING
        extensions = [
          "io.backchat.oauth2.OAuth2Extension$"
        ]
        actor {
          default-dispatcher {
            core-pool-size-factor = 2
            core-pool-size-min = 8
            core-pool-size-max = 8
            max-pool-size-factor = 2
            max-pool-size-min = 8
            max-pool-size-max = 8
          }
        }
      }
      """).withFallback(ConfigFactory.load())

  def mapToConfig(map: Map[String, Any]): Config = {
    import scala.collection.JavaConverters._
    ConfigFactory.parseMap(map.asJava)
  }

  def getCallerName: String = getNonBaseCallerName("AkkaSpecification")

  def getNonBaseCallerName(name: String): String = {
    val s = Thread.currentThread.getStackTrace map (_.getClassName) drop 1 dropWhile (_ matches ".*%s.?$".format(name))
    s.head.replaceFirst(""".*\.""", "").replaceAll("[^a-zA-Z_0-9]", "_")
  }

}

abstract class AkkaSpecification(_system: ActorSystem) extends TestKit(_system) with OAuth2ServerSpecification with Logging {

  def this(config: Config) = this(ActorSystem(AkkaSpecification.getCallerName, config.withFallback(AkkaSpecification.testConf)))

  def this(s: String) = this(ConfigFactory.parseString(s))

  def this(configMap: Map[String, _]) = this(AkkaSpecification.mapToConfig(configMap))

  def this() = this(ActorSystem(AkkaSpecification.getCallerName, AkkaSpecification.testConf))

  override def map(fs: ⇒ Fragments) = super.map(fs) ^ Step(stopActors)

  private def stopActors = {
    import scala.util.control.Exception.ignoring
    ignoring(classOf[Throwable]) {
      system.shutdown()
      try Await.ready(system.asInstanceOf[ActorSystemImpl].terminationFuture, 5 seconds) catch {
        case _: TimeoutException ⇒ system.log.warning("Failed to stop [{}] within 5 seconds", system.name)
      }
    }
  }
}
