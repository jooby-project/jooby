# jooby-hbs

Mustache/Handlebars templates for [Jooby](/). Exposes a [Handlebars](https://github.com/jknack/handlebars.java) and [renderer](/apidocs/org/jooby/Renderer.html).

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-hbs</artifactId>
  <version>0.11.0</version>
</dependency>
```

## usage
It is pretty straightforward:

```java
{
  use(new Hbs());

  get("/", req -> Results.html("index").put("model", new MyModel());
}
```

public/index.html:

```
{{ "{{ model " }}}}
```

Templates are loaded from root of classpath: ```/``` and must end with: ```.html``` file extension.

## req locals

A template engine has access to ```request locals``` (a.k.a attributes). Here is an example:

```java
{
  use(new Hbs());

  get("*", req -> {
    req.set("req", req);
    req.set("session", req.session());
  });
}
```

By default, there is no access to ```req``` or ```session``` from your template. This example shows how to do it.

## helpers

Simple/basic helpers are add it at startup time:

```java
{
  use(new Hbs().doWith((hbs, config) -> {
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
  use(new Hbs().with(Helpers.class));
}
```

The ```Helpers``` will be injected by [Guice](https://github.com/google/guice) and [Handlebars](https://github.com/jknack/handlebars.java) will scan and discover any helper method.

## template loader

Templates are loaded from the root of classpath and must end with ```.html```. You can
change the default template location and extensions too:

```java
{
  use(new Hbs("/", ".hbs"));
}
```

## cache

Cache is OFF when ```env=dev``` (useful for template reloading), otherwise is ON.

Cache is backed by [Guava](https://github.com/google/guava) and the default cache will expire after ```100``` entries.

If ```100``` entries is not enough or you need a more advanced cache setting, just set the
```hbs.cache``` option:

```properties
hbs.cache = "expireAfterWrite=1h"
```

See [CacheBuilderSpec](http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/cache/CacheBuilderSpec.html) for more detailed expressions.

That's all folks! Enjoy it!!!
