/**
 * Source: uglifyjs --self -b -o uglifyjs-2.4.24.js
 */
(function (source, options, filename) {
  options.filename = filename;

  assets.load('lib/uglifyjs-2.4.24.js');

  try {
    UglifyJS.base54.reset();

    // 1. parse
    var ast = UglifyJS.parse(source, options);
  
    // 2. compress
    if (options.compress) {
      var compress = { warnings: options.warnings };
      UglifyJS.merge(compress, options.compress);
      ast.figure_out_scope();
      ast = ast.transform(UglifyJS.Compressor(compress));
    }

    // 3. mangle
    if (options.mangle) {
      ast.figure_out_scope(options.mangle);
      ast.compute_char_frequency(options.mangle);
      ast.mangle_names(options.mangle);
    }

    // 4. output
    var output = {};
    if (options.output) {
      UglifyJS.merge(output, options.output);
    }

    var stream = UglifyJS.OutputStream(output);
    ast.print(stream);

    return stream.toString();
  } catch (ex) {
    return [{
      line: ex.line,
      column: ex.col,
      message: ex.message,
      filename: ex.filename
    }];
  }
});
