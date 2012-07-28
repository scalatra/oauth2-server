package org.scalatra
package oauth2
package model

import command.{ ValidationSupport, Command }

trait OAuth2ModelCommand[TModel <: Product] extends Command with ValidationSupport with ModelCommand[TModel]
