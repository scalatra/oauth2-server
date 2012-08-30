package org.scalatra
package oauth2
package commands

import databinding._
import model.fieldNames

trait IdFromParamsBagCommand extends ForceFromParams { self: Command ⇒
  val namesToForce: Set[String] = Set("id")
}

trait TokenFromParamsBagCommand extends JsonCommand with ForceFromParams { this: Command ⇒
  val namesToForce: Set[String] = Set(fieldNames.token)

  val token = asType[String](fieldNames.token).notBlank
}
