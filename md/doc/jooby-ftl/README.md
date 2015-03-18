# jooby-ftl

Freemarker templates for Jooby. Exposes a Configuration and [Body.Formatter].

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-ftl</artifactId>
  <version>{{version}}</version>
</dependency>
```
## usage
It is pretty straightforward:

```java
{
  use(new Ftl());

  get("/", req {@literal ->} View.of("index", "model", new MyModel());
}
```

public/index.html:

```java
  ${model}
```

Templates are loaded from root of classpath: ```/``` and must end with: ```.html```
file extension.

## configuration
There are two ways of changing a Freemarker configuration:

### application.conf
Just add a ```freemarker.*``` option to your ```application.conf``` file:

```
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

Cache is backed by Guava and default cache will expire after ```100``` entries.

If ```100``` entries is not enough or you need a more advanced cache setting, just set the
```freemarker.cache``` option:

```
freemarker.cache = "expireAfterWrite=1h"
```

See {@link CacheBuilderSpec}.

That's all folks! Enjoy it!!!
