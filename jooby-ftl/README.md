# freemarker

[Freemarker](http://freemarker.org) templates for [Jooby](/). Exposes a Configuration and [renderer](/apidocs/Renderer.html).

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-ftl</artifactId>
  <version>0.15.0</version>
</dependency>
```

## usage
It is pretty straightforward:

```java
{
  use(new Ftl());

  get("/", req -> Results.html("index").put("model", new MyModel());
}
```

public/index.html:

```java
${model}
```

Templates are loaded from root of classpath: ```/``` and must end with: ```.html```
file extension.

## req locals

A template engine has access to ```request locals``` (a.k.a attributes). Here is an example:

```java
{
  use(new Ftl());

  get("*", req -> {
    req.set("req", req);
    req.set("session", req.session());
  });
}
```

By default, there is no access to ```req``` or ```session``` from your template. This example shows how to do it.


## configuration
There are two ways of changing a Freemarker configuration:

### application.conf
Just add a ```freemarker.*``` option to your ```application.conf``` file:

```properties
freemarker.default_encoding = UTF-8
```

### programmatically

```java
{
  use(new Ftl().doWith((freemarker, config) -> {
    freemarker.setDefaultEncoding("UTF-8");
  });
}
```

Keep in mind this is just an example and you don't need to set the default encoding. Default
encoding is set to: ```application.charset``` which is ```UTF-8``` by default.

## template loader
Templates are loaded from the root of classpath and must end with ```.html```. You can
change the default template location and extensions too:

```java
{
  use(new Ftl("/", ".ftl"));
}
```

## cache

Cache is OFF when ```env=dev``` (useful for template reloading), otherwise is ON.

Cache is backed by [Guava](https://github.com/google/guava) and default cache will expire after ```100``` entries.

If ```100``` entries is not enough or you need a more advanced cache setting, just set the
```freemarker.cache``` option:

```properties
freemarker.cache = "expireAfterWrite=1h"
```

See [CacheBuilderSpec](http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/cache/CacheBuilderSpec.html) for more detailed expressions.

That's all folks! Enjoy it!!!

## freemarker.conf

```properties
#freemarker defaults

freemarker.locale = ${application.lang}

freemarker.number_format = ${application.numberFormat}

freemarker.date_format = ${application.dateFormat}

freemarker.time_zone = ${application.tz}

freemarker.object_wrapper = default

freemarker.template_exception_handler = default

freemarker.defaultEncoding = ${application.charset}

# cache for env != dev

freemarker.cache = "maximumSize=100"
```
