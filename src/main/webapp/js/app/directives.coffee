'use strict'

# Directive

# Create an object to hold the module.
mod = {}

mod.appVersion = [
  'version'

(version) ->

  (scope, elm, attrs) ->
    elm.text(version)
]
#
#mod.sameAs = () ->
#  (scope, element, attrs) ->
#

mod.fadeOutAfter = ['$log', ($log) ->
  link: (scope, elem, attrs, ctrl) ->
    $(elem)
      .fadeOut parseInt(attrs.fadeOutAfter), () ->
        $(@).remove
]

alertList = (alert, template) ->
  norm = if angular.isString(alert) then { message: alert } else alert
  al = if angular.isArray(norm) then norm else [norm]
  msgs = _.map al, (m) -> template(m)
  notif = _.reduce msgs, (acc, item) -> acc + "<br />" + item
  notif


mod.notificationList = ['$log', '$interpolate', ($log, $interpolate) ->
  terminal: true
  link: (scope, elem, attrs) ->
    itemTempl = (level) ->
      '<div class="alert alert-{{level}}"><a class="close" data-dismiss="alert">×</a>{{message}}</li>'

    addAlert = (level, alert) ->
      merged = angular.extend(alert, {level: level})
      notif = $(alertList(merged, $interpolate(itemTempl(level))))
      notif.al = merged
      $(elem).append notif
      notif.fadeOut 5000, -> $(@).remove

    _.each ["error", "success", "warn", "info"], (level) ->
      scope.$on "notification."+level, (sc, notification) ->
        addAlert(level, notification)

]

mod.authenticated = ['$log', '$location', ($log, $location) ->
  (scope, elem, attrs) ->
    pth = attrs.authenticated || attrs.redirectTo || "/login"
    $location.url(pth) unless angular.isObject(scope.$root.currentUser)
]

mod.validationErrors = [
  '$interpolate'
  '$log'
  'validationFormats'
  ($interpolate, $log, validationFormats) ->
    (scope, elem, attrs) ->
      scopeKey = attrs.validationErrors or "validationErrors"
      scope.$watch attrs.name + ".$dirty && " + attrs.name + ".$error", ((value) ->
        return  unless angular.isObject(value)
        invalid = []
        Object.keys(value).forEach (key) ->
          err = value[key]
          if err
            if angular.isArray(err)
              err.forEach (i) ->
                vals = field_name: i.$name
                formatString = validationFormats[key]
                if angular.isString(formatString) and formatString
                  vals.message = $interpolate(formatString)(vals)
                  invalid.push vals
                else
                  $log.error "No validation message found for key: " + key

        scope[scopeKey] = invalid
      ), true
]

mod.validationSummary = ->
  restrict: "AE"
  scope: true
  controller: ($scope, $attrs) ->
    count = 0
    $scope.errors = []
    $scope.$watch $attrs.validationSummary or $attrs.errors or "validationErrors", (newValue) ->
      $scope.errors = newValue  if newValue
  templateUrl: "/templates/shared/validation_errors.html"




# register the module with Angular
angular.module('app.directives', [
  # require the 'app.service' module
  'app.services'
]).directive(mod)