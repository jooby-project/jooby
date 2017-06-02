/**
 * Source: browserify node_modules/less/lib/less/index.js --s createFromEnvironment -o less-2.5.1.js
 */
(function (source, options, filename) {

  /**
   * load environment
   *
   * Source: browserify node_modules/less-node/environment.js --s lessEnv -o less-env-2.5.1.js
   */
  assets.load('lib/less-env-2.5.1.js');

  assets.load('lib/less-2.5.1.js');

  var less = createFromEnvironment(lessEnv);

  /** logger logs! */
  less.logger.addListener({
    debug: console.debug,
    info: console.info,
    warn: console.warn,
    error: console.err
  });

  /**
   * File manager plugin. Loads from fs or classpath.
   */
  var fsPlugin = function () {
    function FileManager() {
    }

    FileManager.prototype = new less.AbstractFileManager();

    FileManager.prototype.supports = function() {
      return true;
    };

    var loadFiles = function (files) {
      for(var i = 0; i < files.length; i++) {
        var contents = assets.readFile(files[i]);
        if (contents) {
          return {
            filename: files[i],
            contents: contents
          };
        }
      }
    };

    var noLeadingSlash = function (path) {
      if (path.indexOf('/') === 0) {
        return path.substring(1);
      }
      return path;
    };

    FileManager.prototype.loadFile = function(filename, dir, options, env, callback) {
      var filenames = [dir + noLeadingSlash(filename), filename];
      var file = loadFiles(filenames);
      if (file) {
        callback(null, file);
      } else {
        callback(new Error('File not found: [' + filenames.join(', ') + ']'), null);
      }
    };

    return {
      install: function (less, pluginManager) {
        pluginManager.addFileManager(new FileManager());
      }
    };
  };

  // No plugin supports!
  options.plugins = [fsPlugin()];
  options.filename = filename;

  var css;
  var problems = [];

  /**
   * Renderer output
   */
  less.render(source, options, function (error, output) {
    if (error) {
      problems.push({
        filename: error.filename || filename,
        line: error.line || -1,
        column: error.column || -1,
        message: error.message
      });
    } else {
      css = output.css;
    }
  });

  return {
    errors: problems,
    output: css
  };
})
