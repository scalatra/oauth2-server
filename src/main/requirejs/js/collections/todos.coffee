define [ "underscore", "backbone", "lib/backbone/localstorage", "cs!models/todo" ], (_, Backbone, Store, Todo) ->
  TodosCollection = Backbone.Collection.extend(
    model: Todo
    localStorage: new Store("todos-backbone")
    completed: ->
      @filter (todo) ->
        todo.get "completed"

    remaining: ->
      @without.apply this, @completed()

    nextOrder: ->
      return 1  unless @length
      @last().get("order") + 1

    comparator: (todo) ->
      todo.get "order"
  )
  new TodosCollection
