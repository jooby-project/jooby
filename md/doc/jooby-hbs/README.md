# jooby-hbs

Mustache/Handlebars templates for Jooby. Exposes a [Handlebars](https://github.com/jknack/handlebars.java) and [Body.Formatter].

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-hbs</artifactId>
  <version>{{version}}</version>
</dependency>
```

## usage
It is pretty straightforward:

```java
{
  use(new Hbs());

  get("/", req {@literal ->} View.of("index", "model", new MyModel());
}
```

public/index.html:

```
  {{model}}
```

Templates are loaded from root of classpath: ```/``` and must end with: ```.html``` file extension.

## helpers

Simple/basic helpers are add it at startup time:

```java
{
  use(new Hbs().doWith((hbs, config) {@literal ->} {
    hbs.registerHelper("myhelper", (ctx, options) {@literal ->} {
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

The ```Helpers``` will be injected by Guice and Handlebars will scan and discover any helper method.

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

Cache is backed by Guava and the default cache will expire after ```100``` entries.

If ```100``` entries is not enough or you need a more advanced cache setting, just set the
```hbs.cache``` option:

```
hbs.cache = "expireAfterWrite=1h"
```

See [CacheBuilderSpec] for more detailed expressions.

That's all folks! Enjoy it!!!
