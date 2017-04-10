# conf, env and logging

Jooby delegates configuration management to the [Config library](https://github.com/typesafehub/config).

By defaults Jooby expects to find an ```application.conf``` file at the root of classpath. You can find the `.conf` file under the `conf` classpath directory.

## getting properties

via script:

```java
{
  get("/", req -> {
    Config conf = require(Config.class);
    String myprop = conf.getString("myprop");
    ...
  });
}
```

via ```@Named``` annotation:

```java
public class Controller {

  @Inject
  public Controller(@Named("myprop") String myprop) {
    ...
  }
}
```

via [Module]({{defdocs}}/Jooby.Module.html):

```java

public class MyModule implements Jooby.Module {

  public void configure(Env env, Config conf, Binder binder) {
    String myprop = conf.getString("myprop");
    ...
  }
}
```

### type conversion

Automatic type conversion is provided when a type:

1) Is a primitive, primitive wrapper or String

2) Is an enum

3) Has a public **constructor** that accepts a single **String** argument

4) Has a static method **valueOf** that accepts a single **String** argument

5) Has a static method **fromString** that accepts a single **String** argument. Like ```java.util.UUID```

6) Has a static method **forName** that accepts a single **String** argument. Like ```java.nio.charset.Charset```

You're free to inject the entire ```com.typesafe.config.Config``` object or `sub-path/tree` of it.

## environment

Jooby internals and the module system rely on the ```application.env``` property. By defaults, this property is set to: ```dev```.

This special property is represented at runtime with the [Env]({{apidocs}}/org/jooby/Env.html) class.

For example: a module might decided to create a connection pool, cache, etc when ```application.env``` isn't set to `dev`.

The `application.env` property can be set as command line argument as well:

Using a `fat jar`:

    java -jar myfat.jar prod


Using [stork](/doc/stork):

    bin/myapp --start prod

## turning features on or off

As described before, the ```application.env``` property defines the environment where the application is being executed. It's possible to turn on/off specific features based on the application environment:

```java
{
  on("dev", () -> {
    use(new DevModule());
  });

  on("prod", () -> {
    use(new ProdModule());
  });
}
```

There is a `~` (complement) operator:

```java
{
  on("dev", () -> {
    use(new DevModule());
  }).orElse(() -> {
    use(new ProdModule());
  });
}
```

The ```environment callback``` has access to the ```config``` object, see:

```java
{
  on("dev", conf -> {
    use(new DevModule(conf.getString("myprop")));
  });
}
```


## special properties

Here is the list of special properties available in Jooby:

* **pid**: application process ID.
* **application.name**: describes the name of your application. Default is: `single package name` of where you define your bootstrap class, for example for `com.foo.App` application's name is `foo`.
* **application.version**: application version. Default is: `getClass().getPackage().getImplementationVersion()` (automatically set by Maven).
* **application.class**: fully qualified name of the bootstrap class.
* **application.secret**: If present, the session cookie will be signed with the `application.secret`.
* **application.tmpdir**: location of the application temporary directory. Default is: `${java.io.tmpdir}/${application.name}`.
* **application.charset**: charset to use. Default is: `UTF-8`.
* **application.lang**: locale to use. Default is: `Locale.getDefault()`.
* **application.dateFormat**: date format to use. Default is: `dd-MM-yyyy`.
* **application.numberFormat**: number format to use. Default is: `DecimalFormat.getInstance("application.lang")`.
* **application.tz**: time zone to use. Default is: `ZoneId.systemDefault().getId()`.
* **runtime.processors**: number of available processors.
* **runtime.processors-plus1**: number of processors.
* **runtime.processors-plus2**: number of processors + 2.
* **runtime.processors-x2**: number of processors * 2.

## precedence

Configuration files are loaded in the following order:

* system properties
* arguments properties
* (file://[application].[env].[conf])?
* (cp://[application].[env].[conf])?
* ([application].[conf])?
* [module].[conf]*

The first occurence of a property will take precedence.

### system properties

System properties can override any other property. A system property is set at startup time, like: 

    java -Dapplication.env=prod -jar myapp.jar

### arguments properties

Command line arguments has precedence over `file system` or `classpath` configuration files:

    java -jar myapp.jar application.env=prod

Or using the shortcut for `application.env`:

    java -jar myapp.jar prod

Unqualified properties are bound to `application`, so:

    java -jar myapp.jar port=8888 path=/myapp

automatically translate to:

    java -jar myapp.jar application.port=8888 application.path=/myapp

### file://[application].[env].[conf]

A `file system` configuration file has precedence over `classpath` configuration file. Usefult to override a `classpath` configuration file.

### cp://[application].[env].[conf]

A `classpath system conf` file has precedence over `default conf` file.

### application.conf

The default `classpath conf` file: `application.conf`

### [module].[conf]

A [Module]({{defdocs}}/Jooby.Module.html) might have defined its own set of (default) properties via the [Module.config]({{defdocs}}/Jooby.Module.html#config--) method.

```java
{
  use(new Jdbc());
}
```

### custom .conf

As mentioned earlier, the default `conf` file is `application.conf`, but you can use whatever name you prefer:

```java
{
  conf("myapp.conf");
}
```

### example

```
.
└── conf
    ├── application.conf
    ├── application.uat.conf
    ├── application.prod.conf
    ├── logback.xml
    ├── logback.uat.xml
    └── logback.prod.xml
```

```java
{
  // import Foo and Bar modules:
  use(new Foo());

  use(new Bar());
}
```

* Starting the application in `dev` produces a `conf` tree similar to:

```
.
└── system properties
    ├── command line
    ├── application.conf
    ├── foo.conf
    ├── bar.conf
    ├── ...
    └── ...
```

* Starting the application in `prod` produces a `conf` tree similar to:

```
.
└── system properties
    ├── command line
    ├── application.prod.conf
    ├── application.conf
    ├── foo.conf
    ├── bar.conf
    ├── ...
    └── ...
```

First-listed are higher priority.

For more details, please refer to the [config documentation](https://github.com/typesafehub/config).

{{logging.md}}
