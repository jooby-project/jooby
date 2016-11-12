# Gradle

Collection of Gradle plugins for Jooby including debug and reload of applications and assets processing.

> NOTE: Jooby is being developed with Maven and it is currently the *official* build tool. Even though
it is a long-term goal of the project to accommodate alternative tooling, it is not known when such 
support will materialized itself. In the meantime, some compromises have to be made to walk off the beaten path.
>
> The goal of this document is to document the current state of the art to duplicate a Maven development environment to Gradle.

## A minimal `build.gradle`

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

/*
 * We are writing a standalone application.
 */
apply plugin: 'application'

apply plugin: 'jooby'

/*
 * Some classic project configuration.
 */
version = 0.1
mainClassName = "my.package.Main"
sourceCompatibility = 1.8

/*
 * Dependencies will be downloaded from the Maven repository.
 */
repositories {
  mavenCentral()
}

/*
 * A minimal project needs the framework and a webserver.
 */
dependencies {
  compile group: 'org.jooby', name: 'jooby', version: '{{version}}'
  compile group: 'org.jooby', name: 'jooby-netty', version: '{{version}}'
}

/*
 * We diverge from the default resources structure to adopt the Jooby standard.
 */
sourceSets.main.resources {
  srcDirs = ["conf", "public"]
}
```

## What works

- **Compiling the application** can be done as usual with `gradle build`.

- **Running/Hotreload the application** can be done with `gradle joobyRun`.

- **Asset processing** can be done with `gradle joobyAssets`. See [assets module](https://github.com/jooby-project/jooby/tree/master/jooby-assets)

- **Route Spec** can be done with `gradle joobySpec`. See [route spec](https://github.com/jooby-project/jooby/tree/master/jooby-spec) 

For more details please read the [gradle-plugin](https://github.com/jooby-project/jooby/tree/master/jooby-gradle-plugin) documentation

## What does not work

- **Everything else.**
