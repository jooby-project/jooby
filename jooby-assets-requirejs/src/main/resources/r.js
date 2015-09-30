/** Fake browser objects. */
var navigator = {},
  document = {},
  location = {
    href: ''
  };

/** Naive/Dummy implementation of XMLHttpRequest (no async supported). */
var XMLHttpRequest = function () {
  var fetch;
  return {
    open: function (method, path) {
      if (method.toUpperCase() === 'GET') {
        fetch = function () {
          return assets.readFile(path);
        };
      } else {
        // Assume HEAD
        fetch = function () {
          return assets.exists(path);
        };
      }
    },

    send: function () {
      this.readyState = 4;
      var contents = fetch();
      if (contents) {
        this.status = 200;
        this.responseText = contents;
      } else {
        this.status = 404;
      }
      if (this.onreadystatechange) {
        this.onreadystatechange();
      }
    }
  };
};

(function (source, options, filename) {
  options.name = options.name || filename.replace('.js', '');

  /** Source: https://github.com/jrburke/r.js/blob/master/dist/r.js */
  assets.load('lib/r-2.1.20.js');

  var errors = [];

  /** logger logs! */
  define('logger', function () {
    return {
      logLevel: function (level) {
        // NOOP
      },

      trace: console.debug,

      info: console.info,

      warn: console.warn,

      error: console.error
    };
  });

  /** hack! file system. */
  define('env!env/file', ['prim'], function (prim) {
    var first = true;
    return {
      exists: function (path) {
        return assets.exists(path);
      },

      absPath: function (path) {
        return path;
      },

      normalize: function (path) {
        return path;
      },

      readFile: function (path) {
        if (first) {
          first = false;
          return source;
        }
        return assets.readFile(path);
      },

      readFileAsync: function (path) {
        var d = prim();
        var contents;
        console.log(path);
        if (first) {
          first = false;
          contents = source;
        } else {
          contents = assets.readFile(path);
        }
        if (contents) {
          d.resolve(contents);
        } else {
          d.reject(new Error('File not found: ' + path));
        }
        return d.promise;
      }
    };
  });

  var output = '';

  options.optimize = options.optimize || 'none';
  options.out = function (out) {
    output = out;
  };

  var callback = function () {};

  console.log('r.js v' + requirejs.version);

  requirejs.optimize(options, callback, function (msg) {
    var cleanmsg = msg.toString().replace('Error:', '').replace('Error:', '').trim();
    console.error(cleanmsg);
    errors.push({
      filename: filename,
      message: cleanmsg
    });
  });

  return {
    errors: errors,
    output: output
  };
});
