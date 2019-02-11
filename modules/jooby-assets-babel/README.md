[![Maven](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/org/jooby/jooby-assets-babel/maven-metadata.xml.svg)](http://mvnrepository.com/artifact/org.jooby/jooby-assets-babel/1.6.0)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-assets-babel.svg)](https://javadoc.io/doc/org.jooby/jooby-assets-babel/1.6.0)
[![jooby-assets-babel website](https://img.shields.io/badge/jooby-assets-babel-brightgreen.svg)](http://jooby.org/doc/assets-babel)
# babel

<a href="http://babeljs.io/">Babel</a> is a JavaScript compiler with a set of ES2015 syntax transformers that allow you to use new syntax, right now without waiting for browser support.

Make sure you've already set up the [assets module](https://github.com/jooby-project/jooby/tree/master/jooby-assets) in your project!

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-assets-babel</artifactId>
  <version>1.6.0</version>
  <scope>provided</scope>
</dependency>
```

## usage

```
assets {
 fileset {
   home: ...
 }
 pipeline {
   dev: [babel]
   dist: [babel]
 }
}
```

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-assets-babel</artifactId>
  <version>1.6.0</version>
  <scope>provided</scope>
</dependency>
```

## options

```
assets {
 fileset {
   home: ...
 }
 pipeline {
   dev: [babel]
   dist: [babel]
 }
 babel {
  dev {
    sourceMaps: inline
  }
  presets: [es2015, react]
 }
}
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
