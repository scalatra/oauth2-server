({
    appDir: "../src/main/backbone",
    baseUrl: "js",
    dir: "../src/main/webapp",
    mainConfigFile: '../src/main/backbone/js/main.js',
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
      "jquery": "empty:"
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