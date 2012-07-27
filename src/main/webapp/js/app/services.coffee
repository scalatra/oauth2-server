'use strict'
# Services

# Create an object to hold the module.
mod = {}

mod.version = -> "0.1"

mod.Permission = ['$resource', ($resource) ->
  $resource("/permissions/:id", {id: "@id"})
]

mod.loginService = ['$rootScope', '$http', '$log', 'notificationService', ($rootScope, $http, $log, notificationService) ->
  svc = 
    authenticated: (user) ->
      if user?
        $rootScope.currentUser = user
      else
        angular.isObject($rootScope.currentUser)
    logout: ->
      $rootScope.currentUser = null
    anonymous: ->
      not $rootScope.currentUser?
    restoreAuth: ->
      $http
        .get("/check_auth")
        .success (response, status, headers, config) ->
          @authenticated response.data
        .error (response, status, headers, config) ->
          @logout()


  $rootScope.$watch "currentUser", (newValue, oldValue) ->
    unless angular.equals(oldValue, newValue)
      $log.info("changing user from " + angular.toJson(oldValue, true) + " to " + angular.toJson(newValue, true))
      evt = if angular.isObject(newValue) then ["authenticated", newValue] else ["loggedOut", oldValue]
      $rootScope.$broadcast(evt[0], evt[1])

  svc
]

mod.notificationService = ['$rootScope', '$log', ($rootScope, $log) ->
  svc =
    notify: (level, obj) ->
      $log.info("notifiying " + level)
      $rootScope.$broadcast("notification."+level, obj)
    errors: (validationErrors, obj) ->
      if angular.isArray(obj)
        validations = []
        errors = []
        _.each obj, (item) ->
          if angular.isString(item.field_name)
            validations.push(item)
          else
            errors.push('error', item)
        validationErrors.push.apply(validationErrors, validations)
        @error()
    info: (obj) -> @notify('info', obj)
    warn: (obj) -> @notify('warn', obj)
    success: (obj) -> @notify('success', obj)
    error: (obj) -> @notify('error', obj)

  svc
]

angular.module('app.services', []).factory(mod)