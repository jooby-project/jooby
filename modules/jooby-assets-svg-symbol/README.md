[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-assets-svg-symbol/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-assets-svg-symbol)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-assets-svg-symbol.svg)](https://javadoc.io/doc/org.jooby/jooby-assets-svg-symbol/1.2.0)
[![jooby-assets-svg-symbol website](https://img.shields.io/badge/jooby-assets-svg-symbol-brightgreen.svg)](http://jooby.org/doc/assets-svg-symbol)
# svg-symbol

SVG ```symbol``` for icons: merge svg files from a folder and generates a ```sprite.svg``` and ```sprite.css``` files.

Make sure you've already set up the [assets module](https://github.com/jooby-project/jooby/tree/master/jooby-assets) in your project!

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-assets-svg-symbol</artifactId>
 <version>1.2.0</version>
 <scope>provided</scope>
</dependency>
```

## usage

```
assets {
  fileset {
    sprite: svg-symbol
  }

  svg-symbol {
    input: "images/svg"
  }
}
```

Previous example looks for ```*.svg``` files inside the ```images/svg``` folder and generate a ```sprite.svg``` and ```sprite.css``` files.

You can display the svg icons using id reference:

```xml
<svg>
 <use xlink:href="#approved" />
</svg>
```

This technique is described here: <a href="https://css-tricks.com/svg-symbol-good-choice-icons">SVG `symbol` a Good Choice for Icons</a>

## options

### output

Defines where to write the ```svg``` and ```css``` files. Default value is: ```sprite```.

```
svg-symbol {
  output: "folder/symbols"
}
```

There are two more specific output options: ```svg.output``` and ```css.output``` if any of these options are present the ```output``` option is ignored:

```
svg-symbol {
  css {
    output: "css/sprite.css"
  },

  svg {
    output: "img/sprite.svg"
  }

}
```

### id prefix and suffix

ID is generated from ```svg file names```. These options prepend or append something to the generated id.

```
svg-symbol {
  output: "sprite"

  id {
    prefix: "icon-"
  }

}
```

Generates IDs like: ```icon-approved```, while:

```
svg-symbol {
  output: "sprite"

  id {
    suffix: "-icon"
  }

}
```

Generates IDs like: ```approved-icon```

### css prefix

Prepend a string to a generated css class. Here is the css class for ```approved.svg```:

```java
.approved {
  width: 18px;
  height: 18px;
}
```

If we set a ```svg``` css prefix:

```
{
  svg-symbol: {
    css {
      prefix: "svg"
    }
  }

}
```

The generated css class will be:

```
svg.approved {
  width: 18px;
  height: 18px;
}
```

This option is useful for generating more specific css class selectors.

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
