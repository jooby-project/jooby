(function (source, options, filename) {

  assets.load('lib/jscs-2.1.1.js');

  var evidence = function (source, line) {
    if (line < 0) {
      return '';
    }
    var lines = source.split('\n');
    var pline = Math.max(line - 1, 0);
    var nline = Math.min(pline + 3, lines.length);
    return lines.slice(pline, nline).join('\n') + '';
  };

  var checker = new Checker();
  var errors = [];

  checker.registerDefaultRules();
  checker.configure(options);
  var results = checker.checkString(source);

  results.getErrorList().forEach(function(error) {
    errors.push({
      filename: filename,
      line: error.line,
      column: error.column,
      message: error.message + '.',
      evidence: evidence(source, error.line - 1)
    });
    console.error(filename + ': ' + results.explainError(error));
  });

  return {
    errors: errors,
    output: source
  };
});
