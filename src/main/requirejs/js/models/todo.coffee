define [ "underscore", "backbone" ], (_, Backbone) ->
  TodoModel = Backbone.Model.extend(
    defaults:
      title: "empty todo..."
      completed: false

    initialize: ->
      @set title: @defaults.title  unless @get("title")

    toggle: ->
      @save completed: not @get("completed")

    clear: ->
      @destroy()
  )
  TodoModel
