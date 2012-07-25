'use strict'
# Services

# Create an object to hold the module.
mod = {}

mod.version = -> "0.1"

mod.Permission = ['$resource', ($resource) ->
  $resource("/permissions/:id", {id: "@id"})
]



angular.module('app.services', []).factory(mod)