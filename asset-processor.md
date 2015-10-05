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

## CSS processors

* [props](/doc/assets-props): replace application properties in CSS files.

* [csslint](/doc/assets-csslint): check and validate css rules.

* [sass](/doc/assets-sass): Sass support from <a href="https://github.com/sass/sass">Sass (ruby)</a>.

* [less4j](/doc/assets-less4j): Less support from [less4j](https://github.com/SomMeri/less4j)

* [less](/doc/assets-less): Less support from [less.js](http://lesscss.org)

* [clean-css](/doc/assets-clean-css): minify css.

* [yui-css](/doc/assets-yui-compressor): YUI CSS optimizer.

## JavaScript processors

* [props](/doc/assets-props): replace application properties in JavaScript files.

* [jscs](/doc/assets-jscs): JavaScript code style checker.

* [jshint](/doc/assets-jshint): JavaScript linter, helps to detect errors and potential problems in code..

* [ng-annotate](/doc/assets-ng-annotate): Add, remove and rebuild AngularJS dependency injection annotations.

* [closure-compiler](/doc/assets-closure-compiler): Google JavaScript optimizer and minifier.

* [uglify](/doc/assets-uglify): uglify.js optimizer.

* [requirejs](/doc/assets-requirejs): r.js optimizer.

* [yui-js](/doc/assets-yui-compressor): YUI JS optimizer.
