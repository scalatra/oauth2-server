package org.scalatra
package oauth2
package commands

import command._
import model.fieldNames

trait IdFromParamsBagCommand extends ForceFromParams { self: Command ⇒
  val namesToForce: Set[String] = Set("id")
}

trait TokenFromParamsBagCommand extends Command with ValidationSupport with CommandValidators with ForceFromParams { this: Command ⇒
  val namesToForce: Set[String] = Set(fieldNames.token)

  val token = bind[String](fieldNames.token).validate(nonEmptyString(fieldNames.token))
}
