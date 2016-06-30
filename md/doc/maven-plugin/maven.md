# jooby:run

```jooby:run``` is a {{maven}} plugin for running, debugging and reloading your application.

## usage

```bash
mvn jooby:run
```

Prints something similar to:

```bash
[HotSwap|main|12:14:46]: Hotswap available on: [/my-app]
[HotSwap|main|12:14:46]:   includes: [**/*.class,**/*.conf,**/*.properties,*.js, src/*.js]
[HotSwap|main|12:14:46]:   excludes: []
INFO  [2015-03-31 17:47:33,000] [dev@netty]: App server started in 401ms

  GET /assets/**           [*/*]     [*/*]    (anonymous)
  GET /                    [*/*]     [*/*]    (anonymous)

listening on:
  http://localhost:8080/
```

## hot reload

The ```jooby:run``` tool restart the application every time a change is detected on:

- classes (*.class)
- config files (*.conf and *.properties)

Changes on templates and/or static files (*.html, *.js, *.css) wont restart the application, because they are not compiled or cached while running on ```application.env = dev```.

It's worth to mention that dynamic reload of classes is done via {{jboss-modules}}.

## options

```xml
<plugin>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-maven-plugin</artifactId>
  <version>{{version}}</version>
  <configuration>
    <mainClass>${application.class}</mainClass>
    <commands>
    </commands>
    <compiler>on</compiler>
    <fork>false</fork>
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

A {{maven}} property that contains the fully qualified name of the ```main class```. **Required**.

### compiler

The compiler is ```on``` by default, unless:

* A ```.classpath``` file is present in the project directory. If present, means you're a Eclipse user and we turn off the compiler and let Eclipse recompiles the code on save.

* The compiler is set to ```off```.

On compilation success, the application is effectively reloaded.

On compilation error, the application won't reload.

Compilation success or error messages are displayed in the console (not at the browser).

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

List of commands to execute before starting the ```application```. Useful for {{npm}}, {{grunt}}, etc...

```xml
<plugin>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-maven-plugin</artifactId>
  <version>{{version}}</version>
  <configuration>
    <mainClass>${application.class}</mainClass>
    <commands>
      <command>npm install</command>
      <command>grunt local</command>
    </commands>
  </configuration>
</plugin>
```

All processes are stopped it on ```CTRL+C```

### fork

Allows running the application in a separate JVM. If false it uses the JVM started by {{maven}}, while if true it will use a new JVM. Default is: ```false```.

### vmArgs

Set one or more ```JVM args```:

```xml
<plugin>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-maven-plugin</artifactId>
  <version>{{version}}</version>
  <configuration>
    <mainClass>${application.class}</mainClass>
    <fork>true</fork>
    <vmArgs>
      <vmArg>-Xms512m</vmArg>
      <vmArg>-Xmx1024m</vmArg>
    </vmArgs>
  </configuration>
</plugin>
```

Make sure to enable the ```fork``` option too, otherwise ```vmArgs``` are ignored.

### includes / excludes

List of file patterns to listen for file changes.
