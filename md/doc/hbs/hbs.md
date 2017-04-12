# handlebars

Logic-less and semantic templates via [Handlebars.java](https://github.com/jknack/handlebars.java).

## exports

* ```Handlebars```
* [ViewEngine]({{defdocs}}/View.Engine.html)

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-hbs</artifactId>
  <version>{{version}}</version>
</dependency>
```

## usage

```java
{
  use(new Hbs());

  get("/", req -> Results.html("index").put("model", new MyModel());
}
```

public/index.html:

```
{{model}}
```

Templates are loaded from the root of the classpath: ```/``` and must end with: ```.html``` file extension.

## request locals

A template engine has access to ```request locals``` (a.k.a attributes). Here is an example:

```java
{
  use(new Hbs());

  get("*", req -> {
    req.set("foo", bar);
  });
}
```

Then from template:

```
{{foo}}
```

## helpers

Simple/basic helpers:

```java
{
  use(new Hbs().doWith(hbs -> {
    hbs.registerHelper("myhelper", (ctx, options) -> {
      return ...;
    });
    hbs.registerHelpers(Helpers.class);
  });
}
```

Now, if the helper depends on a service and require injection:

```java
{
  use(new Hbs().with(HelperSource.class));
}
```

The ```HelperSource``` class will be injected by [Guice](https://github.com/google/guice).

## template loader

Templates are loaded from the root of classpath and must end with ```.html```. You can change the default template location and extensions too:

```java
{
  use(new Hbs("/", ".hbs"));
}
```

## cache

Cache is OFF when ```env=dev``` (useful for template reloading), otherwise is ON.

The cache is backed by [Guava](https://github.com/google/guava) and the default cache will expire after ```100``` entries.

If ```100``` entries is not enough or you need a more advanced cache setting, just set the
```hbs.cache``` option:

```properties
hbs.cache = "expireAfterWrite=1h"
```

See [CacheBuilderSpec](http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/cache/CacheBuilderSpec.html) for more detailed expressions.

{{appendix}}
