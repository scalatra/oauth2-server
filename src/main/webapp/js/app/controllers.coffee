
mod = {}

mod.AppController = ['$scope', '$http', ($scope, $http) ->
  $scope.errors = []
  $scope.user = {}

  $scope.errorClass = (field) ->
    console.log("checking for error: " + field)
    showError = _.any $scope.errors, (item) ->
      item.fieldName == field

    if showError then "error" else ""

  $http
    .get("/check_auth")
    .success((response, status, headers, config) ->
      $scope.user = response.data)
    .error((response, status, headers, config) -> 
      $scope.user = {}
      console.log("status: "+status)
    )

  
]

mod.HomeController = ['$scope', '$http', ($scope, $http) ->

]

mod.LoginController = ['$scope', '$http', ($scope, $http) ->

]


mod.ResetController = ['$scope', '$http', ($scope, $http) ->

]

mod.RegisterController = ['$scope', '$http', ($scope, $http) ->

  $scope.register =  (user) ->
    console.log("registering user")
    console.log(user)
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
