[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-assets-autoprefixer/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-assets-autoprefixer)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-assets-autoprefixer.svg)](https://javadoc.io/doc/org.jooby/jooby-assets-autoprefixer/1.2.1)
[![jooby-assets-autoprefixer website](https://img.shields.io/badge/jooby-assets-autoprefixer-brightgreen.svg)](http://jooby.org/doc/assets-autoprefixer)
# auto-prefixer

<a href="https://github.com/postcss/postcss">PostCSS</a> plugin to parse CSS and add vendor prefixes to CSS rules using values from <a href="http://caniuse.com">Can I Use</a>. It is recommended by Google and used in Twitter, and Taobao.

Make sure you've already set up the [assets module](https://github.com/jooby-project/jooby/tree/master/jooby-assets) in your project!

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-assets-auto-prefixer</artifactId>
 <version>1.2.1</version>
 <scope>provided</scope>
</dependency>
```

## usage

```
assets {
  pipeline {
    dev: [auto-prefixer]
    dist: [auto-prefixer]
  }
}
```

Once configured, write your CSS rules without vendor prefixes (in fact, forget about them entirely):

```css
:fullscreen a {
  display: flex

}
```

Output:

```css
:-webkit-full-screen a {
   display: -webkit-box;
   display: flex
}
:-moz-full-screen a {
   display: flex
}
:-ms-fullscreen a {
   display: -ms-flexbox;
   display: flex
}
:fullscreen a {
   display: -webkit-box;
   display: -ms-flexbox;
   display: flex
}
```

## options

```
{
  auto-prefixer {

    browsers: ["> 1%", "IE 7"]
    cascade: true
    add: true
    remove: true
    supports: true
    flexbox: true
    grid: true
    stats: {}
  }

}
```

For complete documentation about available options, please refer to the <a href="https://github.com/postcss/autoprefixer">autoprefixer</a> site.

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
