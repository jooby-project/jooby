/**
 * Source: browserify ng-annotate-main.js  --s ngAnnotate -o ng-annotate-1.0.2.js
 */
(function (source, options, filename) {

  assets.load('lib/ng-annotate-1.0.2.js')

  var rsp = ngAnnotate(source, options);
  var errors = [];

  if (rsp.errors) {
    rsp.errors.forEach(function (msg) {
      errors.push({
        filename: filename,
        message: msg
      })
    });
  }

  return {
    output: rsp.src,
    errors: errors
  }
})
