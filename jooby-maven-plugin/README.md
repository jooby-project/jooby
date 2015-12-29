# jooby:run

A [Maven](http://maven.apache.org/) plugin for running, debugging and reloading your application.

## usage

```bash
mvn jooby:run
```

You should see something similar:

```bash
Hotswap available on: [myapp/public, myapp/conf, myapp/target/classes]
  includes: [**/*.class,**/*.conf,**/*.properties]
  excludes: []
INFO  [2015-03-31 17:47:33,000] [dev@netty]: App server started in 401ms

  GET /assets/**           [*/*]     [*/*]    (anonymous)
  GET /                    [*/*]     [*/*]    (anonymous)

listening on:
  http://localhost:8080/
```

## hot reload

The plugin bounces the application every time a change is detected on:

- classes (*.class)
- config files (*.conf and *.properties)

Changes on templates and/or static files (*.html, *.js, *.css) wont restart the application, because they are not compiled/cached it while running on ```application.env = dev```.

**NOTE: For the time being, you need to use a tool that compiles your source code, usually an IDE. Otherwise, no changes will be found.**

Is it worth to mention that dynamic reload of classes at runtime is done via [JBoss Modules](https://github.com/jboss-modules/jboss-modules).

## options

```xml
<plugin>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-maven-plugin</artifactId>
  <version>0.13.0</version>
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

A [Maven](http://maven.apache.org/) property that contains the fully qualified name of the ```main class```. **Required**.

### debug

The JVM is started in **debug mode by default**. You can attach a remote debugger at the ```8000``` port.

This property can be one of these:

* ```true```: Turn on debug mode using: **-agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=n**
* ```false```: Turn off debug mode (run normally)
* ```int```: Turn on debug mode using the given number as debug port: ```<debug>8000</debug>```
* ```string```: Turn on debug via ```string``` value, something like: **-agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=n**

Finally this property can be set from command line using the ```application.debug``` system property:

```
mvn jooby:run -Dapplication.debug=9999
```

### commands

List of commands to execute before starting the ```application```. Useful for [npm](https://www.npmjs.com), [npm](http://gruntjs.com), etc...

```xml
<plugin>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-maven-plugin</artifactId>
  <version>0.13.0</version>
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
  <version>0.13.0</version>
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
