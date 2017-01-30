# ng-annotate

<a href="https://github.com/olov/ng-annotate">ng-annotate</a> add, remove and rebuild AngularJS dependency injection annotations.

Make sure you already setup the [assets module](https://github.com/jooby-project/jooby/tree/master/jooby-assets) in your project!

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-assets-ng-annotate</artifactId>
  <version>1.0.2</version>
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
   dev: [ng-annotate]
   dist: [ng-annotate]
 }
}
```

## options

```
assets {
 ...
 ng-annotate {
   add: true
   remove: false
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

* [requirejs](https://github.com/jooby-project/jooby/tree/master/jooby-assets-requirejs): r.js optimizer.

* [yui-js](https://github.com/jooby-project/jooby/tree/master/jooby-assets-yui-compressor#yui-js): YUI JS optimizer.
