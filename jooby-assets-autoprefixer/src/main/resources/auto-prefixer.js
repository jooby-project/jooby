var global = {};

(function (source, options) {
  /** Source: https://raw.github.com/ai/autoprefixer-rails/master/vendor/autoprefixer.js */
  assets.load('lib/autoprefixer.js');

  var output;

  try {
    output = global.autoprefixer.process(source, options);
  } catch (x) {
    output.error = x.toString();
    console.log(x);
  }

  output.warnings().forEach(function (warn) {
    console.warn(warn.toString());
  });

  var errors = [];
  if (output.error) {
    errors.push(error);
  }

  return {
    output: output.css,
    errors: errors
  };
});