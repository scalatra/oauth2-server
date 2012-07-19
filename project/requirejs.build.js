({
  "appDir": "../src/main/requirejs",
  "baseUrl": "js",
  "dir": "../target/requirejs",
  "mainConfigFile": "../src/main/requirejs/js/main.js",
  "uglify": {
    "ascii_only": true,
    "max_line_length": 1000
  },
  "pragmasOnSave": {
    "excludeCoffeeScript": true,
    "excludeJade": true
  },
  "paths": {
    "jquery": "empty:"
  },
  "stubModules": ["cs", "jade"],
  "modules": [
    {
      "name": "main",
      "exclude": ["coffee-script", "jade"]
    }
  ]
})