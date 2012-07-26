'use strict'
# Services

# Create an object to hold the module.
mod = {}

mod.version = -> "0.1"

mod.Permission = ['$resource', ($resource) ->
  $resource("/permissions/:id", {id: "@id"})
]

mod.loginService = ['$rootScope', ($rootScope) ->
  svc = 
    authenticated: (user) ->
      if user?
        console.log("broadcasting authenticated.")
        $rootScope.currentUser = user
        $rootScope.$broadcast("authenticated", user)
      else
        console.log("returning current user value: "+$rootScope.currentUser?)
        $rootScope.currentUser?
    logout: ->
      u = $rootScope.currentUser
      $rootScope.currentUser = null
      $rootScope.$broadcast("loggedOut", u)
    anonymous: () ->
      not $rootScope.currentUser?


  $rootScope.$watch "currentUser", (oldValue, newValue) ->
    console.log("current user has changed.")
    newValue
  
  svc
]

angular.module('app.services', []).factory(mod)