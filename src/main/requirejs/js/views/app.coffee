define [ "jquery", "underscore", "backbone", "cs!collections/todos", "cs!views/todos", "jade!templates/stats", "cs!common" ], ($, _, Backbone, Todos, TodoView, statsTemplate, Common) ->
  AppView = Backbone.View.extend(
    el: $("#todoapp")
    template: statsTemplate
    events:
      "keypress #new-todo": "createOnEnter"
      "click #clear-completed": "clearCompleted"
      "click #toggle-all": "toggleAllComplete"

    initialize: ->
      @input = @$("#new-todo")
      @allCheckbox = @$("#toggle-all")[0]
      Todos.on "add", @addOne, this
      Todos.on "reset", @addAll, this
      Todos.on "all", @render, this
      @$footer = $("#footer")
      @$main = $("#main")
      Todos.fetch()

    render: ->
      completed = Todos.completed().length
      remaining = Todos.remaining().length
      if Todos.length
        @$main.show()
        @$footer.show()
        @$footer.html @template(
          completed: completed
          remaining: remaining
        )
        @$("#filters li a").removeClass("selected").filter("[href='#/" + (Common.TodoFilter or "") + "']").addClass "selected"
      else
        @$main.hide()
        @$footer.hide()
      @allCheckbox.checked = not remaining

    addOne: (todo) ->
      view = new TodoView(model: todo)
      $("#todo-list").append view.render().el

    addAll: ->
      @$("#todo-list").html ""
      switch Common.TodoFilter
        when "active"
          _.each Todos.remaining(), @addOne
        when "completed"
          _.each Todos.completed(), @addOne
        else
          Todos.each @addOne, this

    newAttributes: ->
      title: @input.val().trim()
      order: Todos.nextOrder()
      completed: false

    createOnEnter: (e) ->
      return  if e.keyCode isnt Common.ENTER_KEY
      return  unless @input.val().trim()
      Todos.create @newAttributes()
      @input.val ""

    clearCompleted: ->
      _.each Todos.completed(), (todo) ->
        todo.clear()

      false

    toggleAllComplete: ->
      completed = @allCheckbox.checked
      Todos.each (todo) ->
        todo.save completed: completed
  )
  AppView
