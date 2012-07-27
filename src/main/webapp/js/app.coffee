"use strict"

# Declare app level module which depends on filters, and services
App = angular.module('app', [
  'ngCookies'
  'ngResource'
  'bootstrap'
  'ui'
  'app.controllers'
  'app.directives'
  'app.filters'
  'app.services'
])

App.config([
  '$routeProvider'
  '$locationProvider'


($routeProvider, $locationProvider, config) ->

  $routeProvider
    .when("/", {templateUrl: '/templates/home.html', controller: 'HomeController'})
    .when("/login", {templateUrl: '/templates/login.html', controller: 'LoginController'})
    .when("/logout", { templateUrl: '/external/logout.html' })
    .when("/forgot", {templateUrl: '/templates/forgot.html', controller: 'ForgotController'})
    .when("/reset/:token", {templateUrl: '/templates/reset.html', controller: "ResetController"})
    .when("/register", { templateUrl: '/templates/register.html', controller: 'RegisterController' })
    .when('/permissions', {templateUrl: '/templates/permissions/list.html', controller: "PermissionList"})   
    .when('/auth/facebook', { templateUrl: '/external/auth/facebook.html' }) 
    .when('/auth/twitter', { templateUrl: '/external/auth/twitter.html' })
    .otherwise({redirectTo: '/'})

  # Without server side support html5 must be disabled.
  # (server side support being everything must render the angular view partial)
  $locationProvider.html5Mode true

])

App.constant 'validationFormats',
  required: "{{field_name}} must be present."
  emptyCollection: "{{field_name}} must not be empty."
  email: "{{field_name}} must be a valid email."
  url: "{{field_name}} must be a valid url."
  pattern: "{{field_name}} is invalid."
  sameAs: "{{field_name}} must match confirmation."
  minlength: "{{field_name}} is too short."
  maxlength: "{{field_name}} is too long."
  min: "{{field_name}} is too small."
  max: "{{field_name}} is too large"

App.run ['$rootScope', '$location', '$window', '$log', ($rootScope, $location, $window, $log) ->

#  $rootScope.currentUser = null

  $rootScope.$on "$routeChangeStart", (event, next, current) ->
#    $log.info("routing to: " + next.templateUrl)
#    $log.info(next)
    isExternal = /^\/external/i.test(next.templateUrl)
    if isExternal
      $window.location.href = /^\/external(.*)\.html/i.exec(next.templateUrl)[1]

    false
]

angular.element(document).ready ->
  angular.bootstrap(document, ['app'])