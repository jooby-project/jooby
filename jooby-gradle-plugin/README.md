# gradle plugin

Collection of [Gradle](http://gradle.org) plugins for [Jooby](http://jooby.org) applications.

## joobyRun

Run, debug and reload applications.

### usage

```js

buildscript {

  repositories {
    mavenCentral()
  }

  dependencies {
    /** joobyRun */
    classpath group: 'org.jooby', name: 'jooby-gradle-plugin', version: '1.0.2'
  }
}

apply plugin: 'jooby'

}
```

```bash
gradle joobyRun
```

Prints something similar to:

```bash
>>> jooby:run[info|main]: Hotswap available on: /my-app
>>> jooby:run[info|main]:   includes: [**/*.class,**/*.conf,**/*.properties,*.js, src/*.js]
>>> jooby:run[info|main]:   excludes: []
INFO  [2015-03-31 17:47:33,000] [dev@netty]: App server started in 401ms

  GET /assets/**           [*/*]     [*/*]    (anonymous)
  GET /                    [*/*]     [*/*]    (anonymous)

listening on:
  http://localhost:8080/
```

### hot reload

The ```joobyRun``` tool restart the application every time a change is detected on:

- classes (*.class)
- config files (*.conf and *.properties)

Changes on templates and/or static files (*.html, *.js, *.css) wont restart the application, because they are not compiled or cached while running on ```application.env = dev```.

It's worth to mention that dynamic reload of classes is done via [JBoss Modules](https://github.com/jboss-modules/jboss-modules).

### options

```js
joobyRun {
  mainClassName = 'com.mycompany.App'
  compiler = 'on'
  includes = ['**/*.class', '**/*.conf', '**/*.properties']
  excludes = []
  logLevel = 'info'
}

```

#### mainClassName

A [Gradle](http://gradle.org) property that contains the fully qualified name of the ```main class```. **Required**.

#### compiler

The compiler is ```on``` by default, unless:

* A ```.classpath``` file is present in the project directory. If present, means you're a Eclipse user and we turn off the compiler and let Eclipse recompiles the code on save.

* The compiler is set to ```off```.

On compilation success, the application is effectively reloaded.

On compilation error, the application won't reload.

Compilation success or error messages are displayed in the console (not at the browser).

#### includes / excludes

List of file patterns to listen for file changes.

## joobyAssets

This is a [Gradle](http://gradle.org) task for the [asset module]([assets module](https://github.com/jooby-project/jooby/tree/master/jooby-assets)). The [asset module]([assets module](https://github.com/jooby-project/jooby/tree/master/jooby-assets)) validate, concatenate, minify or compress JavaScript and CSS assets.

### usage

```js
buildscript {

  repositories {
    mavenCentral()
  }

  dependencies {
    /** jooby:run */
    classpath group: 'org.jooby', name: 'jooby-gradle-plugin', version: '1.0.0.CR7'
  }
}

apply plugin: 'jooby'

}
```

```bash
gradle joobyAssets
```


### options

```js
joobyAssets {
  maxAge = '365d'
}

```

#### maxAge

Specify the max age cache header, default is: `365d```

## joobySpec

This is a [Gradle](http://gradle.org) task for the [route spec](https://github.com/jooby-project/jooby/tree/master/jooby-spec) module. The [route spec](https://github.com/jooby-project/jooby/tree/master/jooby-spec) module allows you to export your API/microservices outside a [Jooby](http://jooby.org) application.

### usage

```js
buildscript {

  repositories {
    mavenCentral()
  }

  dependencies {
    /** jooby:run */
    classpath group: 'org.jooby', name: 'jooby-gradle-plugin', version: '1.0.0.CR7'
  }
}

apply plugin: 'jooby'

```

```bash
gradle joobySpec
```

This task has no options, the ```.spec``` file can found at the ```build/classes/main```
