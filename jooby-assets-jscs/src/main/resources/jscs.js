(function (source, options, filename) {

  assets.load('lib/jscs-2.1.1.js');

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
      message: error.message
    });
    console.error(filename + ': ' + results.explainError(error));
  });

  return {
    errors: errors,
    output: source
  };
});
