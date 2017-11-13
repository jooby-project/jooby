[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-assets/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.jooby/jooby-assets)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-assets.svg)](https://javadoc.io/doc/org.jooby/jooby-assets/1.2.3)
[![jooby-assets website](https://img.shields.io/badge/jooby-assets-brightgreen.svg)](http://jooby.org/doc/assets)
# assets

The asset module is library to validate, concatenate, minify or compress JavaScript and CSS assets. It also adds the ability to write these assets in other languages and process/compile them to another language. Finally, it help you to write high quality code by validate JavaScript and CSS too.

A variety of processors are available: ([jshint](https://github.com/jooby-project/jooby/tree/master/jooby-assets-jshint), [clean-css](https://github.com/jooby-project/jooby/tree/master/jooby-assets-clean-css), [jscs](https://github.com/jooby-project/jooby/tree/master/jooby-assets-jscs), [uglify](https://github.com/jooby-project/jooby/tree/master/jooby-assets-uglify), [closure-compiler](https://github.com/jooby-project/jooby/tree/master/jooby-assets-closure-compiler), etc..), but also you might want to write your owns.

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-assets</artifactId>
  <version>1.2.3</version>
</dependency>
```

## getting started

The first thing you need to do is to define your assets. Definition is done in your ```.conf``` file or in a special file: ```assets.conf```.

**assets.conf**

```text
assets {
 fileset {
   home: [assets/home.js, assets/home.css]
 }
}
```

**App.java**

```java
{
  use(new Assets());
}
```

The assets module will publish 4 request local variables for ```home``` fileset: ```_css``` and ```_js``` each of these variables is a list of string with the corresponding files. There are two more variables: ```_styles``` and ```_scripts``` :

```html
 <html>
 <head>
   {{& home_styles}}
 <body>
   ...
   {{& home_scripts}}
 </body>
 </head>
 </html>
```

The variables: ```_styles``` and ```_scripts``` produces one ore more ```link``` and ```script``` tags. The example above, shows you how to render these variables in the template engine of your choice (handlebars, here).

Now, let's see how to configure the Maven plugin to process our assets at build-time:

**pom.xml**

```html
 <plugin>
   <groupId>org.jooby</groupId>
   <artifactId>jooby-maven-plugin</artifactId>
   <executions>
     <execution>
       <goals>
         <goal>assets</goal>
       </goals>
     </execution>
   </executions>
 </plugin>
```

The plugin will process all your assets and include them to the final ```.jar```, ```.zip``` or ```.war```.

Cool, isn't?

## how it works?

The ```assets.fileset``` defines all your assets. In ```dev``` assets are rendered/processed at runtime. In ```prod``` at built-time.

Assets are rendered at runtime using ```*_styles``` or ```*_scripts ``` variables. So you define your assets in one single place: ```assets.conf```.

Also, at build-time, the asset compiler concatenates all the files from a fileset and generate a fingerprint. The fingerprint is a SHA-1 hash of the content of the fileset. Thanks to the fingerprint an asset can be cached it for ever! Defaults cache max age is: ```365 days```.

That isn't all! the ```*_styles``` and ```*_scripts``` are updated with the fingerprint version of assets, so you don't have to do or change anything in your views! It just works!!!

## fileset

A fileset is a group of assets within a name. The fileset name is expanded into 4 request local variables, for example:

```text
assets {
 fileset {
   home: [assets/home.js, assets/home.css]
   pageA: [assets/pageA.js, assets/pageA.css]
 }
}
```

Produces 4 variables for ```home```:

* home_css: a list of all the ```css``` files 
* home_styles: a string, with all the ```css``` files rendered as ```link``` tags  
* home_js: a list of all the ```js``` files 
* home_scripts: a string, with all the ```js``` files rendered as ```script``` tags 

Another 4 variables will be available for the ```pageA``` fileset!

### extending filesets

Extension or re-use of filesets is possible via the: ```<``` operator:

```text
assets {
 fileset {
   base: [js/lib/jquery.js, css/normalize.css]
   home < base: [js/home.js]
   pageA < base: [js/pageA.js]
 }
}
```

## processors

An [AssetProcessor](/apidocs/org/jooby/assets/AssetProcessor.html) usually checks or modify an asset content in one way or another. They are defined in the ```assets.conf``` files using the ```pipeline``` construction:

```text
assets {
 fileset {
   home: [js/home.js, css/home.css]
 }
 pipeline {
   dev: [jshint, jscs, csslint, sass]
   dist: [uglify, sass, clean-css]
 }
}
```

Example above, defines a **pipeline** for development (dev) and one generic for prod (dist).

In ```dev``` the code will be checked it against js-hint, jscs and csslint! But also, we want to use sass for css!!

The generic ```dist``` will be used it for any other environment and here we just want to optimize our javascript code with uglify, compile sass to css and then optimize the css using clean-css!!

## live compiler and error report

This module comes with a live compiler which is enabled by default in ```dev```:

<img alt="live compiler" width="75%" src="http://jooby.org/resources/images/jshint.png">

If you want to turn it off, just set the ```assets.watch = false```.

The fancy error page is generated by [whoops](https://github.com/jooby-project/jooby/tree/master/jooby-whoops), here is an example on how to enable [whoops](https://github.com/jooby-project/jooby/tree/master/jooby-whoops):

```java
{
  // required
  use(new Whoops());

  use(new Assets());
}
```

# asset processor

Checks, validates and/or modifies asset contents. An [AssetProcessor](/apidocs/org/jooby/assets/AssetProcessor.html) is usually provided as a separate dependency.

## usage

Start by adding the dependency to your ```pom.xml```:

```xml
  <dependency>
    <groupId>org.jooby</groupId>
    <artifactId>jooby-assets-my-processor</artifactId>
    <scope>provided</scope>
  </dependency>
```

Notice the **provided** scope. The processor is only required for development, since the assets are processed at runtime in this case. In ```prod```, assets are processed at build-time via the Maven or Gradle plugins, so the dependency is not needed there. This also helps to keep the number of dependencies and the jar size smaller.

After the dependency is declared, all that's needed is to add the processor to the pipeline:


```text
assets {
  pipeline: {
    dev: [my-processor]
  }
}
```

## configuration

It's possible to configure or set options as well:

```text
assets {
  pipeline: {
    dev: [my-processor]
    dist: [my-processor]
  }
  my-processor {
    foo: bar
  }
}
```

The previous example sets the ```foo``` property to ```bar```. Options can be set per environment as well:

```text
assets {
  pipeline: {
    dev: [my-processor]
    dist: [my-processor]
  }
  my-processor {
    dev {
      bar: bar
    }
    dist {
      foo: bar
    }
    foo: foo
  }
}
```

In this example, the processor will have two properties in the ```dev``` environment: ```foo:foo``` and ```bar:bar```, while in ```dist``` the processor will only have ```foo:bar```

## binding

The ```my-processor``` token will be resolved to the: ```org.jooby.assets.MyProcessor``` class. The processor name is converted to ```MyProcessor``` by converting the hyphenated name to upper camel case and by placing it in the ```org.jooby.assets``` package (a default for processors).

A custom binding is provided via the ```class``` property:

```text
assets {
  pipeline: {
    dev: [my-processor]
    dist: [my-processor]
  }
  my-processor {
    class: whatever.i.Want
  }
}
```

# asset aggregator

Contributes new or dynamically generated content to a ```fileset```. Content generated by an aggregator might be processed by an {@link AssetProcessor}.

## usage

Start by adding the dependency to your ```pom.xml```:

```xml
<dependency>
    <groupId>org.jooby</groupId>
    <artifactId>jooby-assets-dr-svg-sprites</artifactId>
    <scope>provided</scope>
  </dependency>

```

Notice the **provided** scope. The aggregator is only required for development, since the assets are processed at runtime in this case. In ```prod```, assets are processed at build-time via the Maven or Gradle plugins, so the dependency is not needed there. This also helps to keep the number of dependencies and the jar size smaller.

After the dependency is declared, all that's needed is to add the ```svg-sprites``` aggregator to a fileset:

```
assets {
  fileset {

    home: [
      // 1) Add the aggregator to a fileset
      svg-sprites,
      css/style.css,
      js/app.js
    ]
  }

  svg-sprites {
    // 2) The `css/sprite.css` file is part of the `home` fileset.
    spritePath: "css/sprite.css"
    spriteElementPath: "images/svg",
  }

}
```

In this example, the ```svg-sprites``` aggregator contributes the ```css/sprite.css``` file to the ```home``` fileset. The fileset then looks like:

```
assets {
  fileset {
    home: [
      css/sprite.css,
      css/style.css,
      js/app.js
    ]
  }
}
```

It replaces the aggregator name with one or more files from the [AssetAggregator.fileset](/apidocs/org/jooby/assets/AssetAggregator.html#fileset--) method.

# available processors

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
