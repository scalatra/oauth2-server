define [ "jquery", "underscore", "backbone", "jade!templates/todos", "cs!common" ], ($, _, Backbone, todosTemplate, Common) ->
  TodoView = Backbone.View.extend(
    tagName: "li"
    template: todosTemplate
    events:
      "click .toggle": "togglecompleted"
      "dblclick .view": "edit"
      "click .destroy": "clear"
      "keypress .edit": "updateOnEnter"
      "blur .edit": "close"

    initialize: ->
      @model.on "change", @render, this
      @model.on "destroy", @remove, this

    render: ->
      $el = $(@el)
      $el.html @template(@model.toJSON())
      $el.toggleClass "completed", @model.get("completed")
      @input = @$(".edit")
      this

    togglecompleted: ->
      @model.toggle()

    edit: ->
      $(@el).addClass "editing"
      @input.focus()

    close: ->
      value = @input.val().trim()
      @clear()  unless value
      @model.save title: value
      $(@el).removeClass "editing"

    updateOnEnter: (e) ->
      @close()  if e.keyCode is Common.ENTER_KEY

    clear: ->
      @model.clear()
  )
  TodoView
