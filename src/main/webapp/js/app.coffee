"use strict"

# Declare app level module which depends on filters, and services
App = angular.module('app', [
  'ngCookies'
  'ngResource'
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

    # Catch all
    .otherwise({redirectTo: '/'})

  # Without serve side support html5 must be disabled.
  $locationProvider.html5Mode true

])

angular.element(document).ready ->
  angular.bootstrap(document, ['app'])