'use strict'

mod = {}

mod.AppController = ['$scope', '$http', '$location', '$rootScope', ($scope, $http, $location, $rootScope) ->
  $scope.pageTitle = "Scalatra OAuth2"
  $scope.notifications =
    error: []
    info: []
    success: []
    warn: []

  $rootScope.currentUser = null

  $scope.errorClass = (field) ->
    showError = _.any $scope.notifications.error, (item) ->
      item.field_name == field

    if showError then "error" else ""

  $http
    .get("/check_auth")
    .success((response, status, headers, config) ->
      $rootScope.currentUser = response.data)
    .error((response, status, headers, config) -> 
      $rootScope.currentUser = null
    )

  $rootScope.$watch "currentUser", (newValue, oldValue) ->
    if (!oldValue && !newValue) || (oldValue && newValue != oldValue)
      $location.url("/login")
]

mod.HomeController = ['$scope', '$location', ($scope, $location) ->
]

mod.LoginController = ['$scope', '$http', ($scope, $http) ->

]


mod.ResetController = ['$scope', '$http', ($scope, $http) ->

]

mod.RegisterController = ['$scope', '$http', '$timeout', "$location", ($scope, $http, $timeout, $location) ->
  $scope.user = {}
  removePasswordKeys = (obj) ->
    delete obj['password']
    delete obj['password_confirmation']

  $scope.register =  (user) ->
    $http
      .post("/register", user)
      .success((response, status, headers, config) ->
        u = angular.copy(response.data)
        removePasswordKeys(u)
        $scope.user = u
        $scope.errors = response.errors
        $location.url("/login") if response.errors.isEmpty
      )
      .error((response, status, headers, config) ->
        removePasswordKeys($scope.user)
        $scope.notifications.error = response.errors
      )

  $scope.reset = () ->
    $scope.user = {}
    $scope.notifications.error = []
    $location.url("/login")

  $scope.isValidForm = () ->
    $scope.registerUserForm.password.$setValidity(
      "sameAs",
      false) unless $scope.registerUserForm.password == $scope.user.password_confirmation
    $scope.registerUserForm.$invalid

]

mod.ForgotController = ['$scope', '$http', ($scope, $http) ->

]

mod.PermissionList = ['$scope', '$http', (s, $http) ->
  s.permissions = []
  $http
    .get('/permissions')
    .success((response, status, headers, config) ->
      s.permissions = response.data)
    .error ((data, status, headers, config) -> 
      console.log(data))
]

angular.module('app.controllers', []).controller(mod)
