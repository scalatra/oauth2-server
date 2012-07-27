'use strict'

mod = {}

mod.AppController = [
  '$scope'
  '$location'
  'loginService'
  'notificationService'
  ($scope, $location, loginService, notificationService) ->
    $scope.pageTitle = "Scalatra OAuth2"

    $scope.errorClass = (field) ->
      showError = _.any $scope.validationErrors || [], (item) -> item.field_name == field
      if showError then "error" else ""

    $scope.$on "authenticated", (sc, user) ->
      notificationService.notify "info", {message: ("Welcome, " + user.name)}      
      $location.path("/")

    # $scope.loadView = ->
    #   console.log("Loading view")


]

mod.HomeController = ['$scope', '$location', ($scope, $location) ->
]

mod.LoginController = [
  '$scope' 
  '$http'
  '$location'
  'loginService'
  'notificationService' 
  ($scope, $http, $location, loginService, notificationService) ->
    $scope.credentials = {}
    $scope.validationErrors = []
    $scope.login = (credentials) ->
      $http
        .post("login", credentials)
        .success (response) ->
          loginService.authenticated(response.data)
        .error (response, status, headers) ->
          loginService.logout()
          $scope.credentials =
            login: response.data.login
            remember: response.data.remember
          switch status
            when 401 
              notificationService.notify('error', 'There was a problem with your username or password.')
            else
              notificationService.notify('error', response.errors)
    counter = 0

]

mod.ResetController = [
  '$scope'
  '$http'
  '$location'
  '$routeParams'
  'loginService'
  'notificationService'
  ($scope, $http, $location, $routeParams, loginService, notificationService) ->
    $scope.validationErrors = []

    $scope.hasErrors =  ->
      $scope.resetPasswordForm.password.$setValidity("sameAs", $scope.password == $scope.confirmation) if $scope.resetPasswordForm.password?
      $scope.resetPasswordForm.$invalid

    $scope.resetPassword = (password, confirmation) ->
      data =
        password: password
        passwordConfirmation: confirmation

      $http
        .post("/reset/"+$routeParams.token, data)
        .success (response) ->
          loginService.authenticated(response.data)
        .error (response, status, headers) ->
          notificationService.notify response.errors
          $scope.password = null
          $scope.confirmation = null

]

mod.RegisterController = ['$scope', '$http', '$timeout', "$location", ($scope, $http, $timeout, $location) ->
  $scope.user = {}
  $scope.validationErrors = []
  removePasswordKeys = (obj) ->
    delete obj['password']
    delete obj['password_confirmation']

  $scope.register =  (user) ->
    $http
      .post("/register", user)
      .success (response, status, headers, config) ->
        u = angular.copy(response.data)
        removePasswordKeys(u)
        $scope.user = u
        $scope.errors = response.errors
        $location.url("/login") if response.errors.isEmpty

      .error (response, status, headers, config) ->
        removePasswordKeys($scope.user)
        $scope.notifications.error = response.errors


  $scope.reset = () ->
    $scope.user = {}
    $scope.notifications.error = []
    $location.url("/login")

  $scope.isValidForm = () ->
    $scope.registerUserForm.password.$setValidity(
      "sameAs",
      $scope.password == $scope.confirmation) if $scope.registerUserForm.password?
    $scope.registerUserForm.$invalid

]

mod.ForgotController = ['$scope', '$http', '$location', ($scope, $http, $location) ->
  $scope.validationErrors = []
  $scope.emailSent = null
  $scope.forgot = (login) ->
    $http
      .post("/forgot", { login: login })
      .success (response, status, headers, config) ->
        $scope.emailSent = true
      .error (response, status, headers, config) ->
        $scope.emailSent = false
        $scope.notifications.error = response.error


]

mod.PermissionList = ['$scope', '$http', (s, $http) ->
  s.validationErrors = []
  s.permissions = []
  $http
    .get('/permissions')
    .success((response, status, headers, config) ->
      s.permissions = response.data)
    .error ((data, status, headers, config) -> 
      console.log(data))
]

angular.module('app.controllers', []).controller(mod)
