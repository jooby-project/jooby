# rocker

Java 8 optimized, memory efficient, speedy template engine producing statically typed, plain java objects.

<a href="https://github.com/fizzed/rocker">Rocker</a> is a Java 8 optimized (runtime compat with 6+), near zero-copy rendering, speedy template engine that produces statically typed, plain java object templates that are compiled along with the rest of your project.

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-rocker</artifactId>
 <version>1.1.0</version>
</dependency>
```

## exports

* Rocker [renderer](/apidocs/org/jooby/Renderer.html)

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

The **dynamic** flavor is available via * [View](/apidocs/org/jooby/View.html) objects:

```java
{
  use(new Rockerby());

  get("/", () -> Results.html("views/index").put("message", "Rocker"));
}
```

Which is syntax sugar for:

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
