package org.scalatra
package oauth2

import grizzled.slf4j.Logger

trait Logging {

  @transient lazy val logger: Logger = Logger(getClass)
}