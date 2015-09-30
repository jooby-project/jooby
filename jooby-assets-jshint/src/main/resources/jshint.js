/**
 * Run jshint and returns a list with problems (if any).
 *
 * @param {String} source Source or content to test.
 * @param {Object} options JsHint options.
 * @param {Object} predef Predefined variables.
 */
(function (source, options, predef, filename) {

  /** Source: https://github.com/jshint/jshint/blob/master/dist/jshint.js */
  assets.load('lib/jshint-2.9.0.js');

  JSHINT(source, options || {}, predef || {});
  var data = JSHINT.data();
  if (data.errors) {
    var problems = [];
    data.errors.forEach(function (error) {
      // sad, but true! error can be undefined
      if (error) {
        problems.push({
          filename: filename,
          line: error.line,
          column: error.character,
          message: error.reason
        });
      }
    });
    return problems;
  } else {
    return source;
  }
});
