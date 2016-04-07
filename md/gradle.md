# Gradle

Jooby is being developed with Maven and it is currently the *official* build
tool. Even though it is a long-term goal of the project to accommodate alternative
tooling, it is not known when such support will materialized itself. In the
meantime, some compromises have to be made to walk off the beaten path.

The goal of this document is to document the current state of the art to
duplicate a Maven development environment to Gradle.

## A minimal `build.gradle`

```
/*
 * We are writing a standalone application.
 */
apply plugin: 'application'

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
 * A minimal project needs the framwork and a webserver.
 */
dependencies {
  compile group: 'org.jooby', name: 'jooby', version: '0.16.0'
  compile group: 'org.jooby', name: 'jooby-netty', version: '0.16.0'
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

- **Running the application** can be done as usual with `gradle run` but the
  automatic reloading of resources or java classes does not work.


## What does not work

- **Everything else.**
