define [ "jquery", "backbone", "cs!collections/todos", "cs!common" ], ($, Backbone, Todos, Common) ->
  Workspace = Backbone.Router.extend(
    routes:
      "*filter": "setFilter"

    setFilter: (param) ->
      Common.TodoFilter = param.trim() or ""
      Todos.trigger "reset"
  )
  Workspace
