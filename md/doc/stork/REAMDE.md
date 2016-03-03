# stork

Build, package and distribute your application using [Stork](https://github.com/fizzed/stork).

[Stork](https://github.com/fizzed/stork) is a collection of utilities for optimizing your **after-build** workflow by filling in the gap between your Java build system and eventual end-user app execution.

## usage

[Stork](https://github.com/fizzed/stork) integration is provided via [Maven Profiles](http://maven.apache.org/guides/introduction/introduction-to-profiles.html).

* Write your [app.stork launcher](https://github.com/fizzed/stork#launcher-configuration) and save it in the ```src/etc/stork``` directory

* Open a console and type:

  ```mvn clean package```

* It builds a ```[app-name].zip``` file inside the ```target``` directory

## profile activation

The ```stork``` Maven profile is activated by the presence of the ```src/etc/stork/app.stork``` file.

## launcher configuration

You must name your [launcher](https://github.com/fizzed/stork#launcher-configuration) as ```app.stork``` and save it inside the ```src/etc/stork``` directory.

Any {{maven}} properties defined here will be resolved and merged into the final output. Examples of these properties are: ```${application.class}```, ```${project.groupId}```, etc...

## app.stork

Here is a simple example of the stork launcher:

```yml
# Name of application (make sure it has no spaces)
name: "${project.artifactId}"

# Type of launcher (CONSOLE or DAEMON)
type: DAEMON

# Java class to run
main_class: "${application.class}"

domain: "${project.groupId}"

short_description: "${project.artifactId}"

# Platform launchers to generate (WINDOWS, LINUX, MAC_OSX)
# Linux launcher is suitable for Bourne shells (e.g. Linux/BSD)
platforms: [ LINUX ]

# Working directory for app
#  RETAIN will not change the working directory
#  APP_HOME will change the working directory to the home of the app
#    (where it was intalled) before running the main class
working_dir_mode: RETAIN

# Minimum version of java required (system will be searched for acceptable jvm)
min_java_version: "1.8"

# Min/max fixed memory (measured in MB)
min_java_memory: 512
max_java_memory: 1024

# Min/max memory by percentage of system
#min_java_memory_pct: 10
#max_java_memory_pct: 20

# Try to create a symbolic link to java executable in <app_home>/run with
# the name of "<app_name>-java" so that commands like "ps" will make it
# easier to find your app
symlink_java: true
```

Happy coding!!
