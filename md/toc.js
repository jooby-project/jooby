// Rhino or Nashorn ;)

var File = java.io.File,
    FileOutputStream = java.io.FileOutputStream,
    PrintWriter = java.io.PrintWriter,
    Scanner = java.util.Scanner,
    StringBuilder = java.lang.StringBuilder,
    syso = java.lang.System.out;

var count = function (line) {
  var start = line.indexOf("#");
  if (start === 0) {
    return line.substring(start, line.lastIndexOf("#") + 1).trim().length();
  }
  return 0;
};

var indent = function (count) {
  var str = '';
  for(var i = 0; i < count - 1; i++) {
    str += '  ';
  }
  return str;
};

var toc = function (file) {
  var toc = new StringBuilder(),
      content = new StringBuilder(),
      scanner = new Scanner(file).useDelimiter("\n");

  while (scanner.hasNext()) {
    var line = scanner.next(),
        c = count(line);

    if (c > 0) {
      // clean up md links
      var item = line.replaceAll("#|\\[|\\]|\\(.+\\)", "").trim();
      toc.append(indent(c)).append("- [").append(item).append("](#").append(item.replaceAll("\\s+", "-")).append(")\n");
    }
    content.append(line).append("\n");
  }
  return content.toString().replace("{{toc.md}}", toc.toString());
};

var generate = function(file) {
  syso.println("building toc: " + file);
  var data = toc(file);
  var writer = new PrintWriter(new FileOutputStream(file));

  writer.write(data);
  writer.flush();
  writer.close();
  syso.println("done toc: " + file);
};

generate(new File(new File(new File("target"), "gh-pages"), inputmd));
