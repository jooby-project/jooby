# Build

# Build everything from maven

- `mvn clean install -P gradlePluin`

# Build just the project

- Run at least once `mvn clean install -P gradlePluin`
- `./gradlew build -PjoobyVersion=${project.version}` (project.version is the current version of project built in previous step)

# Test changes locally

- First run `mvn clean install -P gradlePluin`
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

