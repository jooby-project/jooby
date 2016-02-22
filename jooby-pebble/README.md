# pebble

<a href="http://www.mitchellbosecke.com/pebble">Pebble</a> a lightweight but rock solid Java templating engine.

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-pebble</artifactId>
 <version>0.15.0</version>
</dependency>
```

## usage

```java
{
  use(new Pebble());

  get("/", req -> Results.html("index").put("model", new MyModel());

  // or Pebble API
  get("/pebble-api", req -> {

    PebbleEngine pebble = req.require(PebbleEngine.class);
    PebbleTemplate template = pebble.getTemplate("template");
    template.evaluate(...);
  });

}
```

Templates are loaded from root of classpath: ```/``` and must end with: ```.html``` file extension.

## template loader

Templates are loaded from the root of classpath and must end with ```.html```. You can change the default template location and extensions too:

```java
{
  use(new Pebble("templates", ".pebble"));

}
```

## template cache

Cache is OFF when ```env=dev``` (useful for template reloading), otherwise is ON.

Cache is backed by Guava and the default cache will expire after ```200``` entries.

If ```200``` entries is not enough or you need a more advanced cache setting, just set the ```pebble.cache``` option:

```
pebble.cache = "expireAfterWrite=1h;maximumSize=200"
```

See ```com.google.common.cache.CacheBuilderSpec```.

## tag cache

It works like template cache, except the cache is controlled by the property: ```pebble.tagCache```

## advanced configuration

Advanced configuration if provided by callback:

```java
{
  use(new Pebble().doWith(pebble -> {

    pebble.extension(...);
    pebble.loader(...);
  }));

}
```

That's all folks! Enjoy it!!!
