[![Maven](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/org/jooby/jooby-assets-svg-sprites/maven-metadata.xml.svg)](http://mvnrepository.com/artifact/org.jooby/jooby-assets-svg-sprites/1.5.1)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-assets-svg-sprites.svg)](https://javadoc.io/doc/org.jooby/jooby-assets-svg-sprites/1.5.1)
[![jooby-assets-svg-sprites website](https://img.shields.io/badge/jooby-assets-svg-sprites-brightgreen.svg)](http://jooby.org/doc/assets-svg-sprites)
# svg-sprites

An [AssetAggregator](/apidocs/org/jooby/assets/AssetAggregator.html) that creates SVG sprites with PNG fallbacks at needed sizes via <a href="https://github.com/drdk/dr-svg-sprites">dr-svg-sprites</a>.

Make sure you've already set up the [assets module](https://github.com/jooby-project/jooby/tree/master/jooby-assets) in your project!

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-assets-svg-sprites</artifactId>
 <version>1.5.1</version>
 <scope>provided</scope>
</dependency>
```

## usage

```
assets {
  fileset {

    sprite: svg-sprites
    home: home.scss
  }

  svg-sprites {

    spriteElementPath: "images/svg-source",
    spritePath: "css"
  }

}
```

The ```spriteElementPath``` contains all the ```*.svg``` files you want to process. The ```spritePath``` indicates where to save the sprite, here you will find the following generated files: ```css/sprite.css```, ```css/sprite.svg``` and ```css/sprite.png```.

## options

```
assets {
  fileset {

    sprite: svg-sprites
    home: home.scss
  }

  svg-sprites {

    spriteElementPath: "images/svg-source",
    spritePath: "css",
    layout: "vertical",
    sizes: {
      large: 24,
      small: 16
    },
    refSize: "large"
  }

}
```

Please refer to <a href="https://github.com/drdk/dr-svg-sprites">dr-svg-sprites</a> for more details.

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
