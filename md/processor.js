var File = java.io.File,
    FileOutputStream = java.io.FileOutputStream,
    PrintWriter = java.io.PrintWriter,
    Scanner = java.util.Scanner,
    StringBuilder = java.lang.StringBuilder,
    Pattern = java.util.regex.Pattern,
    URI = java.net.URI,
    Files = java.nio.file.Files,
    paths = java.nio.file.Paths,
    console = {
      log: function (message) {
        java.lang.System.out.println(message);
      }
    };


var mdindir = new File(basedir, "md"),
    target = new File(basedir, "target"),
    mdoutdir = new File(target, "md"),
    ghpagesdir = new File(target, "gh-pages");

/**
 * Ask Maven central for latest jooby version.
 */
var version = function () {
  // fetch it!
  var scanner = new Scanner(
      URI.create('http://search.maven.org/solrsearch/select?q=id:%22org.jooby:jooby%22&rows=1')
          .toURL()
          .openStream()
  );
  var json = '';
  while (scanner.hasNext()) {
    json += scanner.next();
  }
  scanner.close();
  var rsp = JSON.parse(json);
  return rsp.response.docs[0].latestVersion;
};

/**
 * copy a file to another.
 */
var copy = function (fin, fout) {
  var stream = new FileOutputStream(fout);
  Files.copy(paths.get(fin.absolutePath), stream);
  stream.close();
};

/**
 * read content from a file.
 */
var readString = function (fin) {
  return new java.lang.String(Files.readAllBytes(paths.get(fin.absolutePath)));
};

/**
 * write content to a file.
 */
var writeString = function (fout, data) {
   Files.write(paths.get(fout.absolutePath), new java.lang.String(data).getBytes());
};

var toc = function (data) {
  var scanner = new Scanner(data).useDelimiter('\n'),
      toc = new StringBuilder();

  var count = function (line) {
    var start = line.indexOf('#');
    if (start === 0) {
      return line.substring(start, line.lastIndexOf('#') + 1).trim().length();
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

  while (scanner.hasNext()) {
    var line = scanner.next(),
      c = count(line);

    if (c > 0 && c < 3) {
      // clean up md links
      var item = line.replaceAll('#|\\[|\\]|\\(.+\\)', '').trim();
      toc.append(indent(c)).append('- [').append(item).append('](#')
          .append(item.replaceAll('\\s+', '-')).append(')\n');
    }
  }

  return toc.toString();
};

/**
 * List all files from a given directory and sub-directories.
 */
var ls = function (dir, filter) {
  var files = [];
    filter = filter || function (file) {return file.name.endsWith('.md');}

  java.util.Arrays.asList(dir.listFiles() || []).forEach(function (it) {
    if (it.isDirectory()) {
      files = files.concat(ls(it, filter));
    } else if (filter(it)) {
      files.push(it);
    }
  });

  return files;
};

var freplace = function (source, token, value) {
  while(source.indexOf(token) !== -1) {
    source = source.replace(token, value);
  }
  return source;
}

var readConf = function (file) {
  var name = file.parentFile.name,
      dir = paths.get(basedir, name, "src", "main", "resources");
  var data = '';
  ls(dir.toFile(), function (file) {
    return file.name.endsWith('.conf');
  }).forEach(function (file) {
    data += '## appendix: ' + file.name + '\n```properties\n' + readString(file) + '\n```\n';
  });
  return data;
}

/**
 * clean working directory.
 */
ls(mdoutdir).forEach(function (file) {
  console.log('deleting file: ' + file);
  file['delete']();
});

/**
 * copy files to working dir and extract file variables, to used them later.
 */
var vars = ls(mdindir).reduce(function (vars, file) {
  var fout = new File(mdoutdir, file.absolutePath.substring(mdindir.absolutePath.length()));
  fout.parentFile.mkdirs();
  console.log('writing file: ' + fout);

  copy(file, fout);

  vars.push(file);

  return vars;
}, []);

/**
 * clone README.md
 */
ls(mdoutdir, function (file) {
  return file.name.equals('README.md');
}).forEach(function (file) {
  var index = new File(file.parentFile, 'index.md');
  if (!index.exists()) {
    console.log('clonning file: ' + file);
    var page = '---\nlayout: index\ntitle: ' + file.parentFile.name + '\n'
        + 'version: {{version}}\n---\n\n'
        + readString(file);

    writeString(index, page);
  }
});

/**
 * Append latest version var.
 */
vars.push({
  name: 'version',
  data: version()
});

vars.push({
  name: 'site',
  data: '/'
});

vars.push({
  name: 'apidocs',
  data: '/apidocs'
});

vars.push({
  name: 'defdocs',
  data: '/apidocs/org/jooby'
});

vars.push({
  name: 'appendix'
});

vars.push({
  name: 'maven',
  data: '[Maven](http://maven.apache.org/)'
});

vars.push({
  name: 'guice',
  data: '[Guice](https://github.com/google/guice)'
});

vars.push({
  name: 'jooby',
  data: '[Jooby](http://jooby.org/)'
});

vars.push({
  name: 'netty',
  data: '[Netty](http://netty.io/)'
});

vars.push({
  name: 'npm',
  data: '[npm](https://www.npmjs.com/)'
});

vars.push({
  name: 'grunt',
  data: '[npm](http://gruntjs.com/)'
});

/**
 * replace expressions like: {{var}}
 */
ls(mdoutdir, function (file) {
  return file.name.equals('index.md') || file.name.equals('README.md');
}).forEach(function (file) {
  var data = readString(file);

  console.log('processing: ' + file);
  vars.forEach(function (ovar) {
    var name = '{{' + ovar.name + '}}';
    var value = ovar.data;

    if (!value) {
      if (ovar.name.startsWith('appendix')) {
        // load property file
        value = readConf(file);
      } else {
        value = readString(ovar);
      }
    }

    
    console.log('   applying placeholder ' + name);
    data = freplace(data, name, value);
  });

  // toc
  console.log('   applying {{toc.md}}');
  data = freplace(data, '{{toc.md}}', toc(data));

  // dump file
  var fout = new File(ghpagesdir, file.absolutePath.substring(mdoutdir.absolutePath.length()));
  fout.parentFile.mkdirs();
  writeString(fout, data);
  console.log('  done: ' + fout);
});

/**
 * move README.md to /, this will update github doc
 */
ls(ghpagesdir, function (file) {
  return file.name.equals('README.md');
}).forEach(function (file) {
  console.log('moving: ' + file);

  // dump file
  var path = file.toPath();
  path = path.subpath(paths.get(ghpagesdir).nameCount, path.nameCount);
  if (path.startsWith('doc')) {
    path = path.subpath(1, path.nameCount);
  }

  var fout = path.toFile();
  copy(file, fout);
  file['delete']();
  console.log('  done: ' + fout);
});

