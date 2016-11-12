# gradle

{{gradle}} plugin for running, debugging and reloading your application.

## usage

```js
buildscript {

  repositories {
    mavenCentral()
  }

  dependencies {
    /** joobyRun */
    classpath group: 'org.jooby', name: 'jooby-gradle-plugin', version: '{{version}}'
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

## hot reload

The ```joobyRun``` tool restart the application every time a change is detected on:

- classes (*.class)
- config files (*.conf and *.properties)

Changes on templates and/or static files (*.html, *.js, *.css) wont restart the application, because they are not compiled or cached while running on ```application.env = dev```.

It's worth to mention that dynamic reload of classes is done via {{jboss-modules}}.

## options

```js

joobyRun {
  mainClassName = 'com.mycompany.App'
  compiler = 'on'
  includes = ['**/*.class', '**/*.conf', '**/*.properties']
  excludes = []
  logLevel = 'info'
}

```

### mainClassName

A {{gradle}} property that contains the fully qualified name of the ```main class```. **Required**.

### compiler

The compiler is ```on``` by default, unless:

* A ```.classpath``` file is present in the project directory. If present, means you're a Eclipse user and we turn off the compiler and let Eclipse recompiles the code on save.

* The compiler is set to ```off```.

On compilation success, the application is effectively reloaded.

On compilation error, the application won't reload.

Compilation success or error messages are displayed in the console (not at the browser).

### includes / excludes

List of file patterns to listen for file changes.
