---
layout: index
title: maven-plugin
version: 0.6.2
---

# mvn jooby:run

A [Maven](http://maven.apache.org/) plugin for executing [Jooby](http://jooby.org) applications.

## usage

```bash
mvn jooby:run
```

You should see something similar:

```bash
Picked up JAVA_TOOL_OPTIONS: -Dfile.encoding=UTF8
Hotswap available on: [myapp/public, myapp/config, myapp/target/classes]
  includes: [**/*.class,**/*.conf,**/*.properties]
  excludes: []
INFO  [2015-03-31 17:47:33,000] [dev@netty]: App server started in 401ms

  GET /assets/**           [*/*]     [*/*]    (anonymous)
  GET /                    [*/*]     [*/*]    (anonymous)

listening on:
  http://0.0.0.0:8080/
```

## hot reload

The plugin bounces the application every time a change is detected on:

- classes (*.class)
- config files (*.conf and *.properties)

Changes on templates and/or static files (*.html, *.js, *.css) wont restart the application, because they are not compiled/cached it while running on ```application.env = dev```.

For the time being, you need to use a tool that compiles your source code, usually an IDE. Otherwise, no changes will be found.

Is it worth to mention that dynamic reload of classes at runtime is done via [JBoss Modules](https://github.com/jboss-modules/jboss-modules).

## options

```xml
<plugin>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-maven-plugin</artifactId>
  <version>0.6.2</version>
  <configuration>
    <mainClass>${application.class}</mainClass>
    <commands>
    </commands>
    <vmArgs></vmArgs>
    <debug>true</debug>
    <includes>
      <include>**/*.class</include>
      <include>**/*.conf</include>
      <include>**/*.properties</include>
    </includes>
    <excludes>
    </excludes>
  </configuration>
</plugin>
```

### ${application.class}

A [Maven](http://maven.apache.org/) property that contains the fully qualified name of the ```main class```. Required.

### debug

The JVM is started in debug mode by default. You can attach a remote debugger at the ```8000``` port.

### commands

List of commands to execute before starting the ```application```. Useful for [npm](https://www.npmjs.com), [npm](http://gruntjs.com), etc...

```xml
<plugin>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-maven-plugin</artifactId>
  <version>0.6.2</version>
  <configuration>
    <mainClass>${application.class}</mainClass>
    <commands>
      <command>npm install</command>
      <command>grunt local</command>
    </commands>
  </configuration>
</plugin>
```

Processes will be stopped it on ```CTRL+C```

### vmArgs

Set one or more ```JVM args```:

```xml
<plugin>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-maven-plugin</artifactId>
  <version>0.6.2</version>
  <configuration>
    <mainClass>${application.class}</mainClass>
    <vmArgs>
      <vmArg>-Xms512m</vmArg>
      <vmArg>-Xmx1024m</vmArg>
    </vmArgs>
  </configuration>
</plugin>
```

### includes / excludes

List of file patterns to change for file changes.
