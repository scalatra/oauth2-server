
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
    "underscore": 'lib/underscore/underscore',
    "backbone": 'lib/backbone/backbone',
    "cs": "lib/require/cs",
    "jade": "lib/jade/jade",
    "coffee-script": "lib/coffee-script/coffee-script",
    "bootstrap": "lib/bootstrap/bootstrap"
  }

});

require(['cs!common', 'bootstrap', 'backbone'], function( Common, Bootstrap, Backbone ){

  // Initialize routing and start Backbone.history()
//  var TodoRouter = new Workspace;


  Backbone.history.stop();
  Backbone.history.start();

  // Initialize the application view
//  var app_view = new AppView;

  // see http://www.slideshare.net/nzakas/enterprise-javascript-error-handling-presentation slide 46
  window.onerror = function(msg, url, line) {
    if (Common.DEBUG_MODE) {
      return false;
    } else {
      // TODO: use atmosphere or plain websockets to log this stuff on the server
      console.log(msg);
      return true;
    }
  }
});
