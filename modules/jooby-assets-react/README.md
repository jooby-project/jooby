# react

Write <a href="https://facebook.github.io/react">React</a> applications easily in the JVM.

Make sure you've already set up the [assets module](https://github.com/jooby-project/jooby/tree/master/jooby-assets) in your project!

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-react</artifactId>
 <version>1.1.2</version>
 <scope>provided</scope>
</dependency>
```

## usage

Download <a href="https://unpkg.com/react@15/dist/react.js">react.js</a> and <a href="https://unpkg.com/react-dom@15/dist/react-dom.js">react-dom.js</a> into ```public/js/lib``` folder.

Then add the react processor to ```conf/assets.conf```:

```
assets {
  fileset {

    index: index.js
  }

  pipeline {

    dev: [react]
    dist: [react]
  }

}
```

Write some react code ```public/js/index.js```:

```java
  import React from 'react';
  import ReactDOM from 'react-dom';

  const Hello = () => (
    <p>Hello React</p>
  )

  ReactDOM.render(<Hello />, document.getElementById('root'));
```

Choose one of the available <a href="http://jooby.org/doc/parser-and-renderer/#template-engines">template engines</a> add the ```index.js``` to the page:

```java
<!doctype html>
<html lang="en">
<body>
  <div id="root"></div>
  {{ index_scripts | raw}}
</body>
</html>
```

The ```{{ index_scripts | raw}}``` here is <a href="jooby.org/doc/pebble">pebble expression</a>. Open an browser and try it.

## how it works?

This module give you a ready to use react environment with: ```ES6``` and ```JSX``` support via <a href="http://babeljs.io">babel.js</a> and <a href="https://github.com/rollup/rollup">rollup.js</a>.

You don't need to install ```node.js```, ```npm```, ... nothing, <a href="http://babeljs.io">babel.js</a> and <a href="https://github.com/rollup/rollup">rollup.js</a> run on top of <a href="https://github.com/eclipsesource/J2V8">j2v8</a> as part of the JVM process.

## options

### react-router

Just drop the <a href="https://unpkg.com/react-router-dom/umd/react-router-dom.js">react-router-dom.js</a> into the ```public/js/lib``` folder and use it.

### rollup

It supports all the option of <a href="http://jooby.org/doc/assets-rollup/">rollup.js</a> processor.

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

* [react](https://github.com/jooby-project/jooby/tree/master/jooby-assets-react): <a href="https://facebook.github.io/react">React</a> support.

* [rollup](https://github.com/jooby-project/jooby/tree/master/jooby-assets-rollup): <a href="http://rollupjs.org/">rollup.js</a> the next-generation ES6 module bundler.

* [ng-annotate](https://github.com/jooby-project/jooby/tree/master/jooby-assets-ng-annotate): Add, remove and rebuild AngularJS dependency injection annotations.

* [closure-compiler](https://github.com/jooby-project/jooby/tree/master/jooby-assets-closure-compiler): Google JavaScript optimizer and minifier.

* [uglify](https://github.com/jooby-project/jooby/tree/master/jooby-assets-uglify): uglify.js optimizer.

* [requirejs](https://github.com/jooby-project/jooby/tree/master/jooby-assets-requirejs): r.js optimizer.

* [yui-js](https://github.com/jooby-project/jooby/tree/master/jooby-assets-yui-compressor#yui-js): YUI JS optimizer.
