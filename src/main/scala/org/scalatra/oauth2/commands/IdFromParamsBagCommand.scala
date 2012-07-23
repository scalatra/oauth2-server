package org.scalatra
package oauth2
package commands

import command._

trait IdFromParamsBagCommand extends ForceFromParams { self: Command ⇒
  val namesToForce: Set[String] = Set("id")
}
