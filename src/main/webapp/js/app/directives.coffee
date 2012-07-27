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

# register the module with Angular
angular.module('app.directives', [
  # require the 'app.service' module
  'app.services'
]).directive(mod)