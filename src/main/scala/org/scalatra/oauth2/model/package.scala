package org.scalatra
package oauth2

import scalaz._
import Scalaz._
import org.scalatra.validation.ValidationError

package object model {

  type ModelValidation[T] = Validation[NonEmptyList[ValidationError], T]

  object fieldNames {
    val login = "login"
    val id = "id"
    val _id = "_id"
    val email = "email"
    val password = "password"
    val passwordConfirmation = "passwordConfirmation"
    val createdAt = "createdAt"
    val updatedAt = "updatedAt"
    val secret = "secret"
    val displayName = "displayName"
    val rememberedAt = "rememberedAt"
    val link = "link"
    val redirectUri = "redirectUri"
    val urlWhitelist = "urlWhitelist"
    val scope = "scope"
    val authorizationType = "authorizationType"
    val revoked = "revoked"
    val tokensGranted = "tokensGranted"
    val tokensRevoked = "tokensRevoked"
    val clientId = "clientId"
    val state = "state"
    val responseType = "responseType"
    val grantCode = "grantCode"
    val accessToken = "accessToken"
    val identity = "identity"
    val expiresAt = "expiresAt"
    val lastAccess = "lastAccess"
    val previousAccess = "previousAccess"
    val name = "name"
    val grantedAt = "grantedAt"
    val token = "token"
    val pwd = "pwd"
    val salt = "salt"
    val currentSignInIp = "currentSignInIp"
    val previousSignInIp = "previousSignInIp"
    val currentSignInAt = "currentSignInAt"
    val previousSignInAt = "previousSignInAt"
    val remembered = "remembered"
    val remember = "remember"
    val confirmation = "confirmation"
    val reset = "reset"
    val stats = "stats"
    val clientType = "clientType"
    val profile = "profile"
    val code = "code"
    val description = "description"
    val isSystem = "isSystem"
  }
}

