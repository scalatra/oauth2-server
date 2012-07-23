define ['jquery', 'underscore', 'backbone', 'jade!templates/permissions/list'], ($, _, Backbone, permissionsList) ->
  PermissionListView = Backbone.View.extend
    template: permissionsList
