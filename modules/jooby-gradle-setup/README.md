This project exists to easily integrate the maven build and release process with the gradle plugin: `jooby-gradle-plugin`.

It runs at the end of the build lifecycle to ensure dependencies are built before gradle plugin.

# How to test locally

- First fun `mvn clean install -P gradlePluin`
- Previous step install plugin in `.m2/repository`
- Creates a gradle project
- Go to `settings.gradle` and add these lines

```groovy
pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = 'your project name'
```
