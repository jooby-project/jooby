[![Maven](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/org/jooby/jooby-assets-rollup/maven-metadata.xml.svg)](http://mvnrepository.com/artifact/org.jooby/jooby-assets-rollup/1.6.3)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-assets-rollup.svg)](https://javadoc.io/doc/org.jooby/jooby-assets-rollup/1.6.3)
[![jooby-assets-rollup website](https://img.shields.io/badge/jooby-assets-rollup-brightgreen.svg)](http://jooby.org/doc/assets-rollup)
# rollup.js

<a href="http://rollupjs.org/">rollup.js</a> the next-generation ES6 module bundler.

Make sure you've already set up the [assets module](https://github.com/jooby-project/jooby/tree/master/jooby-assets) in your project!

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-rollup.js</artifactId>
 <version>1.6.3</version>
 <scope>provided</scope>
</dependency>
```

## usage

```java
assets {
  fileset {
    home: ...
  }

  pipeline {
    ...
    dev: [rollup]
    dist: [rollup]
  }

}
```

## options

### generate

```
rollup {
    genereate {
      format: es
    }
  }

```

See <a href="https://github.com/rollup/rollup/wiki/JavaScript-API#bundlegenerate-options-">generate options</a>.

### plugins

#### babel

```
rollup {
    plugins {
      babel {
        presets: [[es2015, {modules: false}]]
      }
    }
  }

```

See <a href="https://babeljs.io/">https://babeljs.io</a> for more options.

#### legacy

This plugins add a ```export default``` line to legacy modules:

```
rollup {
    plugins {
      legacy {
        "/js/lib/react.js": React
      }
    }
  }

```

#### alias

Set an alias to a common (probably long) path.

```
rollup {
    plugins {
      alias {
        "/js/lib/react.js": "react"
      }
    }
  }

```

Instead of:

```js
import React from 'js/lib/react.js';
```

Now, you can import a module like:

```js
import React from 'react';
```

# see also

## css processors

* [autoprefixer](https://github.com/jooby-project/jooby/tree/master/jooby-assets-autoprefixer): parse CSS and add vendor prefixes to CSS rules via [autoprefixer](https://github.com/postcss/autoprefixer).

* [props](https://github.com/jooby-project/jooby/tree/master/jooby-assets-props): replace application properties in CSS files.

* [clean-css](https://github.com/jooby-project/jooby/tree/master/jooby-assets-clean-css): minify css.

* [csslint](https://github.com/jooby-project/jooby/tree/master/jooby-assets-csslint): check and validate css rules.

* [less4j](https://github.com/jooby-project/jooby/tree/master/jooby-assets-less4j): Less support from [less4j](https://github.com/SomMeri/less4j).

* [less](https://github.com/jooby-project/jooby/tree/master/jooby-assets-less): Less support from [less.js](http://lesscss.org).

* [sass](https://github.com/jooby-project/jooby/tree/master/jooby-assets-sass): Sass support from <a href="https://github.com/bit3/jsass">Java Sass Compiler (libsass)</a>.

* [svg-sprites](https://github.com/jooby-project/jooby/tree/master/jooby-assets-svg-sprites): Generates SVG and CSS sprites with PNG fallbacks via [dr-svg-sprites](https://github.com/drdk/dr-svg-sprites).

* [svg-symbol](https://github.com/jooby-project/jooby/tree/master/jooby-assets-svg-symbol): Generates SVG and CSS sprites using svg `symbols`.

* [yui-css](https://github.com/jooby-project/jooby/tree/master/jooby-assets-yui-compressor): YUI CSS optimizer.

## js processors

* [props](https://github.com/jooby-project/jooby/tree/master/jooby-assets-props): replace application properties in JavaScript files.

* [jscs](https://github.com/jooby-project/jooby/tree/master/jooby-assets-jscs): JavaScript code style checker.

* [jshint](https://github.com/jooby-project/jooby/tree/master/jooby-assets-jshint): JavaScript linter, helps to detect errors and potential problems in code..

* [babel](https://github.com/jooby-project/jooby/tree/master/jooby-assets-babel): Ecma6 now via <a href="http://babeljs.io/">Babel</a>.

* [rollup](https://github.com/jooby-project/jooby/tree/master/jooby-assets-rollup): <a href="http://rollupjs.org/">rollup.js</a> the next-generation ES6 module bundler.

* [ng-annotate](https://github.com/jooby-project/jooby/tree/master/jooby-assets-ng-annotate): Add, remove and rebuild AngularJS dependency injection annotations.

* [closure-compiler](https://github.com/jooby-project/jooby/tree/master/jooby-assets-closure-compiler): Google JavaScript optimizer and minifier.

* [uglify](https://github.com/jooby-project/jooby/tree/master/jooby-assets-uglify): uglify.js optimizer.

* [replace](https://github.com/jooby-project/jooby/tree/master/jooby-assets-replace): replace strings in files while bundling them.

* [requirejs](https://github.com/jooby-project/jooby/tree/master/jooby-assets-requirejs): r.js optimizer.

* [yui-js](https://github.com/jooby-project/jooby/tree/master/jooby-assets-yui-compressor#yui-js): YUI JS optimizer.
