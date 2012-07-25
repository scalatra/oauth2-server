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

# register the module with Angular
angular.module('app.directives', [
  # require the 'app.service' module
  'app.services'
]).directive(mod)