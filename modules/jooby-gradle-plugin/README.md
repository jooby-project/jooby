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

