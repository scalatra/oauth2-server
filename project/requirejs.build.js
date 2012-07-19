({
    appDir: "../src/main/requirejs",
    baseUrl: "js",
    dir: "../src/main/webapp",
    uglify: {
      ascii_only: true,
      max_line_length: 1000
    },
    pragmasOnSave: {
        //Just an example
        excludeCoffeeScript: true,
        excludeJade: true
    },
    paths: {
      "jquery": "empty:",
      "underscore": 'lib/underscore/underscore',
      "backbone": 'lib/backbone/backbone',
      "text": 'lib/require/text',
      "cs": "lib/require/cs",
      "jade": "lib/jade/jade",
      "coffee-script": "lib/coffee-script/coffee-script"

    },
    keepBuildDir: true,
    stubModules: ['cs', 'jade'],
    modules: [
        {
            name: "main",
            exclude: ['coffee-script', 'jade']
        }
    ]
})