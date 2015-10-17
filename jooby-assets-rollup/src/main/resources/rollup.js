/**
 * Executes rollup using the given source.
 *
 * @param {String} source Source to process.
 * @param {Object} options Rollup options.
 */
(function (source, options, filename) {

  // remove output/gen options from root options
  var genopts;
  if (options.output) {
    genopts = options.output;
    delete options.output;
  } else {
    genopts = {};
  }

  /**
   * Normalize path by removing double slash (//), replace ./ with / and append .js extension
   * if missing.
   */
  var normalizePath = function (path) {
    var cleanpath = ('/' + path.replace('./', '/')).replace(/\/+/g, '/'),
        suffix = '.js',
        endsWith = cleanpath.indexOf(suffix, cleanpath.length - suffix.length) !== -1;

    if (!endsWith) {
      cleanpath = cleanpath + '.js';
    }
    return cleanpath;
  };

  /**
   * Tests if any of the given path exists and returns the first that exists.
   */
  var resolveFirst = function () {
    for(var i = 0; i < arguments.length; i++) {
      var path = normalizePath(arguments[i]);
      if (assets.exists(path)) {
        return path;
      }
    }
    return null;
  };

  /** Source: https://github.com/requirejs/prim/blob/master/prim.js */
  assets.load('lib/prim.js');
  window.Promise = prim;

  /** Source: npm install rollup; npm_modules/rollup/rollup.browser.js */
  assets.load('lib/rollup-0.19.1.js');

  var output,
    errors = [];

  rollup.rollup({
    entry: filename,

    /**
     * relative modules have precedence over absolute modules.
     */
    resolveId: function (importee, importer) {
      if (!importer) {
        return importee;
      }
      var basedir = '';
      var segments = importer.split('/');
      if (segments.length > 2) {
        basedir = segments.slice(0, -1).join('/');
      }
      return resolveFirst(basedir + '/' + importee, importee) || importee;
    },

    /**
     * load a module by id (a.k.a path). 
     */
    load: function (id) {
      console.debug('loading: ' + id);
      if (id === filename) {
        return source;
      }
      var contents = assets.readFile(id);
      if (contents) {
        return contents;
      } else {
        throw 'File not found: ' + id;
      }
    }
  }).catch(function (ex) {
    errors.push({
      message: ex.toString()
    });
  }).then(function (bundle) {
    if (bundle) {
      var result = bundle.generate(genopts);
  
      output = result.code;

      /** inline sourceMap only. */
      if (genopts.sourceMap === 'inline') {
        output += '\n//#sourceMappingURL=' + result.map.toUrl();
      }
    }
  }).catch(function (ex) {
    errors.push({
      message: ex.toString()
    });
  });

  return {
    output: output,
    errors: errors
  };
});
