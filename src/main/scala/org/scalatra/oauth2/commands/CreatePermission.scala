package org.scalatra
package oauth2
package commands

import command._
import model.fieldNames

class CreatePermission extends Command with ValidationSupport {

  val code = bind[String](fieldNames.code)
  val name = bind[String](fieldNames.name)
  val description = bind[String](fieldNames.description)
  val isSystem = bind[Boolean](fieldNames.isSystem)
}
