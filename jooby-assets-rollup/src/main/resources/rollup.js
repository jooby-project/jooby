/**
 * Executes rollup using the given source.
 *
 * @param {String} source Source to process.
 * @param {Object} options Rollup options.
 */
(function (source, options, filename) {

  // remove output/gen options from root options
  var genopts;
  if (options.generate) {
    genopts = options.generate;
    delete options.generate;
  } else {
    genopts = {};
  }

  if (!genopts.format) {
    genopts.format = 'es';
  }

  var plugins = options.plugins || {};
  delete options.plugins;

  var aliasOptions = plugins.alias;
  var babelOptions = plugins.babel;
  var legacyOptions = plugins.legacy;

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
  /** Required by rollup */
  window.performance = {now: function () {return Date.now;}};

  /**
   * Source: https://github.com/requirejs/prim/blob/master/prim.js
   * Required by rollup.
   */
  assets.load('lib/prim.js');
  window.Promise = prim;

  /** Source: npm install rollup; npm_modules/rollup/rollup.browser.js */
  assets.load('lib/rollup-0.41.6.js');

  var plugins = [];

  if (aliasOptions) {
    var aliasPlugin = {
      name: 'alias',

      resolveId: function (importee, importer) {
        var alias = aliasOptions[importee];
        if (alias) {
          console.debug('alias ', alias, ':', importee, ' from', importer);
          return alias;
        }
      }
    };
    plugins.push(aliasPlugin);
  }

  var loaderPlugin = {
    name: 'classpath-loader',

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
      var relative = basedir + '/' + importee;
      return resolveFirst(relative, relative + '.js', importee, importee + '.js') || importee;
    },

    /**
     * load a module by id (a.k.a path). 
     */
    load: function (id) {
      console.debug('loading: ', id);
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
  };

  plugins.push(loaderPlugin);

  if (legacyOptions) {
    var legacyPlugin = {
      name: 'legacy',

      transform: function (code, id) {
        var name = legacyOptions[id];
        if (name) {
          console.debug('legacy: ', id, ' -> ', name);
          return code + '\nexport default ' + name + ';';
        }
      }
    };
    plugins.push(legacyPlugin);
  }

  if (babelOptions) {
    // Babel: https://github.com/babel/babel-standalone
    assets.load('lib/babel-6.24.0.min.js');
    var filter = j2v8.createFilter(babelOptions.includes, babelOptions.excludes);
    delete babelOptions.includes;
    delete babelOptions.excludes;
    var babelPlugin = {
      name: 'babel',

      transform: function (code, id) {
        if (!filter(id)) {
          console.debug('babel ignoring ', id)
          return null;
        }
        var transformed = Babel.transform(code, babelOptions);
        return {
          code: transformed.code,
          map: transformed.map
        };
      }
    };
    plugins.push(babelPlugin);
  }

  var output,
    errors = [];

  rollup.rollup({
    entry: filename,
    plugins: plugins,
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
