/**
 * Run babel!
 *
 * @param {String} source Source or content to test.
 * @param {Object} options JsHint options.
 * @param {String} filename File's name.
 */
(function (source, options, filename) {

  options.filename = filename;

  var evidence = function (source, line) {
    if (line < 0) {
      return '';
    }
    var lines = source.split('\n');
    var pline = Math.max(line - 1, 0);
    var nline = Math.min(pline + 3, lines.length);
    return lines.slice(pline, nline).join('\n').trim();
  };

  /**
   * Source: https://github.com/babel/babel-standalone
   */
  assets.load('lib/babel-6.24.0.min.js');

  try {
    var result = Babel.transform(source, options);
    return result.code;
  } catch (ex) {
    var loc = ex.loc || {}
        line = loc.line || -1,
        column = loc.column || ex.pos,
        lines = ex.toString().split("\n"),
        message = lines[0].replace('(' + line + ':' + column + ')', '').trim();
    var err = {
      message: message,
      line: line,
      filename: filename,
      column: column || -1,
      evidence: evidence(source, line)
    };
    return err;
  }
});
