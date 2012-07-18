
// Require.js allows us to configure shortcut alias
require.config({

  // The shim config allows us to configure dependencies for 
  // scripts that do not call define() to register a module
  shim: {
      'underscore': {
          exports: '_'
      },

      'backbone': {
          deps: ['underscore', 'jquery'],
          exports: 'Backbone'
      },

  },

  paths: {
    underscore: 'lib/underscore/underscore',
    backbone: 'lib/backbone/backbone',
    text: 'lib/require/text',
    "cs": "lib/require/cs",
    "jade": "lib/jade/jade",
    "coffee-script": "lib/coffee-script/coffee-script"
  }

});

require(['cs!views/app', 'cs!routers/router'], function( AppView, Workspace ){

  // Initialize routing and start Backbone.history()
  var TodoRouter = new Workspace;
  Backbone.history.start();

  // Initialize the application view
  var app_view = new AppView;

});
