'use strict'
# Services

# Create an object to hold the module.
mod = {}

mod.version = -> "0.1"

mod.Permission = ['$resource', ($resource) ->
  $resource("/permissions/:id", {id: "@id"})
]

mod.loginService = [
  '$rootScope'
  '$http'
  '$location'
  '$log'
  'notificationService'
  ($rootScope, $http, $location, $log, notificationService) ->
    svc =
      authenticated: (user) ->
        if user?
          $rootScope.currentUser = user
        else
          angular.isObject($rootScope.currentUser)
      logOut: (redirect) ->
        $http
          .get("/logout")
          .success (response) ->
            $rootScope.currentUser = null
            if redirect
              notificationService.notify('info', { message: 'Logged out.'})
              $location.path("/login")
          .error (response) ->
            notificationService.errors(response.errors||[])
      anonymous: ->
        not $rootScope.currentUser?
      restoreAuth: ->
        $http
          .get("/")
          .success (response, status, headers, config) ->
            @authenticated response.data
          .error (response, status, headers, config) ->
            @logOut()


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