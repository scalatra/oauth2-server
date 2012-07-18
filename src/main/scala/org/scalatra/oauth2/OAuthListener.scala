package org.scalatra
package oauth2

import javax.servlet.annotation.WebListener
import javax.servlet.{ ServletContextEvent, ServletContextListener }

@WebListener
class OAuthListener extends ServletContextListener {
  def contextInitialized(sce: ServletContextEvent) {
    println("OAuth listener initialized")
  }

  def contextDestroyed(sce: ServletContextEvent) {
    println("OAuth listener disabled")
  }
}
