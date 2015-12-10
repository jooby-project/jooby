# assets

The asset module is library to validate, concatenate, minify or compress JavaScript and CSS assets. It also adds the ability to write these assets in other languages and process/compile them to another language. Finally, it help you to write high quality code by validate JavaScript and CSS too.

A variety of processors are available: ([jshint](https://github.com/jooby-project/jooby/tree/master/jooby-assets-jshint), [clean-css](https://github.com/jooby-project/jooby/tree/master/jooby-assets-clean-css), [jscs](https://github.com/jooby-project/jooby/tree/master/jooby-assets-jscs), [uglify](https://github.com/jooby-project/jooby/tree/master/jooby-assets-uglify), [closure-compiler](https://github.com/jooby-project/jooby/tree/master/jooby-assets-closure-compiler), etc..), but also you might want to write your owns.

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-assets</artifactId>
  <version>0.12.0</version>
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

This module comes with a live compiler and fancy error reporter, which is enabled by default in ```dev```:

<img alt="live compiler" width="75%" src="http://jooby.org/resources/images/jshint.png">

If you want to turn it off, just set the ```assets.watch = false```.

# asset processor

Checks, validate and/or modify asset contents. An [AssetProcessor](/apidocs/org/jooby/assets/AssetProcessor.html) is usually provided as a separated dependency.

## how to use it?

First thing to do is to add the dependency:

```xml
  <dependency>
    <groupId>org.jooby</groupId>
    <artifactId>jooby-assets-my-processor</artifactId>
    <scope>test</scope>
  </dependency>
```

Did you see the **test** scope? We just need the processor for development, because assets are processed on the fly. For ```prod```, assets are processed at built-time via Maven plugin, at runtime we don't need this. This also, helps to keep our dependencies and the jar size to minimum.

Now we have the dependency all we have to do is to add it to our pipeline:

```text
assets {
  pipeline: {
    dev: [my-processor]
  }
}
```

## configuration

It is possible to configure or set options too:

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

Previous example, set a ```foo``` property to ```bar```! Options can be set per environment too:

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

Here, in ```dev``` processor has two properties: ```foo:foo``` and ```bar:bar```, while in ```dist``` the processor only has ```foo:bar```

## binding

The ```my-processor``` will be resolved it to: ```org.jooby.assets.MyProcessor``` class. The processor name is converted to ```MyProcessor```, it converts the hyphenated name to upper camel and by default processors are defined in the ```org.jooby.assets``` package.

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

# available processors

## css processors

* [props](https://github.com/jooby-project/jooby/tree/master/jooby-assets-props): replace application properties in CSS files.

* [csslint](https://github.com/jooby-project/jooby/tree/master/jooby-assets-csslint): check and validate css rules.

* [sass](https://github.com/jooby-project/jooby/tree/master/jooby-assets-sass): Sass support from <a href="https://github.com/sass/sass">Sass (ruby)</a>.

* [less4j](https://github.com/jooby-project/jooby/tree/master/jooby-assets-less4j): Less support from [less4j](https://github.com/SomMeri/less4j)

* [less](https://github.com/jooby-project/jooby/tree/master/jooby-assets-less): Less support from [less.js](http://lesscss.org)

* [clean-css](https://github.com/jooby-project/jooby/tree/master/jooby-assets-clean-css): minify css.

* [yui-css](https://github.com/jooby-project/jooby/tree/master/jooby-assets-yui-compressor): YUI CSS optimizer.

## js processors

* [props](https://github.com/jooby-project/jooby/tree/master/jooby-assets-props): replace application properties in JavaScript files.

* [jscs](https://github.com/jooby-project/jooby/tree/master/jooby-assets-jscs): JavaScript code style checker.

* [jshint](https://github.com/jooby-project/jooby/tree/master/jooby-assets-jshint): JavaScript linter, helps to detect errors and potential problems in code..

* [babel](https://github.com/jooby-project/jooby/tree/master/jooby-assets-babel): Ecma6 now via <a href="http://babeljs.io/">Babel</a>.

* [rollup](https://github.com/jooby-project/jooby/tree/master/jooby-assets-rollup): <a href="http://rollupjs.org/">rollup.js</a> the next-generation ES6 module bundler..

* [ng-annotate](https://github.com/jooby-project/jooby/tree/master/jooby-assets-ng-annotate): Add, remove and rebuild AngularJS dependency injection annotations.

* [closure-compiler](https://github.com/jooby-project/jooby/tree/master/jooby-assets-closure-compiler): Google JavaScript optimizer and minifier.

* [uglify](https://github.com/jooby-project/jooby/tree/master/jooby-assets-uglify): uglify.js optimizer.

* [requirejs](https://github.com/jooby-project/jooby/tree/master/jooby-assets-requirejs): r.js optimizer.

* [yui-js](https://github.com/jooby-project/jooby/tree/master/jooby-assets-yui-compressor): YUI JS optimizer.
