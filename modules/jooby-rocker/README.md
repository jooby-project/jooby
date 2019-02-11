[![Maven](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/org/jooby/jooby-rocker/maven-metadata.xml.svg)](http://mvnrepository.com/artifact/org.jooby/jooby-rocker/1.6.0)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-rocker.svg)](https://javadoc.io/doc/org.jooby/jooby-rocker/1.6.0)
[![jooby-rocker website](https://img.shields.io/badge/jooby-rocker-brightgreen.svg)](http://jooby.org/doc/rocker)
# rocker

Java 8 optimized, memory efficient, speedy template engine producing statically typed, plain java objects.

<a href="https://github.com/fizzed/rocker">Rocker</a> is a Java 8 optimized (runtime compat with 6+), near zero-copy rendering, speedy template engine that produces statically typed, plain java object templates that are compiled along with the rest of your project.

## exports

* Rocker [renderer](/apidocs/org/jooby/Renderer.html)

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-rocker</artifactId>
 <version>1.6.0</version>
</dependency>
```

## usage

```java
{
  use(new Rockerby());

  // Rocker API:
  get("/", () -> views.index.template("Rocker"));
}
```

## rocker idioms

<a href="https://github.com/fizzed/rocker">Rocker</a> support two flavors.

The **static**, **efficient** and **type-safe** flavor:

```java
{
  use(new Rockerby());

  get("/", () -> views.index.template("Rocker"));
}
```

Or the **dynamic** flavor is available via [View](/apidocs/org/jooby/View.html) objects:

```java
{
  use(new Rockerby());

  get("/", () -> Results.html("views/index").put("message", "Rocker"));
}
```

which is syntax sugar for:

```java
{
  use(new Rockerby());

  get("/", () -> {
    return Rocker.template("views/index.rocker.html").bind("message", "Rocker");
  });

}
```

## code generation

### maven

We do provide code generation via Maven profile. All you have to do is to write a ```rocker.activator``` file inside the ```src/etc``` folder. File presence triggers generation of source code.

### gradle

Please refer to <a href="https://github.com/fizzed/rocker/issues/33">Rocker documentation</a> for Gradle.

## hot reload

You don't need <a href="https://github.com/fizzed/rocker#hot-reloading">Rocker hot reload</a> as long as you start your application in development with <a href="http://jooby.org/doc/devtools/">jooby:run</a>. Because <a href="http://jooby.org/doc/devtools/">jooby:run</a> already restart the application on ```class changes```.

That's all folks!!
