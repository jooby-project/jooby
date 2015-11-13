// Hack for require('http');
var global = {
  XMLHttpRequest: function () {
    return {
      open: function (p, k) {
      }
    };
  },

  location: {
    host: '/'
  }
};
(function (source, options) {
  /** Source: See org.jooby.assets.Generator.java */
  assets.load('lib/clean-css-3.4.3.js');

  var cleancss = new global.CleanCSS(options);
  var output = cleancss.minify(source);

  var errors = [];
  output.errors.concat(output.warnings).forEach(function (error) {
    errors.push({
      message: error
    });
  });

  return {
    output: output.styles,
    errors: errors
  };
});
