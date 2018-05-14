(function (source, options, filename) {
  /** Source: https://github.com/CSSLint/csslint/tree/master/release/clint.js */
  assets.load('lib/csslint-0.10.0.js');

  /** Add default rules and override with options .*/
  var rules = CSSLint.getRuleset();
  for (var name in options) {
    rules[name] = options[name];
  }

  var msg = function (err) {
    var evidence = err.evidence.trim();

    return filename + ':' + err.type + ':' + err.line + ':' + err.col + ': '
        + err.message + '\n    ' + evidence + '\n    ' + new Array(err.col).join('-') + '^';
  };

  var results = CSSLint.verify(source, rules).messages,
      errors = [];
  results.forEach(function (err) {
    var type = err.type === 'warning' ? 'warn' : 'error',
        warning = type === 'warn';

    console[type](msg(err));

    if (!warning) {
      errors.push({
        line: err.line,
        column: err.col,
        filename: filename,
        message: err.message,
        evidence: err.evidence
      });
   }
  });

  return {
    errors: errors,
    output: source
  };
});
