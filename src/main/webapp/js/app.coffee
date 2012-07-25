"use strict"

# Declare app level module which depends on filters, and services
App = angular.module('app', [
  'ngCookies'
  'ngResource'
  'ui',
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
    .when("/forgot", {templateUrl: '/templates/forgot.html', controller: 'ForgotController'})
    .when("/reset", {templateUrl: '/templates/reset.html', controller: "ResetController"})
    .when("/register", { templateUrl: '/templates/register.html', controller: 'RegisterController' })
    .when('/permissions', {templateUrl: '/templates/permissions/list.html', controller: "PermissionList"})   
    .when('/auth/facebook', { templateUrl: '/external/auth/facebook.html' }) 
    .when('/auth/twitter', { templateUrl: '/external/auth/twitter.html' }) 

    # Catch all
    .otherwise({redirectTo: '/'})

  # Without serve side support html5 must be disabled.
  $locationProvider.html5Mode true

])

App.run ['$rootScope', '$location', '$window', ($rootScope, $location, $window) ->
  $rootScope.currentUser = null
  allowed = [
    "/templates/login.html"
    "/templates/forgot.html"
    "/templates/register.html"
  ]
  $rootScope.$on "$routeChangeStart", (event, next, current) ->
    isExternal = /^\/external/i.test(next.templateUrl)
    unless isExternal
      isUnauthenticatedUrl = _.contains(allowed, next.templateUrl)    
      if not $rootScope.currentUser? and not isUnauthenticatedUrl
        $location.path("/login")
        event.stopPropagation() if event.stopPropagation?
      else if $rootScope.currentUser? and isUnauthenticatedUrl
        event.stopPropagation() if event.stopPropagation?
    else
      $window.location.href = /^\/external(.*)\.html/i.exec(next.templateUrl)[1]


]

angular.element(document).ready ->
  angular.bootstrap(document, ['app'])